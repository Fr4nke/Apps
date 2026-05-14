package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import no.secret24h.data.WhisperApi
import no.secret24h.data.WhisperConversation

@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onConversation: (secretId: String, otherUserId: String) -> Unit,
) {
    var conversations by remember { mutableStateOf<List<WhisperConversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                conversations = WhisperApi.getConversations()
            } catch (e: Exception) {
                error = e.message
            }
            isLoading = false
        }
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
                        "Inbox",
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
                } else if (conversations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💬", fontSize = 40.sp)
                            Text("No whispers yet.", color = SmTextDim, fontSize = 14.sp, fontFamily = GeistFamily)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(conversations, key = { "${it.secretId}/${it.otherUserId}" }) { conv ->
                            ConversationRow(
                                conv = conv,
                                onClick = { onConversation(conv.secretId, conv.otherUserId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationRow(conv: WhisperConversation, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SmBorder, RoundedCornerShape(14.dp))
            .background(SmSurface, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SmAccent.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, SmAccent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("💬", fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = conv.secretText.take(60).let { if (conv.secretText.length > 60) "$it…" else it },
                    color = SmText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeistFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conv.lastMessage,
                    color = SmTextDim,
                    fontSize = 12.sp,
                    fontFamily = GeistFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (conv.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(SmAccent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (conv.unreadCount > 9) "9+" else conv.unreadCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
