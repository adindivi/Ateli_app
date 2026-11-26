package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Garment
import com.example.ui.theme.AtelierChampagne
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Computed 3D Anatomical Anchor & Perspective Rig for overlaying garments onto the 3D Mannequin.
 */
data class Overlay3DAnatomicalRig(
    val yawAngleDeg: Float,
    val perspectiveCos: Float,
    val parallaxSin: Float,
    val outerAnchorOffsetDp: Offset,
    val topAnchorOffsetDp: Offset,
    val bottomAnchorOffsetDp: Offset,
    val shoesAnchorOffsetDp: Offset,
    val outerWidthScale: Float,
    val topWidthScale: Float,
    val bottomWidthScale: Float,
    val bottomHeightScale: Float
)

object Overlay3DRigCalculator {
    fun computeRig(
        metrics: AvatarBodyMetrics,
        yawAngleDeg: Float
    ): Overlay3DAnatomicalRig {
        val clampedYaw = yawAngleDeg.coerceIn(-50f, 50f)
        val rad = (clampedYaw * PI / 180.0).toFloat()
        val cosVal = cos(rad).coerceIn(0.62f, 1.0f)
        val sinVal = sin(rad).coerceIn(-0.78f, 0.78f)

        // Height & leg proportion adjustments
        val heightFactor = ((metrics.heightCm - 172f) / 140f).coerceIn(-0.12f, 0.14f)
        val legRatioFactor = (metrics.normalizedLegRatio - 0.46f) * 85f

        // Parallax X shift per depth layer as the 3D mannequin rotates
        val outerParallaxX = sinVal * 18f
        val topParallaxX = sinVal * 11f
        val bottomParallaxX = sinVal * 9f
        val shoesParallaxX = sinVal * 7f

        // Anatomical Y anchor offsets relative to the canvas center
        val topAnchorY = -54f - (heightFactor * 18f) - (legRatioFactor * 0.35f)
        val outerAnchorY = topAnchorY + 4f
        val bottomAnchorY = 48f - (legRatioFactor * 0.25f)
        val shoesAnchorY = 148f + (heightFactor * 22f)

        // Perspective foreshortening combined with user's shoulder/hip widths
        val shoulderScale = metrics.normalizedShoulderScale.coerceIn(0.86f, 1.20f)
        val hipScale = metrics.normalizedHipScale.coerceIn(0.86f, 1.20f)

        val outerW = (shoulderScale * (0.82f + 0.18f * cosVal)).coerceIn(0.74f, 1.22f)
        val topW = (shoulderScale * (0.80f + 0.20f * cosVal)).coerceIn(0.72f, 1.20f)
        val bottomW = (hipScale * (0.82f + 0.18f * cosVal)).coerceIn(0.74f, 1.20f)
        val bottomH = (1.0f + (metrics.normalizedLegRatio - 0.46f) * 1.1f).coerceIn(0.90f, 1.15f)

        return Overlay3DAnatomicalRig(
            yawAngleDeg = clampedYaw,
            perspectiveCos = cosVal,
            parallaxSin = sinVal,
            outerAnchorOffsetDp = Offset(outerParallaxX, outerAnchorY),
            topAnchorOffsetDp = Offset(topParallaxX, topAnchorY),
            bottomAnchorOffsetDp = Offset(bottomParallaxX, bottomAnchorY),
            shoesAnchorOffsetDp = Offset(shoesParallaxX, shoesAnchorY),
            outerWidthScale = outerW,
            topWidthScale = topW,
            bottomWidthScale = bottomW,
            bottomHeightScale = bottomH
        )
    }
}

/**
 * Dedicated 3D Mannequin + Multi-Layer Garment Overlay Fitting Canvas UI Component.
 * Renders a volumetric 3D-shaded mannequin from the user's body measurements (`AvatarBodyMetrics`)
 * and overlays selected garments (`OUTER`, `TOP`, `BOTTOM`, `SHOES`) with anatomical anchor snapping,
 * 3D perspective foreshortening, X-Ray opacity inspection, and Z-layer controls.
 *
 * Benchmark References:
 * - IDM-VTON / OOTDiffusion: https://github.com/yisol/IDM-VTON , https://github.com/levihsu/OOTDiffusion
 * - 4D-Humans Parametric Rig: https://github.com/shubham-goel/4D-Humans
 */
