
package com.life360.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatbotScreen(onBack: () -> Unit) {

    val darkGreen = Color(0xFF176B45)
    val backgroundColor = Color(0xFFF6F8F6)

    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Hello! 👋 I'm your LiFE360 Sustainability Assistant.",
                false
            ),
            ChatMessage(
                "Ask me about sustainable living, climate change, recycling, energy, water and more. I'm ready to help!",
                false
            )
        )
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler {
        onBack()
    }

    // SEND MESSAGE TO GEMINI
    fun sendMessage() {

        val typedMessage = messageText.trim()

        if (typedMessage.isBlank() || isLoading) {
            return
        }

        messages.add(
            ChatMessage(
                text = typedMessage,
                isUser = true
            )
        )

        messageText = ""
        isLoading = true

        scope.launch {
            try {
                val aiReply =
                    GeminiAssistantService.sendMessage(typedMessage)

                messages.add(
                    ChatMessage(
                        text = aiReply,
                        isUser = false
                    )
                )

            } catch (e: Exception) {

                messages.add(
                    ChatMessage(
                        text = "Sorry, I couldn't connect to the AI assistant. Please try again.\n\nDetails: ${e.message ?: "Unknown error"}",
                        isUser = false
                    )
                )

            } finally {
                isLoading = false
            }
        }
    }

    // AUTOMATICALLY SCROLL TO THE LATEST MESSAGE
    LaunchedEffect(messages.size, isLoading) {

        val lastIndex = messages.size +
                if (isLoading) 0 else -1

        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    // MAIN CHAT SCREEN
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .imePadding()
    ) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(onClick = onBack) {
                Text(
                    text = "← Back",
                    color = darkGreen,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {

                Text(
                    text = "AI Sustainability Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )

                Text(
                    text = "LiFE360 • Powered by Gemini AI",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        HorizontalDivider()

        // CHAT MESSAGES
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(messages) { message ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (message.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                ) {

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.isUser) {
                            darkGreen
                        } else {
                            Color.White
                        },
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {

                        Text(
                            text = message.text,
                            modifier = Modifier.padding(14.dp),
                            color = if (message.isUser) {
                                Color.White
                            } else {
                                Color.DarkGray
                            },
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // AI LOADING INDICATOR
            if (isLoading) {

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp
                        ) {

                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = darkGreen
                                )

                                Spacer(
                                    modifier = Modifier.width(10.dp)
                                )

                                Text(
                                    text = "Gemini is thinking...",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // MESSAGE INPUT AREA
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {

                Text(
                    text = "Ask LiFE360 AI about sustainability.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = messageText,

                        onValueChange = { newText ->
                            messageText = newText
                        },

                        modifier = Modifier.weight(1f),

                        placeholder = {
                            Text("Type your message...")
                        },

                        shape = RoundedCornerShape(24.dp),

                        maxLines = 3,

                        enabled = !isLoading,

                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),

                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendMessage()
                            }
                        ),

                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray,
                            cursorColor = darkGreen,
                            focusedBorderColor = darkGreen,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {
                            sendMessage()
                        },

                        enabled = messageText.isNotBlank() &&
                                !isLoading,

                        colors = ButtonDefaults.buttonColors(
                            containerColor = darkGreen
                        ),

                        shape = RoundedCornerShape(24.dp)
                    ) {

                        Text("Send")
                    }
                }
            }
        }
    }
}
