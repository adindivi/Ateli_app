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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
    var priceText by remember { mutableStateOf("59000") }

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
                text = "⚡ 에이클로젯 5초 초고속 의류 등록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "영수증이나 사진 없이도 집에 있는 옷을 카테고리·색상 칩 터치만으로 즉시 옷장에 추가합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("의류 이름 (예: 캐시미어 니트)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("quick_add_name_input")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("구매가(원)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("quick_add_price_input")
                )
            }

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
                        label = { Text("두께: $label", style = MaterialTheme.typography.labelSmall) }
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
                            purchasePrice = priceText.toLongOrNull() ?: 59000L,
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
                Text("옷장에 즉시 추가하기", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
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
        AclosetWardrobeAnalyzer.analyzeGarmentBreakEven(garment, allGarments, targetCpwGoal = 10_000L)
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
                        text = "💰 에이클로젯 감가상각 & 순수 체감 CPW 분석",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("명목 1회 착용가", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format("%,d", analysis.currentCpw)}원",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("당근 예상 중고가", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format("%,d", analysis.estimatedResaleValue)}원",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("실질 체감 CPW", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format("%,d", analysis.netEffectiveCpw)}원",
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
                        text = if (analysis.remainingWearsToGoal == 0L) {
                            "🎉 목표 손익분기(${analysis.targetTotalWears}회 착용)를 이미 달성한 효자 아이템입니다!"
                        } else {
                            "목표 CPW(10,000원) 달성까지 앞으로 ${analysis.remainingWearsToGoal}회 더 입으면 뽕을 뽑습니다!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (analysis.compatibleItems.isNotEmpty()) {
                Text(
                    text = "✨ 이 옷과 가장 잘 어울리는 내 옷장 크로스 코디 Top ${analysis.compatibleItems.size}",
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
                Text("오늘 이 옷 입기 (+1회 착용 기록)", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
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
                        text = "🎰 Whering 'Dress Me' 슬롯머신 & 색채학 매칭",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${harmony.harmonyTypeKo} · 색채 ${harmony.colorScore}점 / 실루엣 ${harmony.silhouetteScore}점",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "종합 ${harmony.totalHarmonyScore}점",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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

            Text(
                text = "🎨 ${harmony.styleFeedbackKo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onSpinUnlockedSlots,
                colors = ButtonDefaults.buttonColors(containerColor = ZigzagCtaBlack, contentColor = ZigzagCtaText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whering_spin_unlocked_button")
            ) {
                Text("잠금 제외 슬롯 랜덤 스핀", fontWeight = FontWeight.Bold, color = ZigzagCtaText)
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
// 3. Stylebook (스타일북) 벤치마킹 UI: 스마트 여행·출장 패킹 캡슐 & 손익분기 트래커
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
                Column {
                    Text(
                        text = "스마트 패킹 캡슐 플래너",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "최소 짐으로 최대 코디 조합 · 캐리어 무게 ${capsulePlan.estimatedTotalWeightKg}kg · 부피 ${capsulePlan.spaceSavedPercent}% 절감",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(2 to "1박 2일", 3 to "2박 3일", 4 to "3박 4일").forEach { (days, label) ->
                    FilterChip(
                        selected = tripDays == days,
                        onClick = { tripDays = days },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("packing_days_$days")
                    )
                }
                listOf("비즈니스 출장", "주말 여행").forEach { purpose ->
                    FilterChip(
                        selected = tripPurpose == purpose,
                        onClick = { tripPurpose = purpose },
                        label = { Text(purpose, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📦 ${capsulePlan.tripLabel} 추천 패킹: 단 ${capsulePlan.packedItems.size}벌로 ${capsulePlan.possibleOutfitCombinationsCount}가지 코디 완성",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = capsulePlan.weatherPackingTip,
                        style = MaterialTheme.typography.bodySmall,
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
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StylebookBreakEvenTrackerCard(
    garments: List<Garment>,
    onWearPlusOne: (Garment) -> Unit
) {
    var targetCpwGoal by remember { mutableLongStateOf(10_000L) }
    val analyses = remember(garments, targetCpwGoal) {
        garments.map { g ->
            g to AclosetWardrobeAnalyzer.analyzeGarmentBreakEven(g, garments, targetCpwGoal)
        }.sortedByDescending { (_, a) -> a.remainingWearsToGoal }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stylebook_breakeven_tracker_card"),
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
                    text = "본전 뽑기 트래커",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "몇 번 더 입으면 본전인지 알려드려요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5_000L to "목표 5천원", 10_000L to "목표 1만원", 20_000L to "목표 2만원").forEach { (goal, label) ->
                    FilterChip(
                        selected = targetCpwGoal == goal,
                        onClick = { targetCpwGoal = goal },
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
                                    text = "한 번 입을 때 ${String.format("%,d", a.currentCpw)}원 (중고로 팔면 약 ${String.format("%,d", a.estimatedResaleValue)}원)",
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
                            text = if (a.remainingWearsToGoal == 0L) {
                                "✅ 본전 뽑았어요! (${g.wearCount}/${a.targetTotalWears}번 입음)"
                            } else {
                                "현재 ${g.wearCount}번 입음 · ${a.remainingWearsToGoal}번 더 입으면 본전이에요!"
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
// 4. Stitch Fix & 무신사 벤치마킹 UI: '이 옷 살까 말까?' 구매 전 옷장 궁합 시뮬레이터
// ============================================================================

@Composable
fun StitchFixBuyOrNotSimulatorCard(
    ownedGarments: List<Garment>,
    onAskAiStylist: (String) -> Unit
) {
    var selectedCandidate by remember {
        mutableStateOf(StitchFixPurchaseAdvisor.presetCandidates.first())
    }
    var showCustomInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("OUTER") }
    var customPriceText by remember { mutableStateOf("129000") }

    val verdict = remember(selectedCandidate, ownedGarments) {
        StitchFixPurchaseAdvisor.evaluateCandidatePurchase(selectedCandidate, ownedGarments)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stitchfix_buy_or_not_card"),
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
                        text = "이 옷 살까요 말까요?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "사기 전에 내 옷들과 잘 어울릴지 미리 봐요.",
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("고민 중인 상품명") },
                                singleLine = true,
                                modifier = Modifier.weight(1.1f)
                            )
                            OutlinedTextField(
                                value = customPriceText,
                                onValueChange = { customPriceText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("가격(원)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                        Button(
                            onClick = {
                                selectedCandidate = CandidatePurchaseItem(
                                    name = customName.ifBlank { "고민 중인 신상 아이템" },
                                    category = customCategory,
                                    colorName = "네이비/블랙",
                                    colorHex = "#2C3E50",
                                    expectedPrice = customPriceText.toLongOrNull() ?: 129_000L
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
                    StitchFixPurchaseAdvisor.presetCandidates.forEach { candidate ->
                        FilterChip(
                            selected = selectedCandidate.name == candidate.name,
                            onClick = { selectedCandidate = candidate },
                            label = {
                                Text(
                                    "${candidate.name} (${String.format("%,d", candidate.expectedPrice)}원)",
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
                            Text("10번 입으면 한 번에", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format("%,d", verdict.expectedCpwAt10Wears)}원",
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
                    val prompt = "'${selectedCandidate.name}'(${String.format("%,d", selectedCandidate.expectedPrice)}원, ${selectedCandidate.colorName}) 구매를 고민 중입니다. 내 옷장 아이템들과 매칭했을 때 살 가치가 있는지, 어떤 보유 아이템과 입으면 좋을지 분석해 주세요!"
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
