package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.secret24h.data.MOOD_EMOJIS
import no.secret24h.data.MOODS

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeBox(
    isSending: Boolean,
    onSubmit: (text: String, mood: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("annet") }
    val remaining = 280 - text.length
    val canSubmit = text.trim().length >= 5 && !isSending

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glowBehind(color = DuskGlow, radius = 24.dp, alpha = 0.12f)
            .border(1.dp, DuskBorder, RoundedCornerShape(20.dp))
            .background(DuskCardBg, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 280) text = it },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp),
            textStyle = TextStyle(
                color = DuskText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontFamily = GeistFamily,
            ),
            cursorBrush = SolidColor(DuskAccent),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "Del en hemmelighet anonymt... 🤫",
                            color = DuskMuted,
                            fontSize = 15.sp,
                            fontFamily = GeistFamily,
                        )
                    }
                    inner()
                }
            },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MOODS.forEach { m ->
                val selected = m == mood
                val ec = EMOTION_COLORS[m] ?: EMOTION_COLORS["annet"]!!
                Surface(
                    onClick = { mood = m },
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) ec.glow.copy(alpha = 0.6f) else DuskBorder,
                            RoundedCornerShape(50),
                        )
                        .background(
                            if (selected) ec.bg else Color.Transparent,
                            RoundedCornerShape(50),
                        ),
                ) {
                    Text(
                        text = "${MOOD_EMOJIS[m]} $m",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        color = if (selected) ec.fg else DuskMuted,
                        fontFamily = GeistFamily,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔒 anonym · 24t",
                fontSize = 11.sp,
                color = DuskMuted,
                fontFamily = GeistFamily,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$remaining",
                    fontSize = 11.sp,
                    color = if (remaining < 20) Color(0xFFFF6B8A) else DuskMuted,
                    fontFamily = GeistFamily,
                )
                Button(
                    onClick = {
                        if (canSubmit) {
                            onSubmit(text.trim(), mood)
                            text = ""
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DuskGlow,
                        disabledContainerColor = Color(0xFF2A2040),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = if (canSubmit) Modifier.glowBehind(DuskGlow, radius = 16.dp, alpha = 0.4f) else Modifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (isSending) "Sender..." else "Slipp den fri ✦",
                        fontSize = 13.sp,
                        fontFamily = GeistFamily,
                        color = if (canSubmit) Color.White else DuskMuted,
                    )
                }
            }
        }
    }
}
