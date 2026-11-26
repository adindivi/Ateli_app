package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.CuratedWardrobePresets
import com.example.data.model.Garment
import com.example.data.model.UserProfile
import com.example.ui.components.AclosetGarmentDetailModal
import com.example.ui.components.AclosetQuickAddBottomSheet
import com.example.ui.components.CapsuleWardrobeReportSheet
import com.example.ui.components.FriendlyReassuranceDialog
import com.example.ui.components.GarmentCutoutIllustration
import com.example.ui.components.GarmentPhotoCutoutSheet
import com.example.ui.components.PersonalColorDiagnosisDialog
import com.example.ui.components.ZigzagBottomSheetDragHandle
import com.example.ui.components.drawGarmentSilhouette
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.ZigzagAppBanner
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagStrong
import com.example.util.CapsuleWardrobeOptimizer
import com.example.util.PersonalColorAnalyzer
import com.example.util.PersonalColorSeason
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.DataUiState

/**
 * WardrobeScreen
 *
 * 에디토리얼 비주얼 그리드, 멀티 탭 코디 조합(Floating Lookbar), 실물 의류 누끼 및 코디 추천을 제공하는 스마트 옷장 메인 화면.
 *
 * Benchmark References:
 * - Libre-Closet: https://github.com/Lazztech/Libre-Closet
 * - wardrobe-graph: https://github.com/SirLocke/wardrobe-graph
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeScreen(
    garmentsState: DataUiState<List<Garment>>,
    aiRegistrationState: AiTaskState<List<Garment>>,
    cutoutBitmaps: Map<String, Bitmap>,
    onSeedStarterWardrobe: () -> Unit,
    onParseReceipt: (String) -> Unit,
    onProcessPhoto: (Bitmap, Long, String) -> Unit,
    onQuickAddGarment: (Garment) -> Unit = {},
    onPhotoCutoutRegistered: (Garment, Bitmap?) -> Unit = { _, _ -> },
    onIncrementWear: (Garment) -> Unit,
    onDeleteGarment: (String) -> Unit,
    onResetAiState: () -> Unit,
    onOpenVirtualFitting: () -> Unit,
    onOpenVirtualFittingWithLook: (List<Garment>) -> Unit = { onOpenVirtualFitting() },
    userProfileState: DataUiState<UserProfile> = DataUiState.Success(UserProfile()),
    onSavePersonalColorTone: (PersonalColorSeason) -> Unit = {},
    onShowToast: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile = (userProfileState as? DataUiState.Success)?.data ?: UserProfile()
    val activeSeason = remember(userProfile.personalColorTone) {
        PersonalColorSeason.fromCode(userProfile.personalColorTone)
    }

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showReceiptSheet by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showPersonalColorDialog by remember { mutableStateOf(false) }
    var showCapsuleReportSheet by remember { mutableStateOf(false) }
    var detailModalGarment by remember { mutableStateOf<Garment?>(null) }
    var pendingDeleteGarment by remember { mutableStateOf<Garment?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()
    var selectedGarmentIdsForLook by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedGarmentsForLook = remember(selectedGarmentIdsForLook, garments) {
        garments.filter { g ->
            val key = g.stableKey
            selectedGarmentIdsForLook.contains(key)
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("RECENT") }

    val capsuleReport = remember(garments) { CapsuleWardrobeOptimizer.analyze(garments) }
    val frequentWearRate = remember(garments) {
        if (garments.isEmpty()) 0
        else {
            val frequentCount = garments.count { it.wearCount >= 2L }
            ((frequentCount.toDouble() / garments.size.toDouble()) * 100.0).toInt()
        }
    }

    val filteredGarments = remember(garments, selectedCategoryFilter, searchQuery, sortMode) {
        val byCat = if (selectedCategoryFilter == "ALL") garments
        else garments.filter { it.category == selectedCategoryFilter }

        val byQuery = if (searchQuery.isBlank()) byCat
        else {
            val q = searchQuery.trim().lowercase()
            byCat.filter { g ->
                g.name.lowercase().contains(q) ||
                    g.brand.lowercase().contains(q) ||
                    g.colorName.lowercase().contains(q) ||
                    g.subCategory.lowercase().contains(q) ||
                    g.tpoTags.any { it.lowercase().contains(q) }
            }
        }

        when (sortMode) {
            "BEST_CPW" -> byQuery.sortedWith(compareBy<Garment> { it.costPerWear }.thenByDescending { it.wearCount })
            "SLEEPER_FIRST" -> byQuery.sortedWith(compareBy<Garment> { it.wearCount }.thenByDescending { it.purchasePrice })
            "MOST_WORN" -> byQuery.sortedByDescending { it.wearCount }
            else -> byQuery
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 162.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (selectedGarmentsForLook.isNotEmpty()) 104.dp else 84.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxHeight()
                .testTag("wardrobe_grid")
        ) {
            // 1. Slim Minimal Registration Bar (Zero-Scroll & Apple Store Style)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wardrobe_quick_action_bar"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button 1: 영수증 등록
                    Button(
                        onClick = {
                            onResetAiState()
                            showReceiptSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZigzagStrong,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("open_receipt_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "영수증 등록",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "영수증 등록",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Button 2: 사진 등록
                    OutlinedButton(
                        onClick = {
                            onResetAiState()
                            showPhotoSheet = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ZigzagStrong
                        ),
                        border = BorderStroke(1.dp, ZigzagHairline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("open_camera_cutout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "사진 등록",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "사진 등록",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Button 3: 직접 등록
                    Surface(
                        onClick = { showQuickAddSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .width(76.dp)
                            .height(44.dp)
                            .testTag("open_quick_add_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "직접 등록",
                                    tint = ZigzagStrong,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "직접",
                                    style = TextStyle(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ZigzagStrong,
                                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 1.5 Apple Style Minimalist Wardrobe 3-Metric Summary Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wardrobe_metrics_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF5F5F7),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Metric 1: 총 내 옷장
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "내 옷장",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "총 ${garments.size}벌",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZigzagStrong,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "나만의 컬렉션",
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Divider 1
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(Color.Black.copy(alpha = 0.08f))
                        )

                        // Metric 2: 가능한 코디 조합 수
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "코디 조합",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${capsuleReport.possibleOutfitsCount}가지",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0071E3),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "무한 스타일링",
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Divider 2
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(Color.Black.copy(alpha = 0.08f))
                        )

                        // Metric 3: 스타일 활용도 & 캡슐 점수
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "스타일 활용도",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${capsuleReport.capsuleEfficiencyScore}점",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZigzagStrong,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "자주 입는 옷 ${frequentWearRate}%",
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF86868B),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 1.8 Personal Color & Capsule Wardrobe Quick Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 퍼스널 컬러 진단 버튼
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { showPersonalColorDialog = true }
                            .testTag("btn_open_personal_color"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(activeSeason.representativeHex)))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeSeason.labelKo} 진단",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZigzagStrong,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 캡슐 옷장 결핍 리포트 버튼
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { showCapsuleReportSheet = true }
                            .testTag("btn_open_capsule_report"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0071E3).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF0071E3).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📊 캡슐 결핍(Gap) 리포트",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0071E3),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 2. Search & Smart Sort + Category Filter Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("옷 이름이나 색깔로 찾아보세요") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wardrobe_search_input")
                    )

                    // Apple Store Style Products Category Showcase Cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val appleCategories = listOf(
                            Triple("ALL", "전체", "OUTER"),
                            Triple("OUTER", "아우터", "OUTER"),
                            Triple("TOP", "상의", "TOP"),
                            Triple("BOTTOM", "하의", "BOTTOM"),
                            Triple("SHOES", "신발", "SHOES"),
                            Triple("BAG", "잡화", "BAG")
                        )
                        appleCategories.forEach { (code, label, iconCategory) ->
                            val selected = selectedCategoryFilter == code
                            Surface(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(86.dp)
                                    .clickable {
                                        selectedCategoryFilter = code
                                        if (selectedGarmentIdsForLook.isEmpty()) {
                                            onShowToast(if (code == "ALL") "전체 옷을 보여드려요" else "${label} 목록을 보여드려요")
                                        }
                                    }
                                    .testTag("filter_chip_$code"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFF0071E3).copy(alpha = 0.08f) else Color(0xFFF5F5F7),
                                border = if (selected) BorderStroke(1.5.dp, Color(0xFF0071E3)) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.04f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier.size(42.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawGarmentSilhouette(
                                                category = iconCategory,
                                                subCategory = "기본",
                                                baseColor = if (selected) Color(0xFF0071E3) else Color(0xFF3A3A3C),
                                                pattern = "솔리드"
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (code == "ALL") "전체 (${garments.size})" else label,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color(0xFF0071E3) else ZigzagStrong,
                                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val sorts = listOf(
                            "RECENT" to "최신순",
                            "BEST_CPW" to "많이 입은순",
                            "SLEEPER_FIRST" to "안 입는 순"
                        )
                        sorts.forEach { (code, label) ->
                            val selected = sortMode == code
                            AssistChip(
                                onClick = {
                                    sortMode = code
                                    if (selectedGarmentIdsForLook.isEmpty()) {
                                        onShowToast("${label}으로 정렬했어요")
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = if (selected) com.example.ui.theme.ZigzagTabSelected.copy(fontSize = 13.sp) else com.example.ui.theme.ZigzagMetadata.copy(fontSize = 13.sp),
                                        color = if (selected) ZigzagStrong else ZigzagMuted
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected) ZigzagAppBanner else ZigzagCanvas,
                                    labelColor = if (selected) ZigzagStrong else ZigzagMuted
                                ),
                                border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (selected) ZigzagForeground else ZigzagHairline
                                ),
                                modifier = Modifier.testTag("sort_chip_$code")
                            )
                        }
                    }
                }
            }

            // 3. 의류 목록 카드 그리드
            if (filteredGarments.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = ZigzagMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "조건에 맞는 옷이 없어요",
                                style = MaterialTheme.typography.titleMedium,
                                color = ZigzagStrong
                            )
                        }
                    }
                }
            } else {
                items(filteredGarments, key = { it.stableKey }) { garment ->
                    val itemKey = garment.stableKey
                    val isSelected = selectedGarmentIdsForLook.contains(itemKey)
                    GarmentWardrobeCard(
                        garment = garment,
                        itemKey = itemKey,
                        personalColorSeason = activeSeason,
                        cutoutBitmap = cutoutBitmaps[garment.id] ?: cutoutBitmaps[itemKey],
                        isSelected = isSelected,
                        hasActiveSelection = selectedGarmentIdsForLook.isNotEmpty(),
                        onToggleSelect = {
                            selectedGarmentIdsForLook = if (isSelected) {
                                selectedGarmentIdsForLook - itemKey
                            } else {
                                selectedGarmentIdsForLook + itemKey
                            }
                        },
                        onIncrementWear = {
                            onIncrementWear(garment)
                            onShowToast("'${garment.name}' 착용 1회가 기록되었어요")
                        },
                        onDelete = { pendingDeleteGarment = garment },
                        onOpenFitting = { detailModalGarment = garment }
                    )
                }
            }
        }

        // 4. 멀티 탭 코디 메이커: 플로팅 룩 조합 바
        FloatingLookCombinerBar(
            selectedGarments = selectedGarmentsForLook,
            cutoutBitmaps = cutoutBitmaps,
            activeSeason = activeSeason,
            onClearSelection = { selectedGarmentIdsForLook = emptySet() },
            onFitLook = {
                onShowToast("${selectedGarmentsForLook.size}벌 조합으로 아바타 피팅을 시작해요")
                onOpenVirtualFittingWithLook(selectedGarmentsForLook)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }

    // Modal Bottom Sheet 1: Electronic Receipt
    if (showReceiptSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var receiptInput by remember {
            mutableStateOf(CuratedWardrobePresets.sampleReceiptTemplates.first().rawReceiptText)
        }

        ModalBottomSheet(
            onDismissRequest = { showReceiptSheet = false },
            sheetState = sheetState,
            dragHandle = { ZigzagBottomSheetDragHandle() },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = ZigzagCanvas
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "전자영수증 · 쇼핑몰 내역 AI 자동 등록",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "결제 완료 문자나 이메일 영수증을 선택하거나 붙여넣으면 Gemini AI가 브랜드·가격·색상·카테고리·TPO 태그를 추출해 옷장에 등록합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onParseReceipt(receiptInput)
                        showReceiptSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("영수증 분석 및 등록하기")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet 2: AI Photo Cutout with RMBG-2.0 / ML Kit
    if (showPhotoSheet) {
        GarmentPhotoCutoutSheet(
            onDismissRequest = { showPhotoSheet = false },
            onGarmentCreated = { garment, bmp ->
                onPhotoCutoutRegistered(garment, bmp)
                onShowToast("'${garment.name}'이(가) 옷장에 깔끔하게 등록되었어요")
            }
        )
    }

    // Modal Dialog: Personal Color Diagnosis
    if (showPersonalColorDialog) {
        PersonalColorDiagnosisDialog(
            currentToneCode = userProfile.personalColorTone,
            onDismissRequest = { showPersonalColorDialog = false },
            onToneSelected = { season ->
                onSavePersonalColorTone(season)
                onShowToast("퍼스널 컬러가 '${season.labelKo}'(으)로 적용되었어요")
            }
        )
    }

    // Modal Bottom Sheet 3: Capsule Wardrobe Gap Report
    if (showCapsuleReportSheet) {
        CapsuleWardrobeReportSheet(
            garments = garments,
            onDismissRequest = { showCapsuleReportSheet = false },
            onShowToast = onShowToast
        )
    }

    pendingDeleteGarment?.let { garmentToDelete ->
        FriendlyReassuranceDialog(
            title = "이 옷을 옷장에서 보낼까요?",
            reassuranceText = "'${garmentToDelete.brand} ${garmentToDelete.name}'을(를) 삭제해도 지금까지의 착용 기록(${garmentToDelete.wearCount}회)과 가성비 리포트는 보존돼요.",
            primaryButtonText = "옷장에 그대로 두기",
            onPrimaryClick = { pendingDeleteGarment = null },
            secondaryButtonText = "삭제하기",
            onSecondaryClick = {
                onDeleteGarment(garmentToDelete.id)
                pendingDeleteGarment = null
                onShowToast("옷장에서 깔끔하게 정리되었어요")
            },
            onDismissRequest = { pendingDeleteGarment = null },
            accentBadgeText = "안심 안내"
        )
    }

    if (showQuickAddSheet) {
        AclosetQuickAddBottomSheet(
            onDismiss = { showQuickAddSheet = false },
            onSaveGarment = onQuickAddGarment
        )
    }

    detailModalGarment?.let { target ->
        AclosetGarmentDetailModal(
            garment = target,
            allGarments = garments,
            onDismiss = { detailModalGarment = null },
            onWearPlusOne = onIncrementWear
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GarmentWardrobeCard(
    garment: Garment,
    itemKey: String = garment.stableKey,
    personalColorSeason: PersonalColorSeason,
    cutoutBitmap: Bitmap?,
    isSelected: Boolean = false,
    hasActiveSelection: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onIncrementWear: () -> Unit,
    onDelete: () -> Unit,
    onOpenFitting: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF0F7FF) else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF0071E3) else Color(0xFFF1F5F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (hasActiveSelection) onToggleSelect() else onOpenFitting()
            }
            .testTag("garment_card_${itemKey}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Cutout studio box (에디토리얼 비주얼 박스 - 160dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
            ) {
                GarmentCutoutIllustration(
                    garment = garment,
                    cutoutBitmap = cutoutBitmap,
                    showCheckerboard = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )

                // Source badge
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (garment.sourceType == "CAMERA_AI" || garment.sourceType == "PHOTO_CUTOUT") "사진" else "영수증",
                        style = TextStyle(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )
                }

                // Selection Check Circle (멀티 탭 코디 메이커 토글 - 36dp 터치 타깃)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onToggleSelect() }
                        .testTag("select_toggle_${itemKey}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF0071E3) else Color.Black.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text(
                                text = "✓",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                }

                // Color swatch pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(parseHexColorSafe(garment.colorHex))
                            .border(0.5.dp, Color(0xFFCBD5E1), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = garment.colorName,
                        style = TextStyle(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = garment.brand,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.3.sp,
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = garment.name,
                style = TextStyle(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 퍼스널 컬러 매칭 뱃지 (koreai / rizz-up 알고리즘)
                val colorMatch = PersonalColorAnalyzer.evaluateGarmentColorMatch(garment.colorHex, personalColorSeason)
                Surface(
                    color = if (colorMatch.isRecommended) Color(0xFF0071E3).copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, if (colorMatch.isRecommended) Color(0xFF0071E3).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = colorMatch.badgeLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colorMatch.isRecommended) Color(0xFF0071E3) else Color(0xFF475569),
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                garment.tpoTags.take(1).forEach { tag ->
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "#$tag",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // 착용 횟수 뱃지 (패션 라이프스타일 지표)
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "${garment.wearCount}회 착용",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Wear +1 Action & Delete Control (No financial numbers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { onIncrementWear() }
                        .testTag("wear_plus_button_${itemKey}")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "오늘 입었어요 +1",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "의류 삭제",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * 선택한 의류 조합의 카테고리 다양성 및 퍼스널컬러 어울림 수를 바탕으로 시너지 점수(88~99점)를 산출합니다.
 */
