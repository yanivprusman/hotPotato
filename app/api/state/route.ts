import { NextResponse } from "next/server";
import { withState, remaining } from "@/lib/hp/store";
import { pullFromBridge } from "@/lib/hp/sync";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const payload = await withState(async (s) => {
      await pullFromBridge(s);
      return {
        sale: s.sale,
        remaining: remaining(s),
        claims: s.claims,
        currentStop: s.currentStop,
        updatedAt: new Date().toISOString(),
      };
    });
    return NextResponse.json(payload);
  } catch (e) {
    return NextResponse.json({ error: String(e) }, { status: 502 });
  }
}
