package no.secret24h.data

data class Whisper(
    val id: String,
    val secretId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val createdAt: String,
    val readAt: String?,
)

data class WhisperConversation(
    val secretId: String,
    val secretText: String,
    val otherUserId: String,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int,
)
