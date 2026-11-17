package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Garment
import com.example.data.model.UserProfile
import com.example.data.remote.WeatherContext
import com.example.ui.theme.AtelierChampagne
import com.example.ui.theme.AtelierCoral
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import kotlin.math.abs
import kotlin.math.roundToInt

// ============================================================================
// B2B PRO ENGINE 1: CAD Tailoring Fit & Ease Allowance (여유분·텐션) Engine
// ============================================================================

data class ZoneFitSpec(
    val zoneNameKo: String,
    val bodyMeasureCm: Float,
    val garmentSpecCm: Float,
    val easeAllowanceCm: Float,
    val tensionPercent: Int, // 0..100 (45..70 is optimal studio drape)
    val statusBadge: String,
    val alterationNoteKo: String
)

data class TailoringFitReport(
    val recommendedKrSize: String,
    val overallFitGrade: String,
    val overallEaseScore: Int,
    val zoneSpecs: List<ZoneFitSpec>,
    val masterTailorSummaryKo: String
)

object TailoringFitSpecEngine {
    fun evaluateCadFitSpec(
        metrics: AvatarBodyMetrics,
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        fitScaleMultiplier: Float = 1.0f
    ): TailoringFitReport {
        val scale = fitScaleMultiplier.coerceIn(0.80f, 1.25f)
        val topThicknessEase = when (top?.thickness ?: "MEDIUM") {
            "HEAVY" -> 7.5f
            "LIGHT" -> 4.0f
            else -> 5.8f
        }
        val outerExtraEase = if (outer != null) 4.2f else 0f

        val shoulderGarment = (metrics.shoulderWidthCm + (2.4f + outerExtraEase * 0.35f) * scale)
        val shoulderEase = shoulderGarment - metrics.shoulderWidthCm
        val shoulderTension = (100f - (shoulderEase * 9.5f)).roundToInt().coerceIn(25, 92)

        val chestGarment = (metrics.chestCm + (topThicknessEase + outerExtraEase) * scale)
        val chestEase = chestGarment - metrics.chestCm
        val chestTension = (100f - (chestEase * 4.5f)).roundToInt().coerceIn(28, 90)

        val isWideBottom = bottom?.subCategory?.contains("와이드") == true || bottom?.name?.contains("와이드") == true
        val waistBaseEase = if (isWideBottom) 3.4f else 2.2f
        val waistGarment = (metrics.waistCm + waistBaseEase * scale)
        val waistEase = waistGarment - metrics.waistCm
        val waistTension = (100f - (waistEase * 12f)).roundToInt().coerceIn(30, 94)

        val hipBaseEase = if (isWideBottom) 9.5f else 5.5f
        val hipGarment = (metrics.hipCm + hipBaseEase * scale)
        val hipEase = hipGarment - metrics.hipCm
        val hipTension = (100f - (hipEase * 5.2f)).roundToInt().coerceIn(25, 88)

        val zones = listOf(
            ZoneFitSpec(
                zoneNameKo = "어깨 프레임 (Shoulder)",
                bodyMeasureCm = metrics.shoulderWidthCm,
                garmentSpecCm = (shoulderGarment * 10f).roundToInt() / 10f,
                easeAllowanceCm = (shoulderEase * 10f).roundToInt() / 10f,
                tensionPercent = shoulderTension,
                statusBadge = classifyTensionBadge(shoulderTension),
                alterationNoteKo = if (shoulderEase < 1.8f) "암홀 당김 주의 · 어깨 패드 0.5cm 확장 권장" else "정교한 세미 드롭숄더 각도 유지"
            ),
            ZoneFitSpec(
                zoneNameKo = "체스트 볼륨 (Chest)",
                bodyMeasureCm = metrics.chestCm,
                garmentSpecCm = (chestGarment * 10f).roundToInt() / 10f,
                easeAllowanceCm = (chestEase * 10f).roundToInt() / 10f,
                tensionPercent = chestTension,
                statusBadge = classifyTensionBadge(chestTension),
                alterationNoteKo = if (outer != null) "이너+아우터 이중 레이어드 여유분 확보 완료" else "단품 착용 시 자연스러운 드레이프 형성"
            ),
            ZoneFitSpec(
                zoneNameKo = "웨이스트 라인 (Waist)",
                bodyMeasureCm = metrics.waistCm,
                garmentSpecCm = (waistGarment * 10f).roundToInt() / 10f,
                easeAllowanceCm = (waistEase * 10f).roundToInt() / 10f,
                tensionPercent = waistTension,
                statusBadge = classifyTensionBadge(waistTension),
                alterationNoteKo = "벨트 루프 기준 +${(waistEase * 10f).roundToInt() / 10f}cm 쾌적한 활동성 확보"
            ),
            ZoneFitSpec(
                zoneNameKo = "힙 & 크로치 (Hip/Rise)",
                bodyMeasureCm = metrics.hipCm,
                garmentSpecCm = (hipGarment * 10f).roundToInt() / 10f,
                easeAllowanceCm = (hipEase * 10f).roundToInt() / 10f,
                tensionPercent = hipTension,
                statusBadge = classifyTensionBadge(hipTension),
                alterationNoteKo = if (isWideBottom) "와이드 스트레이트 통으로 하체 라인 완벽 보정" else "테이퍼드 핏 표준 여유분 부합"
            )
        )

        val recommendedSize = when {
            metrics.heightCm >= 180f || metrics.shoulderWidthCm >= 47f -> "KR 105 (XL · B2B Tailor Long)"
            metrics.heightCm >= 172f || metrics.shoulderWidthCm >= 43f -> "KR 100 (L · Standard Regular)"
            metrics.heightCm >= 164f -> "KR 95 (M · Contemporary Slim)"
            else -> "KR 90 (S · Petite Atelier)"
        }

        val avgDeviation = zones.map { abs(it.tensionPercent - 56) }.average().toInt()
        val overallScore = (98 - avgDeviation).coerceIn(82, 99)
        val grade = when {
            overallScore >= 94 -> "S+ (Bespoke Master Fit)"
            overallScore >= 89 -> "A+ (Ready-to-Wear Optimal)"
            else -> "B+ (Minor Alteration Advised)"
        }

        return TailoringFitReport(
            recommendedKrSize = recommendedSize,
            overallFitGrade = grade,
            overallEaseScore = overallScore,
            zoneSpecs = zones,
            masterTailorSummaryKo = "CAD 패턴 시뮬레이션 결과 권장 사이즈는 [$recommendedSize]이며, 4대 부위 평균 드레이프 적합도는 ${overallScore}%($grade)입니다."
        )
    }

