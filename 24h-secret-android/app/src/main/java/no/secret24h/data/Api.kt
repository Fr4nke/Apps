package no.secret24h.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object Api {
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

    suspend fun getSecrets(
        sort: Sort,
        moodFilter: String? = null,
        reactionSort: String? = null,
    ): List<Secret> = withContext(Dispatchers.IO) {
        val order = when {
            sort == Sort.Recent -> "created_at.desc"
            reactionSort == "me_too" -> "reaction_me_too.desc"
            reactionSort == "wild" -> "reaction_wild.desc"
            reactionSort == "doubtful" -> "reaction_doubtful.desc"
            else -> "reaction_me_too.desc"
        }
        val now = Instant.now().toString()
        var url = "/rest/v1/secrets?select=id,text,mood,expires_at,reaction_me_too,reaction_wild,reaction_doubtful,user_id" +
                "&expires_at=gt.$now&order=$order&limit=50"
        if (moodFilter != null) url += "&mood=eq.$moodFilter"

        val req = baseRequest(url).get().build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Error: ${res.code}")
            val arr = JSONArray(res.body!!.string())
            List(arr.length()) { arr.getJSONObject(it).toSecret() }
        }
    }

    suspend fun postSecret(text: String, mood: String): Secret = withContext(Dispatchers.IO) {
        val obj = JSONObject().put("text", text).put("mood", mood)
        UserSession.userId?.let { obj.put("user_id", it) }
        val body = obj.toString().toRequestBody(json)
        val req = baseRequest("/rest/v1/secrets")
            .header("Prefer", "return=representation")
            .post(body)
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Could not save: ${res.code}")
            JSONArray(res.body!!.string()).getJSONObject(0).toSecret()
        }
    }

    suspend fun react(secretId: String, colName: String) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("secret_id", secretId)
            .put("col_name", colName)
            .toString()
            .toRequestBody(json)
        val req = baseRequest("/rest/v1/rpc/increment_reaction").post(body).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Reaction failed: ${res.code}")
        }
    }
}
