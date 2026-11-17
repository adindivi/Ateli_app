package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CuratedWardrobePresets
import com.example.data.model.Garment
import com.example.data.model.GarmentCategory
import com.example.ui.components.AclosetGarmentDetailModal
import com.example.ui.components.AclosetQuickAddBottomSheet
import com.example.ui.components.GarmentCutoutIllustration
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.AtelierChampagne
import com.example.ui.theme.AtelierCoral
import com.example.ui.theme.AtelierDeepPlum
import com.example.ui.theme.AtelierIvory
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagStrong
import com.example.ui.theme.ZigzagForeground
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagAppBanner
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import com.example.ui.theme.ZigzagPillText
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.DataUiState

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
    onIncrementWear: (Garment) -> Unit,
    onDeleteGarment: (String) -> Unit,
    onResetAiState: () -> Unit,
    onOpenVirtualFitting: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showReceiptSheet by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var detailModalGarment by remember { mutableStateOf<Garment?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        selectedBitmap = bmp
                        showPhotoSheet = true
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Zero-permission Thumbnail Camera Capture
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            selectedBitmap = bmp
            showPhotoSheet = true
        }
    }

    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("RECENT") } // RECENT, BEST_CPW, SLEEPER_FIRST, MOST_WORN

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

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 162.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("wardrobe_grid")
    ) {
        // 1. Hero Header Banner (ZIGZAG Style: Clean white card, hairline border, 8px subtle corner)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
                border = BorderStroke(1.dp, ZigzagHairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = ZigzagAppBanner,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ZigzagStrong,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ZERO-EFFORT AI WARDROBE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ZigzagStrong
                                    )
                                }
                            }
                            Text(
                                text = "총 ${garments.size}벌 보유",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = ZigzagSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "영수증 연동 & AI 자동 누끼 옷장",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ZigzagStrong
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "쇼핑몰 전자영수증을 붙여넣거나 옷 사진을 올리면 AI가 배경을 오려내고 계절·색상·TPO 태그와 가격을 자동 기록합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZigzagSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onResetAiState()
                                    showReceiptSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZigzagCtaBlack,
                                    contentColor = ZigzagCtaText
                                ),
                                shape = RoundedCornerShape(24.dp), // ZIGZAG control: 24px
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("open_receipt_parser_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "영수증 자동등록",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("영수증 AI 연동", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onResetAiState()
                                    showPhotoSheet = true
                                },
                                shape = RoundedCornerShape(24.dp), // ZIGZAG control: 24px
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ZigzagPillText
                                ),
                                border = BorderStroke(1.dp, ZigzagForeground),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("open_camera_cutout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "AI 누끼 촬영",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("사진 AI 누끼", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showQuickAddSheet = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ZigzagSecondary
                            ),
                            border = BorderStroke(1.dp, ZigzagHairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("open_quick_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "5초 퀵 등록",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ 에이클로젯 5초 초고속 수동 등록 (칩 선택)", style = MaterialTheme.typography.labelMedium)
                        }
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
                    placeholder = { Text("브랜드, 의류명, 색상(베이지·네이비), TPO(#출근) 검색...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wardrobe_search_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val categories = listOf(
                        "ALL" to "전체 (${garments.size})",
                        "OUTER" to "아우터",
                        "TOP" to "상의",
                        "BOTTOM" to "하의",
                        "SHOES" to "신발",
                        "BAG" to "가방·잡화"
                    )
                    categories.forEach { (code, label) ->
                        val selected = selectedCategoryFilter == code
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategoryFilter = code },
                            label = {
                                Text(
                                    text = label,
                                    style = if (selected) com.example.ui.theme.ZigzagTabSelected else com.example.ui.theme.ZigzagMetadata,
                                    color = if (selected) ZigzagCtaText else ZigzagSecondary
                                )
                            },
                            shape = RoundedCornerShape(24.dp), // ZIGZAG pill
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZigzagStrong,
                                selectedLabelColor = ZigzagCtaText,
                                containerColor = ZigzagCanvas,
                                labelColor = ZigzagSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) ZigzagStrong else ZigzagHairline
                            ),
                            modifier = Modifier.testTag("filter_chip_$code")
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val sorts = listOf(
                        "RECENT" to "최신 등록순",
                        "BEST_CPW" to "가성비(CPW) 최고순",
                        "SLEEPER_FIRST" to "미착용 방치템순",
                        "MOST_WORN" to "착용 횟수 많은순"
                    )
                    sorts.forEach { (code, label) ->
                        val selected = sortMode == code
                        AssistChip(
                            onClick = { sortMode = code },
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

        // 3. Empty state with 1-tap Curated Wardrobe Seeding
        if (garmentsState is DataUiState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZigzagStrong)
                }
            }
        } else if (filteredGarments.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ZigzagCanvas
                    ),
                    border = BorderStroke(1.dp, ZigzagHairline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZigzagAppBanner),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = ZigzagStrong,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "내 디지털 옷장이 비어 있어요",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ZigzagStrong
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "무신사·29CM·W컨셉 샘플 영수증을 AI로 연동하거나, 아래 버튼을 눌러 9벌의 스타터 캡슐 옷장을 1초 만에 채워보세요!",
                            style = com.example.ui.theme.ZigzagBody,
                            color = ZigzagSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onSeedStarterWardrobe,
                            shape = RoundedCornerShape(24.dp), // ZIGZAG control: 24px
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZigzagCtaBlack,
                                contentColor = ZigzagCtaText
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("seed_starter_wardrobe_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ZigzagCtaText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("스타터 스마트 옷장 9벌 자동 채우기", style = com.example.ui.theme.ZigzagBuyCta.copy(fontSize = 15.sp))
                        }
                    }
                }
            }
        } else {
            items(filteredGarments, key = { it.id.ifBlank { it.name + it.brand } }) { garment ->
                GarmentWardrobeCard(
                    garment = garment,
                    cutoutBitmap = cutoutBitmaps[garment.id],
                    onIncrementWear = { onIncrementWear(garment) },
                    onDelete = { onDeleteGarment(garment.id) },
                    onOpenFitting = { detailModalGarment = garment }
                )
            }
        }
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

    // Modal Bottom Sheet 1: Electronic Receipt / Shopping Mall Auto-Registration
    if (showReceiptSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var receiptInput by remember {
            mutableStateOf(CuratedWardrobePresets.sampleReceiptTemplates.first().rawReceiptText)
        }

        ModalBottomSheet(
            onDismissRequest = { showReceiptSheet = false },
            sheetState = sheetState
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

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "원클릭 쇼핑몰 영수증 템플릿",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CuratedWardrobePresets.sampleReceiptTemplates.forEach { tpl ->
                        AssistChip(
                            onClick = { receiptInput = tpl.rawReceiptText },
                            label = { Text("${tpl.storeName} · ${tpl.title}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = receiptInput,
                    onValueChange = { receiptInput = it },
                    label = { Text("전자영수증 / 쇼핑몰 주문 텍스트") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_text_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                AiRegistrationFeedbackSection(aiRegistrationState = aiRegistrationState)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onParseReceipt(receiptInput) },
                    enabled = aiRegistrationState !is AiTaskState.Loading && receiptInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_receipt_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI로 영수증 파싱 & 옷장에 자동 등록")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet 2: Camera / Photo Picker AI Background Cutout (누끼) & Auto-Tagging
    if (showPhotoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var enteredBrand by remember { mutableStateOf("무신사 스탠다드") }
        var enteredPriceText by remember { mutableStateOf("89000") }

        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "AI 배경 제거(누끼) & 비전 메타데이터 태깅",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "의류 사진을 촬영하거나 앨범에서 선택하면 외곽 배경을 자동으로 투명하게 오려내고(누끼) 색상·패턴·TPO 태그를 추출합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("갤러리 선택")
                    }
                    OutlinedButton(
                        onClick = { cameraPreviewLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("카메라 촬영")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = enteredBrand,
                        onValueChange = { enteredBrand = it },
                        label = { Text("브랜드명") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = enteredPriceText,
                        onValueChange = { enteredPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("구매 가격(원)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AiRegistrationFeedbackSection(aiRegistrationState = aiRegistrationState)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val bmp = selectedBitmap ?: createSampleStudioGarmentBitmap()
                        val price = enteredPriceText.toLongOrNull() ?: 89000L
                        onProcessPhoto(bmp, price, enteredBrand)
                    },
                    enabled = aiRegistrationState !is AiTaskState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_photo_ai_cutout_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedBitmap != null) "선택한 사진 AI 누끼 제거 & 옷장 등록"
                        else "스튜디오 샘플 의류 이미지로 AI 누끼 & 태깅 실행"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AiRegistrationFeedbackSection(
    aiRegistrationState: AiTaskState<List<Garment>>
) {
    AnimatedVisibility(visible = aiRegistrationState !is AiTaskState.Idle) {
        when (aiRegistrationState) {
            is AiTaskState.Loading -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = aiRegistrationState.stepMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is AiTaskState.Success -> {
                Surface(
                    color = AtelierSage.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AtelierSage
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI 자동 추출 및 등록 완료 (${aiRegistrationState.result.size}벌)",
                                style = MaterialTheme.typography.titleMedium,
                                color = AtelierSage
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        aiRegistrationState.result.forEach { item ->
                            Text(
                                text = "• [${item.brand}] ${item.name} (${item.colorName} · ${item.subCategory} · ₩${String.format("%,d", item.purchasePrice)})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            is AiTaskState.Error -> {
                Surface(
                    color = AtelierCoral.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiRegistrationState.message,
                        color = AtelierCoral,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            AiTaskState.Idle -> {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GarmentWardrobeCard(
    garment: Garment,
    cutoutBitmap: Bitmap?,
    onIncrementWear: () -> Unit,
    onDelete: () -> Unit,
    onOpenFitting: () -> Unit
) {
    // ZIGZAG Product-card specification: 0px rounded, 0px 0px 8px padding, flat elevation, #ecedee thumbnail
    Card(
        shape = RoundedCornerShape(0.dp), // ZIGZAG square: 0px
        colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
        border = BorderStroke(1.dp, ZigzagHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // ZIGZAG: shadow none
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFitting() }
            .testTag("garment_card_${garment.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Cutout studio box (ZIGZAG product-thumbnail: #ECEDEE, 0px radius)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .background(ZigzagHairline)
            ) {
                GarmentCutoutIllustration(
                    garment = garment,
                    cutoutBitmap = cutoutBitmap,
                    showCheckerboard = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )

                // Source badge
                Surface(
                    color = ZigzagStrong,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (garment.sourceType == "CAMERA_AI") "AI 누끼" else "영수증 연동",
                        style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = ZigzagCtaText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (garment.waterResistant) {
                    Surface(
                        color = ZigzagStrong,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "방수/발수",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
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
                        .clip(RoundedCornerShape(24.dp))
                        .background(ZigzagCanvas.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseHexColorSafe(garment.colorHex))
                            .border(0.5.dp, ZigzagHairline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = garment.colorName,
                        style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 11.sp),
                        color = ZigzagStrong
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ZIGZAG Product metadata & body text
            Text(
                text = garment.brand,
                style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 12.sp, color = ZigzagMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = garment.name,
                style = com.example.ui.theme.ZigzagBody.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                garment.tpoTags.take(2).forEach { tag ->
                    Surface(
                        color = ZigzagAppBanner,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 11.sp, color = ZigzagSecondary),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CPW & Wear Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1회 착용비용",
                        style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 11.sp, color = ZigzagMuted)
                    )
                    Text(
                        text = "₩${String.format("%,d", garment.costPerWear)}",
                        style = com.example.ui.theme.ZigzagTabSelected.copy(fontSize = 15.sp, color = ZigzagStrong)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onIncrementWear() }
                            .testTag("wear_plus_button_${garment.id}")
                    ) {
                        Text(
                            text = "입음 +1 (${garment.wearCount}회)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "의류 삭제",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createSampleStudioGarmentBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(245, 245, 242)
    }
    canvas.drawRect(0f, 0f, 240f, 240f, bgPaint)

    val coatPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(52, 73, 94)
    }
    canvas.drawRoundRect(55f, 40f, 185f, 200f, 24f, 24f, coatPaint)
    return bmp
}
