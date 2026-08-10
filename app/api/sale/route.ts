import { NextResponse } from "next/server";
import { withState, archiveState } from "@/lib/hp/store";
import { nowBridgeTs } from "@/lib/hp/sync";

export const dynamic = "force-dynamic";

interface StartBody {
  action: "start";
  item: string;
  emoji?: string;
  qtyTotal: number;
  price: number;
}

interface EndBody {
  action: "end";
}

export async function POST(req: Request) {
  const body = (await req.json()) as StartBody | EndBody;
  if (body.action === "start") {
    if (!body.item || !Number.isInteger(body.qtyTotal) || body.qtyTotal < 1) {
      return NextResponse.json(
        { error: "start requires item and qtyTotal >= 1" },
        { status: 400 },
      );
    }
    const sale = await withState(async (s) => {
      await archiveState(s); // previous round's ledger is never lost
      s.sale = {
        id: `sale-${Date.now()}`,
        item: body.item,
        emoji: body.emoji ?? "🍉",
        qtyTotal: body.qtyTotal,
        price: body.price,
        status: "active",
        startedAt: new Date().toISOString(),
        adjust: 0,
      };
      s.claims = [];
      s.currentStop = null;
      s.cursor = nowBridgeTs();
      return s.sale;
    });
    return NextResponse.json({ sale });
  }
  if (body.action === "end") {
    const sale = await withState((s) => {
      if (!s.sale || s.sale.status !== "active") {
        throw new Error("no active sale");
      }
      s.sale.status = "ended";
      s.sale.endedAt = new Date().toISOString();
      return s.sale;
    }).catch((e: unknown) => ({ error: String(e) }));
    if (sale && "error" in sale) {
      return NextResponse.json(sale, { status: 409 });
    }
    return NextResponse.json({ sale });
  }
  return NextResponse.json({ error: "unknown action" }, { status: 400 });
}
