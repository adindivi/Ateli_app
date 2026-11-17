package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText

/**
 * 토스 · 네이버페이 스타일의 프리미엄 플로팅 캡슐 토스트 (Fintech Floating Capsule Toast)
 *
 * 기본 머티리얼 사각 스낵바 대신, 하단 내비게이션 바 위에 부드럽게 떠 있는
 * 다크 슬레이트 캡슐(24dp) 형태로 세련된 인터랙션 피드백을 전달합니다.
 */
@Composable
fun FintechFloatingToast(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("fintech_floating_toast"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZigzagCtaBlack,
            border = BorderStroke(0.5.dp, Color(0xFF334155)),
            shadowElevation = 8.dp,
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 토스트 본문 메시지 (Anti-Squishing & PlatformTextStyle 적용)
                Text(
                    text = snackbarData.visuals.message,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZigzagCtaText,
                        lineHeight = 18.sp,
                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 선택적 액션 버튼 (예: "실행 취소", "확인")
                snackbarData.visuals.actionLabel?.let { action ->
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = action,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA), // 토스 블루 액션 틴트
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Context 기반 즉시 실행형 토스트 유틸리티 (필요 시 시스템 토스트 대체)
 */
fun showFintechToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
