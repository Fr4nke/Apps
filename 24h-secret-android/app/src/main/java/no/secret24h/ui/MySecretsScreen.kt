package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import no.secret24h.data.MySecretsApi
import no.secret24h.data.MOOD_EMOJIS
import no.secret24h.data.Secret
import java.time.Instant
import java.time.OffsetDateTime

@Composable
fun MySecretsScreen(onBack: () -> Unit) {
    var secrets by remember { mutableStateOf<List<Secret>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                secrets = MySecretsApi.getMySecrets()
            } catch (e: Exception) {
                error = e.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF3A0F15),
                        0.35f to Color(0xFF120608),
                        1.0f to Color(0xFF07030A),
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmTextDim)
                    }
                    Text(
                        "My Secrets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = SmText,
                        fontFamily = GeistFamily,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                error?.let {
                    Text(
                        it,
                        color = Color(0xFFFF5F5F),
                        fontSize = 13.sp,
                        fontFamily = GeistFamily,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SmAccent, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                } else if (secrets.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No secrets yet.", color = SmTextDim, fontSize = 14.sp, fontFamily = GeistFamily)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(secrets, key = { it.id }) { secret ->
                            MySecretCard(
                                secret = secret,
                                onRepublish = {
                                    scope.launch {
                                        try {
                                            MySecretsApi.republishSecret(secret.id)
                                            load()
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MySecretCard(secret: Secret, onRepublish: () -> Unit) {
    val isExpired = try {
        OffsetDateTime.parse(secret.expiresAt).toInstant().isBefore(Instant.now())
    } catch (_: Exception) { false }

    val emotion = EMOTION_COLORS[secret.mood] ?: EMOTION_COLORS["annet"]!!

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glowBehind(color = emotion.glow, radius = 16.dp, alpha = 0.10f)
            .border(
                1.dp,
                if (isExpired) SmBorder.copy(alpha = 0.4f) else SmBorder,
                RoundedCornerShape(16.dp),
            )
            .background(SmSurface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = secret.text,
                color = if (isExpired) SmTextDim else SmText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontFamily = InstrumentSerif,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val emoji = MOOD_EMOJIS[secret.mood] ?: "💭"
                    Text("$emoji ${secret.mood}", fontSize = 11.sp, color = emotion.fg)

                    if (isExpired) {
                        Text("Expired", fontSize = 11.sp, color = Color(0xFFFF5F5F))
                    } else {
                        Countdown(secret.expiresAt)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🙋 ${secret.reactionMeToo}", fontSize = 11.sp, color = SmTextFaint)
                    Text("🤯 ${secret.reactionWild}", fontSize = 11.sp, color = SmTextFaint)
                    Text("🤨 ${secret.reactionDoubtful}", fontSize = 11.sp, color = SmTextFaint)
                }
            }

            if (isExpired) {
                Button(
                    onClick = onRepublish,
                    colors = ButtonDefaults.buttonColors(containerColor = SmAccent),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Republish", fontSize = 12.sp, color = Color.White, fontFamily = GeistFamily)
                }
            }
        }
    }
}
