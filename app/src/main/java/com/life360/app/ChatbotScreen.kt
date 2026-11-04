
package com.life360.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatbotScreen(onBack: () -> Unit) {

    val darkGreen = Color(0xFF176B45)
    val backgroundColor = Color(0xFFF6F8F6)

    var messageText by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Hello! 👋 I'm your LiFE360 Sustainability Assistant.",
                false
            ),
            ChatMessage(
                "You can ask me about sustainable living, climate change, recycling, energy, water and more. AI features are coming soon!",
                false
            )
        )
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(onClick = onBack) {
                Text("← Back", color = darkGreen)
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
                    text = "LiFE360 • Coming Soon",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        HorizontalDivider()

        // Chat messages
        LazyColumn(
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
                    horizontalArrangement = if (message.isUser) {
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
        }

        // Message input
        Surface(
            color = Color.White,
            shadowElevation = 4.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {

                Text(
                    text = "AI chat integration will be enabled in a future update.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Type your message...")
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val typedMessage = messageText.trim()

                            if (typedMessage.isNotEmpty()) {

                                messages.add(
                                    ChatMessage(
                                        text = typedMessage,
                                        isUser = true
                                    )
                                )

                                messages.add(
                                    ChatMessage(
                                        text = "Thanks for your message! The AI assistant will be connected in a future LiFE360 update.",
                                        isUser = false
                                    )
                                )

                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
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
