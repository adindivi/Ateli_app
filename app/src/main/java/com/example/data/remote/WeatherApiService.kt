package com.example.data.remote

import com.example.data.model.Garment
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.hypot
import kotlin.math.roundToInt

data class OpenMeteoResponse(
    @Json(name = "current") val current: CurrentWeatherDto? = null,
    @Json(name = "daily") val daily: DailyWeatherDto? = null
)

data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 18.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 55,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 17.5,
    @Json(name = "precipitation") val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 2.5
)

data class DailyWeatherDto(
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double>? = null,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double>? = null
)

interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "precipitation_probability_max,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): OpenMeteoResponse
}

data class CityCoordinates(
    val id: String,
    val labelKo: String,
    val lat: Double,
    val lon: Double
)

data class WeatherContext(
    val cityId: String = "Seoul",
    val cityLabelKo: String = "서울",
    val latitude: Double = 37.5665,
    val longitude: Double = 126.9780,
    val isCurrentGpsLocation: Boolean = false,
    val temperatureC: Double = 17.0,
    val apparentTempC: Double = 16.5,
    val minTempC: Double = 12.0,
    val maxTempC: Double = 21.0,
    val humidityPercent: Int = 55,
    val rainProbabilityPercent: Int = 10,
    val windSpeedKmh: Double = 8.0,
    val conditionLabelKo: String = "맑음",
    val isRainyOrSnowy: Boolean = false,
    val recommendedThickness: List<String> = listOf("LIGHT", "MEDIUM"),
    val recommendedSeason: String = "SPRING_FALL",
    val stylingAdviceKo: String = "일교차가 있는 선선한 날씨예요. 가벼운 셔츠나 니트에 트렌치코트·블레이저 레이어드를 추천합니다."
) {
    val dailyTempRangeC: Double
        get() = (maxTempC - minTempC).coerceAtLeast(0.0)

    val needsOuterLayer: Boolean
        get() = apparentTempC < 21.0 || dailyTempRangeC >= 8.0 || isRainyOrSnowy || windSpeedKmh >= 14.0

    val summaryText: String
        get() = "$cityLabelKo ${temperatureC.toInt()}°C · $conditionLabelKo (강수확률 $rainProbabilityPercent%)"
}

/**
 * Represents a garment from the user's wardrobe evaluated against the current location's weather.
 */
data class WeatherMatchedGarment(
    val garment: Garment,
    val suitabilityScore: Int, // 0..100
    val primaryBadge: String,
    val matchReasons: List<String>,
    val isTopPickForCategory: Boolean = false
)

/**
 * Aggregated result of matching the user's wardrobe against the current location's weather.
 */
data class WeatherWardrobeSuggestionSummary(
    val allRankedGarments: List<WeatherMatchedGarment>,
    val suitableGarments: List<WeatherMatchedGarment>,
    val bestOuter: WeatherMatchedGarment?,
    val bestTop: WeatherMatchedGarment?,
    val bestBottom: WeatherMatchedGarment?,
    val bestShoes: WeatherMatchedGarment?,
    val weatherStrategyHeadline: String,
    val weatherStrategyDetail: String
)

/**
 * Multi-factor rule & scoring engine that evaluates every item in the user's wardrobe
 * against current location weather telemetry (temperature, wind chill/apparent temp,
 * diurnal temperature swing, humidity, rain/snow probability, and wind speed).
 */
object WeatherWardrobeMatcher {

