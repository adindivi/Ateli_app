package com.example.ui.components

import android.graphics.Color as AndroidColor
import com.example.data.model.BodyShapeType
import com.example.data.model.Garment
import com.example.data.remote.WeatherContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ============================================================================
// 1. Acloset 벤치마킹 엔진: 의류 착용 활용도 & 크로스 코디 매칭 분석
// ============================================================================

data class GarmentStylingAnalysis(
    val wearCount: Long,
    val targetSignatureWears: Long,
    val remainingWearsToSignature: Long,
    val progressRatio: Float,
    val closetTierLabel: String,
    val compatibleItems: List<Garment>
)

object AclosetWardrobeAnalyzer {
    private const val SIGNATURE_TARGET_WEARS = 15L

    fun analyzeGarmentStyling(
        target: Garment,
        allGarments: List<Garment>,
        targetSignatureWears: Long = SIGNATURE_TARGET_WEARS
    ): GarmentStylingAnalysis {
        val safeTargetWears = targetSignatureWears.coerceAtLeast(5L)
        val remaining = (safeTargetWears - target.wearCount).coerceAtLeast(0L)
        val progress = (target.wearCount.toFloat() / safeTargetWears.toFloat()).coerceIn(0f, 1f)

        val tierLabel = when {
            target.wearCount >= safeTargetWears -> "🏆 시그니처 주력템"
            progress >= 0.45f -> "🔥 자주 입는 데일리"
            target.wearCount in 1L..2L -> "🌱 워밍업 아이템 (착용 권장)"
            else -> "💤 잠자는 방치템 (코디 추천)"
        }

        val complementaryCategories = when (target.category) {
            "OUTER" -> listOf("TOP", "BOTTOM", "SHOES")
            "TOP" -> listOf("BOTTOM", "OUTER", "SHOES")
            "BOTTOM" -> listOf("TOP", "OUTER", "SHOES")
            "SHOES" -> listOf("BOTTOM", "TOP")
            else -> listOf("TOP", "BOTTOM")
        }

        val compatible = allGarments
            .filter { it.id != target.id && it.category in complementaryCategories }
            .sortedByDescending { candidate ->
                var score = 0
                if (candidate.season == target.season || candidate.season == "ALL" || target.season == "ALL") {
                    score += 30
                }
                val sharedTpo = candidate.tpoTags.intersect(target.tpoTags.toSet()).size
                score += sharedTpo * 20
                score += ColorHarmonyAnalyzer.pairCompatibilityScore(target.colorHex, candidate.colorHex)
                score
            }
            .take(4)

        return GarmentStylingAnalysis(
            wearCount = target.wearCount,
            targetSignatureWears = safeTargetWears,
            remainingWearsToSignature = remaining,
            progressRatio = progress,
            closetTierLabel = tierLabel,
            compatibleItems = compatible
        )
    }
}

// ============================================================================
// 2. Whering (웨어링) & Outfitly 심층 엔진: 6대 색채학 하모니 & 실루엣 밸런스
// ============================================================================

data class OutfitHarmonyEvaluation(
    val totalHarmonyScore: Int,
    val colorScore: Int,
    val silhouetteScore: Int,
    val harmonyTypeKo: String,
    val silhouetteFormulaKo: String,
    val styleFeedbackKo: String,
    val paletteHexes: List<String>
)

