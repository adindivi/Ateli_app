package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Garment
import com.example.ui.theme.ZigzagHairline
import com.example.ui.theme.ZigzagMuted
import com.example.ui.theme.ZigzagStrong
import com.example.util.CapsuleWardrobeOptimizer
import com.example.util.CapsuleWardrobeReport

/**
 * CapsuleWardrobeReportSheet
 *
 * 깃허브 wardrobe-graph 기반 캡슐 옷장 결핍(Gap) 분석 및 코디 확장 리포트 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleWardrobeReportSheet(
    garments: List<Garment>,
    onDismissRequest: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val report = CapsuleWardrobeOptimizer.analyze(garments)

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
                        text = "캡슐 옷장 리포트",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZigzagStrong,
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

            // 1. 캡슐 효율성 점수 카드
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capsule_efficiency_card"),
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
                        Column {
                            Text(
                                text = "캡슐 옷장 활용도",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = ZigzagMuted,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${report.capsuleEfficiencyScore}점",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0071E3),
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
                                text = "${report.possibleOutfitsCount}가지 코디",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0071E3),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { report.capsuleEfficiencyScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF0071E3),
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 카테고리 분포 캡슐
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        report.categoryDistribution.forEach { (cat, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cat,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = ZigzagMuted,
                                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${count}벌",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZigzagStrong,
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

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 핵심 결핍 아이템(Wardrobe Gap) 추천 카드
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capsule_gap_card"),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A), // 핀테크 다크
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0071E3)
                        ) {
                            Text(
                                text = "추천 1벌",
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "+${report.potentialNewOutfitsAdded}개 코디 확장",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = report.recommendedGapItemName,
                        style = TextStyle(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = report.stylingAdvice,
                        style = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onShowToast("'${report.recommendedGapItemName}'을(를) 위시리스트에 담았어요")
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_add_gap_wishlist"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "위시리스트 담기",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
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
