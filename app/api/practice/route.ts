import { NextResponse } from "next/server";
import { withState, archiveState } from "@/lib/hp/store";
import { nowBridgeTs } from "@/lib/hp/sync";

export const dynamic = "force-dynamic";

// Practice round: 3 units, two pre-seeded fake claims (2 + 1), fake current
// stop — confirming both reaches zero and fires the sold-out alarm. No
// WhatsApp involved; the fake contacts dial the modem's bot SIM, so the 📞
// button can be tested without ringing a real person.
const PRACTICE_PHONE = "+972559448186";

export async function POST() {
  const sale = await withState(async (s) => {
    await archiveState(s);
    const now = new Date().toISOString();
    s.sale = {
      id: `practice-${Date.now()}`,
      item: "אבטיח (תרגול)",
      emoji: "🍉",
      qtyTotal: 3,
      price: 10,
      status: "active",
      startedAt: now,
      adjust: 0,
    };
    s.claims = [
      {
        id: `practice-a-${Date.now()}`,
        chatJid: "practice-a@test",
        name: "🧪 לקוח תרגול א׳",
        phone: PRACTICE_PHONE,
        qty: 2,
        text: "היי, אשמח ל 2 אבטיחים 🙏",
        ts: now,
        status: "pending",
      },
      {
        id: `practice-b-${Date.now()}`,
        chatJid: "practice-b@test",
        name: "🧪 לקוח תרגול ב׳",
        phone: PRACTICE_PHONE,
        qty: 1,
        text: "אפשר אחד? נמצא בלגו 6",
        ts: now,
        status: "pending",
      },
    ];
    s.currentStop = {
      chatJid: "practice-a@test",
      name: "🧪 לקוח תרגול א׳",
      phone: PRACTICE_PHONE,
      ts: now,
    };
    s.cursor = nowBridgeTs();
    return s.sale;
  });
  return NextResponse.json({ sale });
}
