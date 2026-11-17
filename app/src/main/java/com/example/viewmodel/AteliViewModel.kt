package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FittingHistoryEntity
import com.example.data.local.LocalFittingHistoryRepository
import com.example.data.model.BodyShapeType
import com.example.data.model.CuratedWardrobePresets
import com.example.data.model.Garment
import com.example.data.model.SavedOutfit
import com.example.data.model.UserProfile
import com.example.data.remote.WeatherContext
import com.example.data.remote.WeatherRepository
import com.example.data.remote.WeatherWardrobeMatcher
import com.example.data.repository.WardrobeRepository
import com.example.ui.components.AvatarBodyMetrics
import com.example.util.BitmapUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

private const val TAG = "AteliVM"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data class Loading(val stepMessage: String) : AiTaskState<Nothing>
    data class Success<T>(val result: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class AiRecommendedCoordi(
    val title: String,
    val tpoOccasion: String,
    val outerGarment: Garment?,
    val topGarment: Garment?,
    val bottomGarment: Garment?,
    val shoesGarment: Garment?,
    val aiStylingNote: String,
    val fitScore: Int,
    val weatherMatchReason: String
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val modelBadge: String = ""
)


class AteliViewModel(
    private val repository: WardrobeRepository,
    private val currentUserId: String,
    private val localFittingRepository: LocalFittingHistoryRepository? = null
) : ViewModel() {

    // 1. Structured Output Model for Receipt / Image Auto-Registration (Zero-Effort Wardrobe)
    private val receiptParserSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Korean garment product name"),
                        "brand" to Schema.string(description = "Brand or store name"),
                        "category" to Schema.enumeration(
                            values = listOf("OUTER", "TOP", "BOTTOM", "SHOES", "BAG"),
                            description = "Primary garment category"
                        ),
                        "subCategory" to Schema.string(description = "Specific Korean subcategory e.g. 트렌치코트, 니트, 슬랙스"),
                        "colorName" to Schema.string(description = "Korean color name e.g. 베이지, 네이비"),
                        "colorHex" to Schema.string(description = "Hex color string starting with # e.g. #D2B48C"),
                        "season" to Schema.enumeration(
                            values = listOf("SPRING_FALL", "SUMMER", "WINTER", "ALL"),
                            description = "Suitable season"
                        ),
                        "pattern" to Schema.string(description = "Pattern in Korean e.g. 솔리드, 스트라이프, 체크"),
                        "thickness" to Schema.enumeration(
                            values = listOf("LIGHT", "MEDIUM", "HEAVY"),
                            description = "Fabric thickness"
                        ),
                        "waterResistant" to Schema.boolean(description = "True if water-repellent or suitable for rain"),
                        "tpoTags" to Schema.array(
                            items = Schema.string(),
                            description = "2 to 4 TPO tags in Korean such as 출근, 데이트, 하객룩, 운동, 주말 나들이"
                        ),
                        "purchasePrice" to Schema.integer(description = "Price in KRW integer"),
                        "receiptStore" to Schema.string(description = "Store name e.g. 무신사, 29CM, W컨셉")
                    )
                ),
                description = "Extracted clothing items from receipt or image"
            )
        )
    )

    private val receiptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptParserSchema
            }
        )
    }

    // 2. Structured Output Model for Context-Aware TPO & Weather Styling Recommendation
    private val tpoCoordiSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Catchy Korean title for the recommended outfit"),
            "outerId" to Schema.string(description = "Selected OUTER garment ID or empty string if none needed"),
            "topId" to Schema.string(description = "Selected TOP garment ID"),
            "bottomId" to Schema.string(description = "Selected BOTTOM garment ID"),
            "shoesId" to Schema.string(description = "Selected SHOES garment ID"),
            "fitScore" to Schema.integer(description = "Match score between 80 and 99"),
            "weatherMatchReason" to Schema.string(description = "Short Korean explanation of how weather was handled"),
            "aiStylingNote" to Schema.string(description = "Detailed Korean styling advice tailored to user's body shape and TPO")
        )
    )

    private val tpoStylingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tpoCoordiSchema
            }
        )
    }

    // Real-time Firestore StateFlows with Two-Tier Error Handling
    val profileState: StateFlow<DataUiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, DataUiState<UserProfile>> { profile ->
            DataUiState.Success(profile ?: UserProfile(userId = currentUserId))
        }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(DataUiState.Error(e.localizedMessage ?: "프로필을 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val garmentsState: StateFlow<DataUiState<List<Garment>>> = repository.observeGarments(currentUserId)
        .map<List<Garment>, DataUiState<List<Garment>>> { list ->
            val sorted = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
            DataUiState.Success(sorted)
        }
        .catch { e ->
            Log.w(TAG, "Error observing garments", e)
            emit(DataUiState.Error(e.localizedMessage ?: "옷장 데이터를 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val savedOutfitsState: StateFlow<DataUiState<List<SavedOutfit>>> = repository.observeSavedOutfits(currentUserId)
        .map<List<SavedOutfit>, DataUiState<List<SavedOutfit>>> { list ->
            val sorted = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
            DataUiState.Success(sorted)
        }
        .catch { e ->
            Log.w(TAG, "Error observing outfits", e)
            emit(DataUiState.Error(e.localizedMessage ?: "저장된 코디를 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    private val _weatherState = MutableStateFlow(WeatherContext())
    val weatherState: StateFlow<WeatherContext> = _weatherState.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private val _aiRegistrationState = MutableStateFlow<AiTaskState<List<Garment>>>(AiTaskState.Idle)
    val aiRegistrationState: StateFlow<AiTaskState<List<Garment>>> = _aiRegistrationState.asStateFlow()

    private val _aiCoordiState = MutableStateFlow<AiTaskState<AiRecommendedCoordi>>(AiTaskState.Idle)
    val aiCoordiState: StateFlow<AiTaskState<AiRecommendedCoordi>> = _aiCoordiState.asStateFlow()

    private val _processedCutoutBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val processedCutoutBitmaps: StateFlow<Map<String, Bitmap>> = _processedCutoutBitmaps.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val defaultGreetingMessage = ChatMessage(
        id = "welcome_msg",
        text = "안녕하세요! Ateli(아틀리에) 수석 AI 퍼스널 스타일리스트입니다. 회원님의 체형 치수, 실시간 날씨, 그리고 옷장 가성비(CPW) 데이터를 바탕으로 맞춤형 코디와 쇼핑 전략을 제안해 드릴게요. 무엇이든 물어보세요!",
        isUser = false,
        modelBadge = "AI 퍼스널 스타일리스트"
    )

    private val _chatMessages = MutableStateFlow(listOf(defaultGreetingMessage))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _selectedChatModel = MutableStateFlow("gemini-3.8-flash")
    val selectedChatModel: StateFlow<String> = _selectedChatModel.asStateFlow()

    private val _inMemoryFittingHistory = MutableStateFlow<List<FittingHistoryEntity>>(emptyList())
    val recentFittingHistoryState: StateFlow<List<FittingHistoryEntity>> =
        if (localFittingRepository != null) {
            localFittingRepository.allHistoryFlow
                .catch { e ->
                    Log.w(TAG, "Error observing Room fitting history", e)
                    emit(emptyList())
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = emptyList()
                )
        } else {
            _inMemoryFittingHistory.asStateFlow()
        }

    init {
        refreshWeather("Seoul")
    }

    fun recordFittingAttempt(
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        metrics: AvatarBodyMetrics,
        canvasModeLabel: String,
        triggerSourceLabel: String,
        garmentScale: Float,
        skipIfDuplicateOfLatest: Boolean = true,
        showSnackbar: Boolean = false
    ) {
        val activeItems = listOfNotNull(outer, top, bottom, shoes)
        if (activeItems.isEmpty()) return

        viewModelScope.launch {
            if (localFittingRepository != null) {
                localFittingRepository.recordFittingAttempt(
                    outer = outer,
                    top = top,
                    bottom = bottom,
                    shoes = shoes,
                    metrics = metrics,
                    canvasModeLabel = canvasModeLabel,
                    triggerSourceLabel = triggerSourceLabel,
                    garmentScale = garmentScale,
                    skipIfDuplicateOfLatest = skipIfDuplicateOfLatest
                )
            } else {
                val signature = LocalFittingHistoryRepository.buildSignature(outer, top, bottom, shoes)
                val latest = _inMemoryFittingHistory.value.firstOrNull()
                if (!skipIfDuplicateOfLatest || latest == null || latest.combinationSignature != signature) {
                    val entity = LocalFittingHistoryRepository.createEntityFromSelection(
                        outer = outer,
                        top = top,
                        bottom = bottom,
                        shoes = shoes,
                        metrics = metrics,
                        canvasModeLabel = canvasModeLabel,
                        triggerSourceLabel = triggerSourceLabel,
                        garmentScale = garmentScale
                    ).copy(id = System.currentTimeMillis())
                    _inMemoryFittingHistory.value = listOf(entity) + _inMemoryFittingHistory.value
                }
            }
            if (showSnackbar) {
                _statusBannerMessage.value = "🕒 현재 가상 피팅 조합이 '최근 피팅' 타임라인에 로컬 저장되었습니다!"
            }
        }
    }

    fun togglePinFittingRecord(record: FittingHistoryEntity) {
        viewModelScope.launch {
            if (localFittingRepository != null) {
                localFittingRepository.togglePin(record.id, record.isPinned)
            } else {
                _inMemoryFittingHistory.value = _inMemoryFittingHistory.value.map {
                    if (it.id == record.id) it.copy(isPinned = !it.isPinned) else it
                }
            }
        }
    }

    fun deleteFittingRecord(id: Long) {
        viewModelScope.launch {
            if (localFittingRepository != null) {
                localFittingRepository.deleteById(id)
            } else {
                _inMemoryFittingHistory.value = _inMemoryFittingHistory.value.filterNot { it.id == id }
            }
        }
    }

    fun clearUnpinnedFittingHistory() {
        viewModelScope.launch {
            if (localFittingRepository != null) {
                localFittingRepository.clearUnpinned()
            } else {
                _inMemoryFittingHistory.value = _inMemoryFittingHistory.value.filter { it.isPinned }
            }
            _statusBannerMessage.value = "고정(Pin)된 코디를 제외한 최근 피팅 기록을 정리했습니다."
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun resetAiRegistrationState() {
        _aiRegistrationState.value = AiTaskState.Idle
    }

    fun refreshWeather(cityId: String) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            WeatherRepository.fetchWeatherForCity(cityId)
                .onSuccess { weather ->
                    _weatherState.value = weather
                }
                .onFailure { e ->
                    Log.w(TAG, "Weather API fallback notice", e)
                }
            _isWeatherLoading.value = false
        }
    }

    /**
     * Fetches live weather for the user's current GPS/Network coordinates and updates the wardrobe recommendations.
     */
    fun refreshWeatherByLocation(
        latitude: Double,
        longitude: Double,
        addressLabel: String? = null
    ) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            WeatherRepository.fetchWeatherForCoordinates(
                latitude = latitude,
                longitude = longitude,
                resolvedAddressLabel = addressLabel
            )
                .onSuccess { weather ->
                    _weatherState.value = weather
                    _statusBannerMessage.value = "📍 ${weather.cityLabelKo} 실시간 날씨(${weather.temperatureC.toInt()}°C · ${weather.conditionLabelKo}) 기준으로 옷장 추천이 갱신되었습니다!"
                }
                .onFailure { e ->
                    Log.w(TAG, "GPS Weather API fallback notice", e)
                    val nearest = WeatherRepository.findNearestSupportedCity(latitude, longitude)
                    _statusBannerMessage.value = "현위치 인근(${nearest.labelKo}) 기상 데이터를 적용했습니다."
                }
            _isWeatherLoading.value = false
        }
    }

    fun seedStarterWardrobe() {
        viewModelScope.launch {
            val starters = CuratedWardrobePresets.starterWardrobeItems()
            var addedCount = 0
            for (item in starters) {
                val res = repository.addGarment(item)
                if (res.isSuccess) addedCount++
            }
            if (addedCount > 0) {
                _statusBannerMessage.value = "샘플 스마트 옷장 아이템 ${addedCount}벌이 등록되었습니다!"
            }
        }
    }

    fun saveBodyProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
                .onSuccess {
                    _statusBannerMessage.value = "내 체형 프로필(${BodyShapeType.fromCode(profile.bodyShape).labelKo})이 저장되었습니다."
                    refreshWeather(profile.preferredCity)
                }
                .onFailure { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "프로필 저장 실패"
                }
        }
    }

    fun incrementGarmentWearCount(garment: Garment) {
        if (garment.id.isBlank()) return
        viewModelScope.launch {
            repository.incrementWearCount(garment.id, garment.wearCount)
                .onSuccess {
                    val newCount = garment.wearCount + 1L
                    val newCpw = garment.purchasePrice / newCount
                    _statusBannerMessage.value = "'${garment.name}' 착용 +1회 기록! (현재 CPW: ${String.format("%,d", newCpw)}원)"
                }
        }
    }

    fun deleteGarment(garmentId: String) {
        if (garmentId.isBlank()) return
        viewModelScope.launch {
            repository.deleteGarment(garmentId)
                .onSuccess {
                    _statusBannerMessage.value = "선택한 의류가 옷장에서 삭제되었습니다."
                }
        }
    }

    fun addQuickManualGarment(garment: Garment) {
        viewModelScope.launch {
            repository.addGarment(garment)
                .onSuccess {
                    _statusBannerMessage.value = "⚡ '${garment.name}' 아이템이 5초 퀵 등록으로 옷장에 추가되었습니다!"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "의류 등록에 실패했습니다."
                }
        }
    }

    fun saveVirtualOutfit(
        title: String,
        tpoOccasion: String,
        outer: Garment?,
        top: Garment?,
        bottom: Garment?,
        shoes: Garment?,
        aiStylingNote: String,
        fitScore: Int,
        incrementWearCounts: Boolean = true
    ) {
        viewModelScope.launch {
            val selectedList = listOfNotNull(outer, top, bottom, shoes)
            val ids = selectedList.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }
            if (ids.isEmpty()) {
                _statusBannerMessage.value = "최소 1개 이상의 의류를 선택해 주세요."
                return@launch
            }
            val weather = _weatherState.value.summaryText
            val outfit = SavedOutfit(
                title = title,
                tpoOccasion = tpoOccasion,
                weatherSummary = weather,
                garmentIds = ids,
                outerGarmentId = outer?.id,
                topGarmentId = top?.id,
                bottomGarmentId = bottom?.id,
                shoesGarmentId = shoes?.id,
                aiStylingNote = aiStylingNote,
                fitScore = fitScore.toLong()
            )
            repository.saveOutfit(outfit)
                .onSuccess {
                    if (incrementWearCounts) {
                        selectedList.forEach { g ->
                            if (g.id.isNotBlank()) {
                                repository.incrementWearCount(g.id, g.wearCount)
                            }
                        }
                    }
                    _statusBannerMessage.value = "'$title' 코디가 저장되고 착용 횟수가 반영되었습니다!"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "코디 저장에 실패했습니다."
                }
        }
    }

    fun deleteSavedOutfit(outfitId: String) {
        if (outfitId.isBlank()) return
        viewModelScope.launch {
            repository.deleteOutfit(outfitId)
        }
    }

    /**
     * 2.1 [자동 등록] 전자 영수증 / 쇼핑몰 주문 내역 AI 파싱 및 옷장 자동 등록
     */
    fun parseReceiptAndRegister(receiptText: String) {
        if (receiptText.isBlank()) return
        viewModelScope.launch {
            _aiRegistrationState.value = AiTaskState.Loading("AI가 영수증 내역에서 브랜드·가격·소재·TPO 태그를 추출 중입니다...")
            try {
                val prompt = """
                    다음 쇼핑몰 결제 영수증 또는 이메일 주문 내역을 분석하여 각 의류 아이템의 메타데이터를 추출하세요.
                    - 색상은 실제 의류 색상에 맞는 자연스러운 한국어 색상명(colorName)과 헥스 코드(colorHex, 예: #D2B48C, #1F2A44, #F5F5F0)로 지정하세요.
                    - 카테고리(category)는 OUTER, TOP, BOTTOM, SHOES, BAG 중 하나로 분류하세요.
                    - TPO 태그(tpoTags)는 출근, 데이트, 하객룩, 운동, 주말 나들이, 발표 중 어울리는 2~3개를 배열로 넣으세요.
                    
                    [영수증 원문]
                    $receiptText
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    receiptModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("AI 응답이 비어 있습니다.")
                val parsedGarments = parseGarmentsFromJson(jsonString, sourceType = "RECEIPT_AUTO")
                if (parsedGarments.isEmpty()) {
                    _aiRegistrationState.value = AiTaskState.Error("영수증에서 의류 항목을 찾지 못했습니다.")
                    return@launch
                }

                val savedList = mutableListOf<Garment>()
                for (g in parsedGarments) {
                    val result = repository.addGarment(g)
                    val id = result.getOrNull() ?: ""
                    savedList.add(g.copy(id = id))
                }
                _aiRegistrationState.value = AiTaskState.Success(savedList)
                _statusBannerMessage.value = "영수증에서 총 ${savedList.size}벌의 의류가 옷장에 자동 등록되었습니다!"
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt AI parsing timed out", e)
                _aiRegistrationState.value = AiTaskState.Error("AI 영수증 분석 시간이 초과되었습니다 (60초). 다시 시도해 주세요.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt AI parsing failed", e)
                _aiRegistrationState.value = AiTaskState.Error(e.localizedMessage ?: "AI 영수증 파싱 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 2.1 [자동 등록] 카메라/갤러리 의류 사진 AI 배경 제거(누끼) 전처리 + 비전 AI 자동 태깅
     */
    fun processGarmentPhotoAndRegister(
        originalBitmap: Bitmap,
        enteredPrice: Long,
        enteredBrand: String
    ) {
        viewModelScope.launch {
            _aiRegistrationState.value = AiTaskState.Loading("AI 배경 제거(누끼) 및 비전 메타데이터(색상·패턴·계절) 추출 중...")
            try {
                // Step 1: Perform client-side background segmentation (누끼) via BitmapUtils
                val cutoutBitmap = withContext(Dispatchers.Default) {
                    BitmapUtils.removeBackgroundFromGarmentBitmap(originalBitmap)
                }
                val scaledForInference = withContext(Dispatchers.Default) {
                    BitmapUtils.scaleBitmapDown(originalBitmap, maxDimension = 768)
                }

                // Step 2: Multimodal Gemini 3.8 Flash analysis
                val prompt = """
                    이 의류(또는 영수증/착장) 이미지를 분석하여 옷장에 등록할 의류 메타데이터를 추출하세요.
                    사용자가 입력한 브랜드 참고값: '${enteredBrand.ifBlank { "자동 감지" }}', 구매가 참고값: ${enteredPrice}원.
                    가장 중심이 되는 의류 아이템(최대 2개)의 이름, 카테고리(OUTER/TOP/BOTTOM/SHOES/BAG), 상세 분류, 색상명, 정확한 HEX 색상코드(#RRGGBB), 계절감, 패턴, 두께감, 방수 여부, TPO 태그를 JSON 스키마에 맞춰 반환하세요.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    receiptModel.generateContent(
                        content {
                            image(scaledForInference)
                            text(prompt)
                        }
                    )
                }

                val jsonString = response.text ?: throw IllegalStateException("AI 비전 응답이 비어 있습니다.")
                val parsedGarments = parseGarmentsFromJson(
                    jsonString = jsonString,
                    sourceType = "CAMERA_AI",
                    overridePrice = enteredPrice.takeIf { it > 0L },
                    overrideBrand = enteredBrand.takeIf { it.isNotBlank() }
                )

                if (parsedGarments.isEmpty()) {
                    _aiRegistrationState.value = AiTaskState.Error("이미지에서 의류 정보를 인식하지 못했습니다.")
                    return@launch
                }

                val savedList = mutableListOf<Garment>()
                for (g in parsedGarments) {
                    val res = repository.addGarment(g)
                    val newId = res.getOrNull() ?: ""
                    if (newId.isNotBlank()) {
                        _processedCutoutBitmaps.value = _processedCutoutBitmaps.value + (newId to cutoutBitmap)
                    }
                    savedList.add(g.copy(id = newId))
                }

                _aiRegistrationState.value = AiTaskState.Success(savedList)
                _statusBannerMessage.value = "배경 제거(누끼) 및 AI 태깅 완료! ${savedList.size}벌이 옷장에 추가되었습니다."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Vision AI tagging timed out", e)
                _aiRegistrationState.value = AiTaskState.Error("AI 이미지 분석 시간이 초과되었습니다 (60초).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision AI tagging failed", e)
                _aiRegistrationState.value = AiTaskState.Error(e.localizedMessage ?: "AI 이미지 분석에 실패했습니다.")
            }
        }
    }

    /**
     * 2.3 [스마트 추천] 실시간 날씨 + 개인 체형 + TPO 일정 기반 AI 코디 추천
     */
    fun generateTpoCoordiRecommendation(
        tpoKeyword: String,
        calendarNote: String,
        garments: List<Garment>,
        profile: UserProfile
    ) {
        if (garments.isEmpty()) {
            _aiCoordiState.value = AiTaskState.Error("옷장에 등록된 의류가 없습니다. 먼저 옷장에 의류를 등록하거나 샘플 옷장을 불러와 주세요.")
            return
        }
        viewModelScope.launch {
            _aiCoordiState.value = AiTaskState.Loading("오늘의 날씨(${_weatherState.value.summaryText})와 ${BodyShapeType.fromCode(profile.bodyShape).labelKo} 데이터를 결합해 최적의 코디를 조합 중입니다...")
            try {
                val weather = _weatherState.value
                val bodyShape = BodyShapeType.fromCode(profile.bodyShape)
                val weatherMatched = WeatherWardrobeMatcher.evaluateWardrobeForWeather(garments, weather)
                val catalogText = weatherMatched.allRankedGarments.joinToString("\n") { m ->
                    val g = m.garment
                    "- ID: ${g.id} | 날씨적합도: ${m.suitabilityScore}% (${m.matchReasons.joinToString(", ")}) | 카테고리: ${g.category} (${g.subCategory}) | 이름: ${g.name} | 색상: ${g.colorName}(${g.colorHex}) | 두께: ${g.thickness} | 계절: ${g.season} | 방수: ${g.waterResistant} | TPO: ${g.tpoTags.joinToString(",")} | 착용횟수: ${g.wearCount}회 (CPW ${g.costPerWear}원)"
                }

                val prompt = """
                    당신은 개인 맞춤형 AI 수석 스타일리스트입니다.
                    아래 사용자의 실시간 기상 조건, 체형 데이터, 오늘 일정(TPO), 그리고 보유 옷장 목록만 사용하여 최적의 착장 조합을 추천하세요.
                    
                    [실시간 날씨]
                    도시: ${weather.cityLabelKo}, 현재 기온: ${weather.temperatureC}°C (체감 ${weather.apparentTempC}°C), 날씨: ${weather.conditionLabelKo}, 강수확률: ${weather.rainProbabilityPercent}%, 습도: ${weather.humidityPercent}%
                    날씨 1차 필터 가이드: 권장 두께=${weather.recommendedThickness.joinToString("/")}, 우천 대비 필요=${weather.isRainyOrSnowy}
                    
                    [사용자 체형 정보]
                    키: ${profile.heightCm}cm, 몸무게: ${profile.weightKg}kg, 어깨: ${profile.shoulderWidthCm}cm, 허리: ${profile.waistCm}cm
                    체형 분류: ${bodyShape.labelKo} (${bodyShape.descriptionKo})
                    체형 스타일링 팁: ${bodyShape.stylingTip}
                    
                    [오늘의 TPO 및 일정]
                    핵심 TPO: $tpoKeyword
                    세부 일정 메모: ${calendarNote.ifBlank { "특이사항 없음" }}
                    
                    [보유 옷장 목록 (반드시 아래 ID 중에서만 선택)]
                    $catalogText
                    
                    주의사항:
                    1. outerId, topId, bottomId, shoesId에는 반드시 위 목록에 존재하는 실제 ID를 넣으세요. (날씨가 더워 아우터가 불필요하면 outerId는 빈 문자열 ""로 반환)
                    2. 가성비(CPW)를 높이기 위해 너무 방치된 옷이나 해당 TPO에 찰떡인 아이템을 적극 활용하세요.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    tpoStylingModel.generateContent(prompt)
                }

                val jsonString = response.text ?: throw IllegalStateException("AI 코디 응답이 비어 있습니다.")
                val json = JSONObject(jsonString)

                val outerId = json.optString("outerId", "")
                val topId = json.optString("topId", "")
                val bottomId = json.optString("bottomId", "")
                val shoesId = json.optString("shoesId", "")

                val outer = garments.firstOrNull { it.id == outerId }
                val top = garments.firstOrNull { it.id == topId }
                    ?: garments.firstOrNull { it.category == "TOP" }
                val bottom = garments.firstOrNull { it.id == bottomId }
                    ?: garments.firstOrNull { it.category == "BOTTOM" }
                val shoes = garments.firstOrNull { it.id == shoesId }
                    ?: garments.firstOrNull { it.category == "SHOES" }

                val rec = AiRecommendedCoordi(
                    title = json.optString("title", "$tpoKeyword 맞춤 Ateli 코디"),
                    tpoOccasion = tpoKeyword,
                    outerGarment = outer,
                    topGarment = top,
                    bottomGarment = bottom,
                    shoesGarment = shoes,
                    aiStylingNote = json.optString("aiStylingNote", bodyShape.stylingTip),
                    fitScore = json.optInt("fitScore", 94).coerceIn(70, 99),
                    weatherMatchReason = json.optString("weatherMatchReason", weather.stylingAdviceKo)
                )

                _aiCoordiState.value = AiTaskState.Success(rec)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "TPO AI recommendation timed out", e)
                _aiCoordiState.value = AiTaskState.Error("AI 코디 추천 시간이 초과되었습니다 (60초). 다시 시도해 주세요.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "TPO AI recommendation failed", e)
                _aiCoordiState.value = AiTaskState.Error(e.localizedMessage ?: "AI 코디 추천 생성에 실패했습니다.")
            }
        }
    }

    /**
     * Multi-Turn AI Personal Stylist Chat (supports gemini-3.8-flash and gemini-4-argon)
     */
    fun selectChatModel(modelName: String) {
        if (modelName == "gemini-3.8-flash" || modelName == "gemini-4-argon") {
            _selectedChatModel.value = modelName
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(defaultGreetingMessage)
    }

    fun sendChatMessage(
        userMessage: String,
        profile: UserProfile,
        garments: List<Garment>,
        weather: WeatherContext
    ) {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty() || _isChatStreaming.value) return

        val activeModelName = _selectedChatModel.value
        val modelLabel = if (activeModelName == "gemini-4-argon") {
            "Gemini 4 Argon · 심층 스타일 추론"
        } else {
            "Gemini 3.8 Flash · 빠른 코디 상담"
        }

        val previousHistorySnapshot = _chatMessages.value.drop(1) // exclude initial static greeting
        val userMsgId = "user_${System.currentTimeMillis()}"
        val botMsgId = "bot_${System.currentTimeMillis() + 1}"

        _chatMessages.value = _chatMessages.value +
            ChatMessage(id = userMsgId, text = trimmed, isUser = true) +
            ChatMessage(id = botMsgId, text = "스타일링 분석 중...", isUser = false, modelBadge = modelLabel)

        viewModelScope.launch {
            _isChatStreaming.value = true
            try {
                val bodyShape = BodyShapeType.fromCode(profile.bodyShape)
                val wardrobeSummary = if (garments.isEmpty()) {
                    "현재 등록된 의류 없음"
                } else {
                    garments.joinToString("\n") { g ->
                        "- [${g.category}/${g.subCategory}] ${g.brand} ${g.name} (${g.colorName}, 구매가 ${g.purchasePrice}원, 착용 ${g.wearCount}회, CPW ${g.costPerWear}원, TPO: ${g.tpoTags.joinToString(",")})"
                    }
                }

                val systemPrompt = """
                    당신은 'Ateli (아틀리에)'의 수석 AI 퍼스널 스타일리스트입니다.
                    항상 친절하고 전문적인 한국어로 답변하며, 사용자의 실제 옷장 아이템, 체형 특성, 실시간 날씨, 그리고 CPW(1회 착용당 비용) 데이터를 근거로 구체적인 스타일링 조언을 제공하세요.
                    
                    [사용자 프로필 및 체형]
                    - 이름: ${profile.displayName}, 키: ${profile.heightCm}cm, 몸무게: ${profile.weightKg}kg, 어깨: ${profile.shoulderWidthCm}cm, 허리: ${profile.waistCm}cm
                    - 체형 분류: ${bodyShape.labelKo} (${bodyShape.descriptionKo})
                    - 핵심 핏 가이드: ${bodyShape.stylingTip}
                    
                    [실시간 날씨]
                    - ${weather.summaryText} (권장 두께: ${weather.recommendedThickness.joinToString("/")})
                    
                    [사용자 보유 옷장 및 가성비 현황]
                    $wardrobeSummary
                """.trimIndent()

                val chatModel = Firebase.ai.generativeModel(
                    modelName = activeModelName,
                    systemInstruction = content { text(systemPrompt) }
                )

                val chatHistory = previousHistorySnapshot
                    .filter { it.text.isNotBlank() }
                    .map { msg ->
                        content(role = if (msg.isUser) "user" else "model") {
                            text(msg.text)
                        }
                    }

                val chat = chatModel.startChat(history = chatHistory)
                val accumulated = StringBuilder()

                withTimeout(60_000L) {
                    chat.sendMessageStream(trimmed).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulated.append(chunkText)
                            val updatedText = accumulated.toString()
                            _chatMessages.value = _chatMessages.value.map { m ->
                                if (m.id == botMsgId) m.copy(text = updatedText) else m
                            }
                        }
                    }
                }

                if (accumulated.isEmpty()) {
                    _chatMessages.value = _chatMessages.value.map { m ->
                        if (m.id == botMsgId) {
                            m.copy(text = "체형(${bodyShape.labelKo})과 오늘 날씨(${weather.temperatureC.toInt()}°C)를 고려했을 때 상·하의 명도 대비를 주면 더욱 세련된 실루엣을 연출할 수 있습니다.")
                        } else m
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Stylist chat timed out", e)
                _chatMessages.value = _chatMessages.value.map { m ->
                    if (m.id == botMsgId) m.copy(text = "응답 시간이 초과되었습니다 (60초). 다시 질문해 주세요.") else m
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Stylist chat failed", e)
                _chatMessages.value = _chatMessages.value.map { m ->
                    if (m.id == botMsgId) {
                        m.copy(text = "AI 스타일리스트 연결 중 일시적인 오류가 발생했습니다: ${e.localizedMessage ?: "다시 시도해 주세요."}")
                    } else m
                }
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    private fun parseGarmentsFromJson(
        jsonString: String,
        sourceType: String,
        overridePrice: Long? = null,
        overrideBrand: String? = null
    ): List<Garment> {
        val root = JSONObject(jsonString)
        val itemsArray = root.optJSONArray("items") ?: JSONArray()
        val result = mutableListOf<Garment>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            val tagsJson = obj.optJSONArray("tpoTags")
            val tags = mutableListOf<String>()
            if (tagsJson != null) {
                for (t in 0 until tagsJson.length()) {
                    tags.add(tagsJson.getString(t))
                }
            }
            val rawHex = obj.optString("colorHex", "#2C3E50").trim()
            val validHex = if (rawHex.startsWith("#") && (rawHex.length == 7 || rawHex.length == 4)) {
                rawHex
            } else {
                "#2C3E50"
            }

            result.add(
                Garment(
                    name = obj.optString("name", "스타일 아이템"),
                    brand = overrideBrand ?: obj.optString("brand", "브랜드 미상"),
                    category = obj.optString("category", "TOP"),
                    subCategory = obj.optString("subCategory", "기본 아이템"),
                    colorName = obj.optString("colorName", "네이비"),
                    colorHex = validHex,
                    season = obj.optString("season", "ALL"),
                    pattern = obj.optString("pattern", "솔리드"),
                    thickness = obj.optString("thickness", "MEDIUM"),
                    waterResistant = obj.optBoolean("waterResistant", false),
                    tpoTags = tags.ifEmpty { listOf("데일리", "출근") },
                    purchasePrice = overridePrice ?: obj.optLong("purchasePrice", 59000L).coerceAtLeast(0L),
                    wearCount = 0L,
                    sourceType = sourceType,
                    receiptStore = obj.optString("receiptStore", "AI 자동등록")
                )
            )
        }
        return result
    }
}