    fun evaluateWardrobeForWeather(
        garments: List<Garment>,
        weather: WeatherContext
    ): WeatherWardrobeSuggestionSummary {
        if (garments.isEmpty()) {
            return WeatherWardrobeSuggestionSummary(
                allRankedGarments = emptyList(),
                suitableGarments = emptyList(),
                bestOuter = null,
                bestTop = null,
                bestBottom = null,
                bestShoes = null,
                weatherStrategyHeadline = "옷장에 등록된 의류가 없습니다.",
                weatherStrategyDetail = "스마트 옷장 탭에서 의류를 등록하거나 샘플 옷장을 불러오면 현재 날씨에 맞는 아이템을 제안해 드립니다."
            )
        }

        val scored = garments.map { garment ->
            scoreSingleGarment(garment, weather)
        }.sortedWith(
            compareByDescending<WeatherMatchedGarment> { it.suitabilityScore }
                .thenBy { it.garment.wearCount } // Favor under-utilized items with high suitability to improve CPW
        )

        val bestOuter = if (weather.needsOuterLayer) {
            scored.firstOrNull { it.garment.category == "OUTER" && it.suitabilityScore >= 60 }
        } else null
        val bestTop = scored.firstOrNull { it.garment.category == "TOP" }
        val bestBottom = scored.firstOrNull { it.garment.category == "BOTTOM" }
        val bestShoes = scored.firstOrNull { it.garment.category == "SHOES" }

        val topPickIds = setOfNotNull(
            bestOuter?.garment?.id,
            bestTop?.garment?.id,
            bestBottom?.garment?.id,
            bestShoes?.garment?.id
        )

        val markedRanked = scored.map { item ->
            if (item.garment.id in topPickIds) {
                item.copy(isTopPickForCategory = true)
            } else item
        }

        val suitable = markedRanked.filter { it.suitabilityScore >= 68 }

        val headline = buildString {
            append("${weather.cityLabelKo} ${weather.temperatureC.roundToInt()}°C (체감 ${weather.apparentTempC.roundToInt()}°C) 맞춤 옷장 큐레이션")
        }

        val detail = buildString {
            when {
                weather.isRainyOrSnowy -> {
                    append("강수 예보(${weather.rainProbabilityPercent}%)가 있어 생활방수 아우터·신발과 오염에 강한 다크톤 하의를 최우선으로 선별했습니다. ")
                }
                weather.dailyTempRangeC >= 8.0 -> {
                    append("일교차가 ${weather.dailyTempRangeC.roundToInt()}°C로 커서 출퇴근길 탈착이 쉬운 레이어드 아우터와 미디엄 두께 이너를 조합했습니다. ")
                }
                weather.apparentTempC >= 23.0 -> {
                    append("기온이 높아 통기성이 뛰어난 얇은(LIGHT) 두께감과 밝은 톤 아이템 위주로 추천합니다. ")
                }
                weather.apparentTempC <= 8.0 -> {
                    append("체감온도가 낮아 보온성이 우수한 도톰한(HEAVY/MEDIUM) 니트와 코트류를 우선 배치했습니다. ")
                }
                else -> {
                    append("야외 활동에 쾌적한 기온으로, 보유 옷장 중 활용도와 핏 밸런스가 뛰어난 아이템을 매칭했습니다. ")
                }
            }
            append("(총 ${garments.size}벌 중 ${suitable.size}벌 적합 판정)")
        }

        return WeatherWardrobeSuggestionSummary(
            allRankedGarments = markedRanked,
            suitableGarments = suitable,
            bestOuter = markedRanked.firstOrNull { it.garment.id == bestOuter?.garment?.id },
            bestTop = markedRanked.firstOrNull { it.garment.id == bestTop?.garment?.id },
            bestBottom = markedRanked.firstOrNull { it.garment.id == bestBottom?.garment?.id },
            bestShoes = markedRanked.firstOrNull { it.garment.id == bestShoes?.garment?.id },
            weatherStrategyHeadline = headline,
            weatherStrategyDetail = detail
        )
    }

