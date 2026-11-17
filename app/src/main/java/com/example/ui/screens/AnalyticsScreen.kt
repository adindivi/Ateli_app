package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import com.example.data.model.Garment
import com.example.ui.components.B2BInventoryAuditAndEsgCard
import com.example.ui.components.GarmentCutoutIllustration
import com.example.ui.components.showFintechToast
import com.example.ui.components.StitchFixBuyOrNotSimulatorCard
import com.example.ui.components.StylebookBreakEvenTrackerCard
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.ZigzagAppBanner
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagStrong
import com.example.viewmodel.DataUiState

@Composable
fun AnalyticsScreen(
    garmentsState: DataUiState<List<Garment>>,
    cutoutBitmaps: Map<String, Bitmap>,
    onIncrementWear: (Garment) -> Unit,
    onAskAiStylist: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()

    val totalWardrobeValue = remember(garments) { garments.sumOf { it.purchasePrice } }
    val totalWearCount = remember(garments) { garments.sumOf { it.wearCount } }
    val averageCpw = remember(garments) {
        if (garments.isEmpty()) 0L
        else garments.sumOf { it.costPerWear } / garments.size
    }

    // Top 3 Most Worn & Best CPW ("뽕 뽑은 효자 옷 Top 3")
    val top3BestValue = remember(garments) {
        garments.filter { it.wearCount > 0L }
            .sortedWith(compareByDescending<Garment> { it.wearCount }.thenBy { it.costPerWear })
            .take(3)
    }

    // Closet Sleepers ("1년 내내 안 입은 방치템 / 당근마켓 판매 추천")
    val unwornSleepers = remember(garments) {
        garments.filter { it.wearCount <= 1L }
            .sortedByDescending { it.purchasePrice }
    }

    val dormantRatePercent = remember(garments, unwornSleepers) {
        if (garments.isEmpty()) 0
        else ((unwornSleepers.size.toDouble() / garments.size.toDouble()) * 100.0).toInt()
    }

    var copiedResaleNotice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("analytics_screen")
    ) {
        // 1. Summary KPI Dashboard Card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
            border = BorderStroke(1.dp, ZigzagHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "내 옷장 가성비 리포트",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ZigzagStrong
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CPW = (옷 초기 구매가) ÷ (총 착용 횟수). 입을수록 1회당 비용이 내려갑니다!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZigzagSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = "옷장 총 자산가치",
                        value = "₩${String.format("%,d", totalWardrobeValue)}",
                        subLabel = "총 ${garments.size}벌",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "평균 1회 착용비용",
                        value = "₩${String.format("%,d", averageCpw)}",
                        subLabel = "누적 ${totalWearCount}회 착용",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "옷장 방치율",
                        value = "${dormantRatePercent}%",
                        subLabel = "미착용·1회 이하 ${unwornSleepers.size}벌",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1.2 B2B Enterprise Inventory Audit, ESG Carbon & CSV ERP Exporter
        B2BInventoryAuditAndEsgCard(
            garments = garments
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1.5 Stitch Fix & Musinsa Benchmark: 'Buy or Not?' Pre-Purchase Wardrobe Compatibility Simulator
        StitchFixBuyOrNotSimulatorCard(
            ownedGarments = garments,
            onAskAiStylist = onAskAiStylist
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1.8 Stylebook Benchmark: Target CPW Break-Even Goal Tracker (with Net Effective CPW & Resale Value)
        if (garments.isNotEmpty()) {
            StylebookBreakEvenTrackerCard(
                garments = garments,
                onWearPlusOne = onIncrementWear
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. Top 3 Most Worn & Best CPW Leaderboard ("가장 자주 입은 효자 아이템 Top 3")
        Text(
            text = "가장 자주 입은 효자 아이템 Top 3",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ZigzagStrong
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (top3BestValue.isEmpty()) {
            Surface(
                color = ZigzagCanvas,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ZigzagHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "아직 착용 기록이 있는 의류가 없습니다. 옷장이나 코디 화면에서 '입음 +1'을 눌러 가성비를 측정해보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZigzagSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                top3BestValue.forEachIndexed { idx, g ->
                    TopCpwGarmentRow(
                        rank = idx + 1,
                        garment = g,
                        cutoutBitmap = cutoutBitmaps[g.id],
                        onIncrementWear = {
                            onIncrementWear(g)
                            showFintechToast(context, "'${g.name}' 착용 1회가 기록되었어요")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Unworn Closet Sleepers -> Karrot Market (당근마켓) Resale Recommendation
        Text(
            text = "잠자는 옷장 방치템 · 리셀 추천",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ZigzagStrong
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "착용 횟수가 0~1회인 의류입니다. 지금 당근마켓에 중고로 정리하면 옷장 공간과 현금을 동시에 확보할 수 있어요!",
            style = MaterialTheme.typography.bodySmall,
            color = ZigzagSecondary
        )

        if (copiedResaleNotice != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = ZigzagHairline,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = copiedResaleNotice!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = ZigzagStrong,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (unwornSleepers.isEmpty()) {
            Surface(
                color = ZigzagCanvas,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ZigzagHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "축하합니다! 방치된 옷 없이 모든 아이템을 알차게 입고 계시네요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZigzagSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                unwornSleepers.forEach { sleeper ->
                    val suggestedResalePrice = ((sleeper.purchasePrice * 0.62) / 1000).toLong() * 1000L
                    SleeperResaleCard(
                        garment = sleeper,
                        suggestedResalePrice = suggestedResalePrice,
                        cutoutBitmap = cutoutBitmaps[sleeper.id],
                        onCopyKarrotDraft = {
                            val draft = """
                                [당근마켓 판매글 자동완성]
                                제목: [${sleeper.brand}] ${sleeper.name} (${sleeper.colorName}) 상태 최상 양도합니다
                                • 정가: ${String.format("%,d", sleeper.purchasePrice)}원
                                • 희망 판매가: ${String.format("%,d", suggestedResalePrice)}원
                                • 실착용 횟수: ${sleeper.wearCount}회 (보관 상태 최상)
                                • 특징: ${sleeper.season} 시즌 / ${sleeper.subCategory} / ${sleeper.tpoTags.joinToString(", ")} 코디에 잘 어울립니다.
                            """.trimIndent()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("당근마켓 판매글", draft))
                            copiedResaleNotice = "🥕 '${sleeper.name}' 당근마켓 판매글이 클립보드에 복사되었습니다!"
                            showFintechToast(context, "'${sleeper.name}' 당근 판매글이 복사되었어요")
                        },
                        onWearNow = {
                            onIncrementWear(sleeper)
                            showFintechToast(context, "오늘 착용 1회가 기록되었어요")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Wardrobe Category & Color Palette Balance Diagnostics
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
            border = BorderStroke(1.dp, ZigzagHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "옷장 카테고리 & 컬러 밸런스 진단",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ZigzagStrong
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "보유 아이템의 카테고리 비중과 컬러 분포를 분석해 중복 소비를 예방합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZigzagSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val categoryLabels = listOf(
                    "OUTER" to "아우터",
                    "TOP" to "상의",
                    "BOTTOM" to "하의",
                    "SHOES" to "신발",
                    "BAG" to "가방/잡화"
                )
                val totalCount = garments.size.coerceAtLeast(1)

                categoryLabels.forEach { (code, labelKo) ->
                    val count = garments.count { it.category == code }
                    val ratio = count.toFloat() / totalCount.toFloat()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = labelKo,
                            style = MaterialTheme.typography.labelLarge,
                            color = ZigzagForeground,
                            modifier = Modifier.width(72.dp)
                        )
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = ZigzagStrong,
                            trackColor = ZigzagHairline,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${count}벌 (${(ratio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZigzagSecondary,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                if (garments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "내 옷장 컬러 스펙트럼 스와치",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = ZigzagStrong
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        garments.distinctBy { it.colorName }.forEach { g ->
                            Surface(
                                color = ZigzagCanvas,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, ZigzagHairline)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColorSafe(g.colorHex))
                                            .border(1.dp, ZigzagHairline, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = g.colorName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZigzagSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Weekly 5-Day Smart OOTD Rotation Planner (Maximizes CPW across Mon-Fri)
        val tops = remember(garments) { garments.filter { it.category == "TOP" } }
        val bottoms = remember(garments) { garments.filter { it.category == "BOTTOM" } }
        val outers = remember(garments) { garments.filter { it.category == "OUTER" } }

        if (tops.isNotEmpty() && bottoms.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
                border = BorderStroke(1.dp, ZigzagHairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_rotation_planner_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "주간 5일 (월~금) 가성비 코디 로테이션",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ZigzagStrong
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "착용 횟수가 적은 아이템을 우선 배치해 주간 평균 CPW를 낮추는 자동 스케줄입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZigzagSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val days = listOf("월요일", "화요일", "수요일", "목요일", "금요일")
                    val sortedTops = tops.sortedBy { it.wearCount }
                    val sortedBottoms = bottoms.sortedBy { it.wearCount }
                    val sortedOuters = outers.sortedBy { it.wearCount }

                    days.forEachIndexed { idx, dayLabel ->
                        val top = sortedTops[idx % sortedTops.size]
                        val bottom = sortedBottoms[idx % sortedBottoms.size]
                        val outer = if (sortedOuters.isNotEmpty() && idx % 2 == 0) {
                            sortedOuters[idx % sortedOuters.size]
                        } else null

                        Surface(
                            color = ZigzagCanvas,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ZigzagHairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ZigzagStrong,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = buildString {
                                        if (outer != null) append("${outer.name} + ")
                                        append("${top.name} + ${bottom.name}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZigzagForeground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ZigzagCanvas,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ZigzagHairline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ZigzagSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZigzagStrong,
                    fontFeatureSettings = "tnum",
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ZigzagMuted,
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TopCpwGarmentRow(
    rank: Int,
    garment: Garment,
    cutoutBitmap: Bitmap?,
    onIncrementWear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
        border = BorderStroke(1.dp, ZigzagHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ZigzagStrong),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(ZigzagHairline)
                ) {
                    GarmentCutoutIllustration(
                        garment = garment,
                        cutoutBitmap = cutoutBitmap,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[${garment.brand}] ${garment.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ZigzagForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "1회 ₩${String.format("%,d", garment.costPerWear)}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    fontFeatureSettings = "tnum",
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "구매가 ₩${String.format("%,d", garment.purchasePrice)} · ${garment.wearCount}회 (${garment.valueRecoveryPercent}%)",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ZigzagSecondary,
                            fontFeatureSettings = "tnum",
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onIncrementWear,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, ZigzagHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZigzagStrong)
                ) {
                    Text("+1회", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { garment.valueRecoveryPercent / 100f },
                color = ZigzagStrong,
                trackColor = ZigzagHairline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun SleeperResaleCard(
    garment: Garment,
    suggestedResalePrice: Long,
    cutoutBitmap: Bitmap?,
    onCopyKarrotDraft: () -> Unit,
    onWearNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
        border = BorderStroke(1.dp, ZigzagHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(ZigzagHairline)
                ) {
                    GarmentCutoutIllustration(
                        garment = garment,
                        cutoutBitmap = cutoutBitmap,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[${garment.brand}] ${garment.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ZigzagForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "당근 ₩${String.format("%,d", suggestedResalePrice)}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    fontFeatureSettings = "tnum",
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "구매가 ₩${String.format("%,d", garment.purchasePrice)} · ${if (garment.wearCount == 0L) "미착용 방치" else "연 1회 착용"}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ZigzagSecondary,
                            fontFeatureSettings = "tnum",
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCopyKarrotDraft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZigzagCtaBlack,
                        contentColor = ZigzagCtaText
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_karrot_resale_${garment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("당근마켓 판매글 복사", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onWearNow,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, ZigzagHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZigzagSecondary)
                ) {
                    Text("오늘 입기 (+1)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
