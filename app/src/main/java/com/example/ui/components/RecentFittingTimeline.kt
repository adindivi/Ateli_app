package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FittingHistoryEntity
import com.example.data.model.Garment
import com.example.ui.theme.AtelierChampagne
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentFittingTimelineSection(
    historyList: List<FittingHistoryEntity>,
    cutoutBitmaps: Map<String, Bitmap>,
    onRestoreToCanvas: (FittingHistoryEntity) -> Unit,
    onTogglePin: (FittingHistoryEntity) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearUnpinnedHistory: () -> Unit,
    onRecordCurrentSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredHistory = remember(historyList, selectedFilter) {
        when (selectedFilter) {
            "PINNED" -> historyList.filter { it.isPinned }
            "ROULETTE" -> historyList.filter {
                it.triggerSourceLabel.contains("슬롯") || it.triggerSourceLabel.contains("룰렛")
            }
            "HIGH_SCORE" -> historyList.filter { it.harmonyScore >= 95 }
            else -> historyList
        }
    }

    val pinnedCount = remember(historyList) { historyList.count { it.isPinned } }
    val bestHarmonyScore = remember(historyList) {
        historyList.maxOfOrNull { it.harmonyScore } ?: 0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_fitting_timeline_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Timeline Header Summary Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AtelierMidnight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "최근 피팅 타임라인",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AtelierChampagne
                        )
                        Text(
                            text = "시도한 코디 조합 자동 기록",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                    }

                    if (historyList.any { !it.isPinned }) {
                        OutlinedButton(
                            onClick = onClearUnpinnedHistory,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AtelierChampagne),
                            border = BorderStroke(1.dp, AtelierChampagne.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("clear_unpinned_history_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("비우기", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimelineStatChip(
                        label = "피팅 시도",
                        value = "${historyList.size}회",
                        modifier = Modifier.weight(1f)
                    )
                    TimelineStatChip(
                        label = "북마크",
                        value = "${pinnedCount}건",
                        modifier = Modifier.weight(1f)
                    )
                    TimelineStatChip(
                        label = "최고 하모니",
                        value = if (bestHarmonyScore > 0) "${bestHarmonyScore}점" else "-",
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = onRecordCurrentSnapshot,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtelierChampagne,
                        contentColor = AtelierMidnight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_current_fitting_snapshot_button")
                ) {
                    Text("현재 착장 저장", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ALL" to "전체 (${historyList.size})",
                "PINNED" to "⭐ 북마크 ($pinnedCount)",
                "HIGH_SCORE" to "🏆 95점+",
                "ROULETTE" to "🎰 슬롯 스핀"
            ).forEach { (code, label) ->
                FilterChip(
                    selected = selectedFilter == code,
                    onClick = { selectedFilter = code },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("timeline_filter_$code")
                )
            }
        }

        // 3. Vertical Chronological Timeline List
        if (filteredHistory.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "피팅 기록이 아직 없어요",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "가상 피팅 시 자동으로 타임라인에 남아요",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredHistory.forEachIndexed { idx, record ->
                    TimelineNodeRow(
                        record = record,
                        isFirst = idx == 0,
                        isLast = idx == filteredHistory.lastIndex,
                        cutoutBitmaps = cutoutBitmaps,
                        onRestoreToCanvas = { onRestoreToCanvas(record) },
                        onTogglePin = { onTogglePin(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = AtelierChampagne
            )
        }
    }
}

@Composable
private fun TimelineNodeRow(
    record: FittingHistoryEntity,
    isFirst: Boolean,
    isLast: Boolean,
    cutoutBitmaps: Map<String, Bitmap>,
    onRestoreToCanvas: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val previewGarments = remember(record) { record.toPreviewGarments() }
    val timeLabel = remember(record.timestampMillis) {
        formatRelativeAndClockTime(record.timestampMillis)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("timeline_item_${record.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Chronological Timeline Track & Node Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(
                        if (isFirst) Color.Transparent
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (record.isPinned) AtelierChampagne
                        else MaterialTheme.colorScheme.primary
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        if (isLast) Color.Transparent
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
            )
        }

        // Right Timeline Event Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (record.isPinned) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Timestamp + Trigger Badge + Pin & Delete Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = record.triggerSourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "$timeLabel · ${record.canvasModeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("pin_fitting_record_${record.id}")
                        ) {
                            Icon(
                                imageVector = if (record.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "북마크 고정",
                                tint = if (record.isPinned) AtelierChampagne else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_fitting_record_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "기록 삭제",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Title & Harmony / CAD Score Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.sessionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val shortHarmony = record.harmonyFormulaKo.substringBefore(" (")
                            val shortGrade = record.cadFitGrade.substringBefore(" (")
                            val shortSize = record.recommendedKrSize.substringBefore(" (")
                            listOf(shortHarmony, "$shortGrade 핏", shortSize).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AtelierSage.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${record.harmonyScore}점",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AtelierSage,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 4-Slot Garment Thumbnail Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    previewGarments.forEach { g ->
                        TimelineGarmentMiniChip(
                            garment = g,
                            cutoutBitmap = cutoutBitmaps[g.id]
                        )
                    }
                }

                HorizontalDivider()

                // Bottom Row: Visual Color Swatches + 1-Tap Restore to 3D Canvas Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        previewGarments.take(4).forEach { g ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColorSafe(g.colorHex))
                            )
                        }
                        Text(
                            text = "${previewGarments.size}벌",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = onRestoreToCanvas,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("restore_fitting_record_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "다시 입어보기",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineGarmentMiniChip(
    garment: Garment,
    cutoutBitmap: Bitmap?
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF4F1EA))
            ) {
                GarmentCutoutIllustration(
                    garment = garment,
                    cutoutBitmap = cutoutBitmap,
                    modifier = Modifier.padding(3.dp)
                )
            }
            Column {
                Text(
                    text = garment.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(parseHexColorSafe(garment.colorHex))
                    )
                    Text(
                        text = garment.colorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatRelativeAndClockTime(timestampMillis: Long): String {
    val diffSec = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
    val rel = when {
        diffSec < 60L -> "방금 전"
        diffSec < 3600L -> "${diffSec / 60L}분 전"
        diffSec < 86400L -> "${diffSec / 3600L}시간 전"
        else -> "${diffSec / 86400L}일 전"
    }
    val clock = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(timestampMillis))
    return "$rel ($clock)"
}
