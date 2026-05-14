package no.secret24h.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AuthApi {
    private val client = OkHttpClient()
    private val json = "application/json".toMediaType()

    suspend fun signInWithGoogle(idToken: String): UserInfo = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
            .toString()
            .toRequestBody(json)

        val req = Request.Builder()
            .url("${Config.SUPABASE_URL}/auth/v1/token?grant_type=id_token")
            .header("apikey", Config.SUPABASE_ANON_KEY)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw Exception("Auth failed: ${res.code}")
            val j = JSONObject(res.body!!.string())
            val user = j.getJSONObject("user")
            UserInfo(
                accessToken = j.getString("access_token"),
                userId = user.getString("id"),
                email = user.optString("email", ""),
            )
        }
    }
}

data class UserInfo(val accessToken: String, val userId: String, val email: String)
