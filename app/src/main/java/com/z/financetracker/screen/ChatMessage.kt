package com.z.financetracker.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.AiChatRequest
import com.z.financetracker.entity.AiMessage
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,    // "user" or "assistant"
    val content: String
)

@Composable
fun AiChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isSummaryLoading by remember { mutableStateOf(true) }

    val suggestions = listOf(
        "Where am I overspending?",
        "How can I save more?",
        "Am I on track with my goals?",
        "What's my biggest financial risk?"
    )

    // Load AI summary on open
    LaunchedEffect(Unit) {
        try {
            val resp = NetworkClient.getAiApi(context).getSummary()
            if (resp.isSuccessful) {
                val summary = resp.body()?.get("summary") ?: return@LaunchedEffect
                messages = listOf(ChatMessage(role = "assistant", content = summary))
            }
        } finally {
            isSummaryLoading = false
        }
    }

    // Auto-scroll to bottom whenever messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isThinking) return
        val userMsg = ChatMessage(role = "user", content = text.trim())
        messages = messages + userMsg
        inputText = ""
        isThinking = true

        scope.launch {
            try {
                // FIXED: use typed data class instead of Map<String, Any>
                val request = AiChatRequest(
                    messages = messages.map { AiMessage(role = it.role, content = it.content) }
                )
                val resp = NetworkClient.getAiApi(context).chat(request)
                val reply = resp.body()?.get("reply") ?: "Sorry, I couldn't get a response."
                messages = messages + ChatMessage(role = "assistant", content = reply)
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    role = "assistant",
                    content = "Connection error: ${e.message}"
                )
            } finally {
                isThinking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // ── Top bar ────────────────────────────────────────────────
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AI Finance Advisor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Powered by Llama 3",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }

        // ── Messages ───────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (isSummaryLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Analyzing your finances…",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }

                    // Thinking indicator
                    if (isThinking) {
                        item {
                            ThinkingBubble()
                        }
                    }

                    // Suggestion chips — show only when no conversation yet
                    if (messages.size <= 1 && !isThinking) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Try asking:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                suggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = { sendMessage(suggestion) },
                                        label = {
                                            Text(suggestion, fontSize = 13.sp)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = Color(0xFFE5E7EB)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // ── Input bar ──────────────────────────────────────────────
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about your finances…", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (inputText.isNotBlank() && !isThinking)
                                Color(0xFF2563EB) else Color(0xFFE5E7EB),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Chat bubble ────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) Color(0xFF2563EB) else Color.White,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.content,
                color = if (isUser) Color.White else Color(0xFF111827),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2563EB).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Thinking indicator (animated dots) ────────────────────────────

@Composable
private fun ThinkingBubble() {
    var dotCount by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            dotCount = (dotCount % 3) + 1
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFEFF6FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = ".".repeat(dotCount),
                color = Color(0xFF6B7280),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}