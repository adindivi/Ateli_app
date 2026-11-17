package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.data.model.BodyShapeType
import com.example.data.model.Garment
import com.example.data.model.UserProfile
import com.example.data.remote.WeatherContext
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagStrong
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.DataUiState

@Composable
fun AiStylistChatScreen(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    selectedModelName: String,
    profileState: DataUiState<UserProfile>,
    garmentsState: DataUiState<List<Garment>>,
    weather: WeatherContext,
    onSelectModel: (String) -> Unit,
    onSendMessage: (String, UserProfile, List<Garment>, WeatherContext) -> Unit,
    onClearChat: () -> Unit
) {
    val profile = (profileState as? DataUiState.Success)?.data ?: UserProfile()
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()
    val bodyShape = BodyShapeType.fromCode(profile.bodyShape)

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "내 ${bodyShape.labelKo}과 오늘 날씨(${weather.temperatureC.toInt()}°C)에 맞는 출근룩 추천해줘",
        "내 옷장에서 CPW(가성비)가 가장 아쉬운 방치템 활용 코디 짜줘",
        "보유한 상의·하의 색상 조합으로 주말 데이트룩 2가지 비교해줘",
        "내 옷장 구성을 분석해서 다음에 사면 좋을 필수 아이템 1개만 추천해줘"
    )

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_stylist_chat_screen")
    ) {
        // 1. Stylist Persona & Gemini Model Selector Header
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
            border = BorderStroke(1.dp, ZigzagHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ZigzagHairline),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ZigzagStrong,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI 수석 퍼스널 스타일리스트",
                                style = MaterialTheme.typography.titleMedium,
                                color = ZigzagStrong,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "옷장 ${garments.size}벌 · ${bodyShape.labelKo} · ${weather.cityLabelKo} ${weather.temperatureC.toInt()}°C",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZigzagSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "대화 초기화",
                            tint = ZigzagMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Tier Switcher (gemini-3.8-flash vs gemini-4-argon)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedModelName == "gemini-3.8-flash") ZigzagStrong else ZigzagCanvas,
                        border = BorderStroke(1.dp, if (selectedModelName == "gemini-3.8-flash") ZigzagStrong else ZigzagHairline),
                        onClick = { onSelectModel("gemini-3.8-flash") },
                        modifier = Modifier.testTag("model_chip_flash")
                    ) {
                        Text(
                            text = "⚡ 일반 상담 (Flash)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (selectedModelName == "gemini-3.8-flash") Color.White else ZigzagSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedModelName == "gemini-4-argon") ZigzagStrong else ZigzagCanvas,
                        border = BorderStroke(1.dp, if (selectedModelName == "gemini-4-argon") ZigzagStrong else ZigzagHairline),
                        onClick = { onSelectModel("gemini-4-argon") },
                        modifier = Modifier.testTag("model_chip_argon")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (selectedModelName == "gemini-4-argon") Color.White else ZigzagSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "🧠 심층 분석 (Argon)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (selectedModelName == "gemini-4-argon") Color.White else ZigzagSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Quick Prompt Suggestions Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            quickPrompts.forEachIndexed { idx, prompt ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ZigzagCanvas,
                    border = BorderStroke(1.dp, ZigzagHairline),
                    onClick = {
                        if (!isStreaming) {
                            onSendMessage(prompt, profile, garments, weather)
                        }
                    },
                    modifier = Modifier.testTag("quick_prompt_$idx")
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ZigzagSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Scrollable Multi-Turn Conversation Thread
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_message_list")
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Message Input Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("체형 고민, 코디 매칭 등 무엇이든 물어보세요...", color = ZigzagMuted) },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ZigzagCanvas,
                    unfocusedContainerColor = ZigzagCanvas,
                    focusedBorderColor = ZigzagStrong,
                    unfocusedBorderColor = ZigzagHairline,
                    focusedTextColor = ZigzagForeground,
                    unfocusedTextColor = ZigzagForeground,
                    cursorColor = ZigzagStrong
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (inputText.isNotBlank() && !isStreaming) ZigzagCtaBlack else ZigzagHairline,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = inputText.isNotBlank() && !isStreaming) {
                        val toSend = inputText.trim()
                        inputText = ""
                        onSendMessage(toSend, profile, garments, weather)
                    }
                    .testTag("chat_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "메시지 전송",
                            tint = if (inputText.isNotBlank()) ZigzagCtaText else ZigzagMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ZigzagHairline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI 스타일리스트",
                    tint = ZigzagStrong,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            color = if (isUser) ZigzagCtaBlack else ZigzagCanvas,
            border = if (isUser) null else BorderStroke(1.dp, ZigzagHairline),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!isUser && message.modelBadge.isNotBlank()) {
                    Text(
                        text = message.modelBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ZigzagSecondary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) ZigzagCtaText else ZigzagForeground
                )
            }
        }
    }
}
