package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyShapeType
import com.example.data.model.UserProfile
import com.example.ui.theme.AtelierChampagne
import com.example.ui.theme.AtelierCoral
import com.example.ui.theme.AtelierIvory
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Detailed parametric body measurements used to drive the real-time avatar mannequin renderer.
 */
data class AvatarBodyMetrics(
    val displayName: String = "패션 디렉터",
    val heightCm: Float = 172f,
    val weightKg: Float = 64f,
    val shoulderWidthCm: Float = 44f,
    val chestCm: Float = 94f,
    val waistCm: Float = 74f,
    val hipCm: Float = 94f,
    val inseamCm: Float = 78f,
    val bodyShape: BodyShapeType = BodyShapeType.RECTANGLE,
    val skinToneHex: String = "#F3D6C1"
) {
    val bmi: Float
        get() {
            val hMeters = (heightCm / 100f).coerceAtLeast(1.0f)
            return weightKg / (hMeters * hMeters)
        }

    val bmiCategoryKo: String
        get() = when {
            bmi < 18.5f -> "슬림핏 (저체중)"
            bmi < 23.0f -> "스탠다드 (표준)"
            bmi < 25.0f -> "탄탄·세미볼륨"
            else -> "볼륨·릴렉스핏"
        }

    val waistInches: Float
        get() = waistCm / 2.54f

    val shoulderToWaistRatio: Float
        get() = (shoulderWidthCm * 2.15f) / waistCm.coerceAtLeast(45f)

    val waistToHipRatio: Float
        get() = waistCm / hipCm.coerceAtLeast(60f)

    val legToHeightRatio: Float
        get() = inseamCm / heightCm.coerceAtLeast(120f)

    // Normalized parametric scale factors (0.75f .. 1.35f) for 2D & 3D mannequin rendering
    val normalizedShoulderScale: Float
        get() = ((shoulderWidthCm / 43.5f) * 0.7f + bodyShape.shoulderScale * 0.3f).coerceIn(0.80f, 1.32f)

    val normalizedChestScale: Float
        get() = ((chestCm / 93f) * (bmi / 21.5f).coerceIn(0.88f, 1.18f)).coerceIn(0.82f, 1.30f)

    val normalizedWaistScale: Float
        get() = ((waistCm / 74f) * 0.72f + bodyShape.waistScale * 0.28f).coerceIn(0.72f, 1.35f)

    val normalizedHipScale: Float
        get() = ((hipCm / 94f) * 0.70f + bodyShape.hipScale * 0.30f).coerceIn(0.82f, 1.32f)

    val normalizedHeightScale: Float
        get() = (heightCm / 172f).coerceIn(0.86f, 1.16f)

    val normalizedLegRatio: Float
        get() = (legToHeightRatio / 0.453f).coerceIn(0.88f, 1.15f)

    companion object {
        fun fromUserProfile(profile: UserProfile): AvatarBodyMetrics {
            val shape = BodyShapeType.fromCode(profile.bodyShape)
            val h = profile.heightCm.toFloat().coerceIn(140f, 205f)
            val w = profile.weightKg.toFloat().coerceIn(35f, 130f)
            val sh = profile.shoulderWidthCm.toFloat().coerceIn(32f, 60f)
            val wa = profile.waistCm.toFloat().coerceIn(54f, 115f)
            val derivedChest = (sh * 2.12f).coerceIn(76f, 124f)
            val derivedHip = (wa * (1.22f * shape.hipScale / shape.waistScale.coerceAtLeast(0.75f))).coerceIn(80f, 124f)
            val derivedInseam = (h * 0.455f).coerceIn(64f, 96f)
            return AvatarBodyMetrics(
                displayName = profile.displayName,
                heightCm = h,
                weightKg = w,
                shoulderWidthCm = sh,
                chestCm = derivedChest,
                waistCm = wa,
                hipCm = derivedHip,
                inseamCm = derivedInseam,
                bodyShape = shape,
                skinToneHex = profile.skinToneHex
            )
        }

        /**
         * Automatically classifies the user's BodyShapeType from entered body measurements.
         */
        fun autoClassifyBodyShape(
            shoulderWidthCm: Float,
            chestCm: Float,
            waistCm: Float,
            hipCm: Float,
            bmi: Float
        ): BodyShapeType {
            val effectiveShoulderCircumference = shoulderWidthCm * 2.18f
            val waistToHip = waistCm / hipCm.coerceAtLeast(60f)
            val shoulderToHip = effectiveShoulderCircumference / hipCm.coerceAtLeast(60f)
            val waistToChest = waistCm / chestCm.coerceAtLeast(65f)

            return when {
                waistToHip >= 0.93f || bmi >= 26.2f -> BodyShapeType.OVAL
                shoulderToHip >= 1.07f && waistToHip < 0.88f -> BodyShapeType.INVERTED_TRIANGLE
                shoulderToHip <= 0.94f && hipCm - waistCm >= 18f -> BodyShapeType.PEAR
                waistToHip <= 0.79f && waistToChest <= 0.80f -> BodyShapeType.HOURGLASS
                else -> BodyShapeType.RECTANGLE
            }
        }
    }
}

