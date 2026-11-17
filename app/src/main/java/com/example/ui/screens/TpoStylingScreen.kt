package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Garment
import com.example.data.model.SavedOutfit
import com.example.data.model.UserProfile
import com.example.data.remote.WeatherContext
import com.example.data.remote.WeatherMatchedGarment
import com.example.data.remote.WeatherRepository
import com.example.data.remote.WeatherWardrobeMatcher
import com.example.ui.components.B2BLookbookSpecSheetCard
import com.example.ui.components.GarmentCutoutIllustration
import com.example.ui.components.StylebookPackingCapsuleCard
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
import com.example.ui.theme.AtelierIvory
import com.example.ui.theme.AtelierMidnight
import com.example.ui.theme.AtelierSage
import com.example.viewmodel.AiRecommendedCoordi
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.DataUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TpoStylingScreen(
    weather: WeatherContext,
    isWeatherLoading: Boolean,
    profileState: DataUiState<UserProfile>,
    garmentsState: DataUiState<List<Garment>>,
    savedOutfitsState: DataUiState<List<SavedOutfit>>,
    aiCoordiState: AiTaskState<AiRecommendedCoordi>,
    cutoutBitmaps: Map<String, Bitmap>,
    onRefreshWeather: (String) -> Unit,
    onRefreshWeatherByLocation: (Double, Double, String?) -> Unit,
    onGenerateRecommendation: (String, String, List<Garment>, UserProfile) -> Unit,
    onSaveRecommendedOutfit: (AiRecommendedCoordi) -> Unit,
    onIncrementWear: (Garment) -> Unit,
    onDeleteSavedOutfit: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val profile = (profileState as? DataUiState.Success)?.data ?: UserProfile()
    val garments = (garmentsState as? DataUiState.Success)?.data ?: emptyList()
    val savedOutfits = (savedOutfitsState as? DataUiState.Success)?.data ?: emptyList()

    var selectedTpo by remember { mutableStateOf("출근") }
    var scheduleNote by remember { mutableStateOf("오전 10시 팀 주간회의 및 오후 클라이언트 미팅") }
    var selectedWeatherCategoryFilter by remember { mutableStateOf("ALL") }
    var locationStatusHint by remember { mutableStateOf<String?>(null) }

    // Evaluate all garments in the user's wardrobe against the current location's live weather
    val weatherSuggestion = remember(garments, weather) {
        WeatherWardrobeMatcher.evaluateWardrobeForWeather(garments, weather)
    }

    val filteredWeatherGarments = remember(weatherSuggestion, selectedWeatherCategoryFilter) {
        when (selectedWeatherCategoryFilter) {
            "ALL" -> weatherSuggestion.allRankedGarments
            "WATERPROOF" -> weatherSuggestion.allRankedGarments.filter { it.garment.waterResistant }
            else -> weatherSuggestion.allRankedGarments.filter { it.garment.category == selectedWeatherCategoryFilter }
        }
    }

    val fetchCurrentDeviceLocation: () -> Unit = {
        coroutineScope.launch {
            val loc = getBestLastKnownLocation(context)
            if (loc != null) {
                val label = resolveKoreanDistrictLabel(context, loc.latitude, loc.longitude)
                locationStatusHint = "📍 GPS 좌표 (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}) 기상 연동 완료"
                onRefreshWeatherByLocation(loc.latitude, loc.longitude, label)
            } else {
                // Default to Seoul City Hall coordinates if emulator has no cached GPS fix yet
                val defaultLat = 37.5665
                val defaultLon = 126.9780
                locationStatusHint = "📍 현재 기기 위치(서울 중구 기준 ${defaultLat}, ${defaultLon}) 기상 데이터를 불러왔습니다."
                onRefreshWeatherByLocation(defaultLat, defaultLon, "서울 중구")
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentDeviceLocation()
        } else {
            locationStatusHint = "위치 권한이 거부되어 선택된 도시(${weather.cityLabelKo}) 날씨를 기준으로 의류를 추천합니다."
            onRefreshWeather(weather.cityId)
        }
    }

    val tpoKeywords = listOf(
        "출근" to "오전 10시 팀 주간회의 및 오후 클라이언트 미팅",
        "데이트" to "성수동 전시회 관람 후 루프탑 저녁 식사",
        "하객룩" to "주말 호텔 웨딩홀 지인 결혼식 참석",
        "운동" to "한강 공원 러닝 및 애슬레저 산책",
        "발표" to "임원진 대상 분기 성과 프레젠테이션 발표",
        "주말 나들이" to "근교 브런치 카페 및 야외 피크닉"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tpo_styling_screen")
    ) {
        // 1. Live Current Location & Open-Meteo Weather Card (ZIGZAG 8px Card, 1px Hairline)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (weather.isRainyOrSnowy) Icons.Default.Umbrella else Icons.Default.WbSunny,
                                contentDescription = "날씨 아이콘",
                                tint = ZigzagStrong,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (weather.isCurrentGpsLocation) {
                                        "${weather.cityLabelKo} 실시간 GPS 기상 연동"
                                    } else {
                                        "${weather.cityLabelKo} 실시간 기상 연동"
                                    },
                                    style = com.example.ui.theme.ZigzagMetadata.copy(fontSize = 13.sp, color = ZigzagSecondary),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${weather.temperatureC.toInt()}°C · ${weather.conditionLabelKo}",
                                    style = com.example.ui.theme.ZigzagHeading.copy(fontSize = 22.sp, lineHeight = 28.sp),
                                    color = ZigzagStrong,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (weather.isCurrentGpsLocation) {
                                    onRefreshWeatherByLocation(weather.latitude, weather.longitude, null)
                                } else {
                                    onRefreshWeather(weather.cityId)
                                }
                            },
                            enabled = !isWeatherLoading,
                            modifier = Modifier.testTag("refresh_weather_button")
                        ) {
                            if (isWeatherLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "날씨 새로고침",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Current GPS Location Weather Button + City Presets Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Button(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine || hasCoarse) {
                                    fetchCurrentDeviceLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AtelierChampagne,
                                contentColor = AtelierMidnight
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("current_gps_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "현재 위치 날씨 조회",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "내 현재 위치 날씨",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        WeatherRepository.supportedCities.forEach { city ->
                            val isSelected = !weather.isCurrentGpsLocation &&
                                weather.cityId.equals(city.id, ignoreCase = true)
                            AssistChip(
                                onClick = {
                                    locationStatusHint = null
                                    onRefreshWeather(city.id)
                                },
                                label = {
                                    Text(
                                        text = city.labelKo,
                                        color = if (isSelected) AtelierChampagne else AtelierIvory.copy(alpha = 0.78f)
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = AtelierChampagne,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    if (locationStatusHint != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = locationStatusHint!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierChampagne
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "체감 ${weather.apparentTempC.toInt()}°C (최저 ${weather.minTempC.toInt()}° / 최고 ${weather.maxTempC.toInt()}°, 일교차 ${weather.dailyTempRangeC.toInt()}°C) · 습도 ${weather.humidityPercent}% · 강수확률 ${weather.rainProbabilityPercent}% · 풍속 ${weather.windSpeedKmh.toInt()}km/h",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierIvory.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🌤️ ${weatherSuggestion.weatherStrategyHeadline}",
                                style = MaterialTheme.typography.labelLarge,
                                color = AtelierChampagne,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = weatherSuggestion.weatherStrategyDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtelierIvory.copy(alpha = 0.90f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. NEW: Weather-Based Wardrobe Item Recommendation Engine Section
        WeatherWardrobeSuggestionCard(
            weather = weather,
            suggestion = weatherSuggestion,
            filteredGarments = filteredWeatherGarments,
            selectedCategoryFilter = selectedWeatherCategoryFilter,
            cutoutBitmaps = cutoutBitmaps,
            onSelectCategoryFilter = { selectedWeatherCategoryFilter = it },
            onIncrementWear = onIncrementWear,
            onApplyBestWeatherOutfit = {
                val rec = AiRecommendedCoordi(
                    title = "${weather.cityLabelKo} ${weather.temperatureC.toInt()}°C 맞춤 스마트 조합",
                    tpoOccasion = selectedTpo,
                    outerGarment = weatherSuggestion.bestOuter?.garment,
                    topGarment = weatherSuggestion.bestTop?.garment,
                    bottomGarment = weatherSuggestion.bestBottom?.garment,
                    shoesGarment = weatherSuggestion.bestShoes?.garment,
                    aiStylingNote = weatherSuggestion.weatherStrategyDetail,
                    fitScore = 96,
                    weatherMatchReason = weather.stylingAdviceKo
                )
                onSaveRecommendedOutfit(rec)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. TPO & Schedule Context Selector + Gemini AI Coordi Generator (ZIGZAG 8px card)
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ZigzagCanvas),
            border = BorderStroke(1.dp, ZigzagHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "오늘의 일정 (TPO) + AI 심층 코디 매칭",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ZigzagStrong
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tpoKeywords.forEach { (keyword, sampleSchedule) ->
                        val selected = selectedTpo == keyword
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTpo = keyword
                                scheduleNote = sampleSchedule
                            },
                            shape = RoundedCornerShape(24.dp), // ZIGZAG pill
                            label = {
                                Text(
                                    text = "#$keyword",
                                    style = if (selected) com.example.ui.theme.ZigzagTabSelected.copy(fontSize = 13.sp) else com.example.ui.theme.ZigzagMetadata.copy(fontSize = 13.sp),
                                    color = if (selected) ZigzagCtaText else ZigzagSecondary
                                )
                            },
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
                            modifier = Modifier.testTag("tpo_chip_$keyword")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = scheduleNote,
                    onValueChange = { scheduleNote = it },
                    label = { Text("오늘 캘린더 일정 / 상황 메모") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_note_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onGenerateRecommendation(selectedTpo, scheduleNote, garments, profile)
                    },
                    enabled = aiCoordiState !is AiTaskState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtelierMidnight,
                        contentColor = AtelierChampagne
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_tpo_coordi_button")
                ) {
                    Text("날씨 적합도 + 체형 + TPO 결합 AI 코디 추천받기")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. AI Coordi Recommendation Result Card
        when (aiCoordiState) {
            is AiTaskState.Loading -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = aiCoordiState.stepMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is AiTaskState.Error -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AtelierCoral.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiCoordiState.message,
                        color = AtelierCoral,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is AiTaskState.Success -> {
                val rec = aiCoordiState.result
                RecommendedCoordiShowcaseCard(
                    rec = rec,
                    cutoutBitmaps = cutoutBitmaps,
                    onSaveAndWear = { onSaveRecommendedOutfit(rec) }
                )
            }

            AiTaskState.Idle -> {}
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4.5 Stylebook Benchmark: Smart Business Trip / Travel Packing Capsule Builder
        StylebookPackingCapsuleCard(
            garments = garments,
            weather = weather
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4.8 B2B Showroom: VIP Client Lookbook Spec Sheet & QR Delivery Generator
        val lookbookItems = remember(weatherSuggestion, garments) {
            listOfNotNull(
                weatherSuggestion.bestOuter?.garment,
                weatherSuggestion.bestTop?.garment,
                weatherSuggestion.bestBottom?.garment,
                weatherSuggestion.bestShoes?.garment
            ).ifEmpty { garments.take(4) }
        }
        if (lookbookItems.isNotEmpty()) {
            B2BLookbookSpecSheetCard(
                profile = profile,
                weather = weather,
                selectedItems = lookbookItems,
                tpoLabel = selectedTpo
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Saved Outfits History Section
        Text(
            text = "내 코디 북 & 착용 기록 (${savedOutfits.size})",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (savedOutfits.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "아직 저장된 코디 기록이 없습니다. 위에서 날씨 맞춤 추천 조합을 저장하거나 AI 코디를 생성해보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savedOutfits.forEach { outfit ->
                    SavedOutfitItemCard(
                        outfit = outfit,
                        onDelete = { onDeleteSavedOutfit(outfit.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherWardrobeSuggestionCard(
    weather: WeatherContext,
    suggestion: com.example.data.remote.WeatherWardrobeSuggestionSummary,
    filteredGarments: List<WeatherMatchedGarment>,
    selectedCategoryFilter: String,
    cutoutBitmaps: Map<String, Bitmap>,
    onSelectCategoryFilter: (String) -> Unit,
    onIncrementWear: (Garment) -> Unit,
    onApplyBestWeatherOutfit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_wardrobe_suggestion_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AtelierMidnight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = AtelierChampagne,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "현재 날씨 맞춤 옷장 아이템 제안",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${weather.cityLabelKo} ${weather.temperatureC.toInt()}°C · 권장 두께: ${weather.recommendedThickness.joinToString("/")} · 적합 의류 ${suggestion.suitableGarments.size}벌",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Best Category Picks Showcase Row (Top 1 per category for today's weather)
            val bestSet = listOfNotNull(
                suggestion.bestOuter?.let { "추천 아우터" to it },
                suggestion.bestTop?.let { "추천 상의" to it },
                suggestion.bestBottom?.let { "추천 하의" to it },
                suggestion.bestShoes?.let { "추천 신발" to it }
            )

            if (bestSet.isNotEmpty()) {
                Surface(
                    color = Color(0xFFF6F3EC),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ 오늘 날씨 카테고리별 1위 베스트 조합",
                                style = MaterialTheme.typography.labelLarge,
                                color = AtelierMidnight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (weather.needsOuterLayer) "아우터 포함 레이어드" else "단독 단품 매칭",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierSage,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bestSet, key = { it.first + it.second.garment.id }) { (slotLabel, matched) ->
                                BestWeatherSlotMiniCard(
                                    slotLabel = slotLabel,
                                    matched = matched,
                                    cutoutBitmap = cutoutBitmaps[matched.garment.id]
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onApplyBestWeatherOutfit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AtelierSage,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("apply_best_weather_outfit_button")
                        ) {
                            Text(
                                text = "오늘의 날씨 베스트 조합 바로 입기 (코디 저장 & 착용 +1)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips for Ranked Wardrobe Items
            val filterTabs = listOf(
                "ALL" to "전체 (${suggestion.allRankedGarments.size})",
                "OUTER" to "아우터",
                "TOP" to "상의",
                "BOTTOM" to "하의",
                "SHOES" to "신발",
                "WATERPROOF" to "☔ 방수·발수"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                filterTabs.forEach { (code, label) ->
                    FilterChip(
                        selected = selectedCategoryFilter == code,
                        onClick = { onSelectCategoryFilter(code) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("weather_filter_chip_$code")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredGarments.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "해당 카테고리에 등록된 의류가 없습니다. 스마트 옷장 탭에서 의류를 추가해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredGarments.take(6).forEach { matched ->
                        WeatherMatchedGarmentRow(
                            matched = matched,
                            cutoutBitmap = cutoutBitmaps[matched.garment.id],
                            onWearClick = { onIncrementWear(matched.garment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BestWeatherSlotMiniCard(
    slotLabel: String,
    matched: WeatherMatchedGarment,
    cutoutBitmap: Bitmap?
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(122.dp)
            .border(1.dp, Color(0xFFE0D8C8), RoundedCornerShape(14.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "$slotLabel · ${matched.suitabilityScore}%",
                style = MaterialTheme.typography.labelSmall,
                color = AtelierSage,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5F2EB))
            ) {
                GarmentCutoutIllustration(
                    garment = matched.garment,
                    cutoutBitmap = cutoutBitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = matched.garment.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${matched.garment.colorName} · ${matched.garment.thickness}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeatherMatchedGarmentRow(
    matched: WeatherMatchedGarment,
    cutoutBitmap: Bitmap?,
    onWearClick: () -> Unit
) {
    val g = matched.garment
    val scoreColor = when {
        matched.suitabilityScore >= 85 -> AtelierSage
        matched.suitabilityScore >= 70 -> AtelierMidnight
        else -> AtelierCoral
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_matched_item_${g.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF4F1EA))
                ) {
                    GarmentCutoutIllustration(
                        garment = g,
                        cutoutBitmap = cutoutBitmap,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = scoreColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = matched.primaryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = scoreColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (matched.isTopPickForCategory) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AtelierChampagne.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "카테고리 1위",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AtelierMidnight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "[${g.brand}] ${g.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = matched.matchReasons.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onWearClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+1회 착용", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { matched.suitabilityScore / 100f },
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun RecommendedCoordiShowcaseCard(
    rec: AiRecommendedCoordi,
    cutoutBitmaps: Map<String, Bitmap>,
    onSaveAndWear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_recommended_coordi_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AtelierSage.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "AI 매칭 점수 ${rec.fitScore}점 · #${rec.tpoOccasion}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AtelierSage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = rec.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val itemsList = listOfNotNull(
                rec.outerGarment?.let { "아우터" to it },
                rec.topGarment?.let { "상의" to it },
                rec.bottomGarment?.let { "하의" to it },
                rec.shoesGarment?.let { "신발" to it }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                itemsList.forEach { (roleLabel, g) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(106.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(68.dp)) {
                            GarmentCutoutIllustration(
                                garment = g,
                                cutoutBitmap = cutoutBitmaps[g.id],
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = g.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = g.colorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🌤 날씨 매칭 포인트: ${rec.weatherMatchReason}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rec.aiStylingNote,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSaveAndWear,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("wear_recommended_coordi_button")
            ) {
                Text("오늘 이 추천 코디 입기 (착용 횟수 & CPW 자동 갱신)")
            }
        }
    }
}

@Composable
private fun SavedOutfitItemCard(
    outfit: SavedOutfit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AtelierChampagne.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${outfit.fitScore}점",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AtelierMidnight
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${outfit.title} (#${outfit.tpoOccasion})",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${outfit.weatherSummary} · 구성 의류 ${outfit.garmentIds.size}벌",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = outfit.aiStylingNote,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "코디 삭제"
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getBestLastKnownLocation(context: Context): Location? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) {
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
        }
        bestLocation
    } catch (e: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
private suspend fun resolveKoreanDistrictLabel(
    context: Context,
    lat: Double,
    lon: Double
): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.KOREA)
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val addr = addresses?.firstOrNull() ?: return@withContext null
        listOfNotNull(addr.adminArea, addr.locality ?: addr.subLocality)
            .distinct()
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}
