package no.secret24h.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object WhisperApi {
    private val client = OkHttpClient()
    private val json = "application/json".toMediaType()

    private fun baseRequest(path: String): Request.Builder {
        val token = UserSession.accessToken ?: throw IllegalStateException("Not logged in")
        return Request.Builder()
            .url("${Config.SUPABASE_URL}$path")
            .header("apikey", Config.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
    }

    suspend fun getConversations(): List<WhisperConversation> = withContext(Dispatchers.IO) {
        val body = JSONObject().toString().toRequestBody(json)
        val req = baseRequest("/rest/v1/rpc/get_whisper_conversations").post(body).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Error: ${res.code}")
            val arr = JSONArray(res.body!!.string())
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                WhisperConversation(
                    secretId      = o.getString("secret_id"),
                    secretText    = o.optString("secret_text", ""),
                    otherUserId   = o.getString("other_user_id"),
                    lastMessage   = o.optString("last_message", ""),
                    lastMessageAt = o.optString("last_message_at", ""),
                    unreadCount   = o.optInt("unread_count", 0),
                )
            }
        }
    }

    suspend fun getMessages(secretId: String, otherUserId: String): List<Whisper> = withContext(Dispatchers.IO) {
        val myId = UserSession.userId ?: throw IllegalStateException("Not logged in")
        val url = "/rest/v1/whispers?secret_id=eq.$secretId" +
                "&or=(sender_id.eq.$myId,receiver_id.eq.$myId)" +
                "&order=created_at.asc"
        val req = baseRequest(url).get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Error: ${res.code}")
            val arr = JSONArray(res.body!!.string())
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Whisper(
                    id         = o.getString("id"),
                    secretId   = o.getString("secret_id"),
                    senderId   = o.getString("sender_id"),
                    receiverId = o.getString("receiver_id"),
                    text       = o.getString("text"),
                    createdAt  = o.getString("created_at"),
                    readAt     = o.optString("read_at").takeIf { s -> s.isNotEmpty() && s != "null" },
                )
            }
        }
    }

    suspend fun sendMessage(secretId: String, receiverId: String, text: String) = withContext(Dispatchers.IO) {
        val senderId = UserSession.userId ?: throw IllegalStateException("Not logged in")
        val body = JSONObject()
            .put("secret_id", secretId)
            .put("sender_id", senderId)
            .put("receiver_id", receiverId)
            .put("text", text)
            .toString()
            .toRequestBody(json)
        val req = baseRequest("/rest/v1/whispers").post(body).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Send failed: ${res.code}")
        }
    }

    suspend fun markRead(secretId: String, senderId: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("read_at", java.time.Instant.now().toString()).toString().toRequestBody(json)
        val req = baseRequest(
            "/rest/v1/whispers?secret_id=eq.$secretId&sender_id=eq.$senderId&read_at=is.null"
        ).method("PATCH", body).build()
        client.newCall(req).execute().use { /* ignore errors */ }
    }
}
