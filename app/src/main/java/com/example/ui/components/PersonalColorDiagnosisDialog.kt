package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagStrong
import com.example.util.PersonalColorAnalyzer
import com.example.util.PersonalColorDiagnosisResult
import com.example.util.PersonalColorSeason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PersonalColorDiagnosisDialog
 *
 * 깃허브 koreai / rizz-up 기반 온디바이스 얼굴 피부톤 분석 및 퍼스널 컬러 진단 모달
 */
@Composable
fun PersonalColorDiagnosisDialog(
    currentToneCode: String?,
    onDismissRequest: () -> Unit,
    onToneSelected: (PersonalColorSeason) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAnalyzing by remember { mutableStateOf(false) }
    var diagnosisResult by remember {
        mutableStateOf<PersonalColorDiagnosisResult?>(null)
    }
    var selectedSeason by remember {
        mutableStateOf(PersonalColorSeason.fromCode(currentToneCode))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            isAnalyzing = true
            coroutineScope.launch {
                val res = withContext(Dispatchers.Default) {
                    PersonalColorAnalyzer.analyzeFromBitmap(bmp)
                }
                diagnosisResult = res
                selectedSeason = res.season
                isAnalyzing = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("personal_color_diagnosis_dialog"),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                            text = "AI 퍼스널 컬러 진단",
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
                            text = "피부톤 맞춤 컬러 찾기",
                            style = TextStyle(
                                fontSize = 11.5.sp,
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
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = ZigzagMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 진단 액션 버튼 2종
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 카메라 촬영 진단
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { cameraLauncher.launch(null) }
                            .testTag("btn_personal_color_camera"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = ZigzagStrong,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "얼굴 촬영 진단",
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

                    // 2. 1초 간편 자동 진단 (기본 피부톤 기준)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                            isAnalyzing = true
                            coroutineScope.launch {
                                val mockFace = withContext(Dispatchers.Default) {
                                    createMockFaceBitmap()
                                }
                                val res = withContext(Dispatchers.Default) {
                                    PersonalColorAnalyzer.analyzeFromBitmap(mockFace)
                                }
                                diagnosisResult = res
                                selectedSeason = res.season
                                isAnalyzing = false
                            }
                        }
                            .testTag("btn_personal_color_quick"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0071E3).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF0071E3).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0071E3),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1초 간편 진단",
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

                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF0071E3),
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CIELAB L*a*b* 언더톤 분석 중...",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ZigzagStrong,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                } else {
                    // 결과 카드
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personal_color_result_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color(android.graphics.Color.parseColor(selectedSeason.representativeHex))
                                            )
                                            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedSeason.labelKo,
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZigzagStrong,
                                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF0071E3).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = selectedSeason.undertoneKo,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0071E3),
                                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedSeason.descriptionKo,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF475569),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 베스트 팔레트 스와치
                            Text(
                                text = "베스트 컬러",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZigzagMuted,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedSeason.bestPaletteHex.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 워스트 팔레트 스와치
                            Text(
                                text = "피할 컬러",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZigzagMuted,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedSeason.worstPaletteHex.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 8-Tone 수동 빠른 선택 칩
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "직접 선택하기",
                            style = TextStyle(
                                fontSize = 11.5.sp,
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
                            PersonalColorSeason.entries.forEach { season ->
                                val isSelected = selectedSeason == season
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) ZigzagStrong else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) ZigzagStrong else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .clickable { selectedSeason = season }
                                        .testTag("tone_chip_${season.code}")
                                ) {
                                    Text(
                                        text = season.labelKo,
                                        style = TextStyle(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else ZigzagMuted,
                                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 저장 버튼 (핀테크 다크 블랙)
                    Button(
                        onClick = {
                            onToneSelected(selectedSeason)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_personal_color"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZigzagStrong,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "${selectedSeason.labelKo} 적용",
                            style = TextStyle(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
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

private fun createMockFaceBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = Paint().apply {
        color = android.graphics.Color.rgb(250, 218, 195) // 봄 웜 복숭아빛 피부톤
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, 200f, 200f, paint)
    return bmp
}