@Composable
fun ThreeDOverlayFittingCanvas(
    metrics: AvatarBodyMetrics,
    outerGarment: Garment?,
    topGarment: Garment?,
    bottomGarment: Garment?,
    shoesGarment: Garment?,
    cutoutBitmaps: Map<String, Bitmap>,
    garmentScale: Float,
    onGarmentScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3D Mannequin Turntable Yaw Angle (-50° .. +50°)
    var yawAngleDeg by remember { mutableFloatStateOf(0f) }

    // X-Ray Fabric Opacity (0.25f .. 1.0f) to inspect body silhouette underneath overlaid garments
    var fabricOpacity by remember { mutableFloatStateOf(0.94f) }

    // 3D Wireframe Contour & Anchor Pin overlays
    var show3dWireframeContours by remember { mutableStateOf(true) }
    var showAnatomicalAnchors by remember { mutableStateOf(true) }
    var autoMagneticSnap by remember { mutableStateOf(true) }

    // Per-Layer Visibility Toggles (Z1..Z4)
    var showOuterLayer by remember { mutableStateOf(true) }
    var showTopLayer by remember { mutableStateOf(true) }
    var showBottomLayer by remember { mutableStateOf(true) }
    var showShoesLayer by remember { mutableStateOf(true) }

    // Active Layer Selection for fine-tuning
    var activeLayerKey by remember { mutableStateOf("TOP") }

    // Fine user drag delta (in px) relative to the 3D anatomical anchors
    var outerDeltaX by remember { mutableFloatStateOf(0f) }
    var outerDeltaY by remember { mutableFloatStateOf(0f) }
    var topDeltaX by remember { mutableFloatStateOf(0f) }
    var topDeltaY by remember { mutableFloatStateOf(0f) }
    var bottomDeltaX by remember { mutableFloatStateOf(0f) }
    var bottomDeltaY by remember { mutableFloatStateOf(0f) }
    var shoesDeltaX by remember { mutableFloatStateOf(0f) }
    var shoesDeltaY by remember { mutableFloatStateOf(0f) }

    val rig = remember(metrics, yawAngleDeg) {
        Overlay3DRigCalculator.computeRig(metrics, yawAngleDeg)
    }

    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("three_d_overlay_fitting_canvas_root"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top HUD Control Row: 3D Angle Presets + Wireframe/Anchor/Snap toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                0f to "정면 0°",
                -22f to "좌측 3/4 (-22°)",
                22f to "우측 3/4 (+22°)",
                38f to "측면 실루엣 (+38°)"
            ).forEach { (angle, label) ->
                FilterChip(
                    selected = abs(yawAngleDeg - angle) < 4f,
                    onClick = { yawAngleDeg = angle },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("overlay_angle_chip_${angle.toInt()}")
                )
            }

            FilterChip(
                selected = show3dWireframeContours,
                onClick = { show3dWireframeContours = !show3dWireframeContours },
                label = { Text("3D 등고선 메쉬", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )

            FilterChip(
                selected = autoMagneticSnap,
                onClick = {
                    autoMagneticSnap = !autoMagneticSnap
                    if (autoMagneticSnap) {
                        outerDeltaX = 0f; outerDeltaY = 0f
                        topDeltaX = 0f; topDeltaY = 0f
                        bottomDeltaX = 0f; bottomDeltaY = 0f
                        shoesDeltaX = 0f; shoesDeltaY = 0f
                    }
                },
                label = { Text("🧲 관절 자석 스냅", style = MaterialTheme.typography.labelSmall) }
            )
        }

        // 2. Main 3D Volumetric Mannequin + Multi-Layer Garment Overlay Stage
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141824),
                            Color(0xFF1E2436),
                            Color(0xFF121520)
                        )
                    )
                )
                .border(1.5.dp, AtelierChampagne.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .testTag("overlay_fitting_stage_box"),
            contentAlignment = Alignment.Center
        ) {
            // Base Z=0: 3D Volumetric Shaded Mannequin Canvas with Studio Lighting & Turntable
            Volumetric3DMannequinCanvas(
                metrics = metrics,
                rig = rig,
                showWireframeRings = show3dWireframeContours,
                showAnatomicalAnchors = showAnatomicalAnchors,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

            // Top-Left Telemetry Badge (Body Specs + Active 3D Yaw + Snap Status)
            Surface(
                color = Color(0xFF0B0E17).copy(alpha = 0.82f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AtelierChampagne.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .zIndex(12f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "3D RIG · ${metrics.heightCm.toInt()}cm · ${if (yawAngleDeg >= 0) "+${yawAngleDeg.toInt()}" else "${yawAngleDeg.toInt()}"}°",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = AtelierChampagne,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Top-Right Reset & Snap Alignment Action Button
            Surface(
                color = Color(0xFF0B0E17).copy(alpha = 0.82f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .zIndex(12f)
                    .clickable {
                        outerDeltaX = 0f; outerDeltaY = 0f
                        topDeltaX = 0f; topDeltaY = 0f
                        bottomDeltaX = 0f; bottomDeltaY = 0f
                        shoesDeltaX = 0f; shoesDeltaY = 0f
                        yawAngleDeg = 0f
                        onGarmentScaleChange(1.0f)
                    }
                    .testTag("overlay_snap_reset_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "관절 스냅 정렬",
                        tint = AtelierChampagne,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "정위치 스냅",
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierChampagne,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // =========================================================================
            // Z=1: SHOES OVERLAY LAYER (Anchored to 3D Ankle Rig)
            // =========================================================================
            if (shoesGarment != null && showShoesLayer) {
                FittingGarmentOverlayLayer(
                    garment = shoesGarment,
                    cutoutBitmap = cutoutBitmaps[shoesGarment.id],
                    zIndex = 1f,
                    anchorOffsetDp = rig.shoesAnchorOffsetDp,
                    readDeltaX = { shoesDeltaX },
                    readDeltaY = { shoesDeltaY },
                    layerSize = 98.dp,
                    scaleX = garmentScale * (0.85f + 0.15f * rig.perspectiveCos),
                    scaleY = garmentScale,
                    rotationY = -rig.yawAngleDeg * 0.35f,
                    fabricOpacity = fabricOpacity,
                    autoMagneticSnap = autoMagneticSnap,
                    onDragDelta = { dx, dy ->
                        activeLayerKey = "SHOES"
                        shoesDeltaX += dx
                        shoesDeltaY += dy
                    },
                    onSnapReset = {
                        shoesDeltaX = 0f
                        shoesDeltaY = 0f
                    },
                    tag = "overlay_layer_shoes"
                )
            }

            // =========================================================================
            // Z=2: BOTTOM OVERLAY LAYER (Anchored to 3D Waist/Hip/Leg Rig)
            // =========================================================================
            if (bottomGarment != null && showBottomLayer) {
                FittingGarmentOverlayLayer(
                    garment = bottomGarment,
                    cutoutBitmap = cutoutBitmaps[bottomGarment.id],
                    zIndex = 2f,
                    anchorOffsetDp = rig.bottomAnchorOffsetDp,
                    readDeltaX = { bottomDeltaX },
                    readDeltaY = { bottomDeltaY },
                    layerSize = (192f * rig.bottomWidthScale).dp,
                    scaleX = garmentScale,
                    scaleY = garmentScale * rig.bottomHeightScale,
                    rotationY = -rig.yawAngleDeg * 0.45f,
                    fabricOpacity = fabricOpacity,
                    autoMagneticSnap = autoMagneticSnap,
                    onDragDelta = { dx, dy ->
                        activeLayerKey = "BOTTOM"
                        bottomDeltaX += dx
                        bottomDeltaY += dy
                    },
                    onSnapReset = {
                        bottomDeltaX = 0f
                        bottomDeltaY = 0f
                    },
                    tag = "overlay_layer_bottom"
                )
            }

            // =========================================================================
            // Z=3: TOP OVERLAY LAYER (Anchored to 3D Shoulder/Chest/Torso Rig)
            // =========================================================================
            if (topGarment != null && showTopLayer) {
                val topBaseSizeDp = if (outerGarment != null && showOuterLayer) {
                    (116f * rig.topWidthScale).dp
                } else {
                    (144f * rig.topWidthScale).dp
                }
                FittingGarmentOverlayLayer(
                    garment = topGarment,
                    cutoutBitmap = cutoutBitmaps[topGarment.id],
                    zIndex = 3f,
                    anchorOffsetDp = rig.topAnchorOffsetDp,
                    readDeltaX = { topDeltaX },
                    readDeltaY = { topDeltaY },
                    layerSize = topBaseSizeDp,
                    scaleX = garmentScale,
                    scaleY = garmentScale,
                    rotationY = -rig.yawAngleDeg * 0.48f,
                    fabricOpacity = fabricOpacity,
                    autoMagneticSnap = autoMagneticSnap,
                    onDragDelta = { dx, dy ->
                        activeLayerKey = "TOP"
                        topDeltaX += dx
                        topDeltaY += dy
                    },
                    onSnapReset = {
                        topDeltaX = 0f
                        topDeltaY = 0f
                    },
                    tag = "overlay_layer_top"
                )
            }

            // =========================================================================
            // Z=4: OUTER OVERLAY LAYER (Anchored to 3D Collar/Shoulder Rig with Parallax)
            // =========================================================================
            if (outerGarment != null && showOuterLayer) {
                FittingGarmentOverlayLayer(
                    garment = outerGarment,
                    cutoutBitmap = cutoutBitmaps[outerGarment.id],
                    zIndex = 4f,
                    anchorOffsetDp = rig.outerAnchorOffsetDp,
                    readDeltaX = { outerDeltaX },
                    readDeltaY = { outerDeltaY },
                    layerSize = (184f * rig.outerWidthScale).dp,
                    scaleX = garmentScale,
                    scaleY = garmentScale,
                    rotationY = -rig.yawAngleDeg * 0.52f,
                    fabricOpacity = fabricOpacity,
                    autoMagneticSnap = autoMagneticSnap,
                    onDragDelta = { dx, dy ->
                        activeLayerKey = "OUTER"
                        outerDeltaX += dx
                        outerDeltaY += dy
                    },
                    onSnapReset = {
                        outerDeltaX = 0f
                        outerDeltaY = 0f
                    },
                    tag = "overlay_layer_outer"
                )
            }

            // Bottom Floating Z-Layer Stack & Visibility Dock inside the 3D Stage
            Surface(
                color = Color(0xFF0B0E17).copy(alpha = 0.86f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .zIndex(15f)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = AtelierChampagne,
                        modifier = Modifier.size(16.dp)
                    )
                    OverlayLayerPill(
                        zLabel = "Z4 아우터",
                        garment = outerGarment,
                        isVisible = showOuterLayer,
                        isActive = activeLayerKey == "OUTER",
                        onSelect = { activeLayerKey = "OUTER" },
                        onToggleVisible = { showOuterLayer = !showOuterLayer },
                        tag = "layer_pill_outer"
                    )
                    OverlayLayerPill(
                        zLabel = "Z3 상의",
                        garment = topGarment,
                        isVisible = showTopLayer,
                        isActive = activeLayerKey == "TOP",
                        onSelect = { activeLayerKey = "TOP" },
                        onToggleVisible = { showTopLayer = !showTopLayer },
                        tag = "layer_pill_top"
                    )
                    OverlayLayerPill(
                        zLabel = "Z2 하의",
                        garment = bottomGarment,
                        isVisible = showBottomLayer,
                        isActive = activeLayerKey == "BOTTOM",
                        onSelect = { activeLayerKey = "BOTTOM" },
                        onToggleVisible = { showBottomLayer = !showBottomLayer },
                        tag = "layer_pill_bottom"
                    )
                    OverlayLayerPill(
                        zLabel = "Z1 신발",
                        garment = shoesGarment,
                        isVisible = showShoesLayer,
                        isActive = activeLayerKey == "SHOES",
                        onSelect = { activeLayerKey = "SHOES" },
                        onToggleVisible = { showShoesLayer = !showShoesLayer },
                        tag = "layer_pill_shoes"
                    )
                }
            }
        }

        // 3. Interactive 3D Yaw Rotation & X-Ray Silhouette Opacity Sliders
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Slider 1: 3D Mannequin Turntable Rotation (-50° .. +50°)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "3D 마네킹 회전",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3D 마네킹 회전 (${yawAngleDeg.toInt()}°)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(126.dp)
                    )
                    Slider(
                        value = yawAngleDeg,
                        onValueChange = { yawAngleDeg = it },
                        valueRange = -50f..50f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overlay_yaw_slider")
                    )
                }

                // Slider 2: X-Ray Garment Opacity (Inspect body contour fit underneath clothes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "X-Ray 원단 투과도",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "X-Ray 핏 투과 (${(fabricOpacity * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(126.dp)
                    )
                    Slider(
                        value = fabricOpacity,
                        onValueChange = { fabricOpacity = it },
                        valueRange = 0.25f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overlay_xray_opacity_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayLayerPill(
    zLabel: String,
    garment: Garment?,
    isVisible: Boolean,
    isActive: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) AtelierChampagne.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) AtelierChampagne else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .clickable { onSelect() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (garment != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(parseHexColorSafe(garment.colorHex))
                )
            }
            Text(
                text = zLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isVisible) Color.White else Color.White.copy(alpha = 0.45f),
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
            )
            Icon(
                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "레이어 표시 토글",
                tint = if (isVisible) AtelierChampagne else Color.Gray,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onToggleVisible() }
            )
        }
    }
}

