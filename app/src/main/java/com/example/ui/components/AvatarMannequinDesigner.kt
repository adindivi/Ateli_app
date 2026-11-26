package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.AtelierIvory
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import com.example.ui.theme.ZigzagStrong
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagCtaBlack
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
    FRONT("앞"),
    THREE_QUARTER("45°"),
    SIDE("옆")
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
    var isFormExpanded by remember { mutableStateOf(false) }
    var showMeasurementCalipers by remember { mutableStateOf(false) }
    var poseView by remember { mutableStateOf(MannequinPoseView.FRONT) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            text = "내 아바타 만들기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${metrics.heightCm.roundToInt()}cm · ${metrics.weightKg.roundToInt()}kg · ${metrics.bodyShape.labelKo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { isFormExpanded = !isFormExpanded },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ZigzagStrong
                    ),
                    border = BorderStroke(1.dp, ZigzagHairline),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("toggle_measurement_form_button")
                ) {
                    Icon(
                        imageVector = if (isFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFormExpanded) "접기" else "치수 변경",
                        style = MaterialTheme.typography.labelMedium
                    )
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
                        .height(210.dp)
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

                    // Top-Right Pose Switcher Pill (Avoid overlap with left height calipers)
                    Surface(
                        color = ZigzagCtaBlack.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clickable {
                                val nextOrd = (poseView.ordinal + 1) % MannequinPoseView.entries.size
                                poseView = MannequinPoseView.entries[nextOrd]
                            }
                            .testTag("mannequin_pose_toggle")
                    ) {
                        Text(
                            text = "${poseView.labelKo} ↻",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierChampagne,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Right: Live Body Shape Result Summary
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
                                text = "체형",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierChampagne.copy(alpha = 0.78f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = metrics.bodyShape.labelKo,
                                style = MaterialTheme.typography.titleMedium,
                                color = AtelierIvory,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = metrics.bodyShape.descriptionKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierIvory.copy(alpha = 0.85f),
                                maxLines = 2
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "추천 핏",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0071E3),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = metrics.bodyShape.stylingTip,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155),
                                lineHeight = 16.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // 3. Simple & Intuitive Body Measurement Input Form
            AnimatedVisibility(visible = isFormExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "체형 치수 설정",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "내 기기 아바타 비율 계산에만 쓰여요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            label = { Text("닉네임") },
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
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_measurements_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "초기화", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("원래대로")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Height Slider
                    MeasurementSliderRow(
                        label = "키",
                        unit = "cm",
                        value = metrics.heightCm,
                        valueRange = 145f..200f,
                        onValueChange = { newH ->
                            onMetricsChanged(metrics.copy(heightCm = newH))
                        },
                        testTagPrefix = "height"
                    )

                    // Weight Slider
                    MeasurementSliderRow(
                        label = "몸무게",
                        unit = "kg",
                        value = metrics.weightKg,
                        valueRange = 40f..120f,
                        onValueChange = { newW ->
                            onMetricsChanged(metrics.copy(weightKg = newW))
                        },
                        testTagPrefix = "weight"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5 Representative Body Shape Filter Chips
                    Text(
                        text = "대표 체형 바로 고르기",
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
                            text = "아바타 저장하기",
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        val next = (value - 1f).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(next)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("${testTagPrefix}_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "$label 감소",
                        modifier = Modifier.size(18.dp)
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
                        .width(68.dp)
                        .height(44.dp)
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
                        .size(36.dp)
                        .testTag("${testTagPrefix}_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "$label 증가",
                        modifier = Modifier.size(18.dp)
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
