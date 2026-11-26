package com.example

import com.example.data.model.BodyShapeType
import com.example.data.model.CuratedWardrobePresets
import com.example.data.model.Garment
import com.example.data.model.SavedOutfit
import com.example.data.model.UserProfile
import com.example.data.remote.WeatherContext
import com.example.data.remote.WeatherWardrobeMatcher
import com.example.ui.components.AclosetWardrobeAnalyzer
import com.example.ui.components.AvatarBodyMetrics
import com.example.ui.components.ColorHarmonyAnalyzer
import com.example.ui.components.EnterpriseB2BAnalyzer
import com.example.ui.components.StylebookCapsuleBuilder
import com.example.ui.components.TailoringFitSpecEngine
import com.example.ui.components.WardrobeCompatibilityAdvisor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deep verification unit test suite covering:
 * 1. Parametric Avatar Body Metrics & AI Auto Body-Shape Classification
 * 2. Multi-Factor Weather-to-Wardrobe Suitability Scoring & Category Ranking
 * 3. Garment Wear Utilization, Closet Sleeper Detection, and Firestore Serialization
 * 4. 4-Stage Industry Benchmark Engines (Acloset, Whering, Stylebook, Wardrobe Gap Matching)
 */
class AteliDeepVerificationTest {

    @Test
    fun avatarBodyMetrics_autoClassifyBodyShape_correctlyIdentifiesAllFiveTypes() {
        // 1. Inverted Triangle: Broad shoulders relative to hips and slim waist
        val invertedTriangle = AvatarBodyMetrics.autoClassifyBodyShape(
            shoulderWidthCm = 49f,
            chestCm = 104f,
            waistCm = 74f,
            hipCm = 92f,
            bmi = 22.0f
        )
        assertEquals(BodyShapeType.INVERTED_TRIANGLE, invertedTriangle)

        // 2. Hourglass: Defined narrow waist relative to balanced chest and hips
        val hourglass = AvatarBodyMetrics.autoClassifyBodyShape(
            shoulderWidthCm = 41f,
            chestCm = 92f,
            waistCm = 64f,
            hipCm = 94f,
            bmi = 20.5f
        )
        assertEquals(BodyShapeType.HOURGLASS, hourglass)

        // 3. Pear: Narrower shoulders and fuller hips
        val pear = AvatarBodyMetrics.autoClassifyBodyShape(
            shoulderWidthCm = 37f,
            chestCm = 84f,
            waistCm = 68f,
            hipCm = 98f,
            bmi = 21.0f
        )
        assertEquals(BodyShapeType.PEAR, pear)

        // 4. Oval: High waist-to-hip ratio or high BMI
        val oval = AvatarBodyMetrics.autoClassifyBodyShape(
            shoulderWidthCm = 44f,
            chestCm = 102f,
            waistCm = 96f,
            hipCm = 99f,
            bmi = 27.0f
        )
        assertEquals(BodyShapeType.OVAL, oval)

        // 5. Rectangle: Balanced straight silhouette
        val rectangle = AvatarBodyMetrics.autoClassifyBodyShape(
            shoulderWidthCm = 43f,
            chestCm = 92f,
            waistCm = 77f,
            hipCm = 93f,
            bmi = 21.5f
        )
        assertEquals(BodyShapeType.RECTANGLE, rectangle)
    }

    @Test
    fun avatarBodyMetrics_parametricScalesAndBmi_areWithinValidBounds() {
        val profile = UserProfile(
            displayName = "테스트 디렉터",
            heightCm = 178L,
            weightKg = 70L,
            shoulderWidthCm = 47L,
            waistCm = 76L,
            bodyShape = "INVERTED_TRIANGLE",
            skinToneHex = "#F3D6C1"
        )
        val metrics = AvatarBodyMetrics.fromUserProfile(profile)

        assertTrue(metrics.bmi in 21.0f..23.0f)
        assertEquals("스탠다드 (표준)", metrics.bmiCategoryKo)
        assertTrue(metrics.normalizedShoulderScale > 1.0f)
        assertTrue(metrics.normalizedWaistScale in 0.72f..1.35f)
        assertTrue(metrics.normalizedHipScale in 0.82f..1.32f)
        assertTrue(metrics.waistInches in 29.0f..31.0f)
    }

