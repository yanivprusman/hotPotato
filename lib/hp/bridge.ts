import { promises as fs } from "fs";
import path from "path";

// The one linked WhatsApp bridge lives on the NUC (leader). All reads go
// through its authenticated /api/dbquery endpoint — never a second whatsmeow
// session (one account allows exactly one linked bridge client).
interface BridgeConfig {
  api_url: string;
  token: string;
}

let cached: BridgeConfig | null = null;

async function config(): Promise<BridgeConfig> {
  if (cached) return cached;
  const file = path.join(process.cwd(), "data", "bridge.json");
  const cfg = JSON.parse(await fs.readFile(file, "utf8")) as BridgeConfig;
  if (!cfg.api_url || !cfg.token) {
    throw new Error(`bridge config incomplete: ${file} must contain api_url and token`);
  }
  cached = cfg;
  return cfg;
}

export async function dbquery(
  sql: string,
  params: (string | number)[] = [],
): Promise<unknown[][]> {
  const cfg = await config();
  const res = await fetch(`${cfg.api_url}/dbquery`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${cfg.token}`,
    },
    body: JSON.stringify({ database: "messages", sql, params }),
    signal: AbortSignal.timeout(10_000),
  });
  if (!res.ok) {
    throw new Error(`bridge dbquery HTTP ${res.status}: ${await res.text()}`);
  }
  const json = (await res.json()) as {
    success: boolean;
    message?: string;
    rows?: unknown[][];
  };
  if (!json.success) {
    throw new Error(`bridge dbquery failed: ${json.message ?? "unknown error"}`);
  }
  return json.rows ?? [];
}
