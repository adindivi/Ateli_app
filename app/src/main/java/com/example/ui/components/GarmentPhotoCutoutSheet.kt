package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Garment
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagSecondary
import com.example.ui.theme.ZigzagStrong
import com.example.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ENGINE_MODE_RMBG2 = "RMBG2"
private const val ENGINE_MODE_MLKIT = "MLKIT"
private const val SAMPLE_STUDIO_ASSET_PATH = "garments/sample_studio_jacket.jpg"

/**
 * GarmentPhotoCutoutSheet
 *
 * 사진 촬영 또는 갤러리 선택 후 온디바이스 AI(RMBG-2.0 매팅 / ML Kit Subject Segmentation)로
 * 배경을 제거(누끼)하고 새 의류로 등록하는 모달 바텀시트입니다.
 *
 * Benchmark References:
 * - RMBG-2.0: https://github.com/Bria-AI/RMBG-2.0
 * - ML Kit: https://github.com/googlesamples/mlkit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentPhotoCutoutSheet(
    onDismissRequest: () -> Unit,
    onGarmentCreated: (garment: Garment, cutoutBitmap: Bitmap?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingAi by remember { mutableStateOf(false) }
    var cutoutEngineMode by remember { mutableStateOf(ENGINE_MODE_RMBG2) }

    var garmentName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TOP") }

    // 1. 카메라 촬영 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            originalBitmap = bmp
            isProcessingAi = true
            coroutineScope.launch {
                cutoutBitmap = extractCutoutByEngine(bmp, cutoutEngineMode)
                isProcessingAi = false
                if (garmentName.isBlank()) {
                    garmentName = "촬영된 새 의류"
                }
            }
        }
    }

    // 2. 갤러리 사진 선택 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    decodeUriToBitmap(context, uri)
                }
                if (bmp != null) {
                    originalBitmap = bmp
                    isProcessingAi = true
                    cutoutBitmap = extractCutoutByEngine(bmp, cutoutEngineMode)
                    isProcessingAi = false
                    if (garmentName.isBlank()) {
                        garmentName = "갤러리 등록 의류"
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI 사진 누끼 등록",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZigzagStrong,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "온디바이스 AI 자동 배경 제거",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = ZigzagMuted,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = ZigzagMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI 누끼 엔진 선택 세그먼트 탭 (RMBG-2.0 BiRefNet vs ML Kit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isRmbg = cutoutEngineMode == ENGINE_MODE_RMBG2
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { cutoutEngineMode = ENGINE_MODE_RMBG2 }
                        .testTag("engine_tab_rmbg2"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRmbg) Color.White else Color.Transparent,
                    border = if (isRmbg) BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✨ RMBG-2.0 프로 정밀",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = if (isRmbg) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRmbg) Color(0xFF0071E3) else ZigzagMuted,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { cutoutEngineMode = ENGINE_MODE_MLKIT }
                        .testTag("engine_tab_mlkit"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isRmbg) Color.White else Color.Transparent,
                    border = if (!isRmbg) BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⚡ ML Kit 고속 (0.2s)",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = if (!isRmbg) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isRmbg) ZigzagStrong else ZigzagMuted,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1단계: 사진 입력 선택 액션 바
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 카메라 촬영 버튼
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { cameraLauncher.launch(null) }
                        .testTag("cutout_btn_camera"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ZigzagStrong,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "카메라 촬영",
                            style = TextStyle(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZigzagStrong,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 앨범 선택 버튼
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { galleryLauncher.launch("image/*") }
                        .testTag("cutout_btn_gallery"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = ZigzagStrong,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "앨범 선택",
                            style = TextStyle(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZigzagStrong,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 샘플 체험 버튼
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable {
                            isProcessingAi = true
                            coroutineScope.launch {
                                val sample = withContext(Dispatchers.IO) {
                                    createMockStudioClothingBitmap(context)
                                }
                                originalBitmap = sample
                                cutoutBitmap = extractCutoutByEngine(sample, cutoutEngineMode)
                                isProcessingAi = false
                                garmentName = "에어리 필드 유틸리티 자켓"
                                brandName = "세이지 아틀리에"
                                selectedCategory = "OUTER"
                            }
                        }
                        .testTag("cutout_btn_sample"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0071E3).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF0071E3).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFF0071E3),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "샘플 체험",
                            style = TextStyle(
                                fontSize = 12.5.sp,
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

            Spacer(modifier = Modifier.height(16.dp))

            // AI 처리 상태 및 미리보기 영역
            if (isProcessingAi) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0071E3),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI가 옷만 쏙 깔끔하게 분리하고 있어요...",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ZigzagStrong,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Google ML Kit On-Device Engine",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = ZigzagMuted,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else if (cutoutBitmap != null) {
                // 결과 누끼 미리보기 (투명 배경 확인용 체커보드)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("cutout_preview_surface"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.5.dp, Color(0xFF0071E3).copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = cutoutBitmap!!.asImageBitmap(),
                            contentDescription = "누끼 추출 의류",
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        // 투명 누끼 완료 뱃지
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0071E3),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (cutoutEngineMode == ENGINE_MODE_RMBG2) "✨ RMBG-2.0 정밀 누끼" else "⚡ ML Kit 누끼 완료",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 카테고리 선택 칩
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "카테고리",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZigzagMuted,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val categories = listOf(
                            "OUTER" to "아우터",
                            "TOP" to "상의",
                            "BOTTOM" to "하의",
                            "SHOES" to "신발",
                            "BAG" to "잡화"
                        )
                        categories.forEach { (catCode, catLabel) ->
                            val isSel = selectedCategory == catCode
                            Surface(
                                modifier = Modifier
                                    .clickable { selectedCategory = catCode }
                                    .testTag("cutout_cat_$catCode"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) ZigzagStrong else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) ZigzagStrong else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = catLabel,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else ZigzagMuted,
                                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 옷 이름 입력 폼 (가격 제거, 스타일 명칭 집중)
                OutlinedTextField(
                    value = garmentName,
                    onValueChange = { garmentName = it },
                    label = { Text("옷 이름", fontSize = 11.sp) },
                    placeholder = { Text("예: 오버핏 자켓", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cutout_input_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0071E3),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 등록 완료 버튼 (핀테크 다크 블랙)
                Button(
                    onClick = {
                        val newGarment = Garment(
                            name = garmentName.ifBlank { "새 의류" },
                            brand = brandName.ifBlank { "자체 등록" },
                            category = selectedCategory,
                            subCategory = "사진 누끼",
                            wearCount = 0L,
                            sourceType = "PHOTO_CUTOUT"
                        )
                        // 내부 저장소에 파일 저장
                        val savedPath = BitmapUtils.saveGarmentBitmapToInternalStorage(
                            context = context,
                            bitmap = cutoutBitmap!!,
                            prefix = selectedCategory.lowercase()
                        )
                        val finalGarment = newGarment.copy(imageUrl = savedPath)
                        onGarmentCreated(finalGarment, cutoutBitmap)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cutout_btn_submit"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZigzagStrong,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "내 옷장에 쏙 넣기",
                        style = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                // 초기 비어있는 상태 가이드 박스
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "옷 사진을 선택하면 배경을 자동 분리해요",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ZigzagSecondary,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * URI를 비트맵으로 안전하게 디코딩
 */
