package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.secret24h.data.SecretsViewModel
import no.secret24h.data.Sort

@Composable
fun MainScreen(vm: SecretsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var isSending by remember { mutableStateOf(false) }
    // Map: secretId → set of reaction types already used
    val reactedIds = remember { mutableStateMapOf<String, Set<String>>() }

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontFamily = InstrumentSerif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 26.sp,
                                        color = SmText,
                                    )
                                ) { append("24h ") }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = InstrumentSerif,
                                        fontSize = 26.sp,
                                        color = SmAccent,
                                    )
                                ) { append("Secret") }
                            }
                        )
                        Text(
                            "Anonymt · ingen konto · forsvinner etter 24 timer",
                            fontSize = 12.sp,
                            color = SmTextDim,
                            fontFamily = GeistFamily,
                        )
                    }
                }

                // Compose box
                item {
                    ComposeBox(isSending = isSending) { text, mood ->
                        isSending = true
                        vm.postSecret(text, mood) { isSending = false }
                    }
                }

                // Sort toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SortTab("Siste", state.sort == Sort.Recent) { vm.setSort(Sort.Recent) }
                            SortTab("Topp 24t +", state.sort == Sort.Top) { vm.setSort(Sort.Top) }
                        }
                        Text(
                            "${state.secrets.size} hemmeligheter",
                            fontSize = 11.sp,
                            color = SmTextFaint,
                            fontFamily = GeistFamily,
                        )
                    }
                }

                // Error
                state.error?.let { err ->
                    item {
                        Text(err, color = Color(0xFFFF5F5F), fontSize = 13.sp, fontFamily = GeistFamily)
                    }
                }

                // Loading
                if (state.isLoading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = SmAccent,
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }

                // Empty state
                if (!state.isLoading && state.secrets.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("🤫", fontSize = 40.sp)
                            Text(
                                "Ingen hemmeligheter ennå.",
                                color = SmTextDim,
                                fontSize = 14.sp,
                                fontFamily = GeistFamily,
                            )
                        }
                    }
                }

                // Feed
                items(state.secrets, key = { it.id }) { secret ->
                    val reacted = reactedIds[secret.id] ?: emptySet()
                    val idx = state.secrets.indexOf(secret)
                    SecretCard(
                        secret          = secret,
                        rank            = if (state.sort == Sort.Top && idx < 3) idx + 1 else null,
                        reactedMeToo    = "me_too"   in reacted,
                        reactedWild     = "wild"     in reacted,
                        reactedDoubtful = "doubtful" in reacted,
                        onReact = { type ->
                            if (type !in reacted) {
                                reactedIds[secret.id] = reacted + type
                                vm.react(secret.id, type)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SortTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = Color.Transparent,
        modifier = Modifier
            .border(
                1.dp,
                if (selected) SmAccent.copy(alpha = 0.6f) else SmBorder,
                RoundedCornerShape(100.dp),
            )
            .background(
                if (selected) SmAccent else Color.Transparent,
                RoundedCornerShape(100.dp),
            ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else SmTextDim,
            fontFamily = GeistFamily,
        )
    }
}