    @Test
    fun weatherWardrobeMatcher_rainyWeather_prioritizesWaterResistantItems() {
        val starters = CuratedWardrobePresets.starterWardrobeItems().mapIndexed { idx, g ->
            g.copy(id = "item_$idx")
        }
        val rainyWeather = WeatherContext(
            cityId = "Seoul",
            cityLabelKo = "현위치 (서울 중구)",
            isCurrentGpsLocation = true,
            temperatureC = 15.0,
            apparentTempC = 14.0,
            minTempC = 11.0,
            maxTempC = 18.0,
            humidityPercent = 88,
            rainProbabilityPercent = 85,
            windSpeedKmh = 16.0,
            conditionLabelKo = "비 내림",
            isRainyOrSnowy = true,
            recommendedThickness = listOf("LIGHT", "MEDIUM"),
            recommendedSeason = "SPRING_FALL"
        )

        val summary = WeatherWardrobeMatcher.evaluateWardrobeForWeather(starters, rainyWeather)

        assertNotNull(summary.bestOuter)
        assertTrue("Best outer in rain must be water-resistant", summary.bestOuter!!.garment.waterResistant)
        assertNotNull(summary.bestShoes)
        assertTrue("Best shoes in rain must be water-resistant", summary.bestShoes!!.garment.waterResistant)
        assertTrue(summary.suitableGarments.isNotEmpty())
        assertTrue(summary.bestOuter!!.suitabilityScore >= 85)
    }

    @Test
    fun weatherWardrobeMatcher_hotSummerWeather_skipsOuterLayerAndPrefersLightTops() {
        val starters = CuratedWardrobePresets.starterWardrobeItems().mapIndexed { idx, g ->
            g.copy(id = "item_$idx")
        }
        val hotWeather = WeatherContext(
            cityId = "Busan",
            cityLabelKo = "부산",
            temperatureC = 28.0,
            apparentTempC = 29.5,
            minTempC = 24.0,
            maxTempC = 30.0,
            humidityPercent = 70,
            rainProbabilityPercent = 10,
            windSpeedKmh = 6.0,
            conditionLabelKo = "쾌청함",
            isRainyOrSnowy = false,
            recommendedThickness = listOf("LIGHT"),
            recommendedSeason = "SUMMER"
        )

        assertFalse("Hot weather with low temp swing should not require an outer layer", hotWeather.needsOuterLayer)
        val summary = WeatherWardrobeMatcher.evaluateWardrobeForWeather(starters, hotWeather)
        assertNull("bestOuter should be null when needsOuterLayer is false", summary.bestOuter)
        assertNotNull(summary.bestTop)
        assertEquals("LIGHT", summary.bestTop!!.garment.thickness)
    }

    @Test
    fun garmentUtilization_andFirestoreSerialization_meetSchemaRules() {
        val unworn = Garment(
            id = "g_unworn",
            name = "에어리 바람막이",
            brand = "아크테릭스",
            category = "OUTER",
            wearCount = 0L
        )
        assertTrue(unworn.isUnwornClosetSleeper)

        val worn16 = unworn.copy(wearCount = 16L)
        assertFalse(worn16.isUnwornClosetSleeper)

        val map = worn16.toCreateMap("uid_123")
        assertEquals("uid_123", map["userId"])
        assertEquals("OUTER", map["category"])
        assertEquals(16L, map["wearCount"])
        assertFalse(map.containsKey("purchasePrice"))

        val outfitMap = SavedOutfit(
            title = "출근 베스트 코디",
            tpoOccasion = "출근",
            garmentIds = listOf("g1", "g2"),
            fitScore = 96L
        ).toCreateMap("uid_123")
        assertEquals("uid_123", outfitMap["userId"])
        assertEquals(96L, outfitMap["fitScore"])
    }