    private fun scoreSingleGarment(
        garment: Garment,
        weather: WeatherContext
    ): WeatherMatchedGarment {
        var score = 0
        val reasons = mutableListOf<String>()
        val apparent = weather.apparentTempC

        // 1. Thermal & Fabric Thickness Match (Max 40 pts)
        val thicknessScore = when {
            apparent >= 23.0 -> when (garment.thickness) {
                "LIGHT" -> {
                    reasons.add("${apparent.roundToInt()}°C 기온에 쾌적한 가벼운(LIGHT) 소재")
                    40
                }
                "MEDIUM" -> {
                    if (garment.category == "BOTTOM" || garment.category == "SHOES") 32 else 18
                }
                else -> 4
            }
            apparent >= 14.0 -> when (garment.thickness) {
                "MEDIUM" -> {
                    reasons.add("체감 ${apparent.roundToInt()}°C에 딱 맞는 적당한 두께감")
                    40
                }
                "LIGHT" -> {
                    reasons.add("간절기 이너 및 레이어드에 적합한 두께")
                    36
                }
                else -> 16
            }
            apparent >= 7.0 -> when (garment.thickness) {
                "MEDIUM" -> {
                    reasons.add("선선한 기온(${apparent.roundToInt()}°C) 보온과 활동성 균형")
                    38
                }
                "HEAVY" -> {
                    reasons.add("쌀쌀한 바람을 막아주는 도톰한 보온 소재")
                    38
                }
                else -> if (garment.category == "TOP") 26 else 14
            }
            else -> when (garment.thickness) {
                "HEAVY" -> {
                    reasons.add("${apparent.roundToInt()}°C 저온 환경 필수 방한 두께")
                    40
                }
                "MEDIUM" -> 26
                else -> 10
            }
        }
        score += thicknessScore

        // 2. Seasonality Match (Max 25 pts)
        val seasonScore = when {
            garment.season == weather.recommendedSeason -> {
                reasons.add("현재 계절감(${seasonLabelKo(weather.recommendedSeason)}) 완벽 일치")
                25
            }
            garment.season == "ALL" -> {
                reasons.add("사계절 전천후 활용 아이템")
                22
            }
            else -> 8
        }
        score += seasonScore

        // 3. Rain / Snow / Wind / Diurnal Swing Adaptation (Max 25 pts)
        var weatherConditionScore = 15
        if (weather.isRainyOrSnowy || weather.rainProbabilityPercent >= 50) {
            if (garment.waterResistant) {
                weatherConditionScore = 25
                reasons.add("☔ 강수확률 ${weather.rainProbabilityPercent}% 대비 생활방수·발수 기능 탑재")
            } else if (garment.category == "BOTTOM" && isDarkColor(garment.colorHex, garment.colorName)) {
                weatherConditionScore = 22
                reasons.add("우천 시 빗물 오염이 적은 다크톤 하의")
            } else if (garment.subCategory.contains("스웨이드") || garment.colorName.contains("화이트") || garment.colorName.contains("크림")) {
                weatherConditionScore = 5
                reasons.add("우천 시 오염 주의 필요")
            }
        } else {
            // Clear / Mild weather
            if (weather.dailyTempRangeC >= 7.5 && garment.category == "OUTER") {
                weatherConditionScore = 25
                reasons.add("일교차 ${weather.dailyTempRangeC.roundToInt()}°C 대비 탈착하기 좋은 아우터")
            } else if (weather.windSpeedKmh >= 12.0 && garment.category == "OUTER") {
                weatherConditionScore = 24
                reasons.add("바람(${weather.windSpeedKmh.roundToInt()}km/h)을 막아주는 방풍 실루엣")
            } else if (!weather.needsOuterLayer && garment.category == "OUTER") {
                weatherConditionScore = 8
            } else {
                weatherConditionScore = 21
            }
        }
        score += weatherConditionScore

        // 4. Wardrobe Value Recovery (CPW) Bonus (Max 10 pts)
        val cpwBonus = when {
            garment.wearCount == 0L -> {
                reasons.add("미착용 방치템 — 오늘 입으면 가성비(CPW) 대폭 상승!")
                10
            }
            garment.wearCount <= 3L -> {
                reasons.add("착용 횟수 ${garment.wearCount}회 — CPW 절감 추천 아이템")
                8
            }
            else -> 6
        }
        score += cpwBonus

        val finalScore = score.coerceIn(25, 99)

        val badge = when {
            weather.isRainyOrSnowy && garment.waterResistant -> "☔ 우천 방수 추천 (${finalScore}%)"
            weather.dailyTempRangeC >= 7.5 && garment.category == "OUTER" -> "🌡️ 일교차 아우터 (${finalScore}%)"
            finalScore >= 88 -> "🌤️ 오늘 날씨 최적 (${finalScore}%)"
            finalScore >= 75 -> "👍 날씨 적합 (${finalScore}%)"
            else -> "보조 매칭 (${finalScore}%)"
        }

        return WeatherMatchedGarment(
            garment = garment,
            suitabilityScore = finalScore,
            primaryBadge = badge,
            matchReasons = reasons.take(3)
        )
    }

    private fun seasonLabelKo(seasonCode: String): String = when (seasonCode) {
        "SUMMER" -> "여름"
        "WINTER" -> "겨울"
        "SPRING_FALL" -> "봄·가을 간절기"
        else -> "사계절"
    }

    private fun isDarkColor(hex: String, colorName: String): Boolean {
        val lower = colorName.lowercase()
        if (lower.contains("블랙") || lower.contains("차콜") || lower.contains("네이비") ||
            lower.contains("인디고") || lower.contains("브라운")
        ) {
            return true
        }
        val clean = hex.removePrefix("#")
        if (clean.length == 6) {
            val r = clean.substring(0, 2).toIntOrNull(16) ?: 128
            val g = clean.substring(2, 4).toIntOrNull(16) ?: 128
            val b = clean.substring(4, 6).toIntOrNull(16) ?: 128
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            return luminance < 110.0
        }
        return false
    }
}

