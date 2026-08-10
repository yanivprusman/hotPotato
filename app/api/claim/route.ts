import { NextResponse } from "next/server";
import { withState, remaining } from "@/lib/hp/store";

export const dynamic = "force-dynamic";

interface Body {
  id: string;
  action: "confirm" | "reject" | "delivered" | "reopen";
  qty?: number;
  paid?: boolean;
  note?: string;
}

export async function POST(req: Request) {
  const body = (await req.json()) as Body;
  const result = await withState((s) => {
    const claim = s.claims.find((c) => c.id === body.id);
    if (!claim) return null;
    switch (body.action) {
      case "confirm":
        claim.status = "confirmed";
        if (Number.isInteger(body.qty) && body.qty! >= 1) claim.qty = body.qty!;
        break;
      case "reject":
        claim.status = "rejected";
        break;
      case "delivered":
        claim.status = "delivered";
        if (typeof body.paid === "boolean") claim.paid = body.paid;
        break;
      case "reopen":
        claim.status = "pending";
        break;
    }
    if (typeof body.note === "string") claim.note = body.note;
    return { claim, remaining: remaining(s) };
  });
  if (!result) {
    return NextResponse.json({ error: `no claim ${body.id}` }, { status: 404 });
  }
  return NextResponse.json(result);
}