/**
 * Renders the 3D Volumetric Shaded Mannequin with cylindrical Phong lighting,
 * 3D perspective cross-section contour rings, and anatomical joint anchor markers.
 */
@Composable
private fun Volumetric3DMannequinCanvas(
    metrics: AvatarBodyMetrics,
    rig: Overlay3DAnatomicalRig,
    showWireframeRings: Boolean,
    showAnatomicalAnchors: Boolean,
    modifier: Modifier = Modifier
) {
    val baseSkinColor = remember(metrics.skinToneHex) { parseHexColorSafe(metrics.skinToneHex) }
    // Compute 3D highlight, midtone, and deep ambient shadow skin shades for volumetric rendering
    val highlightSkin = remember(baseSkinColor) {
        Color(
            red = (baseSkinColor.red * 1.14f).coerceAtMost(1f),
            green = (baseSkinColor.green * 1.12f).coerceAtMost(1f),
            blue = (baseSkinColor.blue * 1.10f).coerceAtMost(1f),
            alpha = 1f
        )
    }
    val shadowSkin = remember(baseSkinColor) {
        Color(
            red = (baseSkinColor.red * 0.62f).coerceAtLeast(0f),
            green = (baseSkinColor.green * 0.58f).coerceAtLeast(0f),
            blue = (baseSkinColor.blue * 0.56f).coerceAtLeast(0f),
            alpha = 1f
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f + (rig.parallaxSin * w * 0.03f)

        // 1. Studio 3D Perspective Grid & Spotlight Pedestal
        val floorY = h * 0.90f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    AtelierChampagne.copy(alpha = 0.28f),
                    Color(0xFF38BDF8).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, floorY),
                radius = w * 0.38f
            ),
            topLeft = Offset(w * 0.14f, floorY - 24f),
            size = Size(w * 0.72f, 48f)
        )
        drawOval(
            color = AtelierChampagne.copy(alpha = 0.45f),
            topLeft = Offset(w * 0.20f, floorY - 16f),
            size = Size(w * 0.60f, 32f),
            style = Stroke(width = 1.5f)
        )

        // 2. Parametric Anatomical Proportions with 3D Yaw Foreshortening
        val heightScale = ((metrics.heightCm - 170f) / 180f).coerceIn(-0.08f, 0.10f)
        val topHeadY = h * (0.09f - heightScale * 0.25f)
        val neckY = topHeadY + h * 0.095f
        val shoulderY = neckY + h * 0.038f
        val chestY = shoulderY + h * 0.085f
        val waistY = shoulderY + h * 0.205f
        val hipY = waistY + h * 0.095f
        val kneeY = hipY + h * 0.19f
        val ankleY = floorY - h * 0.035f

        val perspX = 0.72f + 0.28f * rig.perspectiveCos
        val shoulderHalfW = (w * 0.165f) * metrics.normalizedShoulderScale * perspX
        val chestHalfW = (w * 0.132f) * metrics.normalizedChestScale * perspX
        val waistHalfW = (w * 0.096f) * metrics.normalizedWaistScale * perspX
        val hipHalfW = (w * 0.136f) * metrics.normalizedHipScale * perspX

        val cylindricalBrush = Brush.horizontalGradient(
            0.0f to shadowSkin,
            0.28f to highlightSkin,
            0.55f to baseSkinColor,
            0.88f to shadowSkin,
            1.0f to Color(0xFF2B221E),
            startX = cx - shoulderHalfW * 1.2f,
            endX = cx + shoulderHalfW * 1.2f
        )

        // 3. Head & Neck (3D Shaded)
        val headRadiusX = w * 0.046f
        val headRadiusY = h * 0.042f
        drawRoundRect(
            brush = cylindricalBrush,
            topLeft = Offset(cx - w * 0.022f, neckY - h * 0.025f),
            size = Size(w * 0.044f, h * 0.048f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(highlightSkin, baseSkinColor, shadowSkin),
                center = Offset(cx - headRadiusX * 0.28f, topHeadY + headRadiusY * 0.75f),
                radius = headRadiusY * 1.35f
            ),
            topLeft = Offset(cx - headRadiusX, topHeadY),
            size = Size(headRadiusX * 2f, headRadiusY * 2f)
        )

        // 4. 3D Sculpted Torso & Pelvis Path
        val torsoPath = Path().apply {
            moveTo(cx - w * 0.024f, neckY)
            cubicTo(
                cx - shoulderHalfW * 0.55f, neckY + 4f,
                cx - shoulderHalfW * 0.92f, shoulderY - 6f,
                cx - shoulderHalfW, shoulderY
            )
            cubicTo(
                cx - shoulderHalfW * 0.96f, chestY - 10f,
                cx - chestHalfW * 1.04f, chestY,
                cx - chestHalfW, chestY + 8f
            )
            cubicTo(
                cx - chestHalfW * 0.92f, (chestY + waistY) * 0.5f,
                cx - waistHalfW * 1.04f, waistY - 8f,
                cx - waistHalfW, waistY
            )
            cubicTo(
                cx - waistHalfW * 1.05f, (waistY + hipY) * 0.5f,
                cx - hipHalfW, hipY - 8f,
                cx - hipHalfW, hipY
            )
            lineTo(cx - hipHalfW * 0.82f, hipY + h * 0.045f)
            lineTo(cx + hipHalfW * 0.82f, hipY + h * 0.045f)
            lineTo(cx + hipHalfW, hipY)
            cubicTo(
                cx + hipHalfW, hipY - 8f,
                cx + waistHalfW * 1.05f, (waistY + hipY) * 0.5f,
                cx + waistHalfW, waistY
            )
            cubicTo(
                cx + waistHalfW * 1.04f, waistY - 8f,
                cx + chestHalfW * 0.92f, (chestY + waistY) * 0.5f,
                cx + chestHalfW, chestY + 8f
            )
            cubicTo(
                cx + chestHalfW * 1.04f, chestY,
                cx + shoulderHalfW * 0.96f, chestY - 10f,
                cx + shoulderHalfW, shoulderY
            )
            cubicTo(
                cx + shoulderHalfW * 0.92f, shoulderY - 6f,
                cx + shoulderHalfW * 0.55f, neckY + 4f,
                cx + w * 0.024f, neckY
            )
            close()
        }
        drawPath(path = torsoPath, brush = cylindricalBrush)
        drawPath(
            path = torsoPath,
            color = AtelierChampagne.copy(alpha = 0.35f),
            style = Stroke(width = 1.4f)
        )

        // 5. 3D Arms & Legs
        val bmiVolume = (metrics.bmi / 21.5f).coerceIn(0.85f, 1.25f)
        val armWidth = w * 0.036f * bmiVolume
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx - shoulderHalfW + armWidth * 0.3f, shoulderY + 6f),
            end = Offset(cx - shoulderHalfW - w * 0.022f, hipY + h * 0.02f),
            strokeWidth = armWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx + shoulderHalfW - armWidth * 0.3f, shoulderY + 6f),
            end = Offset(cx + shoulderHalfW + w * 0.022f, hipY + h * 0.02f),
            strokeWidth = armWidth,
            cap = StrokeCap.Round
        )

        val legSpacing = hipHalfW * 0.46f
        val thighWidth = w * 0.058f * metrics.normalizedHipScale.coerceIn(0.88f, 1.15f)
        val calfWidth = thighWidth * 0.72f

        // Left Leg
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx - legSpacing, hipY + h * 0.02f),
            end = Offset(cx - legSpacing * 0.88f, kneeY),
            strokeWidth = thighWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx - legSpacing * 0.88f, kneeY),
            end = Offset(cx - legSpacing * 0.82f, ankleY),
            strokeWidth = calfWidth,
            cap = StrokeCap.Round
        )

        // Right Leg
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx + legSpacing, hipY + h * 0.02f),
            end = Offset(cx + legSpacing * 0.88f, kneeY),
            strokeWidth = thighWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = cylindricalBrush,
            start = Offset(cx + legSpacing * 0.88f, kneeY),
            end = Offset(cx + legSpacing * 0.82f, ankleY),
            strokeWidth = calfWidth,
            cap = StrokeCap.Round
        )

        // 6. Optional 3D Wireframe Cross-Section Contour Rings (Chest / Waist / Hip)
        if (showWireframeRings) {
            val ringTiltHeight = 14f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            // Chest 3D ellipse ring
            drawOval(
                color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                topLeft = Offset(cx - chestHalfW, chestY - ringTiltHeight * 0.5f),
                size = Size(chestHalfW * 2f, ringTiltHeight),
                style = Stroke(width = 1.6f, pathEffect = dashEffect)
            )
            // Waist 3D ellipse ring
            drawOval(
                color = AtelierChampagne.copy(alpha = 0.85f),
                topLeft = Offset(cx - waistHalfW, waistY - ringTiltHeight * 0.5f),
                size = Size(waistHalfW * 2f, ringTiltHeight),
                style = Stroke(width = 2.0f)
            )
            // Hip 3D ellipse ring
            drawOval(
                color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                topLeft = Offset(cx - hipHalfW, hipY - ringTiltHeight * 0.5f),
                size = Size(hipHalfW * 2f, ringTiltHeight),
                style = Stroke(width = 1.6f, pathEffect = dashEffect)
            )
        }

        // 7. Anatomical Joint Anchor Markers (for visual feedback of garment snapping)
        if (showAnatomicalAnchors) {
            val anchorPoints = listOf(
                Offset(cx, shoulderY),
                Offset(cx, waistY),
                Offset(cx, hipY),
                Offset(cx, ankleY)
            )
            anchorPoints.forEach { pt ->
                drawCircle(
                    color = AtelierChampagne.copy(alpha = 0.35f),
                    radius = 9f,
                    center = pt
                )
                drawCircle(
                    color = AtelierChampagne,
                    radius = 4f,
                    center = pt
                )
            }
        }
    }
}

