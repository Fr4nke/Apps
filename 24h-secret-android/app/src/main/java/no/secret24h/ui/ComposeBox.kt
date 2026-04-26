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
    var mood by remember { mutableStateOf("lettelse") }
    val charCount = text.length
    val canSubmit = text.trim().length >= 5 && !isSending

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SmBorder, RoundedCornerShape(18.dp))
            .background(SmSurface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 280) text = it },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
            textStyle = TextStyle(
                color = SmText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = GeistFamily,
            ),
            cursorBrush = SolidColor(SmAccent),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "Skriv din hemmelighet... ingen ser hvem du er.",
                            color = SmTextFaint,
                            fontSize = 14.sp,
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
                    shape = RoundedCornerShape(100.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) ec.glow.copy(alpha = 0.7f) else SmBorder,
                            RoundedCornerShape(100.dp),
                        )
                        .background(
                            if (selected) ec.bg else Color.Transparent,
                            RoundedCornerShape(100.dp),
                        ),
                ) {
                    Text(
                        text = "${MOOD_EMOJIS[m]} $m",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        color = if (selected) ec.fg else SmTextDim,
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
            Text("🔒 anonym · 24t", fontSize = 11.sp, color = SmTextFaint, fontFamily = GeistFamily)

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$charCount/280",
                    fontSize = 11.sp,
                    color = if (charCount > 260) Color(0xFFFF5F5F) else SmTextFaint,
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
                        containerColor = SmAccent,
                        disabledContainerColor = Color(0xFF2A1008),
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = if (canSubmit) Modifier.glowBehind(SmAccent, radius = 14.dp, cornerRadius = 50.dp, alpha = 0.45f) else Modifier,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (isSending) "Sender..." else "Slipp den fri",
                        fontSize = 12.sp,
                        fontFamily = GeistFamily,
                        color = if (canSubmit) Color.White else SmTextFaint,
                    )
                }
            }
        }
    }
}
