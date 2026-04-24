package no.secret24h.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import no.secret24h.data.MOOD_EMOJIS
import no.secret24h.data.Secret
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
                timeLeft = if (diff <= 0) "Utløpt" else {
                    val h = diff / 3600
                    val m = (diff % 3600) / 60
                    val s = diff % 60
                    "${h}t ${m}m ${s}s"
                }
            } catch (_: Exception) {
                timeLeft = "?"
            }
            delay(1000)
        }
    }

    Text("⏳ $timeLeft", fontSize = 11.sp, color = DuskMuted, fontFamily = GeistFamily)
}

@Composable
fun SecretCard(
    secret: Secret,
    rank: Int? = null,
    reactedMeToo: Boolean,
    reactedHeart: Boolean,
    onReact: (String) -> Unit,
) {
    val rankColors = mapOf(1 to Color(0xFFFFD166), 2 to Color(0xFFB0B0C8), 3 to Color(0xFFCD8847))
    val emotion = EMOTION_COLORS[secret.mood] ?: EMOTION_COLORS["annet"]!!

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (rank != null) 8.dp else 0.dp)
                .glowBehind(color = emotion.glow, radius = 18.dp, alpha = 0.18f)
                .border(1.dp, DuskBorder, RoundedCornerShape(16.dp))
                .background(DuskCardBg, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = secret.text,
                    color = DuskText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontFamily = GeistFamily,
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
                                .clip(RoundedCornerShape(50))
                                .background(emotion.bg)
                                .border(1.dp, emotion.glow.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "$emoji ${secret.mood}",
                                fontSize = 11.sp,
                                color = emotion.fg,
                                fontFamily = GeistFamily,
                            )
                        }
                        Countdown(secret.expiresAt)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReactionButton(
                            emoji = "🙋",
                            label = "meg også",
                            count = secret.reactionMeToo,
                            active = reactedMeToo,
                            activeColor = Color(0xFF2D1A66),
                            activeBorder = DuskAccent,
                        ) { onReact("me_too") }

                        ReactionButton(
                            emoji = "❤️",
                            label = "",
                            count = secret.reactionHeart,
                            active = reactedHeart,
                            activeColor = Color(0xFF3D0A1A),
                            activeBorder = Color(0xFFFF6B8A),
                        ) { onReact("heart") }
                    }
                }
            }
        }

        if (rank != null) {
            val badgeColor = rankColors[rank] ?: DuskMuted
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = 0.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$rank",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0C0A1A),
                    fontFamily = GeistFamily,
                )
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
    activeBorder: Color,
    onClick: () -> Unit,
) {
    val bg = if (active) activeColor else Color(0x1AFFFFFF)
    val fg = if (active) Color.White else DuskMuted
    val border = if (active) activeBorder.copy(alpha = 0.5f) else DuskBorder

    Surface(
        onClick = onClick,
        enabled = !active,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .height(30.dp)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .background(bg, RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(emoji, fontSize = 12.sp)
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg, fontFamily = GeistFamily)
            if (label.isNotEmpty()) Text(label, fontSize = 10.sp, color = fg, fontFamily = GeistFamily)
        }
    }
}