enum class MannequinPoseView(val labelKo: String) {
    FRONT("정면 아나토미"),
    THREE_QUARTER("45° 입체 핏"),
    SIDE("측면 실루엣")
}

/**
 * Interactive Body Measurement Form & Parametric Avatar Mannequin Studio Component.
 * Combines real-time sliders, numeric inputs, auto body-shape classification, and a live
 * Bézier-curve anatomical mannequin preview with measurement calipers.
 */
@Composable
fun AvatarMannequinDesignerCard(
    profile: UserProfile,
    metrics: AvatarBodyMetrics,
    onMetricsChanged: (AvatarBodyMetrics) -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFormExpanded by remember { mutableStateOf(true) }
    var showMeasurementCalipers by remember { mutableStateOf(true) }
    var poseView by remember { mutableStateOf(MannequinPoseView.FRONT) }
    var autoDetectNotice by remember { mutableStateOf<String?>(null) }

    val skinTones = listOf(
        "#F9E4D4" to "포슬린",
        "#F3D6C1" to "아이보리",
        "#E8C3A9" to "내추럴 샌드",
        "#E5B899" to "웜 허니",
        "#C6906B" to "앰버 브론즈",
        "#8D5B3E" to "에스프레소"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("avatar_mannequin_designer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "${metrics.displayName}님의 신체 치수 & 마네킹 설정",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${metrics.heightCm.roundToInt()}cm · ${metrics.weightKg.roundToInt()}kg · BMI ${String.format("%.1f", metrics.bmi)} (${metrics.bmiCategoryKo}) · ${metrics.bodyShape.labelKo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { isFormExpanded = !isFormExpanded },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_measurement_form_button")
                ) {
                    Icon(
                        imageVector = if (isFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFormExpanded) "폼 접기" else "치수 편집")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Live Parametric Base Mannequin Preview + Proportion Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Real-time Parametric Avatar Mannequin Renderer
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(310.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF3EFE8))
                        .border(1.dp, Color(0xFFD8D0C2), RoundedCornerShape(20.dp))
                        .testTag("parametric_mannequin_preview_box")
                ) {
                    ParametricAvatarMannequinCanvas(
                        metrics = metrics,
                        poseView = poseView,
                        showCalipers = showMeasurementCalipers,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-left Pose Switcher Pill
                    Surface(
                        color = AtelierMidnight.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clickable {
                                val nextOrd = (poseView.ordinal + 1) % MannequinPoseView.entries.size
                                poseView = MannequinPoseView.entries[nextOrd]
                            }
                            .testTag("mannequin_pose_toggle")
                    ) {
                        Text(
                            text = "뷰: ${poseView.labelKo} ↻",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierChampagne,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Top-right Caliper Overlay Toggle
                    IconButton(
                        onClick = { showMeasurementCalipers = !showMeasurementCalipers },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .testTag("toggle_calipers_button")
                    ) {
                        Icon(
                            imageVector = if (showMeasurementCalipers) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "치수 가이드라인 표시",
                            tint = AtelierMidnight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Right: Live Body Ratio & Fit Telemetry Summary
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AtelierMidnight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "아바타 체형 리포트",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierChampagne
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = metrics.bodyShape.labelKo,
                                style = MaterialTheme.typography.titleMedium,
                                color = AtelierIvory,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = metrics.bodyShape.descriptionKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierIvory.copy(alpha = 0.82f)
                            )
                        }
                    }

                    RatioMetricTile(
                        title = "어깨 : 허리 비율",
                        value = String.format("1 : %.2f", 1f / metrics.shoulderToWaistRatio.coerceAtLeast(0.5f)),
                        badge = "${metrics.shoulderWidthCm.roundToInt()}cm / ${metrics.waistCm.roundToInt()}cm"
                    )
                    RatioMetricTile(
                        title = "허리 인치 / WHR",
                        value = String.format("%.1f inch", metrics.waistInches),
                        badge = String.format("WHR %.2f", metrics.waistToHipRatio)
                    )
                    RatioMetricTile(
                        title = "하체 비율 (인심)",
                        value = "${(metrics.legToHeightRatio * 100f).roundToInt()}%",
                        badge = "인심 ${metrics.inseamCm.roundToInt()}cm"
                    )

                    OutlinedButton(
                        onClick = {
                            val detected = AvatarBodyMetrics.autoClassifyBodyShape(
                                shoulderWidthCm = metrics.shoulderWidthCm,
                                chestCm = metrics.chestCm,
                                waistCm = metrics.waistCm,
                                hipCm = metrics.hipCm,
                                bmi = metrics.bmi
                            )
                            onMetricsChanged(metrics.copy(bodyShape = detected))
                            autoDetectNotice = "✨ 입력된 치수(어깨 ${metrics.shoulderWidthCm.roundToInt()}cm·허리 ${metrics.waistCm.roundToInt()}cm·힙 ${metrics.hipCm.roundToInt()}cm)를 분석해 '${detected.labelKo}'으로 자동 매칭했습니다."
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_classify_body_shape_button")
                    ) {
                        Text("AI 체형 자동 판별", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (autoDetectNotice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AtelierSage.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = autoDetectNotice!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = AtelierSage,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 3. Expandable Interactive Body Measurement Input Form
            AnimatedVisibility(visible = isFormExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "정밀 신체 치수 입력 폼 (슬라이더 또는 수치 직접 입력)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Nickname & Reset Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = metrics.displayName,
                            onValueChange = { onMetricsChanged(metrics.copy(displayName = it)) },
                            label = { Text("아바타 이름 / 닉네임") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_avatar_name")
                        )
                        OutlinedButton(
                            onClick = {
                                val reset = AvatarBodyMetrics.fromUserProfile(UserProfile(userId = profile.userId))
                                onMetricsChanged(reset)
                                autoDetectNotice = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_measurements_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "초기화", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("표준치 초기화")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sliders + Direct Stepper Controls for Primary & Secondary Measurements
                    MeasurementSliderRow(
                        label = "키 (신장)",
                        unit = "cm",
                        value = metrics.heightCm,
                        valueRange = 145f..200f,
                        onValueChange = { newH ->
                            onMetricsChanged(metrics.copy(heightCm = newH))
                        },
                        testTagPrefix = "height"
                    )

                    MeasurementSliderRow(
                        label = "몸무게 (체중)",
                        unit = "kg",
                        value = metrics.weightKg,
                        valueRange = 40f..120f,
                        onValueChange = { newW ->
                            onMetricsChanged(metrics.copy(weightKg = newW))
                        },
                        testTagPrefix = "weight"
                    )

                    MeasurementSliderRow(
                        label = "어깨너비",
                        unit = "cm",
                        value = metrics.shoulderWidthCm,
                        valueRange = 34f..56f,
                        onValueChange = { newSh ->
                            onMetricsChanged(metrics.copy(shoulderWidthCm = newSh))
                        },
                        testTagPrefix = "shoulder"
                    )

                    MeasurementSliderRow(
                        label = "가슴둘레",
                        unit = "cm",
                        value = metrics.chestCm,
                        valueRange = 76f..120f,
                        onValueChange = { newCh ->
                            onMetricsChanged(metrics.copy(chestCm = newCh))
                        },
                        testTagPrefix = "chest"
                    )

                    MeasurementSliderRow(
                        label = "허리둘레 (${String.format("%.1f", metrics.waistInches)}인치)",
                        unit = "cm",
                        value = metrics.waistCm,
                        valueRange = 56f..110f,
                        onValueChange = { newWa ->
                            onMetricsChanged(metrics.copy(waistCm = newWa))
                        },
                        testTagPrefix = "waist"
                    )

                    MeasurementSliderRow(
                        label = "엉덩이둘레 (힙)",
                        unit = "cm",
                        value = metrics.hipCm,
                        valueRange = 80f..120f,
                        onValueChange = { newHip ->
                            onMetricsChanged(metrics.copy(hipCm = newHip))
                        },
                        testTagPrefix = "hip"
                    )

                    MeasurementSliderRow(
                        label = "다리 인심 (안쪽 기장)",
                        unit = "cm",
                        value = metrics.inseamCm,
                        valueRange = 64f..92f,
                        onValueChange = { newInseam ->
                            onMetricsChanged(metrics.copy(inseamCm = newInseam))
                        },
                        testTagPrefix = "inseam"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Body Shape Selector Chips
                    Text(
                        text = "체형 실루엣 프리셋 (클릭 시 마네킹 골격 비율 즉시 반영)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        BodyShapeType.entries.forEach { shape ->
                            val isSelected = metrics.bodyShape == shape
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onMetricsChanged(metrics.copy(bodyShape = shape))
                                },
                                label = { Text(shape.labelKo) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtelierMidnight,
                                    selectedLabelColor = AtelierChampagne
                                ),
                                modifier = Modifier.testTag("body_shape_chip_${shape.code}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Skin Tone Swatches
                    Text(
                        text = "아바타 피부 톤 설정",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        skinTones.forEach { (hex, toneName) ->
                            val isSelected = metrics.skinToneHex.equals(hex, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .clickable {
                                        onMetricsChanged(metrics.copy(skinToneHex = hex))
                                    }
                                    .testTag("skin_tone_chip_${hex.removePrefix("#")}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColorSafe(hex))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) AtelierMidnight else Color.Gray,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = toneName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Button to Persist to Firestore UserProfile
                    Button(
                        onClick = {
                            val updatedProfile = profile.copy(
                                displayName = metrics.displayName.ifBlank { "패션 디렉터" },
                                heightCm = metrics.heightCm.roundToLong().coerceIn(100L, 230L),
                                weightKg = metrics.weightKg.roundToLong().coerceIn(30L, 200L),
                                shoulderWidthCm = metrics.shoulderWidthCm.roundToLong().coerceIn(30L, 70L),
                                waistCm = metrics.waistCm.roundToLong().coerceIn(45L, 150L),
                                bodyShape = metrics.bodyShape.code,
                                skinToneHex = metrics.skinToneHex
                            )
                            onSaveProfile(updatedProfile)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AtelierMidnight,
                            contentColor = AtelierChampagne
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_avatar_measurements_button")
                    ) {
                        Text(
                            text = "내 신체 치수 & 아바타 마네킹 저장하기",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatioMetricTile(
    title: String,
    value: String,
    badge: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = AtelierChampagne.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = AtelierMidnight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MeasurementSliderRow(
    label: String,
    unit: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTagPrefix: String
) {
    var textInput by remember(value.roundToInt()) { mutableStateOf(value.roundToInt().toString()) }

    LaunchedEffect(value) {
        val rounded = value.roundToInt().toString()
        if (textInput != rounded) {
            textInput = rounded
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val next = (value - 1f).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(next)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("${testTagPrefix}_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "$label 감소",
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(3)
                        textInput = digits
                        digits.toFloatOrNull()?.let { parsed ->
                            if (parsed in valueRange.start..valueRange.endInclusive) {
                                onValueChange(parsed)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .width(70.dp)
                        .height(46.dp)
                        .testTag("${testTagPrefix}_input")
                )

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        val next = (value + 1f).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(next)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("${testTagPrefix}_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "$label 증가",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AtelierMidnight,
                activeTrackColor = AtelierMidnight,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .testTag("${testTagPrefix}_slider")
        )
    }
}

/**
 * High-precision Parametric Avatar Mannequin Renderer.
 * Uses smooth Cubic Bézier curves, anatomical proportions, studio pedestal shading,
 * and optional measurement caliper callouts that respond instantaneously to every body metric.
 */
@Composable
fun ParametricAvatarMannequinCanvas(
    metrics: AvatarBodyMetrics,
    poseView: MannequinPoseView = MannequinPoseView.FRONT,
    showCalipers: Boolean = true,
    modifier: Modifier = Modifier
) {
    val baseSkin = parseHexColorSafe(metrics.skinToneHex, Color(0xFFF3D6C1))
    val highlightSkin = Color(
        red = (baseSkin.red * 1.07f).coerceAtMost(1f),
        green = (baseSkin.green * 1.06f).coerceAtMost(1f),
        blue = (baseSkin.blue * 1.05f).coerceAtMost(1f),
        alpha = 1f
    )
    val shadowSkin = Color(
        red = (baseSkin.red * 0.82f).coerceIn(0f, 1f),
        green = (baseSkin.green * 0.79f).coerceIn(0f, 1f),
        blue = (baseSkin.blue * 0.76f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val seamColor = Color(0xFF2B2838).copy(alpha = 0.42f)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f

        // 1. Studio Height Stadiometer Grid & Pedestal
        drawStudioMeasurementBackdrop(w, h, metrics.heightCm, textMeasurer)

        // 2. Compute Proportional Anatomical Landmarks
        val heightScale = metrics.normalizedHeightScale
        val topHeadY = h * (0.11f - (heightScale - 1f) * 0.18f).coerceIn(0.06f, 0.17f)
        val ankleY = h * 0.87f
        val bodySpan = ankleY - topHeadY

        val headRadius = w * 0.068f
        val headCenterY = topHeadY + headRadius
        val neckTopY = headCenterY + headRadius * 0.88f
        val shoulderY = topHeadY + bodySpan * 0.165f
        val chestY = topHeadY + bodySpan * 0.255f
        val waistY = topHeadY + bodySpan * (0.375f - (metrics.normalizedLegRatio - 1f) * 0.06f)
        val hipY = waistY + bodySpan * 0.115f
        val kneeY = hipY + (ankleY - hipY) * 0.52f

        // Horizontal half-widths modulated by poseView (FRONT, THREE_QUARTER, SIDE)
        val poseWidthFactor = when (poseView) {
            MannequinPoseView.FRONT -> 1.0f
            MannequinPoseView.THREE_QUARTER -> 0.86f
            MannequinPoseView.SIDE -> 0.56f
        }

        val shoulderHalf = w * 0.195f * metrics.normalizedShoulderScale * poseWidthFactor
        val chestHalf = w * 0.155f * metrics.normalizedChestScale * poseWidthFactor
        val waistHalf = w * 0.118f * metrics.normalizedWaistScale * poseWidthFactor
        val hipHalf = w * 0.162f * metrics.normalizedHipScale * poseWidthFactor
        val thighHalf = hipHalf * 0.52f
        val ankleHalf = w * 0.028f

        val bodyBrush = Brush.horizontalGradient(
            colors = listOf(shadowSkin, highlightSkin, baseSkin, shadowSkin),
            startX = cx - shoulderHalf * 1.2f,
            endX = cx + shoulderHalf * 1.2f
        )

        // 3. Pedestal Shadow & Brass Stand
        drawOval(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = Offset(cx - w * 0.24f, ankleY + h * 0.02f),
            size = Size(w * 0.48f, h * 0.055f)
        )
        drawRoundRect(
            color = Color(0xFFD9D1C5),
            topLeft = Offset(cx - w * 0.20f, ankleY + h * 0.03f),
            size = Size(w * 0.40f, h * 0.035f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // 4. Head & Sculpted Neck
        drawOval(
            brush = bodyBrush,
            topLeft = Offset(cx - headRadius * 0.88f, topHeadY),
            size = Size(headRadius * 1.76f, headRadius * 2.15f)
        )
        drawOval(
            color = seamColor,
            topLeft = Offset(cx - headRadius * 0.88f, topHeadY),
            size = Size(headRadius * 1.76f, headRadius * 2.15f),
            style = Stroke(width = 2.2f)
        )

        val neckHalf = w * 0.034f * (0.9f + (metrics.bmi - 21f) * 0.01f).coerceIn(0.8f, 1.15f)
        val neckPath = Path().apply {
            moveTo(cx - neckHalf, neckTopY)
            lineTo(cx + neckHalf, neckTopY)
            lineTo(cx + neckHalf * 1.25f, shoulderY + 4f)
            lineTo(cx - neckHalf * 1.25f, shoulderY + 4f)
            close()
        }
        drawPath(neckPath, brush = bodyBrush)

        // 5. Sculpted Bézier Torso, Pelvis & Legs
        val torsoAndLegsPath = Path().apply {
            // Top center collar to right shoulder
            moveTo(cx, shoulderY - 4f)
            cubicTo(
                cx + shoulderHalf * 0.45f, shoulderY - 4f,
                cx + shoulderHalf * 0.85f, shoulderY - 2f,
                cx + shoulderHalf, shoulderY + 6f
            )
            // Right shoulder cap to chest & waist
            cubicTo(
                cx + shoulderHalf * 0.96f, chestY - 8f,
                cx + chestHalf * 1.04f, chestY + 10f,
                cx + waistHalf, waistY
            )
            // Right waist taper to hip curve
            cubicTo(
                cx + waistHalf * 1.04f, (waistY + hipY) * 0.5f,
                cx + hipHalf * 1.04f, hipY - 10f,
                cx + hipHalf, hipY
            )
            // Right outer leg down to knee and ankle
            cubicTo(
                cx + hipHalf * 0.94f, (hipY + kneeY) * 0.5f,
                cx + thighHalf * 1.12f, kneeY,
                cx + ankleHalf * 2.1f, ankleY
            )
            // Right foot base
            lineTo(cx + ankleHalf * 0.55f, ankleY)
            // Right inner leg up to inseam crotch
            val inseamGap = if (poseView == MannequinPoseView.SIDE) 2f else w * 0.016f
            cubicTo(
                cx + inseamGap * 1.3f, kneeY,
                cx + inseamGap * 1.4f, hipY + 18f,
                cx, hipY + 12f
            )
            // Left inner leg down to left ankle
            cubicTo(
                cx - inseamGap * 1.4f, hipY + 18f,
                cx - inseamGap * 1.3f, kneeY,
                cx - ankleHalf * 0.55f, ankleY
            )
            lineTo(cx - ankleHalf * 2.1f, ankleY)
            // Left outer leg up to left hip
            cubicTo(
                cx - thighHalf * 1.12f, kneeY,
                cx - hipHalf * 0.94f, (hipY + kneeY) * 0.5f,
                cx - hipHalf, hipY
            )
            // Left hip up to left waist
            cubicTo(
                cx - hipHalf * 1.04f, hipY - 10f,
                cx - waistHalf * 1.04f, (waistY + hipY) * 0.5f,
                cx - waistHalf, waistY
            )
            // Left waist up to chest and left shoulder
            cubicTo(
                cx - chestHalf * 1.04f, chestY + 10f,
                cx - shoulderHalf * 0.96f, chestY - 8f,
                cx - shoulderHalf, shoulderY + 6f
            )
            // Left shoulder to collar center
            cubicTo(
                cx - shoulderHalf * 0.85f, shoulderY - 2f,
                cx - shoulderHalf * 0.45f, shoulderY - 4f,
                cx, shoulderY - 4f
            )
            close()
        }

        // 6. Articulated Left & Right Arms
        if (poseView != MannequinPoseView.SIDE) {
            val armWidth = w * 0.036f * (metrics.bmi / 21.5f).coerceIn(0.85f, 1.18f)
            val wristY = hipY + 10f
            // Left Arm
            val leftArmPath = Path().apply {
                moveTo(cx - shoulderHalf + 2f, shoulderY + 6f)
                quadraticTo(
                    cx - shoulderHalf - armWidth * 1.35f, waistY,
                    cx - shoulderHalf - armWidth * 0.85f, wristY
                )
                lineTo(cx - shoulderHalf - armWidth * 0.10f, wristY)
                quadraticTo(
                    cx - shoulderHalf - armWidth * 0.35f, waistY,
                    cx - chestHalf + 2f, chestY + 6f
                )
                close()
            }
            // Right Arm
            val rightArmPath = Path().apply {
                moveTo(cx + shoulderHalf - 2f, shoulderY + 6f)
                quadraticTo(
                    cx + shoulderHalf + armWidth * 1.35f, waistY,
                    cx + shoulderHalf + armWidth * 0.85f, wristY
                )
                lineTo(cx + shoulderHalf + armWidth * 0.10f, wristY)
                quadraticTo(
                    cx + shoulderHalf + armWidth * 0.35f, waistY,
                    cx + chestHalf - 2f, chestY + 6f
                )
                close()
            }
            drawPath(leftArmPath, brush = bodyBrush)
            drawPath(leftArmPath, color = seamColor, style = Stroke(width = 2f))
            drawPath(rightArmPath, brush = bodyBrush)
            drawPath(rightArmPath, color = seamColor, style = Stroke(width = 2f))
        }

        drawPath(torsoAndLegsPath, brush = bodyBrush)
        drawPath(
            path = torsoAndLegsPath,
            color = seamColor,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 7. Tailor's Center Seam & Anatomical Construction Lines
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        drawLine(
            color = seamColor.copy(alpha = 0.28f),
            start = Offset(cx, shoulderY),
            end = Offset(cx, hipY + 10f),
            strokeWidth = 1.5f,
            pathEffect = dashEffect
        )

        // 8. Real-time Caliper Measurement Guides (Shoulder, Chest, Waist, Hip, Inseam)
        if (showCalipers) {
            // Shoulder Caliper (Champagne Gold)
            drawCaliperLine(
                startX = cx - shoulderHalf,
                endX = cx + shoulderHalf,
                y = shoulderY + 4f,
                color = Color(0xFFB8862D),
                label = "어깨 ${metrics.shoulderWidthCm.roundToInt()}cm",
                textMeasurer = textMeasurer
            )

            // Chest Caliper (Navy)
            drawCaliperLine(
                startX = cx - chestHalf,
                endX = cx + chestHalf,
                y = chestY + 2f,
                color = Color(0xFF284B7A),
                label = "가슴 ${metrics.chestCm.roundToInt()}cm",
                textMeasurer = textMeasurer
            )

            // Waist Caliper (Coral Accent)
            drawCaliperLine(
                startX = cx - waistHalf,
                endX = cx + waistHalf,
                y = waistY,
                color = Color(0xFFD94E47),
                label = "허리 ${metrics.waistCm.roundToInt()}cm (${String.format("%.0f\"", metrics.waistInches)})",
                textMeasurer = textMeasurer
            )

            // Hip Caliper (Sage Green)
            drawCaliperLine(
                startX = cx - hipHalf,
                endX = cx + hipHalf,
                y = hipY,
                color = Color(0xFF426B52),
                label = "힙 ${metrics.hipCm.roundToInt()}cm",
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawStudioMeasurementBackdrop(
    w: Float,
    h: Float,
    heightCm: Float,
    textMeasurer: TextMeasurer
) {
    val gridColor = Color(0xFF9E9587).copy(alpha = 0.18f)
    val rows = 8
    val stepY = (h * 0.78f) / rows
    val startY = h * 0.10f

    for (i in 0..rows) {
        val y = startY + i * stepY
        drawLine(
            color = gridColor,
            start = Offset(w * 0.06f, y),
            end = Offset(w * 0.94f, y),
            strokeWidth = 1f
        )
        if (i % 2 == 0) {
            val cmMark = (heightCm * (1f - i.toFloat() / rows)).roundToInt()
            val layout = textMeasurer.measure(
                text = "${cmMark}cm",
                style = TextStyle(fontSize = 9.sp, color = Color(0xFF7B7367))
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(w * 0.06f, y - layout.size.height - 2f)
            )
        }
    }
}

private fun DrawScope.drawCaliperLine(
    startX: Float,
    endX: Float,
    y: Float,
    color: Color,
    label: String,
    textMeasurer: TextMeasurer
) {
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 2.5f
    )
    // End ticks
    drawLine(
        color = color,
        start = Offset(startX, y - 5f),
        end = Offset(startX, y + 5f),
        strokeWidth = 2.5f
    )
    drawLine(
        color = color,
        start = Offset(endX, y - 5f),
        end = Offset(endX, y + 5f),
        strokeWidth = 2.5f
    )

    val measured = textMeasurer.measure(
        text = label,
        style = TextStyle(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    )
    val pillW = measured.size.width + 12f
    val pillH = measured.size.height + 4f
    val cx = (startX + endX) * 0.5f

    drawRoundRect(
        color = color.copy(alpha = 0.90f),
        topLeft = Offset(cx - pillW * 0.5f, y - pillH * 0.5f),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(cx - measured.size.width * 0.5f, y - measured.size.height * 0.5f)
    )
}
