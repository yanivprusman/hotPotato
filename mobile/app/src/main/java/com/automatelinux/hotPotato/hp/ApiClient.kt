package com.automatelinux.hotPotato.hp

import com.automatelinux.hotPotato.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiException(message: String) : Exception(message)

object ApiClient {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun request(method: String, path: String, body: JSONObject?): JSONObject {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8_000
            conn.readTimeout = 25_000
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(text).optString("error") }
                    .getOrNull().takeUnless { it.isNullOrBlank() } ?: "HTTP $code"
                throw ApiException(msg)
            }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun getState(): HpState = withContext(Dispatchers.IO) {
        parseState(request("GET", "/api/state", null))
    }

    suspend fun startSale(item: String, emoji: String, qtyTotal: Int, price: Int) {
        withContext(Dispatchers.IO) {
            request(
                "POST", "/api/sale",
                JSONObject()
                    .put("action", "start")
                    .put("item", item)
                    .put("emoji", emoji)
                    .put("qtyTotal", qtyTotal)
                    .put("price", price),
            )
        }
    }

    suspend fun endSale() = withContext(Dispatchers.IO) {
        request("POST", "/api/sale", JSONObject().put("action", "end"))
        Unit
    }

    suspend fun actOnClaim(id: String, action: String, qty: Int? = null, paid: Boolean? = null) {
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("id", id).put("action", action)
            if (qty != null) body.put("qty", qty)
            if (paid != null) body.put("paid", paid)
            request("POST", "/api/claim", body)
        }
    }

    suspend fun adjust(delta: Int) = withContext(Dispatchers.IO) {
        request("POST", "/api/adjust", JSONObject().put("delta", delta))
        Unit
    }

    suspend fun setCurrentStop(chatJid: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/current-stop", JSONObject().put("chatJid", chatJid))
        Unit
    }
}
