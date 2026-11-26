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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.FriendlyReassuranceDialog
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
    onClearChat: () -> Unit,
    onShowToast: (String) -> Unit = {}
) {
    val profile = (profileState as? DataUiState.Success)?.data ?: UserProfile()
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()
    val bodyShape = BodyShapeType.fromCode(profile.bodyShape)

    var inputText by remember { mutableStateOf("") }
    var showClearChatDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "오늘 뭐 입고 나갈까요?",
        "안 입는 옷 어떻게 입을까요?",
        "주말 코디 추천해줘요",
        "새로 사면 좋을 옷이 뭘까요?"
    )

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 680.dp)
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
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZigzagStrong
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI 코디",
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
                        onClick = { showClearChatDialog = true },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "대화 초기화",
                            tint = ZigzagMuted
                        )
                    }
                }

                if (showClearChatDialog) {
                    FriendlyReassuranceDialog(
                        title = "대화를 처음부터 다시 나눌까요?",
                        reassuranceText = "지금까지 나눈 스타일링 대화 내용만 깔끔하게 비우며, 내 옷장에 등록된 옷과 저장된 코디는 그대로 안전하게 보관돼요.",
                        primaryButtonText = "대화 계속하기",
                        onPrimaryClick = { showClearChatDialog = false },
                        secondaryButtonText = "대화 비우기",
                        onSecondaryClick = {
                            onClearChat()
                            showClearChatDialog = false
                            onShowToast("대화 내용이 깨끗하게 정리되었어요")
                        },
                        onDismissRequest = { showClearChatDialog = false },
                        accentBadgeText = "안심 안내"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Tier Switcher (Integrated Segmented Bar)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isFlash = selectedModelName == "gemini-3.8-flash"
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isFlash) ZigzagStrong else Color.Transparent,
                            onClick = {
                                onSelectModel("gemini-3.8-flash")
                                onShowToast("빠른 추천 모드로 전환했어요")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("model_chip_flash")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "빠른 추천",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isFlash) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isFlash) Color.White else ZigzagSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        val isArgon = selectedModelName == "gemini-4-argon"
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isArgon) ZigzagStrong else Color.Transparent,
                            onClick = {
                                onSelectModel("gemini-4-argon")
                                onShowToast("상세 분석 모드로 전환했어요")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("model_chip_argon")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "상세 분석",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isArgon) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isArgon) Color.White else ZigzagSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
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
                placeholder = { Text("코디 고민을 편하게 남겨보세요...", color = ZigzagMuted) },
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
                color = if (inputText.isNotBlank() && !isStreaming) Color(0xFF0071E3) else Color(0xFFF1F5F9),
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
                            tint = if (inputText.isNotBlank()) Color.White else ZigzagMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                Text(
                    text = "A",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ZigzagStrong
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