    @Test
    fun fourStageBenchmarkEngines_acloset_whering_stylebook_stitchFix_verifyCalculations() {
        val starters = CuratedWardrobePresets.starterWardrobeItems().mapIndexed { idx, g ->
            g.copy(id = "bench_$idx")
        }
        val target = starters.first()

        // 1. Acloset Garment Styling & Cross-Coordination Match
        val stylingAnalysis = AclosetWardrobeAnalyzer.analyzeGarmentStyling(target, starters)
        assertTrue(stylingAnalysis.progressRatio in 0f..1f)
        assertTrue(stylingAnalysis.compatibleItems.isNotEmpty())

        // 2. Whering 6-Formula Color Harmony & Body Shape Silhouette Balance
        val outer = starters.firstOrNull { it.category == "OUTER" }
        val top = starters.firstOrNull { it.category == "TOP" }
        val bottom = starters.firstOrNull { it.category == "BOTTOM" }
        val shoes = starters.firstOrNull { it.category == "SHOES" }
        val harmony = ColorHarmonyAnalyzer.evaluateOutfitHarmony(
            outer = outer,
            top = top,
            bottom = bottom,
            shoes = shoes,
            bodyShape = BodyShapeType.INVERTED_TRIANGLE
        )
        assertTrue(harmony.totalHarmonyScore in 80..99)
        assertTrue(harmony.harmonyTypeKo.isNotBlank())
        assertTrue(harmony.silhouetteFormulaKo.isNotBlank())

        // 3. Stylebook Smart Packing Capsule Builder
        val weather = WeatherContext()
        val capsule = StylebookCapsuleBuilder.buildPackingCapsule(
            garments = starters,
            weather = weather,
            tripDays = 3,
            tripPurpose = "비즈니스 출장"
        )
        assertTrue(capsule.packedItems.isNotEmpty())
        assertTrue(capsule.possibleOutfitCombinationsCount >= 2)
        assertTrue(capsule.estimatedTotalWeightKg > 0f)
        assertEquals(3, capsule.dailySchedulePreview.size)

        // 4. Wardrobe Gap & Compatibility Simulator
        val candidate = WardrobeCompatibilityAdvisor.presetCandidates.first()
        val verdict = WardrobeCompatibilityAdvisor.evaluateStyleCandidate(candidate, starters)
        assertTrue(verdict.synergyScore in 35..98)
        assertTrue(verdict.newOutfitCombinationsCreated > 0)
    }

    @Test
    fun b2bEnterpriseProSuite_cadFitSpecAndEsgAudit_verifyCalculations() {
        val starters = CuratedWardrobePresets.starterWardrobeItems().mapIndexed { idx, g ->
            g.copy(id = "sku_$idx")
        }
        val metrics = AvatarBodyMetrics.fromUserProfile(UserProfile(heightCm = 178L, weightKg = 71L))
        val cadReport = TailoringFitSpecEngine.evaluateCadFitSpec(
            metrics = metrics,
            outer = starters.firstOrNull { it.category == "OUTER" },
            top = starters.firstOrNull { it.category == "TOP" },
            bottom = starters.firstOrNull { it.category == "BOTTOM" },
            fitScaleMultiplier = 1.0f
        )
        assertEquals(4, cadReport.zoneSpecs.size)
        assertTrue(cadReport.overallEaseScore in 82..99)
        assertTrue(cadReport.recommendedKrSize.contains("KR"))

        val audit = EnterpriseB2BAnalyzer.generateEnterpriseAudit(starters)
        assertEquals(starters.size, audit.totalSkuCount)
        assertTrue(audit.totalWearCount > 0L)
        assertTrue(audit.carbonSavedKgCo2e > 0f)
        assertTrue(audit.csvPayload.contains("SKU_ID,BRAND,NAME"))

        // Verify 3D Mannequin Overlay Rig Calculator (Yaw perspective foreshortening & anatomical anchors)
        val rigFront = com.example.ui.components.Overlay3DRigCalculator.computeRig(metrics, yawAngleDeg = 0f)
        val rigQuarter = com.example.ui.components.Overlay3DRigCalculator.computeRig(metrics, yawAngleDeg = 35f)
        assertEquals(1.0f, rigFront.perspectiveCos, 0.01f)
        assertTrue("Quarter-turn view must foreshorten width", rigQuarter.topWidthScale < rigFront.topWidthScale)
        assertTrue("Quarter-turn view must apply positive parallax shift", rigQuarter.outerAnchorOffsetDp.x > 0f)

        // Verify Local Room Fitting History Entity Creation & Preview Reconstruction
        val outer = starters.firstOrNull { it.category == "OUTER" }
        val top = starters.firstOrNull { it.category == "TOP" }
        val bottom = starters.firstOrNull { it.category == "BOTTOM" }
        val shoes = starters.firstOrNull { it.category == "SHOES" }
        val historyEntity = com.example.data.local.LocalFittingHistoryRepository.createEntityFromSelection(
            outer = outer,
            top = top,
            bottom = bottom,
            shoes = shoes,
            metrics = metrics,
            canvasModeLabel = "3D 오버레이 피팅",
            triggerSourceLabel = "🎰 슬롯머신 스핀",
            garmentScale = 1.05f
        )
        assertTrue(historyEntity.combinationSignature.isNotBlank())
        assertTrue(historyEntity.harmonyScore in 80..99)
        assertEquals(4, historyEntity.toPreviewGarments().size)
    }
}
