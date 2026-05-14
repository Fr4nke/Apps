package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation.NavController
import no.secret24h.data.MOODS
import no.secret24h.data.SecretsViewModel
import no.secret24h.data.Sort
import no.secret24h.data.UserSession

@Composable
fun MainScreen(
    vm: SecretsViewModel = viewModel(),
    navController: NavController,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var isSending by remember { mutableStateOf(false) }
    val reactedIds = remember { mutableStateMapOf<String, Set<String>>() }
    var isLoggedIn by remember { mutableStateOf(UserSession.isLoggedIn) }

    // Refresh login state when screen is recomposed after coming back from auth
    LaunchedEffect(Unit) {
        isLoggedIn = UserSession.isLoggedIn
    }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
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
                                "Anonymous · no account · disappears after 24 hours",
                                fontSize = 11.sp,
                                color = SmTextDim,
                                fontFamily = GeistFamily,
                            )
                        }

                        // Top-right icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isLoggedIn) {
                                // Inbox icon
                                IconButton(onClick = { navController.navigate("inbox") }) {
                                    Icon(Icons.Filled.Email, contentDescription = "Inbox", tint = SmTextDim)
                                }
                                // Avatar with first letter
                                val letter = UserSession.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                Surface(
                                    onClick = { navController.navigate("my-secrets") },
                                    shape = CircleShape,
                                    color = SmAccent,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            letter,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = GeistFamily,
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { navController.navigate("auth") }) {
                                    Icon(Icons.Filled.Person, contentDescription = "Sign in", tint = SmTextDim)
                                }
                            }
                        }
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
                            SortTab("Latest", state.sort == Sort.Recent) { vm.setSort(Sort.Recent) }
                            SortTab("Top 24h +", state.sort == Sort.Top) { vm.setSort(Sort.Top) }
                        }
                        Text(
                            "${state.secrets.size} secrets",
                            fontSize = 11.sp,
                            color = SmTextFaint,
                            fontFamily = GeistFamily,
                        )
                    }
                }

                // Top filters (only in Top mode)
                if (state.sort == Sort.Top) {
                    // Mood filter chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                label = "All",
                                selected = state.moodFilter == null,
                                onClick = { vm.setMoodFilter(null) },
                            )
                            MOODS.forEach { mood ->
                                FilterChip(
                                    label = mood,
                                    selected = state.moodFilter == mood,
                                    onClick = { vm.setMoodFilter(if (state.moodFilter == mood) null else mood) },
                                )
                            }
                        }
                    }

                    // Reaction sort row
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReactionSortButton("All", state.reactionSort == null) { vm.setReactionSort(null) }
                            ReactionSortButton("Me Too", state.reactionSort == "me_too") { vm.setReactionSort("me_too") }
                            ReactionSortButton("Wild", state.reactionSort == "wild") { vm.setReactionSort("wild") }
                            ReactionSortButton("Doubtful", state.reactionSort == "doubtful") { vm.setReactionSort("doubtful") }
                        }
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
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("🤫", fontSize = 40.sp)
                            Text(
                                "No secrets yet.",
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
                        onWhisper = { secretId, receiverId ->
                            if (!UserSession.isLoggedIn) {
                                navController.navigate("auth")
                            } else {
                                navController.navigate("whisper/$secretId/$receiverId")
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

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
                if (selected) SmAccent.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(100.dp),
            ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (selected) SmAccent else SmTextDim,
            fontFamily = GeistFamily,
        )
    }
}

@Composable
fun ReactionSortButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = Color.Transparent,
        modifier = Modifier
            .border(
                1.dp,
                if (selected) SmAccent.copy(alpha = 0.5f) else SmBorder,
                RoundedCornerShape(100.dp),
            )
            .background(
                if (selected) SmAccent.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(100.dp),
            ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (selected) SmAccent else SmTextFaint,
            fontFamily = GeistFamily,
        )
    }
}