    private fun classifyTensionBadge(tension: Int): String = when {
        tension in 42..68 -> "OPTIMAL DRAPE"
        tension < 42 -> "RELAXED OVERFIT"
        else -> "TIGHT TENSION"
    }
}

// ============================================================================
// B2B PRO ENGINE 2: Enterprise Inventory Audit, ESG Carbon & CSV Exporter
// ============================================================================

data class EnterpriseAuditReport(
    val totalSkuCount: Int,
    val totalAcquisitionCostKrw: Long,
    val estimatedNetAssetValueKrw: Long,
    val activeUtilizationRatePercent: Int,
    val averagePortfolioCpwKrw: Long,
    val carbonSavedKgCo2e: Float,
    val waterSavedLiters: Int,
    val esgGrade: String,
    val csvPayload: String
)

object EnterpriseB2BAnalyzer {
    fun generateEnterpriseAudit(garments: List<Garment>): EnterpriseAuditReport {
        if (garments.isEmpty()) {
            return EnterpriseAuditReport(
                totalSkuCount = 0,
                totalAcquisitionCostKrw = 0L,
                estimatedNetAssetValueKrw = 0L,
                activeUtilizationRatePercent = 0,
                averagePortfolioCpwKrw = 0L,
                carbonSavedKgCo2e = 0f,
                waterSavedLiters = 0,
                esgGrade = "N/A (재고 없음)",
                csvPayload = "SKU_ID,BRAND,NAME,CATEGORY,COLOR,PRICE_KRW,WEAR_COUNT,CPW_KRW,RESALE_NAV_KRW"
            )
        }

        val totalCost = garments.sumOf { it.purchasePrice }
        val totalNav = garments.sumOf { g ->
            val retention = (0.62 - (g.wearCount.coerceAtMost(30L) * 0.012)).coerceIn(0.20, 0.62)
            ((g.purchasePrice * retention) / 1000.0).toLong() * 1000L
        }
        val activeCount = garments.count { it.wearCount >= 2L }
        val utilization = ((activeCount.toFloat() / garments.size.toFloat()) * 100f).roundToInt()
        val avgCpw = garments.sumOf { it.costPerWear } / garments.size

        // ESG metric: Every wear beyond 5 wears offsets fast-fashion replacement (~1.45 kg CO2e & 180L water)
        val totalWears = garments.sumOf { it.wearCount }
        val carbonSaved = ((totalWears * 1.45f) * 10f).roundToInt() / 10f
        val waterSaved = (totalWears * 180L).toInt()
        val esgGrade = when {
            utilization >= 75 && avgCpw <= 18_000L -> "AAA (ESG 최우수 순환 옷장)"
            utilization >= 50 -> "AA (우수 지속가능 포트폴리오)"
            else -> "A (방치 재고 리밸런싱 권장)"
        }

        val header = "SKU_ID,BRAND,NAME,CATEGORY,SUB_CATEGORY,COLOR,PRICE_KRW,WEAR_COUNT,CPW_KRW,RESALE_NAV_KRW"
        val rows = garments.mapIndexed { idx, g ->
            val sku = generateSkuCode(g, idx)
            val retention = (0.62 - (g.wearCount.coerceAtMost(30L) * 0.012)).coerceIn(0.20, 0.62)
            val nav = ((g.purchasePrice * retention) / 1000.0).toLong() * 1000L
            "$sku,${g.brand.replace(",", " ")},${g.name.replace(",", " ")},${g.category},${g.subCategory},${g.colorName},${g.purchasePrice},${g.wearCount},${g.costPerWear},$nav"
        }

        return EnterpriseAuditReport(
            totalSkuCount = garments.size,
            totalAcquisitionCostKrw = totalCost,
            estimatedNetAssetValueKrw = totalNav,
            activeUtilizationRatePercent = utilization,
            averagePortfolioCpwKrw = avgCpw,
            carbonSavedKgCo2e = carbonSaved,
            waterSavedLiters = waterSaved,
            esgGrade = esgGrade,
            csvPayload = (listOf(header) + rows).joinToString("\n")
        )
    }

