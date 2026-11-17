package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.text.font.FontWeight
import com.example.data.local.FittingHistoryEntity
import com.example.data.model.Garment
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarBodyMetrics
import com.example.ui.components.AvatarMannequinDesignerCard
import com.example.ui.components.B2BTailoringFitSpecCard
import com.example.ui.components.GarmentCutoutIllustration
import com.example.ui.components.ParametricAvatarMannequinCanvas
import com.example.ui.components.RecentFittingTimelineSection
import com.example.ui.components.ThreeDOverlayFittingCanvas
import com.example.ui.components.ThreeJsFittingStudio
import com.example.ui.components.WheringDressMeSlotBar
import com.example.ui.theme.AtelierChampagne
import com.example.ui.theme.AtelierCoral
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagStrong
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.viewmodel.DataUiState
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun VirtualFittingScreen(
    profileState: DataUiState<UserProfile>,
    garmentsState: DataUiState<List<Garment>>,
    cutoutBitmaps: Map<String, Bitmap>,
    recentFittingHistory: List<FittingHistoryEntity> = emptyList(),
    onRecordFittingAttempt: (
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        metrics: AvatarBodyMetrics,
        canvasModeLabel: String,
        triggerSourceLabel: String,
        garmentScale: Float,
        skipIfDuplicateOfLatest: Boolean,
        showSnackbar: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onTogglePinFittingRecord: (FittingHistoryEntity) -> Unit = {},
    onDeleteFittingRecord: (Long) -> Unit = {},
    onClearUnpinnedFittingHistory: () -> Unit = {},
    onSaveBodyProfile: (UserProfile) -> Unit,
    onSaveVirtualOutfit: (
        title: String,
        tpoOccasion: String,
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        aiStylingNote: String,
        fitScore: Int
    ) -> Unit
) {
    val profile = (profileState as? DataUiState.Success)?.data ?: UserProfile()
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()

    // Live parametric body metrics state synchronized with Firestore UserProfile
    var liveMetrics by remember(
        profile.displayName,
        profile.heightCm,
        profile.weightKg,
        profile.shoulderWidthCm,
        profile.waistCm,
        profile.bodyShape,
        profile.skinToneHex
    ) {
        mutableStateOf(AvatarBodyMetrics.fromUserProfile(profile))
    }

    val outers = remember(garments) { garments.filter { it.category == "OUTER" } }
    val tops = remember(garments) { garments.filter { it.category == "TOP" } }
    val bottoms = remember(garments) { garments.filter { it.category == "BOTTOM" } }
    val shoesList = remember(garments) { garments.filter { it.category == "SHOES" } }

    var selectedOuter by remember(outers) { mutableStateOf(outers.firstOrNull()) }
    var selectedTop by remember(tops) { mutableStateOf(tops.firstOrNull()) }
    var selectedBottom by remember(bottoms) { mutableStateOf(bottoms.firstOrNull()) }
    var selectedShoes by remember(shoesList) { mutableStateOf(shoesList.firstOrNull()) }

    var outerLocked by remember { mutableStateOf(false) }
    var topLocked by remember { mutableStateOf(false) }
    var bottomLocked by remember { mutableStateOf(false) }
    var shoesLocked by remember { mutableStateOf(false) }

    // Interactive Drag & Drop offsets and scale per layer
    var outerOffsetX by remember { mutableFloatStateOf(22f) }
    var outerOffsetY by remember { mutableFloatStateOf(-42f) }
    var topOffsetX by remember { mutableFloatStateOf(0f) }
    var topOffsetY by remember { mutableFloatStateOf(-48f) }
    var bottomOffsetX by remember { mutableFloatStateOf(0f) }
    var bottomOffsetY by remember { mutableFloatStateOf(52f) }
    var shoesOffsetX by remember { mutableFloatStateOf(0f) }
    var shoesOffsetY by remember { mutableFloatStateOf(148f) }
    var garmentScale by remember { mutableFloatStateOf(1.0f) }

    // Canvas mode: "3D_OVERLAY" (3D Mannequin + Multi-Layer Garment Overlay Canvas), "3D_WEBGL" (Three.js 360°), "2D_CAD" (2D Blueprint)
    var canvasRenderMode by remember { mutableStateOf("3D_OVERLAY") }
    val is3dStudioMode = canvasRenderMode == "3D_WEBGL"
    var is3dFullScreen by remember { mutableStateOf(false) }
    var showCalipersOnFittingCanvas by remember { mutableStateOf(false) }

    var activeCategoryTab by remember { mutableStateOf("TOP") }
    var rouletteReason by remember { mutableStateOf<String?>(null) }

    // Top sub-tab: "CANVAS" (가상 피팅 캔버스) vs "RECENT_TIMELINE" (최근 피팅 타임라인)
    var fittingSubTab by remember { mutableStateOf("CANVAS") }
    var lastTriggerSource by remember { mutableStateOf("초기 기본 피팅") }

    val canvasModeLabel = when (canvasRenderMode) {
        "3D_OVERLAY" -> "3D 오버레이 피팅"
        "3D_WEBGL" -> "3D 아틀리에 (360°)"
        else -> "2D 아바타 도면"
    }

    LaunchedEffect(garments.size) {
        if (selectedTop == null) selectedTop = tops.firstOrNull()
        if (selectedBottom == null) selectedBottom = bottoms.firstOrNull()
        if (selectedShoes == null) selectedShoes = shoesList.firstOrNull()
    }

    // Automatically record every unique outfit combination tried on the Virtual Fitting Canvas into local Room DB
    LaunchedEffect(selectedOuter?.id, selectedTop?.id, selectedBottom?.id, selectedShoes?.id) {
        if (selectedTop != null || selectedBottom != null || selectedOuter != null) {
            onRecordFittingAttempt(
                selectedOuter,
                selectedTop,
                selectedBottom,
                selectedShoes,
                liveMetrics,
                canvasModeLabel,
                lastTriggerSource,
                garmentScale,
                true,
                false
            )
        }
    }

    val bodyShape = liveMetrics.bodyShape

    if (is3dFullScreen && is3dStudioMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("virtual_fitting_3d_fullscreen")
        ) {
            ThreeJsFittingStudio(
                profile = profile,
                bodyShape = bodyShape,
                outerGarment = selectedOuter,
                topGarment = selectedTop,
                bottomGarment = selectedBottom,
                shoesGarment = selectedShoes,
                garmentScale = garmentScale,
                isFullScreen = true,
                onToggleFullScreen = { is3dFullScreen = false },
                bodyMetrics = liveMetrics,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("virtual_fitting_screen")
    ) {
        // 0. Top Sub-Tab Switcher (ZIGZAG 24dp control pill)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZigzagCanvas,
            border = androidx.compose.foundation.BorderStroke(1.dp, ZigzagHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { fittingSubTab = "CANVAS" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fittingSubTab == "CANVAS") ZigzagStrong else Color.Transparent,
                        contentColor = if (fittingSubTab == "CANVAS") ZigzagCtaText else ZigzagMuted
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("subtab_fitting_canvas")
                ) {
                    Text(
                        text = "가상 피팅 캔버스",
                        style = com.example.ui.theme.ZigzagTabSelected.copy(
                            fontSize = 14.sp,
                            color = if (fittingSubTab == "CANVAS") ZigzagCtaText else ZigzagMuted
                        )
                    )
                }

                Button(
                    onClick = { fittingSubTab = "RECENT_TIMELINE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fittingSubTab == "RECENT_TIMELINE") ZigzagStrong else Color.Transparent,
                        contentColor = if (fittingSubTab == "RECENT_TIMELINE") ZigzagCtaText else ZigzagMuted
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("subtab_recent_fitting")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (fittingSubTab == "RECENT_TIMELINE") ZigzagCtaText else ZigzagMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "최근 피팅 (${recentFittingHistory.size})",
                        style = com.example.ui.theme.ZigzagTabSelected.copy(
                            fontSize = 14.sp,
                            color = if (fittingSubTab == "RECENT_TIMELINE") ZigzagCtaText else ZigzagMuted
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (fittingSubTab == "RECENT_TIMELINE") {
            RecentFittingTimelineSection(
                historyList = recentFittingHistory,
                cutoutBitmaps = cutoutBitmaps,
                onRestoreToCanvas = { record ->
                    selectedOuter = record.outerId?.let { id -> outers.firstOrNull { it.id == id } }
                        ?: record.outerName?.let { name -> outers.firstOrNull { it.name == name } }
                    selectedTop = record.topId?.let { id -> tops.firstOrNull { it.id == id } }
                        ?: record.topName?.let { name -> tops.firstOrNull { it.name == name } }
                        ?: selectedTop
                    selectedBottom = record.bottomId?.let { id -> bottoms.firstOrNull { it.id == id } }
                        ?: record.bottomName?.let { name -> bottoms.firstOrNull { it.name == name } }
                        ?: selectedBottom
                    selectedShoes = record.shoesId?.let { id -> shoesList.firstOrNull { it.id == id } }
                        ?: record.shoesName?.let { name -> shoesList.firstOrNull { it.name == name } }
                        ?: selectedShoes
                    garmentScale = record.garmentScale.coerceIn(0.75f, 1.30f)
                    rouletteReason = "🕒 최근 피팅 타임라인에서 [${record.sessionTitle}] 조합을 캔버스에 복원했습니다!"
                    fittingSubTab = "CANVAS"
                },
                onTogglePin = onTogglePinFittingRecord,
                onDeleteRecord = onDeleteFittingRecord,
                onClearUnpinnedHistory = onClearUnpinnedFittingHistory,
                onRecordCurrentSnapshot = {
                    onRecordFittingAttempt(
                        selectedOuter,
                        selectedTop,
                        selectedBottom,
                        selectedShoes,
                        liveMetrics,
                        canvasModeLabel,
                        "📸 수동 스냅샷",
                        garmentScale,
                        false,
                        true
                    )
                }
            )
            return@Column
        }

        // 1. Dedicated Body Measurement Form & Parametric Base Avatar Mannequin Designer
        AvatarMannequinDesignerCard(
            profile = profile,
            metrics = liveMetrics,
            onMetricsChanged = { updatedMetrics ->
                liveMetrics = updatedMetrics
            },
            onSaveProfile = { updatedProfile ->
                onSaveBodyProfile(updatedProfile)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Interactive Virtual Fitting Canvas (Uses the Parametric Base Mannequin + Garment Layers or 3D Atelier)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F3EC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = canvasRenderMode == "3D_OVERLAY",
                            onClick = { canvasRenderMode = "3D_OVERLAY" },
                            label = { Text("3D 오버레이 피팅") },
                            modifier = Modifier.testTag("toggle_3d_overlay_chip")
                        )
                        FilterChip(
                            selected = canvasRenderMode == "3D_WEBGL",
                            onClick = { canvasRenderMode = "3D_WEBGL" },
                            label = { Text("3D 아틀리에 (360°)") },
                            modifier = Modifier.testTag("toggle_3d_studio_chip")
                        )
                        FilterChip(
                            selected = canvasRenderMode == "2D_CAD",
                            onClick = { canvasRenderMode = "2D_CAD" },
                            label = { Text("2D 아바타 도면") },
                            modifier = Modifier.testTag("toggle_2d_canvas_chip")
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row {
                        Button(
                            onClick = {
                                // Random Coordi Roulette Gamification
                                lastTriggerSource = "🎲 랜덤 룰렛"
                                if (tops.isNotEmpty()) selectedTop = tops[Random.nextInt(tops.size)]
                                if (bottoms.isNotEmpty()) selectedBottom = bottoms[Random.nextInt(bottoms.size)]
                                if (shoesList.isNotEmpty()) selectedShoes = shoesList[Random.nextInt(shoesList.size)]
                                selectedOuter = if (outers.isNotEmpty() && Random.nextBoolean()) {
                                    outers[Random.nextInt(outers.size)]
                                } else outers.firstOrNull()
                                rouletteReason = "🎲 랜덤 코디 룰렛이 [${selectedTop?.colorName ?: ""} + ${selectedBottom?.colorName ?: ""}] 톤앤매너 매칭을 완성했어요!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AtelierCoral,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("random_roulette_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "랜덤 코디 룰렛",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("랜덤 룰렛", style = MaterialTheme.typography.labelLarge)
                        }

                        if (canvasRenderMode == "2D_CAD") {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    outerOffsetX = 22f
                                    outerOffsetY = -42f
                                    topOffsetX = 0f
                                    topOffsetY = -48f
                                    bottomOffsetX = 0f
                                    bottomOffsetY = 52f
                                    shoesOffsetX = 0f
                                    shoesOffsetY = 148f
                                    garmentScale = 1.0f
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "위치 초기화",
                                    tint = AtelierMidnight
                                )
                            }
                        }
                    }
                }

                if (rouletteReason != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = AtelierChampagne.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = rouletteReason!!,
                            style = MaterialTheme.typography.labelLarge,
                            color = AtelierMidnight,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (canvasRenderMode) {
                    "3D_OVERLAY" -> {
                        ThreeDOverlayFittingCanvas(
                            metrics = liveMetrics,
                            outerGarment = selectedOuter,
                            topGarment = selectedTop,
                            bottomGarment = selectedBottom,
                            shoesGarment = selectedShoes,
                            cutoutBitmaps = cutoutBitmaps,
                            garmentScale = garmentScale,
                            onGarmentScaleChange = { garmentScale = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "3D_WEBGL" -> {
                        ThreeJsFittingStudio(
                            profile = profile,
                            bodyShape = bodyShape,
                            outerGarment = selectedOuter,
                            topGarment = selectedTop,
                            bottomGarment = selectedBottom,
                            shoesGarment = selectedShoes,
                            garmentScale = garmentScale,
                            isFullScreen = false,
                            onToggleFullScreen = { is3dFullScreen = true },
                            bodyMetrics = liveMetrics,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        )
                    }

                    else -> {
                    // 2D Parametric Mannequin + Z-index Draggable Garment Layers
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFEDE8DF))
                            .border(1.dp, Color(0xFFD8D0C2), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Base Layer (Z=0): High-Precision Parametric Avatar Mannequin
                        ParametricAvatarMannequinCanvas(
                            metrics = liveMetrics,
                            showCalipers = showCalipersOnFittingCanvas,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0f)
                        )

                        // Toggle chip to inspect base mannequin calipers under clothes
                        Surface(
                            color = AtelierMidnight.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .zIndex(10f)
                                .clickable { showCalipersOnFittingCanvas = !showCalipersOnFittingCanvas }
                        ) {
                            Text(
                                text = if (showCalipersOnFittingCanvas) "치수선 ON" else "치수선 OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierChampagne,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        // Layer 1 (Z=1f): Shoes
                        selectedShoes?.let { shoes ->
                            Box(
                                modifier = Modifier
                                    .zIndex(1f)
                                    .offset { IntOffset(shoesOffsetX.roundToInt(), shoesOffsetY.roundToInt()) }
                                    .size(95.dp)
                                    .scale(garmentScale)
                                    .pointerInput(shoes.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            shoesOffsetX += dragAmount.x
                                            shoesOffsetY += dragAmount.y
                                        }
                                    }
                            ) {
                                GarmentCutoutIllustration(
                                    garment = shoes,
                                    cutoutBitmap = cutoutBitmaps[shoes.id],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Layer 2 (Z=2f): Bottom (Pants / Skirt)
                        selectedBottom?.let { bottom ->
                            Box(
                                modifier = Modifier
                                    .zIndex(2f)
                                    .offset { IntOffset(bottomOffsetX.roundToInt(), bottomOffsetY.roundToInt()) }
                                    .size((156f * liveMetrics.normalizedHipScale.coerceIn(0.88f, 1.16f)).dp)
                                    .scale(garmentScale)
                                    .pointerInput(bottom.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            bottomOffsetX += dragAmount.x
                                            bottomOffsetY += dragAmount.y
                                        }
                                    }
                            ) {
                                GarmentCutoutIllustration(
                                    garment = bottom,
                                    cutoutBitmap = cutoutBitmaps[bottom.id],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Layer 3 (Z=3f): Top (Shirt / Knit)
                        selectedTop?.let { top ->
                            Box(
                                modifier = Modifier
                                    .zIndex(3f)
                                    .offset { IntOffset(topOffsetX.roundToInt(), topOffsetY.roundToInt()) }
                                    .size((148f * liveMetrics.normalizedShoulderScale.coerceIn(0.88f, 1.18f)).dp)
                                    .scale(garmentScale)
                                    .pointerInput(top.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            topOffsetX += dragAmount.x
                                            topOffsetY += dragAmount.y
                                        }
                                    }
                            ) {
                                GarmentCutoutIllustration(
                                    garment = top,
                                    cutoutBitmap = cutoutBitmaps[top.id],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Layer 4 (Z=4f): Outer (Coat / Jacket)
                        selectedOuter?.let { outer ->
                            Box(
                                modifier = Modifier
                                    .zIndex(4f)
                                    .offset { IntOffset(outerOffsetX.roundToInt(), outerOffsetY.roundToInt()) }
                                    .size((168f * liveMetrics.normalizedShoulderScale.coerceIn(0.88f, 1.18f)).dp)
                                    .scale(garmentScale)
                                    .pointerInput(outer.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            outerOffsetX += dragAmount.x
                                            outerOffsetY += dragAmount.y
                                        }
                                    }
                            ) {
                                GarmentCutoutIllustration(
                                    garment = outer,
                                    cutoutBitmap = cutoutBitmaps[outer.id],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scale Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "핏 크기 조절",
                        tint = AtelierMidnight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "의류 핏 볼륨 (${(garmentScale * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = AtelierMidnight
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = garmentScale,
                        onValueChange = { garmentScale = it },
                        valueRange = 0.75f..1.30f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2.5 Whering 'Dress Me' Slot-Machine Controller & 6-Formula Color Harmony Bar
        WheringDressMeSlotBar(
            outer = selectedOuter,
            top = selectedTop,
            bottom = selectedBottom,
            shoes = selectedShoes,
            outerLocked = outerLocked,
            topLocked = topLocked,
            bottomLocked = bottomLocked,
            shoesLocked = shoesLocked,
            bodyShape = bodyShape,
            onToggleOuterLock = { outerLocked = !outerLocked },
            onToggleTopLock = { topLocked = !topLocked },
            onToggleBottomLock = { bottomLocked = !bottomLocked },
            onToggleShoesLock = { shoesLocked = !shoesLocked },
            onCycleOuter = { delta ->
                if (!outerLocked && outers.isNotEmpty()) {
                    lastTriggerSource = "🎰 슬롯 교차 매칭"
                    val idx = outers.indexOfFirst { it.id == selectedOuter?.id }
                    val nextIdx = if (idx < 0) 0 else (idx + delta + outers.size) % outers.size
                    selectedOuter = outers[nextIdx]
                }
            },
            onCycleTop = { delta ->
                if (!topLocked && tops.isNotEmpty()) {
                    lastTriggerSource = "🎰 슬롯 교차 매칭"
                    val idx = tops.indexOfFirst { it.id == selectedTop?.id }.coerceAtLeast(0)
                    selectedTop = tops[(idx + delta + tops.size) % tops.size]
                }
            },
            onCycleBottom = { delta ->
                if (!bottomLocked && bottoms.isNotEmpty()) {
                    lastTriggerSource = "🎰 슬롯 교차 매칭"
                    val idx = bottoms.indexOfFirst { it.id == selectedBottom?.id }.coerceAtLeast(0)
                    selectedBottom = bottoms[(idx + delta + bottoms.size) % bottoms.size]
                }
            },
            onCycleShoes = { delta ->
                if (!shoesLocked && shoesList.isNotEmpty()) {
                    lastTriggerSource = "🎰 슬롯 교차 매칭"
                    val idx = shoesList.indexOfFirst { it.id == selectedShoes?.id }.coerceAtLeast(0)
                    selectedShoes = shoesList[(idx + delta + shoesList.size) % shoesList.size]
                }
            },
            onSpinUnlockedSlots = {
                lastTriggerSource = "🎰 슬롯머신 스핀"
                if (!outerLocked) {
                    selectedOuter = if (outers.isNotEmpty() && Random.nextBoolean()) {
                        outers[Random.nextInt(outers.size)]
                    } else null
                }
                if (!topLocked && tops.isNotEmpty()) {
                    selectedTop = tops[Random.nextInt(tops.size)]
                }
                if (!bottomLocked && bottoms.isNotEmpty()) {
                    selectedBottom = bottoms[Random.nextInt(bottoms.size)]
                }
                if (!shoesLocked && shoesList.isNotEmpty()) {
                    selectedShoes = shoesList[Random.nextInt(shoesList.size)]
                }
                rouletteReason = "🎰 잠금(🔒)된 아이템은 고정한 채 나머지 슬롯을 색채학 하모니 기준으로 리믹스했습니다!"
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2.8 B2B Pro CAD Tailoring Fit & Ease Allowance Inspector
        B2BTailoringFitSpecCard(
            metrics = liveMetrics,
            outer = selectedOuter,
            top = selectedTop,
            bottom = selectedBottom,
            garmentScale = garmentScale
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Swipeable Category Carousels for Outfit Matching
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            val tabs = listOf(
                "OUTER" to "아우터 (${outers.size})",
                "TOP" to "상의 (${tops.size})",
                "BOTTOM" to "하의 (${bottoms.size})",
                "SHOES" to "신발 (${shoesList.size})"
            )
            tabs.forEach { (code, label) ->
                FilterChip(
                    selected = activeCategoryTab == code,
                    onClick = { activeCategoryTab = code },
                    label = { Text(label) }
                )
            }
            if (activeCategoryTab == "OUTER" && selectedOuter != null) {
                TextButton(onClick = {
                    lastTriggerSource = "직접 매칭"
                    selectedOuter = null
                }) {
                    Text("아우터 벗기")
                }
            }
        }

        val activeList = when (activeCategoryTab) {
            "OUTER" -> outers
            "TOP" -> tops
            "BOTTOM" -> bottoms
            else -> shoesList
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(activeList, key = { it.id.ifBlank { it.name } }) { item ->
                val isSelected = when (activeCategoryTab) {
                    "OUTER" -> selectedOuter?.id == item.id
                    "TOP" -> selectedTop?.id == item.id
                    "BOTTOM" -> selectedBottom?.id == item.id
                    else -> selectedShoes?.id == item.id
                }
                SwipeableGarmentMiniCard(
                    garment = item,
                    cutoutBitmap = cutoutBitmaps[item.id],
                    isSelected = isSelected,
                    onClick = {
                        lastTriggerSource = "직접 매칭"
                        when (activeCategoryTab) {
                            "OUTER" -> selectedOuter = if (selectedOuter?.id == item.id) null else item
                            "TOP" -> selectedTop = item
                            "BOTTOM" -> selectedBottom = item
                            "SHOES" -> selectedShoes = item
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Save Virtual Fitting Coordi Button
        Button(
            onClick = {
                onRecordFittingAttempt(
                    selectedOuter,
                    selectedTop,
                    selectedBottom,
                    selectedShoes,
                    liveMetrics,
                    canvasModeLabel,
                    "✅ 코디 확정 저장",
                    garmentScale,
                    false,
                    false
                )
                val summaryTitle = buildString {
                    append(bodyShape.labelKo.substringBefore(" "))
                    append(" 맞춤 ")
                    append(selectedTop?.colorName ?: "데일리")
                    append(" 코디")
                }
                onSaveVirtualOutfit(
                    summaryTitle,
                    "데일리",
                    selectedOuter,
                    selectedTop,
                    selectedBottom,
                    selectedShoes,
                    bodyShape.stylingTip,
                    95
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_virtual_outfit_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("오늘 이 코디 입기 & 저장 (착용 횟수 +1 자동 반영)")
        }
    }
}

@Composable
private fun SwipeableGarmentMiniCard(
    garment: Garment,
    cutoutBitmap: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(126.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F1EA))
            ) {
                GarmentCutoutIllustration(
                    garment = garment,
                    cutoutBitmap = cutoutBitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = garment.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${garment.colorName} · ₩${String.format("%,d", garment.costPerWear)}/회",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
