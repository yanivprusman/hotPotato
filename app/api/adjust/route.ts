import { NextResponse } from "next/server";
import { withState, remaining } from "@/lib/hp/store";

export const dynamic = "force-dynamic";

// delta applies to REMAINING: {"delta": -1} = one unit gone outside
// WhatsApp (in-person handoff); {"delta": 1} undoes it.
export async function POST(req: Request) {
  const body = (await req.json()) as { delta: number };
  if (!Number.isInteger(body.delta) || body.delta === 0) {
    return NextResponse.json({ error: "delta must be a non-zero integer" }, { status: 400 });
  }
  const result = await withState((s) => {
    if (!s.sale || s.sale.status !== "active") return null;
    s.sale.adjust -= body.delta;
    return { remaining: remaining(s) };
  });
  if (!result) {
    return NextResponse.json({ error: "no active sale" }, { status: 409 });
  }
  return NextResponse.json(result);
}