private fun calculateLookSynergyScore(
    selectedGarments: List<Garment>,
    activeSeason: PersonalColorSeason
): Int {
    val distinctCategories = selectedGarments.map { it.category }.distinct().size
    val colorMatchCount = selectedGarments.count {
        PersonalColorAnalyzer.evaluateGarmentColorMatch(it.colorHex, activeSeason).isRecommended
    }
    return (88 + (distinctCategories * 3) + (colorMatchCount * 2)).coerceIn(88, 99)
}

/**
 * 멀티 탭 코디 메이커 하단 플로팅 바: 선택된 의류 썸네일 스택, 코디 시너지 점수,
 * 선택 초기화 및 가상 피팅실(3D 아바타) 바로 입어보기 CTA를 제공합니다.
 */
@Composable
private fun FloatingLookCombinerBar(
    selectedGarments: List<Garment>,
    cutoutBitmaps: Map<String, Bitmap>,
    activeSeason: PersonalColorSeason,
    onClearSelection: () -> Unit,
    onFitLook: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedGarments.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("floating_look_combiner_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        for (garment in selectedGarments.take(3)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFF0071E3), CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GarmentCutoutIllustration(
                                    garment = garment,
                                    cutoutBitmap = cutoutBitmaps[garment.id] ?: cutoutBitmaps[garment.stableKey],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    val synergyScore = calculateLookSynergyScore(selectedGarments, activeSeason)

                    Column {
                        Text(
                            text = "${selectedGarments.size}벌 코디 조합",
                            style = TextStyle(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedGarments.size >= 2) {
                                "✨ 찰떡 스타일링 ${synergyScore}점"
                            } else {
                                "하의/아우터를 더 선택해보세요"
                            },
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF94A3B8),
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_clear_selected_look")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "선택 취소",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onFitLook,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0071E3),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_fit_selected_look")
                    ) {
                        Text(
                            text = "입어보기 >",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
