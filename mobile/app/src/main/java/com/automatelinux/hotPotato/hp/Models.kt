package com.automatelinux.hotPotato.hp

import org.json.JSONObject

data class Sale(
    val id: String,
    val item: String,
    val emoji: String,
    val qtyTotal: Int,
    val price: Int,
    val status: String,
)

data class Claim(
    val id: String,
    val chatJid: String,
    val name: String,
    val phone: String,
    val qty: Int,
    val text: String,
    val status: String,
    val paid: Boolean?,
)

data class CurrentStop(
    val chatJid: String,
    val name: String,
    val phone: String,
)

data class HpState(
    val sale: Sale?,
    val remaining: Int,
    val claims: List<Claim>,
    val currentStop: CurrentStop?,
) {
    val pending: List<Claim> get() = claims.filter { it.status == "pending" }
    val confirmed: List<Claim> get() = claims.filter { it.status == "confirmed" }
    val delivered: List<Claim> get() = claims.filter { it.status == "delivered" }
}

fun parseState(json: JSONObject): HpState {
    val saleJson = json.optJSONObject("sale")
    val sale = saleJson?.let {
        Sale(
            id = it.getString("id"),
            item = it.getString("item"),
            emoji = it.optString("emoji", "🍉"),
            qtyTotal = it.getInt("qtyTotal"),
            price = it.getInt("price"),
            status = it.getString("status"),
        )
    }
    val claimsArr = json.getJSONArray("claims")
    val claims = buildList {
        for (i in 0 until claimsArr.length()) {
            val c = claimsArr.getJSONObject(i)
            add(
                Claim(
                    id = c.getString("id"),
                    chatJid = c.getString("chatJid"),
                    name = c.getString("name"),
                    phone = c.getString("phone"),
                    qty = c.getInt("qty"),
                    text = c.getString("text"),
                    status = c.getString("status"),
                    paid = if (c.has("paid")) c.getBoolean("paid") else null,
                ),
            )
        }
    }
    val stopJson = json.optJSONObject("currentStop")
    val stop = stopJson?.let {
        CurrentStop(
            chatJid = it.getString("chatJid"),
            name = it.getString("name"),
            phone = it.getString("phone"),
        )
    }
    return HpState(
        sale = sale,
        remaining = json.getInt("remaining"),
        claims = claims,
        currentStop = stop,
    )
}
