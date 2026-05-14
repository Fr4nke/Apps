package no.secret24h.data

data class Secret(
    val id: String,
    val text: String,
    val mood: String,
    val expiresAt: String,
    val reactionMeToo: Int,
    val reactionWild: Int,
    val reactionDoubtful: Int,
    val userId: String? = null,
)

enum class Sort { Recent, Top }

val MOODS = listOf("lettelse", "skam", "stolthet", "anger", "lengsel", "sinne", "frykt", "glede", "annet")

val MOOD_EMOJIS = mapOf(
    "lettelse" to "😮‍💨",
    "skam"     to "😳",
    "stolthet" to "💪",
    "anger"    to "😔",
    "lengsel"  to "🌙",
    "sinne"    to "😤",
    "frykt"    to "😨",
    "glede"    to "✨",
    "annet"    to "💭",
)
