package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ZigzagMuted

/**
 * ZIGZAG (ZDS) 표준 Pull-to-Dismiss 바텀 시트 드래그 핸들.
 * 스와이프 다운 제스처(Swipe-to-Dismiss)를 명확히 인지할 수 있는 시각적 인디케이터와
 * 안정적인 드래그 감도를 위한 물리적 높이를 보장합니다.
 */
@Composable
fun ZigzagBottomSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(ZigzagMuted.copy(alpha = 0.45f))
        )
    }
}