private const val MAGNETIC_SNAP_THRESHOLD_PX = 36f

/**
 * Renders a single 3D-rigged draggable garment overlay layer on the virtual mannequin canvas,
 * applying perspective foreshortening, yaw rotation, and magnetic snap-back on release.
 */
@Composable
private fun FittingGarmentOverlayLayer(
    garment: Garment,
    cutoutBitmap: Bitmap?,
    zIndex: Float,
    anchorOffsetDp: Offset,
    readDeltaX: () -> Float,
    readDeltaY: () -> Float,
    layerSize: Dp,
    scaleX: Float,
    scaleY: Float,
    rotationY: Float,
    fabricOpacity: Float,
    autoMagneticSnap: Boolean,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onSnapReset: () -> Unit,
    tag: String
) {
    val density = LocalDensity.current
    val baseXPx = with(density) { anchorOffsetDp.x.dp.toPx() }
    val baseYPx = with(density) { anchorOffsetDp.y.dp.toPx() }

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            .offset {
                IntOffset(
                    (baseXPx + readDeltaX()).roundToInt(),
                    (baseYPx + readDeltaY()).roundToInt()
                )
            }
            .size(layerSize)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
                this.rotationY = rotationY
                alpha = fabricOpacity
            }
            .pointerInput(garment.id, autoMagneticSnap) {
                detectDragGestures(
                    onDragEnd = {
                        if (
                            autoMagneticSnap &&
                            abs(readDeltaX()) < MAGNETIC_SNAP_THRESHOLD_PX &&
                            abs(readDeltaY()) < MAGNETIC_SNAP_THRESHOLD_PX
                        ) {
                            onSnapReset()
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .testTag(tag)
    ) {
        GarmentCutoutIllustration(
            garment = garment,
            cutoutBitmap = cutoutBitmap,
            modifier = Modifier.fillMaxSize()
        )
    }
}