object ColorHarmonyAnalyzer {
    fun evaluateOutfitHarmony(
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        bodyShape: BodyShapeType
    ): OutfitHarmonyEvaluation {
        val items = listOfNotNull(outer, top, bottom, shoes)
        if (items.size < 2) {
            return OutfitHarmonyEvaluation(
                totalHarmonyScore = 82,
                colorScore = 80,
                silhouetteScore = 84,
                harmonyTypeKo = "기본 단일 스타일링",
                silhouetteFormulaKo = "상·하의 매칭 대기",
                styleFeedbackKo = "상·하의와 신발을 모두 선택하면 6대 색채학 공식과 체형 실루엣 밸런스를 정밀 분석합니다.",
                paletteHexes = items.map { it.colorHex }
            )
        }

        val hsvs = items.map { hexToHsv(it.colorHex) }
        val avgSaturation = hsvs.map { it[1] }.average().toFloat()
        val maxValDiff = (hsvs.maxOf { it[2] } - hsvs.minOf { it[2] })

        val topHsv = top?.let { hexToHsv(it.colorHex) } ?: floatArrayOf(0f, 0.1f, 0.7f)
        val bottomHsv = bottom?.let { hexToHsv(it.colorHex) } ?: floatArrayOf(0f, 0.1f, 0.3f)
        val hueDiff = hueDistanceDegrees(topHsv[0], bottomHsv[0])

        val (harmonyType, colorScore, advice) = when {
            avgSaturation < 0.18f -> Triple(
                "모노크롬 미니멀 시크 (Monochrome)",
                96,
                "무채색 계열의 절제된 채도 조합으로 실패 없는 하이엔드 미니멀 룩을 완성했습니다."
            )
            hueDiff <= 32f && maxValDiff >= 0.14f -> Triple(
                "아틀리에 톤온톤 그라데이션 (Tone-on-Tone)",
                97,
                "동일 색상 계열 안에서 명도 차이를 준 고급스러운 톤온톤 레이어링입니다."
            )
            hueDiff in 135f..180f && avgSaturation in 0.20f..0.65f -> Triple(
                "세련된 보색 포인트 (Complementary Accent)",
                94,
                "색상환 반대편의 보색 대비를 차분한 채도로 중화해 감각적인 포인트를 주었습니다."
            )
            hueDiff in 25f..65f && avgSaturation < 0.50f -> Triple(
                "유사색 내추럴 하모니 (Analogous)",
                95,
                "인접한 색상환이 자연스럽게 이어져 시각적으로 편안하고 우아한 인상을 줍니다."
            )
            maxValDiff >= 0.42f -> Triple(
                "클래식 하이 콘트라스트 (High Contrast)",
                93,
                "상·하의의 뚜렷한 명도 대비가 시선을 깔끔하게 분리해 비율을 또렷하게 살려줍니다."
            )
            else -> Triple(
                "톤인톤 모던 밸런스 (Tone-in-Tone)",
                91,
                "서로 다른 색상이 균일한 톤(채도·명도) 안에서 조화롭게 어우러집니다."
            )
        }

        // Silhouette & Body Shape Balance Calculation
        val topVal = topHsv[2]
        val bottomVal = bottomHsv[2]
        val isWideBottom = bottom?.subCategory?.contains("와이드") == true || bottom?.name?.contains("와이드") == true
        val isStructuredOuter = outer != null

        val (silhouetteFormula, silScore) = when (bodyShape) {
            BodyShapeType.INVERTED_TRIANGLE -> {
                if (isWideBottom || topVal <= bottomVal) {
                    "상체 차분한 톤 + 하체 와이드 밸런스 (역삼각형 최적)" to 97
                } else {
                    "어깨 프레임 강조 실루엣 (와이드 팬츠 매치 권장)" to 89
                }
            }
            BodyShapeType.PEAR -> {
                if (topVal >= bottomVal || isStructuredOuter) {
                    "상체 브라이트/아우터 포인트 + 하체 롱앤린 (어반 피어 최적)" to 97
                } else {
                    "안정적인 하체 중심 실루엣" to 90
                }
            }
            BodyShapeType.HOURGLASS -> {
                "허리 라인 & 상·하체 황금 비율 실루엣" to 96
            }
            BodyShapeType.OVAL -> {
                if (isStructuredOuter) {
                    "아우터 세로 절개선 슬림 핏 (소프트 라운드 최적)" to 96
                } else {
                    "편안한 세미 릴렉스 실루엣" to 91
                }
            }
            BodyShapeType.RECTANGLE -> {
                if (isStructuredOuter || isWideBottom) {
                    "레이어드 입체 볼륨 실루엣 (내추럴 체형 최적)" to 96
                } else {
                    "모던 스트레이트 실루엣" to 92
                }
            }
        }

        val finalScore = ((colorScore * 0.55f) + (silScore * 0.45f)).toInt().coerceIn(80, 99)
        return OutfitHarmonyEvaluation(
            totalHarmonyScore = finalScore,
            colorScore = colorScore,
            silhouetteScore = silScore,
            harmonyTypeKo = harmonyType,
            silhouetteFormulaKo = silhouetteFormula,
            styleFeedbackKo = "$advice · $silhouetteFormula",
            paletteHexes = items.map { it.colorHex }
        )
    }

    fun pairCompatibilityScore(hexA: String, hexB: String): Int {
        val hsvA = hexToHsv(hexA)
        val hsvB = hexToHsv(hexB)
        val satDiff = abs(hsvA[1] - hsvB[1])
        val valDiff = abs(hsvA[2] - hsvB[2])
        return when {
            hsvA[1] < 0.18f || hsvB[1] < 0.18f -> 28
            valDiff in 0.15f..0.55f && satDiff < 0.35f -> 25
            else -> 15
        }
    }

