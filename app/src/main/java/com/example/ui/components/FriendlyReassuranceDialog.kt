package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagStrong

/**
 * 토스 · 카카오페이 스타일의 유저 친화적 다정 안심 다이얼로그 (Friendly Reassurance Dialog)
 *
 * 사용자가 불안, 망설임, 죄책감을 느낄 수 있는 순간(삭제, 개인정보 입력, 미착용 등)에
 * 따뜻하고 명쾌한 문구와 신뢰감 있는 핀테크 표준 UI로 안심을 제공합니다.
 */
@Composable
fun FriendlyReassuranceDialog(
    title: String,
    reassuranceText: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    accentBadgeText: String? = "안심 안내",
    isDestructiveAction: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZigzagCanvas,
            border = BorderStroke(1.dp, ZigzagHairline),
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .testTag("friendly_reassurance_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 안심 캡슐 뱃지
                if (accentBadgeText != null) {
                    Surface(
                        color = Color(0xFFF1F5F9), // 핀테크 뉴트럴 틴트
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = accentBadgeText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 다정한 메인 타이틀 (Text-First, no decorative icons)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        lineHeight = 26.sp,
                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                    ),
                    color = ZigzagStrong,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 사용자 불안을 해소해 주는 다정한 안심 설명문
                Text(
                    text = reassuranceText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = ZigzagSecondary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 액션 버튼 영역
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 주 액션 버튼
                    Button(
                        onClick = onPrimaryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructiveAction) Color(0xFFE11D48) else ZigzagCtaBlack,
                            contentColor = ZigzagCtaText
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reassurance_primary_button")
                    ) {
                        Text(
                            text = primaryButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // 보조 액션 버튼 (취소 또는 되돌리기)
                    if (secondaryButtonText != null && onSecondaryClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryClick,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, ZigzagHairline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZigzagSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("reassurance_secondary_button")
                        ) {
                            Text(
                                text = secondaryButtonText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 인라인 화면 또는 바텀시트 내부에 자연스럽게 녹아드는 '다정 안심 캡슐 카드'
 * (개인정보 보호, 데이터 안전 보장, 죄책감 완화 안내용)
 */
@Composable
fun ReassuranceNoticeCard(
    heading: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = heading,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF64748B)
                )
            )
        }
    }
}