object WeatherRepository {
    val supportedCities = listOf(
        CityCoordinates("Seoul", "서울", 37.5665, 126.9780),
        CityCoordinates("Busan", "부산", 35.1796, 129.0756),
        CityCoordinates("Incheon", "인천", 37.4563, 126.7052),
        CityCoordinates("Daegu", "대구", 35.8714, 128.6014),
        CityCoordinates("Daejeon", "대전", 36.3504, 127.3845),
        CityCoordinates("Gwangju", "광주", 35.1595, 126.8526),
        CityCoordinates("Jeju", "제주", 33.4996, 126.5312)
    )

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoService::class.java)
    }

    suspend fun fetchWeatherForCity(cityId: String): Result<WeatherContext> = runCatching {
        val city = supportedCities.firstOrNull { it.id.equals(cityId, ignoreCase = true) }
            ?: supportedCities.first()
        fetchOpenMeteoContext(
            lat = city.lat,
            lon = city.lon,
            cityId = city.id,
            cityLabelKo = city.labelKo,
            isGps = false
        )
    }

    /**
     * Fetches live Open-Meteo weather using the device's current GPS / Network coordinates.
     */
    suspend fun fetchWeatherForCoordinates(
        latitude: Double,
        longitude: Double,
        resolvedAddressLabel: String? = null
    ): Result<WeatherContext> = runCatching {
        val nearestCity = findNearestSupportedCity(latitude, longitude)
        val labelKo = if (!resolvedAddressLabel.isNullOrBlank()) {
            "현위치 ($resolvedAddressLabel)"
        } else {
            "현위치 (${nearestCity.labelKo} 인근)"
        }
        fetchOpenMeteoContext(
            lat = latitude,
            lon = longitude,
            cityId = "GPS_CURRENT",
            cityLabelKo = labelKo,
            isGps = true
        )
    }

    fun findNearestSupportedCity(lat: Double, lon: Double): CityCoordinates {
        return supportedCities.minByOrNull { city ->
            hypot(city.lat - lat, city.lon - lon)
        } ?: supportedCities.first()
    }

    private suspend fun fetchOpenMeteoContext(
        lat: Double,
        lon: Double,
        cityId: String,
        cityLabelKo: String,
        isGps: Boolean
    ): WeatherContext {
        val response = service.getForecast(latitude = lat, longitude = lon)
        val current = response.current ?: CurrentWeatherDto()
        val daily = response.daily
        val rainProb = daily?.precipitationProbabilityMax?.firstOrNull()
            ?: if (current.precipitation > 0.0) 80 else 10
        val maxTemp = daily?.temperatureMax?.firstOrNull() ?: (current.temperature2m + 3.0)
        val minTemp = daily?.temperatureMin?.firstOrNull() ?: (current.temperature2m - 4.0)

        val code = current.weatherCode
        val conditionKo = when (code) {
            0 -> "쾌청함"
            1, 2 -> "구름 조금"
            3 -> "흐림"
            45, 48 -> "안개"
            in 51..67, in 80..82 -> "비 내림"
            in 71..77, 85, 86 -> "눈 내림"
            in 95..99 -> "뇌우·비"
            else -> "맑음"
        }
        val isRainy = code in 51..99 || rainProb >= 60 || current.precipitation > 0.2

        val temp = current.temperature2m
        val thicknessList = when {
            temp >= 23.0 -> listOf("LIGHT")
            temp >= 13.0 -> listOf("LIGHT", "MEDIUM")
            temp >= 6.0 -> listOf("MEDIUM", "HEAVY")
            else -> listOf("HEAVY")
        }
        val seasonCode = when {
            temp >= 23.0 -> "SUMMER"
            temp <= 7.0 -> "WINTER"
            else -> "SPRING_FALL"
        }
        val advice = buildString {
            when {
                temp >= 24.0 -> append("통기성이 좋은 얇은 셔츠나 반팔, 린넨 소재가 쾌적합니다.")
                temp >= 15.0 -> append("활동하기 좋은 기온입니다. 단독 니트나 셔츠에 가벼운 자켓·트렌치를 매치하세요.")
                temp >= 8.0 -> append("공기가 제법 쌀쌀합니다. 보온성 있는 니트와 코트·아우터 레이어드가 필수입니다.")
                else -> append("기온이 낮아 도톰한 헤비 아우터와 방한 니트 중심 코디를 추천합니다.")
            }
            if (isRainy) {
                append(" 비 예보가 있으니 발수·방수 아우터와 어두운 톤 하의를 우선 선택하세요.")
            }
        }

        return WeatherContext(
            cityId = cityId,
            cityLabelKo = cityLabelKo,
            latitude = lat,
            longitude = lon,
            isCurrentGpsLocation = isGps,
            temperatureC = temp,
            apparentTempC = current.apparentTemperature,
            minTempC = minTemp,
            maxTempC = maxTemp,
            humidityPercent = current.relativeHumidity2m,
            rainProbabilityPercent = rainProb,
            windSpeedKmh = current.windSpeed10m,
            conditionLabelKo = conditionKo,
            isRainyOrSnowy = isRainy,
            recommendedThickness = thicknessList,
            recommendedSeason = seasonCode,
            stylingAdviceKo = advice
        )
    }
}