    fun generateSkuCode(garment: Garment, index: Int = 0): String {
        val catPrefix = garment.category.take(3).uppercase()
        val colorCode = garment.colorHex.removePrefix("#").take(4).uppercase()
        val idSuffix = garment.id.takeLast(3).ifBlank { String.format("%03d", index + 1) }.uppercase()
        return "SC-$catPrefix-$colorCode-$idSuffix"
    }
}

// ============================================================================
// B2B PRO UI 1: Enterprise Telemetry & Darkroom Command Bar (전역 상단 바)
// ============================================================================

@Composable
fun EnterpriseProCommandBar(
    isProEnterpriseMode: Boolean,
    isProDarkMode: Boolean,
    skuCount: Int,
    weatherSummary: String,
    onToggleEnterpriseMode: () -> Unit,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isProDarkMode) Color(0xFF0B0E14) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isProDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .testTag("enterprise_pro_command_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isProEnterpriseMode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (isProDarkMode) {
                        Color.White.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (!isProDarkMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier
                        .clickable { onToggleEnterpriseMode() }
                        .testTag("toggle_b2b_pro_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isProEnterpriseMode) {
                                MaterialTheme.colorScheme.primary
                            } else if (isProDarkMode) {
                                AtelierChampagne
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isProEnterpriseMode) "B2B PRO SUITE ON" else "STANDARD MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProEnterpriseMode) {
                                MaterialTheme.colorScheme.primary
                            } else if (isProDarkMode) {
                                AtelierChampagne
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Text(
                    text = "SKU:${String.format("%02d", skuCount)} · SLA 99.9% · $weatherSummary",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProDarkMode) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isProDarkMode) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                border = if (!isProDarkMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier
                    .clickable { onToggleDarkMode() }
                    .testTag("toggle_darkroom_theme_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isProDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "다크룸 테마 전환",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isProDarkMode) "DAYLIGHT" else "DARKROOM",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================================
// B2B PRO UI 2: CAD Tailoring Fit & Ease Allowance Inspector (3D 피팅 탭)
// ============================================================================

@Composable
fun B2BTailoringFitSpecCard(
    metrics: AvatarBodyMetrics,
    outer: Garment?,
    top: Garment?,
    bottom: Garment?,
    garmentScale: Float
) {
    val report = remember(metrics, outer, top, bottom, garmentScale) {
        TailoringFitSpecEngine.evaluateCadFitSpec(
            metrics = metrics,
            outer = outer,
            top = top,
            bottom = bottom,
            fitScaleMultiplier = garmentScale
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("b2b_tailoring_fit_spec_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "📐 [B2B CAD] 테일러링 여유분(Ease) & 텐션 스펙 인스펙터",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "권장 호칭: ${report.recommendedKrSize}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = report.overallFitGrade,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = report.masterTailorSummaryKo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            report.zoneSpecs.forEach { zone ->
                val barColor = when (zone.statusBadge) {
                    "OPTIMAL DRAPE" -> AtelierSage
                    "RELAXED OVERFIT" -> MaterialTheme.colorScheme.primary
                    else -> AtelierCoral
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zone.zoneNameKo,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "신체 ${zone.bodyMeasureCm}cm → 패턴 ${zone.garmentSpecCm}cm (여유분 +${zone.easeAllowanceCm}cm)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { zone.tensionPercent / 100f },
                            color = barColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = zone.alterationNoteKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${zone.statusBadge} (${zone.tensionPercent}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = barColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// B2B PRO UI 3: Client Lookbook Spec Sheet & QR Delivery Card (TPO 추천 탭)
// ============================================================================

@Composable
fun B2BLookbookSpecSheetCard(
    profile: UserProfile,
    weather: WeatherContext,
    selectedItems: List<Garment>,
    tpoLabel: String
) {
    val context = LocalContext.current
    var copiedBanner by remember { mutableStateOf<String?>(null) }
    val docNumber = remember(profile.userId, selectedItems.size, tpoLabel) {
        val hash = abs((profile.displayName + tpoLabel + selectedItems.size).hashCode() % 9000) + 1000
        "LB-2026-$hash"
    }

    val totalLookPrice = remember(selectedItems) { selectedItems.sumOf { it.purchasePrice } }
    val avgLookCpw = remember(selectedItems) {
        if (selectedItems.isEmpty()) 0L else selectedItems.sumOf { it.costPerWear } / selectedItems.size
    }

    val specSheetText = remember(docNumber, profile, weather, selectedItems, tpoLabel) {
        buildString {
            appendLine("==========================================")
            appendLine("[SMART COORDI PRO · B2B CLIENT LOOKBOOK SPEC]")
            appendLine("문서번호: $docNumber | 클라이언트: ${profile.displayName} (${profile.heightCm}cm/${profile.weightKg}kg)")
            appendLine("TPO 세션: #$tpoLabel | 기상 기준: ${weather.summaryText}")
            appendLine("------------------------------------------")
            selectedItems.forEachIndexed { idx, g ->
                val sku = EnterpriseB2BAnalyzer.generateSkuCode(g, idx)
                appendLine("${idx + 1}. [$sku] ${g.brand} ${g.name} (${g.colorName}/${g.subCategory}) - ₩${String.format("%,d", g.purchasePrice)} (CPW ₩${String.format("%,d", g.costPerWear)})")
            }
            appendLine("------------------------------------------")
            appendLine("총 착장 자산가액: ₩${String.format("%,d", totalLookPrice)} | 평균 1회 착용단가: ₩${String.format("%,d", avgLookCpw)}")
            appendLine("==========================================")
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("b2b_lookbook_spec_sheet_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "📋 [B2B 쇼룸] VIP 클라이언트 룩북 스펙 시트 & QR 발행",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "DOC NO: $docNumber · 백화점 퍼스널 쇼퍼 & 스타일 컨설팅 표준 양식",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CLIENT: ${profile.displayName} (${profile.heightCm}cm · ${profile.bodyShape})",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        selectedItems.take(4).forEachIndexed { idx, g ->
                            val sku = EnterpriseB2BAnalyzer.generateSkuCode(g, idx)
                            Text(
                                text = "• $sku | ${g.name} (${g.colorName})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "총 견적 ₩${String.format("%,d", totalLookPrice)} · 평균 CPW ₩${String.format("%,d", avgLookCpw)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Deterministic 2D QR Matrix Canvas for Showroom Handoff
                    DeterministicQrMatrixCanvas(
                        seedText = docNumber,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            if (copiedBanner != null) {
                Text(
                    text = copiedBanner!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = AtelierSage,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("B2B Lookbook Spec", specSheetText))
                        copiedBanner = "✅ 문서번호 [$docNumber] B2B 클라이언트 제안서 스펙 시트가 클립보드에 복사되었습니다."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_b2b_lookbook_spec_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("스펙 시트 복사", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "[Smart Coordi PRO] $docNumber 룩북 제안서")
                                putExtra(Intent.EXTRA_TEXT, specSheetText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "클라이언트 룩북 공유"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("외부 메신저 발송", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DeterministicQrMatrixCanvas(
    seedText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AtelierMidnight.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.padding(6.dp)) {
            val grid = 9
            val cellW = size.width / grid
            val cellH = size.height / grid
            val seed = seedText.hashCode()
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val isCornerFinder = (r in 0..2 && c in 0..2) ||
                        (r in 0..2 && c in (grid - 3) until grid) ||
                        (r in (grid - 3) until grid && c in 0..2)
                    val bit = ((seed shr ((r * grid + c) % 28)) and 1) == 1
                    if (isCornerFinder || bit) {
                        drawRect(
                            color = Color(0xFF11151C),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.92f, cellH * 0.92f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// B2B PRO UI 4: Enterprise Inventory Audit, ESG Carbon & CSV ERP Exporter (가성비 분석 탭)
// ============================================================================

@Composable
fun B2BInventoryAuditAndEsgCard(
    garments: List<Garment>
) {
    val context = LocalContext.current
    val audit = remember(garments) {
        EnterpriseB2BAnalyzer.generateEnterpriseAudit(garments)
    }
    var showSkuTable by remember { mutableStateOf(false) }
    var exportNotice by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("b2b_inventory_audit_esg_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "🏢 [B2B ERP 감사] 의류 자산 회전율 · ESG 탄소절감 & CSV 익스포터",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "등급: ${audit.esgGrade}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = AtelierSage,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                FilterChip(
                    selected = showSkuTable,
                    onClick = { showSkuTable = !showSkuTable },
                    label = { Text(if (showSkuTable) "요약 보기" else "SKU 원장") },
                    modifier = Modifier.testTag("toggle_sku_ledger_chip")
                )
            }

            // 4 Enterprise KPI blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditMetricTile(
                    title = "재고 가동률",
                    value = "${audit.activeUtilizationRatePercent}%",
                    sub = "총 ${audit.totalSkuCount} SKU",
                    modifier = Modifier.weight(1f)
                )
                AuditMetricTile(
                    title = "순자산가치(NAV)",
                    value = "₩${String.format("%,d", audit.estimatedNetAssetValueKrw / 1000)}K",
                    sub = "감가상각 반영",
                    modifier = Modifier.weight(1f)
                )
                AuditMetricTile(
                    title = "탄소 절감(ESG)",
                    value = "-${audit.carbonSavedKgCo2e}kg",
                    sub = "물 ${audit.waterSavedLiters}L 절약",
                    modifier = Modifier.weight(1f)
                )
            }

            if (showSkuTable && garments.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SKU_CODE       | CATEGORY | ITEM_NAME          | PRICE     | WEARS | CPW",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()
                        garments.forEachIndexed { idx, g ->
                            val sku = EnterpriseB2BAnalyzer.generateSkuCode(g, idx)
                            Text(
                                text = "$sku | ${g.category.padEnd(8)} | ${g.name.take(10).padEnd(10)} | ₩${String.format("%,d", g.purchasePrice).padStart(7)} | ${String.format("%3d", g.wearCount)}회 | ₩${String.format("%,d", g.costPerWear)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (exportNotice != null) {
                Text(
                    text = exportNotice!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = AtelierSage,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Ateli_ERP_CSV", audit.csvPayload))
                        exportNotice = "📊 ERP/엑셀 호환 CSV 원장 데이터(${audit.totalSkuCount}건 SKU)가 클립보드에 복사되었습니다."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_b2b_csv_button")
                ) {
                    Text("ERP 호환 CSV 내보내기", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Ateli_PRO_Inventory_Audit.csv")
                                putExtra(Intent.EXTRA_TEXT, audit.csvPayload)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "CSV 감사 리포트 전송"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ESG 감사 리포트 공유", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun AuditMetricTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
