import { promises as fs } from "fs";
import path from "path";

const DATA_DIR = path.join(process.cwd(), "data");
const STATE_FILE = path.join(DATA_DIR, "state.json");
const HISTORY_DIR = path.join(DATA_DIR, "history");

export type SaleStatus = "active" | "ended";
export type ClaimStatus = "pending" | "confirmed" | "rejected" | "delivered";

export interface Sale {
  id: string;
  item: string;
  emoji: string;
  qtyTotal: number;
  price: number;
  status: SaleStatus;
  startedAt: string;
  endedAt?: string;
  // Manual consumption outside WhatsApp (in-person handoffs, kept-at-home).
  // remaining = qtyTotal - adjust - confirmed/delivered claim qty.
  adjust: number;
}

export interface Claim {
  id: string;
  chatJid: string;
  name: string;
  phone: string;
  qty: number;
  text: string;
  ts: string;
  status: ClaimStatus;
  paid?: boolean;
  note?: string;
}

export interface CurrentStop {
  chatJid: string;
  name: string;
  phone: string;
  ts: string;
}

export interface HpState {
  sale: Sale | null;
  claims: Claim[];
  currentStop: CurrentStop | null;
  // Bridge message timestamp high-water mark (RFC3339 with local offset,
  // same format the bridge writes, compared lexicographically in SQL).
  cursor: string;
}

const EMPTY: HpState = { sale: null, claims: [], currentStop: null, cursor: "" };

// All mutations run through one promise chain so concurrent API calls
// never interleave read-modify-write on the state file.
let queue: Promise<unknown> = Promise.resolve();

async function readState(): Promise<HpState> {
  try {
    return JSON.parse(await fs.readFile(STATE_FILE, "utf8")) as HpState;
  } catch (e) {
    if ((e as NodeJS.ErrnoException).code === "ENOENT") return structuredClone(EMPTY);
    throw e;
  }
}

async function writeState(state: HpState): Promise<void> {
  await fs.mkdir(DATA_DIR, { recursive: true });
  const tmp = STATE_FILE + ".tmp";
  await fs.writeFile(tmp, JSON.stringify(state, null, 2));
  await fs.rename(tmp, STATE_FILE);
}

export function withState<T>(fn: (s: HpState) => Promise<T> | T): Promise<T> {
  const run = queue.then(async () => {
    const state = await readState();
    const result = await fn(state);
    await writeState(state);
    return result;
  });
  queue = run.catch(() => {});
  return run;
}

export async function archiveState(state: HpState): Promise<void> {
  if (!state.sale) return;
  await fs.mkdir(HISTORY_DIR, { recursive: true });
  const file = path.join(HISTORY_DIR, `${state.sale.id}.json`);
  await fs.writeFile(file, JSON.stringify(state, null, 2));
}

export function remaining(state: HpState): number {
  if (!state.sale) return 0;
  const claimed = state.claims
    .filter((c) => c.status === "confirmed" || c.status === "delivered")
    .reduce((sum, c) => sum + c.qty, 0);
  return state.sale.qtyTotal - state.sale.adjust - claimed;
}
