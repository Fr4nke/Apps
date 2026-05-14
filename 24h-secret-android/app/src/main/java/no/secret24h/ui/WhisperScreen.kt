package no.secret24h.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import no.secret24h.data.UserSession
import no.secret24h.data.Whisper
import no.secret24h.data.WhisperApi

@Composable
fun WhisperScreen(
    secretId: String,
    otherUserId: String,
    onBack: () -> Unit,
) {
    var messages by remember { mutableStateOf<List<Whisper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun load() {
        scope.launch {
            try {
                messages = WhisperApi.getMessages(secretId, otherUserId)
                // Mark messages from other user as read
                WhisperApi.markRead(secretId, otherUserId)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            } catch (_: Exception) {}
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
                        "Whisper",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = SmText,
                        fontFamily = GeistFamily,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                // Messages list
                if (isLoading) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SmAccent, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isOwn = msg.senderId == UserSession.userId
                            WhisperBubble(msg = msg, isOwn = isOwn)
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A0608))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 500) inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(SmSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            color = SmText,
                            fontSize = 14.sp,
                            fontFamily = GeistFamily,
                        ),
                        cursorBrush = SolidColor(SmAccent),
                        decorationBox = { inner ->
                            Box {
                                if (inputText.isEmpty()) {
                                    Text(
                                        "Send a whisper...",
                                        color = SmTextFaint,
                                        fontSize = 14.sp,
                                        fontFamily = GeistFamily,
                                    )
                                }
                                inner()
                            }
                        },
                    )

                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotEmpty() && !isSending) {
                                isSending = true
                                scope.launch {
                                    try {
                                        WhisperApi.sendMessage(secretId, otherUserId, text)
                                        inputText = ""
                                        load()
                                    } catch (_: Exception) {}
                                    isSending = false
                                }
                            }
                        },
                        enabled = inputText.trim().isNotEmpty() && !isSending,
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.trim().isNotEmpty()) SmAccent else SmTextFaint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhisperBubble(msg: Whisper, isOwn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isOwn) SmAccent.copy(alpha = 0.85f) else Color(0x22FFE8DC),
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isOwn) 14.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 14.dp,
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = msg.text,
                color = if (isOwn) Color.White else SmText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = GeistFamily,
            )
        }
    }
}
