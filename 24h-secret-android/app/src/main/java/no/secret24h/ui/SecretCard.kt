package no.secret24h.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import no.secret24h.data.MOOD_EMOJIS
import no.secret24h.data.Secret
import no.secret24h.data.UserSession
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun Countdown(expiresAt: String) {
    var timeLeft by remember { mutableStateOf("") }

    LaunchedEffect(expiresAt) {
        while (true) {
            try {
                val expiry = OffsetDateTime.parse(expiresAt).toInstant()
                val diff = ChronoUnit.SECONDS.between(Instant.now(), expiry)
                timeLeft = if (diff <= 0) "Expired" else {
                    val h = diff / 3600
                    val m = (diff % 3600) / 60
                    val s = diff % 60
                    "${h}h ${m}m ${s}s"
                }
            } catch (_: Exception) {
                timeLeft = "?"
            }
            delay(1000)
        }
    }

    Text("⏳ $timeLeft", fontSize = 11.sp, color = SmTextFaint)
}

@Composable
fun SecretCard(
    secret: Secret,
    rank: Int? = null,
    reactedMeToo: Boolean,
    reactedWild: Boolean,
    reactedDoubtful: Boolean,
    onReact: (String) -> Unit,
    onWhisper: ((secretId: String, receiverId: String) -> Unit)? = null,
) {
    val rankColors = mapOf(1 to Color(0xFFFFAD45), 2 to Color(0xFFB0A8A0), 3 to Color(0xFFCD8847))
    val emotion = EMOTION_COLORS[secret.mood] ?: EMOTION_COLORS["annet"]!!
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Show whisper button if secret has a userId that is not the current user
    val showWhisper = secret.userId != null && secret.userId != UserSession.userId

    val onShare = {
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    ShareImageGenerator.generate(context, secret)
                }
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file,
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
            } catch (_: Exception) {
                // Fallback to plain text if image generation fails
                val emoji = MOOD_EMOJIS[secret.mood] ?: "🤫"
                val text = "🤫 \"${secret.text}\"\n\n$emoji ${secret.mood} · disappears in 24h\n\nShare your own secret anonymously 👇\nhttps://24h-secret.no"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
        Unit
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (rank != null) 8.dp else 0.dp)
                .glowBehind(color = emotion.glow, radius = 16.dp, alpha = 0.15f)
                .border(1.dp, SmBorder, RoundedCornerShape(16.dp))
                .background(SmSurface, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = secret.text,
                    color = SmText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = InstrumentSerif,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val emoji = MOOD_EMOJIS[secret.mood] ?: "💭"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(emotion.bg)
                                .border(1.dp, emotion.glow.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("$emoji ${secret.mood}", fontSize = 11.sp, color = emotion.fg)
                        }
                        Countdown(secret.expiresAt)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReactionButton("🙋", "me too",   secret.reactionMeToo,    reactedMeToo,    SmAccent)                { onReact("me_too") }
                        ReactionButton("🤯", "wild",     secret.reactionWild,     reactedWild,     Color(0xFFFF6ADB)) { onReact("wild") }
                        ReactionButton("🤨", "doubtful", secret.reactionDoubtful, reactedDoubtful, Color(0xFF42F0D4)) { onReact("doubtful") }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (showWhisper && onWhisper != null) {
                            Surface(
                                onClick = { onWhisper(secret.id, secret.userId!!) },
                                shape = RoundedCornerShape(100.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .height(28.dp)
                                    .border(1.dp, SmBorder, RoundedCornerShape(100.dp))
                                    .background(Color(0x14FFFFFF), RoundedCornerShape(100.dp)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text("💬", fontSize = 12.sp)
                                    Text("Whisper", fontSize = 10.sp, color = SmTextDim)
                                }
                            }
                        }

                        Surface(
                            onClick = onShare,
                            shape = RoundedCornerShape(100.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .height(28.dp)
                                .border(1.dp, SmBorder, RoundedCornerShape(100.dp))
                                .background(Color(0x14FFFFFF), RoundedCornerShape(100.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("↗", fontSize = 12.sp, color = SmTextDim)
                                Text("Share", fontSize = 10.sp, color = SmTextDim)
                            }
                        }
                    }
                }
            }
        }

        if (rank != null) {
            val badgeColor = rankColors[rank] ?: SmTextDim
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = 0.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("#$rank", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF120608))
            }
        }
    }
}

@Composable
fun ReactionButton(
    emoji: String,
    label: String,
    count: Int,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (active) activeColor.copy(alpha = 0.15f) else Color(0x14FFFFFF)
    val fg = if (active) activeColor else SmTextDim
    val border = if (active) activeColor.copy(alpha = 0.4f) else SmBorder

    Surface(
        onClick = onClick,
        enabled = !active,
        shape = RoundedCornerShape(100.dp),
        color = Color.Transparent,
        modifier = Modifier
            .height(22.dp)
            .border(1.dp, border, RoundedCornerShape(100.dp))
            .background(bg, RoundedCornerShape(100.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(emoji, fontSize = 10.sp)
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = fg)
        }
    }
}