private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 에뮬레이터 또는 즉시 체험용 실물 스튜디오 의류 사진 로드 (폴백: 벡터 스튜디오 자켓)
 */
private fun createMockStudioClothingBitmap(context: Context): Bitmap {
    try {
        context.assets.open(SAMPLE_STUDIO_ASSET_PATH).use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)?.let { return it }
        }
    } catch (_: Exception) {
    }

    val width = 400
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val bgPaint = Paint().apply {
        color = android.graphics.Color.rgb(238, 238, 238)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    val clothPaint = Paint().apply {
        color = android.graphics.Color.rgb(30, 41, 59)
        isAntiAlias = true
    }
    canvas.drawRoundRect(100f, 100f, 300f, 330f, 24f, 24f, clothPaint)
    canvas.drawRoundRect(60f, 110f, 140f, 270f, 18f, 18f, clothPaint)
    canvas.drawRoundRect(260f, 110f, 340f, 270f, 18f, 18f, clothPaint)
    return bitmap
}

/**
 * 선택된 온디바이스 AI 누끼 엔진(RMBG-2.0 정밀 매팅 또는 ML Kit 고속 분할)으로 배경을 제거합니다.
 */
private suspend fun extractCutoutByEngine(bitmap: Bitmap, engineMode: String): Bitmap =
    withContext(Dispatchers.Default) {
        if (engineMode == ENGINE_MODE_RMBG2) {
            BitmapUtils.removeBackgroundWithRmbg2(bitmap)
        } else {
            BitmapUtils.removeBackgroundWithMlKit(bitmap)
        }
    }
