package no.secret24h.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object MySecretsApi {
    private val client = OkHttpClient()
    private val json = "application/json".toMediaType()

    private fun baseRequest(path: String): Request.Builder {
        val token = UserSession.accessToken ?: Config.SUPABASE_ANON_KEY
        return Request.Builder()
            .url("${Config.SUPABASE_URL}$path")
            .header("apikey", Config.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
    }

    private fun JSONObject.toSecret() = Secret(
        id               = getString("id"),
        text             = getString("text"),
        mood             = optString("mood", "annet"),
        expiresAt        = getString("expires_at"),
        reactionMeToo    = optInt("reaction_me_too", 0),
        reactionWild     = optInt("reaction_wild", 0),
        reactionDoubtful = optInt("reaction_doubtful", 0),
        userId           = optString("user_id").takeIf { it.isNotEmpty() },
    )

    suspend fun getMySecrets(): List<Secret> = withContext(Dispatchers.IO) {
        val uid = UserSession.userId ?: throw Exception("Not logged in")
        val req = baseRequest(
            "/rest/v1/secrets?select=id,text,mood,expires_at,reaction_me_too,reaction_wild,reaction_doubtful,user_id" +
            "&user_id=eq.$uid&order=created_at.desc"
        ).get().build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Error: ${res.code}")
            val arr = JSONArray(res.body!!.string())
            List(arr.length()) { arr.getJSONObject(it).toSecret() }
        }
    }

    suspend fun republishSecret(secretId: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("p_secret_id", secretId).toString().toRequestBody(json)
        val req = baseRequest("/rest/v1/rpc/republish_secret").post(body).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Republish failed: ${res.code}")
        }
    }
}
