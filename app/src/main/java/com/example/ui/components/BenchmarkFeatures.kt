package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.theme.ZigzagCanvas
import com.example.ui.theme.ZigzagCtaBlack
import com.example.ui.theme.ZigzagCtaText
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BodyShapeType
import com.example.data.model.Garment
import com.example.data.remote.WeatherContext

// ============================================================================
// 1. Acloset 벤치마킹 UI: 5초 초고속 퀵 등록 & 의류 상세 브레이크이븐/크로스코디 모달
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AclosetQuickAddBottomSheet(
    onDismiss: () -> Unit,
    onSaveGarment: (Garment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("내 옷장") }
    var selectedCategory by remember { mutableStateOf("TOP") }
    var selectedColorPair by remember { mutableStateOf("네이비" to "#1F2A44") }
    var selectedSeason by remember { mutableStateOf("ALL") }
    var selectedThickness by remember { mutableStateOf("MEDIUM") }

    val colorChips = listOf(
        "블랙" to "#1E1E22",
        "화이트" to "#F5F5F0",
        "네이비" to "#1F2A44",
        "베이지" to "#D2B48C",
        "차콜" to "#3A3D45",
        "스카이블루" to "#9BB7D4",
        "버건디" to "#6E2638",
        "올리브" to "#556B2F"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { ZigzagBottomSheetDragHandle() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = ZigzagCanvas,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "빠른 옷 등록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "OUTER" to "아우터",
                    "TOP" to "상의",
                    "BOTTOM" to "하의",
                    "SHOES" to "신발"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = selectedCategory == code,
                        onClick = { selectedCategory = code },
                        label = { Text(label) },
                        modifier = Modifier.testTag("quick_add_cat_$code")
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("의류 이름 (예: 캐시미어 니트)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_name_input")
            )

            Text(
                text = "대표 색상 선택",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                colorChips.forEach { pair ->
                    FilterChip(
                        selected = selectedColorPair.first == pair.first,
                        onClick = { selectedColorPair = pair },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColorSafe(pair.second))
                                )
                                Text(pair.first, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("LIGHT" to "얇음", "MEDIUM" to "보통", "HEAVY" to "두꺼움").forEach { (code, label) ->
                    FilterChip(
                        selected = selectedThickness == code,
                        onClick = { selectedThickness = code },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Button(
                onClick = {
                    val safeName = name.ifBlank { "${selectedColorPair.first} 데일리 아이템" }
                    val subCat = when (selectedCategory) {
                        "OUTER" -> "자켓"
                        "TOP" -> "니트"
                        "BOTTOM" -> "슬랙스"
                        "SHOES" -> "로퍼"
                        else -> "기본템"
                    }
                    onSaveGarment(
                        Garment(
                            name = safeName,
                            brand = brand.ifBlank { "내 옷장" },
                            category = selectedCategory,
                            subCategory = subCat,
                            colorName = selectedColorPair.first,
                            colorHex = selectedColorPair.second,
                            season = selectedSeason,
                            pattern = "솔리드",
                            thickness = selectedThickness,
                            waterResistant = selectedCategory == "OUTER",
                            tpoTags = listOf("출근", "데일리", "데이트"),
                            wearCount = 1L,
                            sourceType = "QUICK_MANUAL",
                            receiptStore = "5초 퀵 등록"
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_submit_button")
            ) {
                Text("옷장에 추가", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AclosetGarmentDetailModal(
    garment: Garment,
    allGarments: List<Garment>,
    onDismiss: () -> Unit,
    onWearPlusOne: (Garment) -> Unit
) {
    val analysis = remember(garment, allGarments) {
        AclosetWardrobeAnalyzer.analyzeGarmentStyling(garment, allGarments)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { ZigzagBottomSheetDragHandle() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = ZigzagCanvas,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GarmentCutoutIllustration(
                        garment = garment,
                        modifier = Modifier.size(64.dp)
                    )
                    Column {
                        Text(
                            text = "${garment.brand} · ${garment.subCategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = garment.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = analysis.closetTierLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✨ 착용 활용도 & 스타일 프로필",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("누적 착용", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${analysis.wearCount}회",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("매칭 호환", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${analysis.compatibleItems.size}개 추천 코디",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("추천 TPO", style = MaterialTheme.typography.labelSmall)
                            Text(
                                garment.tpoTags.take(2).joinToString(" · ").ifBlank { "데일리" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { analysis.progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = if (analysis.remainingWearsToSignature == 0L) {
                            "🏆 시그니처 달성 (${analysis.targetSignatureWears}회)"
                        } else {
                            "시그니처까지 ${analysis.remainingWearsToSignature}회"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (analysis.compatibleItems.isNotEmpty()) {
                Text(
                    text = "✨ 추천 크로스 코디",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    analysis.compatibleItems.forEach { partner ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.width(135.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                GarmentCutoutIllustration(
                                    garment = partner,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = partner.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${partner.colorName} · ${partner.subCategory}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onWearPlusOne(garment)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_modal_wear_plus_button")
            ) {
                Text("오늘 입기 (+1)", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// ============================================================================
// 2. Whering (웨어링) & Outfitly 벤치마킹 UI: 'Dress Me' 슬롯머신 & 6대 색채학 하모니
// ============================================================================

@Composable
fun WheringDressMeSlotBar(
    outer: Garment?,
    top: Garment?,
    bottom: Garment?,
    shoes: Garment?,
    outerLocked: Boolean,
    topLocked: Boolean,
    bottomLocked: Boolean,
    shoesLocked: Boolean,
    bodyShape: BodyShapeType,
    onToggleOuterLock: () -> Unit,
    onToggleTopLock: () -> Unit,
    onToggleBottomLock: () -> Unit,
    onToggleShoesLock: () -> Unit,
    onCycleOuter: (Int) -> Unit,
    onCycleTop: (Int) -> Unit,
    onCycleBottom: (Int) -> Unit,
    onCycleShoes: (Int) -> Unit,
    onSpinUnlockedSlots: () -> Unit
) {
    val harmony = remember(outer, top, bottom, shoes, bodyShape) {
        ColorHarmonyAnalyzer.evaluateOutfitHarmony(outer, top, bottom, shoes, bodyShape)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("whering_dress_me_slot_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "스타일 슬롯 믹스",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        harmony.paletteHexes.distinct().take(4).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColorSafe(hex))
                            )
                        }
                        Text(
                            text = "${harmony.harmonyTypeKo.substringBefore(" (")} · 색채 ${harmony.colorScore} · 실루엣 ${harmony.silhouetteScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${harmony.totalHarmonyScore}점",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 4-Slot Lock & Cycle Matrix
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SlotMachineRow(
                    slotTitle = "아우터",
                    garment = outer,
                    isLocked = outerLocked,
                    onToggleLock = onToggleOuterLock,
                    onPrev = { onCycleOuter(-1) },
                    onNext = { onCycleOuter(1) },
                    tagPrefix = "slot_outer"
                )
                SlotMachineRow(
                    slotTitle = "상의",
                    garment = top,
                    isLocked = topLocked,
                    onToggleLock = onToggleTopLock,
                    onPrev = { onCycleTop(-1) },
                    onNext = { onCycleTop(1) },
                    tagPrefix = "slot_top"
                )
                SlotMachineRow(
                    slotTitle = "하의",
                    garment = bottom,
                    isLocked = bottomLocked,
                    onToggleLock = onToggleBottomLock,
                    onPrev = { onCycleBottom(-1) },
                    onNext = { onCycleBottom(1) },
                    tagPrefix = "slot_bottom"
                )
                SlotMachineRow(
                    slotTitle = "신발",
                    garment = shoes,
                    isLocked = shoesLocked,
                    onToggleLock = onToggleShoesLock,
                    onPrev = { onCycleShoes(-1) },
                    onNext = { onCycleShoes(1) },
                    tagPrefix = "slot_shoes"
                )
            }

            Button(
                onClick = onSpinUnlockedSlots,
                colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whering_spin_unlocked_button")
            ) {
                Text("랜덤 코디 스핀", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
            }
        }
    }
}

@Composable
private fun SlotMachineRow(
    slotTitle: String,
    garment: Garment?,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    tagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("${tagPrefix}_lock_btn")
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "슬롯 잠금 해제" else "슬롯 고정",
                        tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = slotTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(42.dp)
                )
                if (garment != null) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseHexColorSafe(garment.colorHex))
                    )
                    Text(
                        text = garment.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "미착용 (아우터 생략)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrev,
                    enabled = !isLocked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 아이템")
                }
                IconButton(
                    onClick = onNext,
                    enabled = !isLocked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 아이템")
                }
            }
        }
    }
}

// ============================================================================
// 3. Stylebook 벤치마킹 UI: 스마트 여행·출장 패킹 캡슐 & 시그니처 착용 트래커
// ============================================================================

@Composable
fun StylebookPackingCapsuleCard(
    garments: List<Garment>,
    weather: WeatherContext
) {
    var tripDays by remember { mutableIntStateOf(3) }
    var tripPurpose by remember { mutableStateOf("비즈니스 출장") }

    val capsulePlan = remember(garments, weather, tripDays, tripPurpose) {
        StylebookCapsuleBuilder.buildPackingCapsule(
            garments = garments,
            weather = weather,
            tripDays = tripDays,
            tripPurpose = tripPurpose
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stylebook_packing_capsule_card"),
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "스마트 패킹 캡슐",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "${capsulePlan.packedItems.size}벌",
                            "${capsulePlan.possibleOutfitCombinationsCount}코디",
                            "${capsulePlan.estimatedTotalWeightKg}kg · -${capsulePlan.spaceSavedPercent}%"
                        ).forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(2 to "1박 2일", 3 to "2박 3일", 4 to "3박 4일").forEach { (days, label) ->
                    FilterChip(
                        selected = tripDays == days,
                        onClick = { tripDays = days },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        modifier = Modifier.testTag("packing_days_$days")
                    )
                }
                listOf("비즈니스 출장", "주말 여행").forEach { purpose ->
                    FilterChip(
                        selected = tripPurpose == purpose,
                        onClick = { tripPurpose = purpose },
                        label = {
                            Text(
                                text = purpose,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📦 ${capsulePlan.tripLabel} · ${capsulePlan.weatherPackingTip}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capsulePlan.packedItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GarmentCutoutIllustration(
                                garment = item,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.subCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                capsulePlan.dailySchedulePreview.forEach { dayLine ->
                    Text(
                        text = "• $dayLine",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StylebookSignatureWearTrackerCard(
    garments: List<Garment>,
    onWearPlusOne: (Garment) -> Unit
) {
    var targetWearsGoal by remember { mutableLongStateOf(15L) }
    val analyses = remember(garments, targetWearsGoal) {
        garments.map { g ->
            g to AclosetWardrobeAnalyzer.analyzeGarmentStyling(g, garments, targetWearsGoal)
        }.sortedByDescending { (_, a) -> a.remainingWearsToSignature }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stylebook_signature_wear_tracker_card"),
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
            Column {
                Text(
                    text = "시그니처 애착템 트래커",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "자주 입는 내 시그니처 옷장 목표를 채워보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10L to "목표 10회", 15L to "목표 15회", 30L to "목표 30회").forEach { (goal, label) ->
                    FilterChip(
                        selected = targetWearsGoal == goal,
                        onClick = { targetWearsGoal = goal },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            analyses.take(4).forEach { (g, a) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${g.name} (${a.closetTierLabel})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${g.brand} · ${g.colorName} (${a.compatibleItems.size}벌과 코디 매치)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { onWearPlusOne(g) },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("+1회 착용", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { a.progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = if (a.remainingWearsToSignature == 0L) {
                                "✅ 시그니처 주력템 달성! (${g.wearCount}/${a.targetSignatureWears}번 착용)"
                            } else {
                                "현재 ${g.wearCount}번 입음 · 시그니처까지 ${a.remainingWearsToSignature}번 남았어요!"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. 새 스타일 아이템 옷장 궁합 시뮬레이터 UI
// ============================================================================

@Composable
fun WardrobeCompatibilitySimulatorCard(
    ownedGarments: List<Garment>,
    onAskAiStylist: (String) -> Unit
) {
    var selectedCandidate by remember {
        mutableStateOf(WardrobeCompatibilityAdvisor.presetCandidates.first())
    }
    var showCustomInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("OUTER") }

    val verdict = remember(selectedCandidate, ownedGarments) {
        WardrobeCompatibilityAdvisor.evaluateStyleCandidate(selectedCandidate, ownedGarments)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wardrobe_compatibility_simulator_card"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "새 스타일 아이템 궁합 시뮬레이터",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "내 옷들과 얼마나 잘 어우러지는지 코디 확장을 미리 봐요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showCustomInput = !showCustomInput },
                    modifier = Modifier.testTag("toggle_custom_candidate_btn")
                ) {
                    Text(if (showCustomInput) "프리셋" else "직접 입력", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (showCustomInput) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("OUTER" to "아우터", "TOP" to "상의", "BOTTOM" to "하의", "SHOES" to "신발").forEach { (code, label) ->
                                FilterChip(
                                    selected = customCategory == code,
                                    onClick = { customCategory = code },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("테스트할 스타일 아이템명") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                selectedCandidate = StyleMatchCandidate(
                                    name = customName.ifBlank { "고민 중인 스타일 아이템" },
                                    category = customCategory,
                                    colorName = "네이비/블랙",
                                    colorHex = "#2C3E50"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("내 옷장 궁합 시뮬레이션 실행", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WardrobeCompatibilityAdvisor.presetCandidates.forEach { candidate ->
                        FilterChip(
                            selected = selectedCandidate.name == candidate.name,
                            onClick = { selectedCandidate = candidate },
                            label = {
                                Text(
                                    "${candidate.name} (${candidate.colorName})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = verdict.verdictBadge,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = verdict.recommendationSummary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("새로운 조합", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "+${verdict.newOutfitCombinationsCreated}개",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text("채움 점수", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${verdict.wardrobeGapFillScore}점",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("스타일 중복도", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${verdict.duplicateRiskPercent}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (verdict.topMatchingOwnedGarments.isNotEmpty()) {
                        Text(
                            text = "함께 입기 좋은 내 옷: ${verdict.topMatchingOwnedGarments.joinToString(", ") { it.name }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val prompt = "'${selectedCandidate.name}'(${selectedCandidate.colorName}) 아이템과 내 옷장의 기존 옷들이 어떻게 어울리는지 코디 조합을 추천해 주세요!"
                    onAskAiStylist(prompt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ask_ai_buy_or_not_button")
            ) {
                Text("AI에게 물어보기", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
            }
        }
    }
}
