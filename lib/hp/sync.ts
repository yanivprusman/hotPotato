import { dbquery } from "./bridge";
import type { HpState } from "./store";

// Yaniv's self-chat (notes-to-self / assistant chatter) — never a customer.
const SELF_JID = "972556677260@s.whatsapp.net";

// The bridge STORES timestamps as `2026-08-10 23:06:10+03:00` — space
// separator, local UTC offset. (Its JSON output reformats with a `T`; the
// lexicographic `timestamp > ?` comparison runs on the STORED text, so the
// cursor must use the stored shape.)
export function nowBridgeTs(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const off = -d.getTimezoneOffset();
  const sign = off >= 0 ? "+" : "-";
  const abs = Math.abs(off);
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    ` ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` +
    `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
  );
}

// Bare numbers are usually addresses ("נמצא בלגו 6"), so only explicit
// quantity shapes count; everything else defaults to 1 and the card shows
// the original text for a one-tap correction.
export function guessQty(text: string): number {
  const explicit = text.match(/ל[ ־-]?(\d{1,2})(?!\d)/);
  if (explicit) {
    const n = parseInt(explicit[1], 10);
    if (n >= 1 && n <= 20) return n;
  }
  const counted = text.match(/(\d{1,2})\s*(?:אבטיחים|יחידות|יח['׳])/);
  if (counted) {
    const n = parseInt(counted[1], 10);
    if (n >= 1 && n <= 20) return n;
  }
  if (/שניים|שתיים|זוג/.test(text)) return 2;
  if (/שלושה|שלוש(?!ים)/.test(text)) return 3;
  if (/ארבעה|ארבע(?!ים)/.test(text)) return 4;
  return 1;
}

// Pull new bridge messages since the cursor. Incoming 1:1 texts become
// pending claim cards; an outgoing Waze share marks that chat as the
// current stop. Driven by the phone's GET /api/state polling — no
// background timers (Next dev HMR kills those).
export async function pullFromBridge(state: HpState): Promise<void> {
  if (!state.sale || state.sale.status !== "active") return;
  const rows = await dbquery(
    `SELECT m.chat_jid, m.is_from_me, m.content, m.media_type, m.timestamp, c.name
     FROM messages m LEFT JOIN chats c ON c.jid = m.chat_jid
     WHERE m.timestamp > ? AND m.chat_jid LIKE '%@s.whatsapp.net' AND m.chat_jid != ?
     ORDER BY m.timestamp ASC LIMIT 500`,
    [state.cursor, SELF_JID],
  );
  for (const row of rows) {
    const [chatJid, isFromMe, content, mediaType, ts, name] = row as [
      string,
      boolean | number,
      string,
      string,
      string,
      string | null,
    ];
    state.cursor = ts;
    const phone = "+" + chatJid.replace("@s.whatsapp.net", "");
    const display = name && name.trim() !== "" ? name.trim() : phone;
    if (isFromMe === true || isFromMe === 1) {
      if (content.includes("waze.com/ul")) {
        state.currentStop = { chatJid, name: display, phone, ts };
      }
      continue;
    }
    const text =
      content.trim() !== "" ? content.trim() : mediaType ? `[${mediaType}]` : "";
    if (text === "") continue;
    const pending = state.claims.find(
      (c) => c.chatJid === chatJid && c.status === "pending",
    );
    if (pending) {
      pending.text = `${pending.text}\n${text}`;
      pending.ts = ts;
      continue;
    }
    state.claims.push({
      id: `${chatJid}:${ts}`,
      chatJid,
      name: display,
      phone,
      qty: guessQty(text),
      text,
      ts,
      status: "pending",
    });
  }
}