    private fun hueDistanceDegrees(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2) % 360f
        return min(diff, 360f - diff)
    }

    private fun hexToHsv(hex: String): FloatArray {
        val out = FloatArray(3)
        try {
            val cleaned = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            val c = AndroidColor.parseColor(cleaned)
            AndroidColor.colorToHSV(c, out)
        } catch (_: Exception) {
            out[0] = 210f
            out[1] = 0.2f
            out[2] = 0.5f
        }
        return out
    }
}

// ============================================================================
// 3. Stylebook (스타일북) 심층 엔진: 스마트 패킹 매트릭스 & 무게·부피 절감 계산
// ============================================================================

data class PackingCapsulePlan(
    val tripLabel: String,
    val daysCount: Int,
    val packedItems: List<Garment>,
    val possibleOutfitCombinationsCount: Int,
    val estimatedTotalWeightKg: Float,
    val spaceSavedPercent: Int,
    val weatherPackingTip: String,
    val dailySchedulePreview: List<String>
)

object StylebookCapsuleBuilder {
    fun buildPackingCapsule(
        garments: List<Garment>,
        weather: WeatherContext,
        tripDays: Int,
        tripPurpose: String
    ): PackingCapsulePlan {
        val outers = garments.filter { it.category == "OUTER" }
            .sortedByDescending {
                var s = 50
                if (weather.isRainyOrSnowy && it.waterResistant) s += 40
                if (it.thickness in weather.recommendedThickness) s += 25
                s
            }
        val tops = garments.filter { it.category == "TOP" }
            .sortedByDescending { if (it.thickness in weather.recommendedThickness) 80 else 40 }
        val bottoms = garments.filter { it.category == "BOTTOM" }
        val shoes = garments.filter { it.category == "SHOES" }

        val neededOuters = if (weather.needsOuterLayer) outers.take(if (tripDays >= 4) 2 else 1) else emptyList()
        val neededTops = tops.take(tripDays.coerceIn(2, 4))
        val neededBottoms = bottoms.take(max(2, (tripDays + 1) / 2))
        val neededShoes = shoes.take(if (tripDays >= 4) 2 else 1)

        val packed = neededOuters + neededTops + neededBottoms + neededShoes
        val combos = max(1, neededTops.size) * max(1, neededBottoms.size) * max(1, neededOuters.size.coerceAtLeast(1))

        // Estimate luggage weight in kg based on garment category & thickness
        val totalWeightGrams = packed.sumOf { g ->
            val baseG = when (g.category) {
                "OUTER" -> 850
                "BOTTOM" -> 480
                "SHOES" -> 780
                "TOP" -> 320
                else -> 400
            }
            val mult = when (g.thickness) {
                "HEAVY" -> 1.35
                "LIGHT" -> 0.75
                else -> 1.0
            }
            (baseG * mult).toInt()
        }
        val weightKg = (totalWeightGrams / 100f).toInt() / 10f

        // Compare against naive packing (4 items per day)
        val naiveItemCount = tripDays * 3
        val spaceSaved = if (naiveItemCount > packed.size && packed.isNotEmpty()) {
            (((naiveItemCount - packed.size).toFloat() / naiveItemCount.toFloat()) * 100f).toInt().coerceIn(15, 65)
        } else 30

        val weatherTip = when {
            weather.isRainyOrSnowy -> "☔ 현지 우천 예보(${weather.rainProbabilityPercent}%)에 대비해 방수·발수 아우터와 어두운 하의를 우선 배정했습니다."
            weather.dailyTempRangeC >= 8.0 -> "🌡️ 일교차(${weather.dailyTempRangeC.toInt()}°C)가 커서 낮에는 벗고 아침·저녁에 걸치기 좋은 레이어드 캡슐로 구성했습니다."
            else -> "✨ ${weather.cityLabelKo} 기온(${weather.temperatureC.toInt()}°C)에 최적화된 크로스 매칭 캡슐입니다."
        }

        val schedule = (1..tripDays).map { day ->
            val t = neededTops.getOrNull((day - 1) % neededTops.size.coerceAtLeast(1))?.name ?: "기본 상의"
            val b = neededBottoms.getOrNull((day - 1) % neededBottoms.size.coerceAtLeast(1))?.name ?: "기본 하의"
            val s = neededShoes.getOrNull((day - 1) % neededShoes.size.coerceAtLeast(1))?.name ?: "기본 신발"
            val o = neededOuters.getOrNull((day - 1) % neededOuters.size.coerceAtLeast(1))?.name
            if (o != null) "Day $day: $o + $t + $b ($s)" else "Day $day: $t + $b ($s)"
        }

        return PackingCapsulePlan(
            tripLabel = "${tripDays - 1}박 ${tripDays}일 $tripPurpose",
            daysCount = tripDays,
            packedItems = packed,
            possibleOutfitCombinationsCount = combos,
            estimatedTotalWeightKg = weightKg,
            spaceSavedPercent = spaceSaved,
            weatherPackingTip = weatherTip,
            dailySchedulePreview = schedule
        )
    }
}

