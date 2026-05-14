package no.secret24h.data

object UserSession {
    var accessToken: String? = null
    var userId: String? = null
    var email: String? = null
    val isLoggedIn get() = accessToken != null

    fun clear() {
        accessToken = null
        userId = null
        email = null
    }
}
