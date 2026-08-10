import { NextResponse } from "next/server";
import { withState } from "@/lib/hp/store";

export const dynamic = "force-dynamic";

// Manual override: mark a claim's chat as the current stop (the automatic
// path is an outgoing Waze share detected by sync.ts).
export async function POST(req: Request) {
  const body = (await req.json()) as { chatJid: string };
  if (!body.chatJid) {
    return NextResponse.json({ error: "chatJid required" }, { status: 400 });
  }
  const stop = await withState((s) => {
    const claim = s.claims.find((c) => c.chatJid === body.chatJid);
    if (!claim) return null;
    s.currentStop = {
      chatJid: claim.chatJid,
      name: claim.name,
      phone: claim.phone,
      ts: new Date().toISOString(),
    };
    return s.currentStop;
  });
  if (!stop) {
    return NextResponse.json({ error: `no claim for ${body.chatJid}` }, { status: 404 });
  }
  return NextResponse.json({ currentStop: stop });
}