// ============================================================================
// 4. 옷장 빈자리(Gap) & 새 스타일 아이템 궁합 매칭 엔진
// ============================================================================

data class StyleMatchCandidate(
    val name: String,
    val category: String,
    val colorName: String,
    val colorHex: String
)

data class StyleCompatibilityVerdict(
    val verdictBadge: String,
    val synergyScore: Int, // 0..100
    val wardrobeGapFillScore: Int, // 0..100
    val newOutfitCombinationsCreated: Int,
    val duplicateRiskPercent: Int,
    val topMatchingOwnedGarments: List<Garment>,
    val recommendationSummary: String
)

object WardrobeCompatibilityAdvisor {
    val presetCandidates = listOf(
        StyleMatchCandidate("시어링 블랙 레더 자켓", "OUTER", "클래식 블랙", "#1C1C1E"),
        StyleMatchCandidate("크림 와이드 코튼 팬츠", "BOTTOM", "버터 크림", "#F3E5AB"),
        StyleMatchCandidate("버건디 케이블 가디건", "TOP", "딥 버건디", "#6E2638"),
        StyleMatchCandidate("화이트 미니멀 스니커즈", "SHOES", "화이트", "#F5F5F0")
    )

    fun evaluateStyleCandidate(
        candidate: StyleMatchCandidate,
        ownedGarments: List<Garment>
    ): StyleCompatibilityVerdict {
        val sameCategoryOwned = ownedGarments.filter { it.category == candidate.category }
        val sameColorAndCat = sameCategoryOwned.count {
            it.colorName.contains(candidate.colorName.take(2)) ||
                ColorHarmonyAnalyzer.pairCompatibilityScore(it.colorHex, candidate.colorHex) >= 27
        }

        val duplicateRisk = if (sameCategoryOwned.isEmpty()) 5
        else ((sameColorAndCat.toDouble() / sameCategoryOwned.size.toDouble()) * 80.0).toInt().coerceIn(8, 88)

        // Wardrobe Gap Fill Score: higher if this category or color is underrepresented in the closet
        val categoryShare = if (ownedGarments.isEmpty()) 0f
        else sameCategoryOwned.size.toFloat() / ownedGarments.size.toFloat()
        val gapFillScore = ((1f - categoryShare) * 70f + (100 - duplicateRisk) * 0.3f).toInt().coerceIn(25, 98)

        val complementaryOwned = ownedGarments.filter { it.category != candidate.category }
        val matchingOwned = complementaryOwned.sortedByDescending {
            ColorHarmonyAnalyzer.pairCompatibilityScore(candidate.colorHex, it.colorHex)
        }

        val topsCount = ownedGarments.count { it.category == "TOP" }.coerceAtLeast(1)
        val bottomsCount = ownedGarments.count { it.category == "BOTTOM" }.coerceAtLeast(1)
        val newCombos = when (candidate.category) {
            "OUTER" -> topsCount * bottomsCount
            "TOP" -> bottomsCount * 2
            "BOTTOM" -> topsCount * 2
            else -> topsCount + bottomsCount
        }

        val synergyScore = ((gapFillScore * 0.5f) + ((100 - duplicateRisk) * 0.3f) + (newCombos.coerceAtMost(10) * 2f))
            .toInt()
            .coerceIn(35, 98)

        val verdict = when {
            synergyScore >= 78 && duplicateRisk <= 35 -> "✅ 찰떡 스타일 매치 (옷장 빈자리 해결 · 시너지 ${synergyScore}점)"
            synergyScore >= 60 && duplicateRisk <= 58 -> "💡 포인트 코디 확장 (스타일 확장 · 시너지 ${synergyScore}점)"
            else -> "⚠️ 스타일 중복 주의 (유사 색상·카테고리 보유 · 중복도 ${duplicateRisk}%)"
        }

        val summary = "현재 옷장과 결합 시 즉시 ${newCombos}가지 신규 코디가 탄생하며, 옷장 부족 영역 보완 지수는 ${gapFillScore}점(중복 위험도 ${duplicateRisk}%)입니다."

        return StyleCompatibilityVerdict(
            verdictBadge = verdict,
            synergyScore = synergyScore,
            wardrobeGapFillScore = gapFillScore,
            newOutfitCombinationsCreated = newCombos,
            duplicateRiskPercent = duplicateRisk,
            topMatchingOwnedGarments = matchingOwned.take(3),
            recommendationSummary = summary
        )
    }
}
