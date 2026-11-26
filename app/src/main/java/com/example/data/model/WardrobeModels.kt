package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BodyShapeType(
    val code: String,
    val labelKo: String,
    val descriptionKo: String,
    val shoulderScale: Float,
    val waistScale: Float,
    val hipScale: Float,
    val stylingTip: String
) {
    INVERTED_TRIANGLE(
        code = "INVERTED_TRIANGLE",
        labelKo = "역삼각형 체형",
        descriptionKo = "어깨가 넓고 하체가 슬림한 탄탄한 실루엣",
        shoulderScale = 1.18f,
        waistScale = 0.88f,
        hipScale = 0.90f,
        stylingTip = "시선을 아래로 분산시키는 와이드 팬츠나 V넥 상의가 잘 어울립니다."
    ),
    HOURGLASS(
        code = "HOURGLASS",
        labelKo = "모래시계형 체형",
        descriptionKo = "어깨와 힙의 균형이 잡히고 허리 라인이 잘록한 실루엣",
        shoulderScale = 1.05f,
        waistScale = 0.78f,
        hipScale = 1.06f,
        stylingTip = "허리선이 강조되는 벨티드 자켓이나 세미 크롭 코디가 가장 빛납니다."
    ),
    RECTANGLE(
        code = "RECTANGLE",
        labelKo = "직사각형 (내추럴) 체형",
        descriptionKo = "전체적으로 슬림하고 모던한 직선형 실루엣",
        shoulderScale = 1.0f,
        waistScale = 0.92f,
        hipScale = 0.96f,
        stylingTip = "레이어드 룩이나 구조적인 아우터로 입체감을 주면 세련된 핏이 완성됩니다."
    ),
    PEAR(
        code = "PEAR",
        labelKo = "삼각형 (어반 피어) 체형",
        descriptionKo = "상체가 슬림하고 하체가 안정감 있는 실루엣",
        shoulderScale = 0.92f,
        waistScale = 0.85f,
        hipScale = 1.12f,
        stylingTip = "밝은 톤 상의나 어깨 패드 자켓으로 상체에 포인트를 주고 롱 슬랙스를 매치해보세요."
    ),
    OVAL(
        code = "OVAL",
        labelKo = "타원형 (소프트 라운드) 체형",
        descriptionKo = "부드러운 곡선미와 볼륨감이 있는 실루엣",
        shoulderScale = 1.04f,
        waistScale = 1.08f,
        hipScale = 1.02f,
        stylingTip = "세로 절개 라인이 들어간 롱 코트나 톤온톤 매치로 길고 슬림한 비율을 연출하세요."
    );

    companion object {
        fun fromCode(code: String): BodyShapeType =
            entries.firstOrNull { it.code == code } ?: RECTANGLE
    }
}

enum class GarmentCategory(val code: String, val labelKo: String, val zIndexOrder: Int) {
    SHOES("SHOES", "신발", 1),
    BOTTOM("BOTTOM", "하의", 2),
    TOP("TOP", "상의", 3),
    OUTER("OUTER", "아우터", 4),
    BAG("BAG", "가방·잡화", 5);

    companion object {
        fun fromCode(code: String): GarmentCategory =
            entries.firstOrNull { it.code == code } ?: TOP
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "패션 디렉터",
    val heightCm: Long = 172L,
    val weightKg: Long = 64L,
    val shoulderWidthCm: Long = 44L,
    val waistCm: Long = 74L,
    val bodyShape: String = BodyShapeType.RECTANGLE.code,
    val skinToneHex: String = "#F3D6C1",
    val personalColorTone: String = "SPRING_WARM_LIGHT",
    val preferredCity: String = "Seoul",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to displayName.take(60).ifBlank { "패션 디렉터" },
        "heightCm" to heightCm.coerceIn(100L, 230L),
        "weightKg" to weightKg.coerceIn(30L, 200L),
        "shoulderWidthCm" to shoulderWidthCm.coerceIn(30L, 70L),
        "waistCm" to waistCm.coerceIn(45L, 150L),
        "bodyShape" to BodyShapeType.fromCode(bodyShape).code,
        "skinToneHex" to skinToneHex.take(10).ifBlank { "#F3D6C1" },
        "personalColorTone" to personalColorTone.take(30).ifBlank { "SPRING_WARM_LIGHT" },
        "preferredCity" to preferredCity.take(50).ifBlank { "Seoul" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.take(60).ifBlank { "패션 디렉터" },
        "heightCm" to heightCm.coerceIn(100L, 230L),
        "weightKg" to weightKg.coerceIn(30L, 200L),
        "shoulderWidthCm" to shoulderWidthCm.coerceIn(30L, 70L),
        "waistCm" to waistCm.coerceIn(45L, 150L),
        "bodyShape" to BodyShapeType.fromCode(bodyShape).code,
        "skinToneHex" to skinToneHex.take(10).ifBlank { "#F3D6C1" },
        "personalColorTone" to personalColorTone.take(30).ifBlank { "SPRING_WARM_LIGHT" },
        "preferredCity" to preferredCity.take(50).ifBlank { "Seoul" },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Garment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "TOP",
    val subCategory: String = "셔츠",
    val colorName: String = "화이트",
    val colorHex: String = "#F5F5F0",
    val season: String = "ALL",
    val pattern: String = "솔리드",
    val thickness: String = "MEDIUM",
    val waterResistant: Boolean = false,
    val tpoTags: List<String> = emptyList(),
    val purchasePrice: Long = 0L,
    val wearCount: Long = 0L,
    val imageUrl: String? = null,
    val sourceType: String = "RECEIPT_AUTO",
    val receiptStore: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * CPW (Cost Per Wear): (Initial Purchase Price) / (Wear Count)
     * If wearCount is 0, CPW equals full purchasePrice.
     */
    val costPerWear: Long
        get() = if (wearCount <= 0L) purchasePrice else purchasePrice / wearCount

    val isUnwornClosetSleeper: Boolean
        get() = wearCount == 0L

    /**
     * Value extraction percentage ("뽕 뽑기 지수"):
     * Target CPW is 5,000 KRW per wear or 15+ wears.
     */
    val valueRecoveryPercent: Int
        get() {
            if (purchasePrice <= 0L) return 100
            if (wearCount <= 0L) return 0
            val targetWears = (purchasePrice / 8000L).coerceAtLeast(5L)
            return ((wearCount.toDouble() / targetWears.toDouble()) * 100.0).toInt().coerceIn(5, 100)
        }

    /** Unique UI key shared by Wardrobe grid cards, selection state, and fitting overlay cards. */
    val stableKey: String
        get() = id.ifBlank { "${name}_${brand}_${category}" }

    fun toCreateMap(uid: String): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.take(100).ifBlank { "데님 셔츠" },
            "brand" to brand.take(80).ifBlank { "스탠다드" },
            "category" to GarmentCategory.fromCode(category).code,
            "subCategory" to subCategory.take(50).ifBlank { "기본 아이템" },
            "colorName" to colorName.take(40).ifBlank { "네이비" },
            "colorHex" to colorHex.take(10).ifBlank { "#2C3E50" },
            "season" to if (season in listOf("SPRING_FALL", "SUMMER", "WINTER", "ALL")) season else "ALL",
            "pattern" to pattern.take(40).ifBlank { "솔리드" },
            "thickness" to if (thickness in listOf("LIGHT", "MEDIUM", "HEAVY")) thickness else "MEDIUM",
            "waterResistant" to waterResistant,
            "tpoTags" to tpoTags.map { it.take(30) }.take(10),
            "purchasePrice" to purchasePrice.coerceIn(0L, 100_000_000L),
            "wearCount" to wearCount.coerceIn(0L, 10_000L),
            "sourceType" to if (sourceType in listOf("RECEIPT_AUTO", "CAMERA_AI", "CURATED_SAMPLE", "PHOTO_CUTOUT")) sourceType else "RECEIPT_AUTO",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!imageUrl.isNullOrBlank() && !imageUrl.startsWith("asset://")) {
            base["imageUrl"] = imageUrl
        }
        if (!receiptStore.isNullOrBlank()) {
            base["receiptStore"] = receiptStore.take(80)
        }
        return base
    }
}

@IgnoreExtraProperties
data class SavedOutfit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val tpoOccasion: String = "출근",
    val weatherSummary: String = "서울 18°C · 맑음",
    val garmentIds: List<String> = emptyList(),
    val outerGarmentId: String? = null,
    val topGarmentId: String? = null,
    val bottomGarmentId: String? = null,
    val shoesGarmentId: String? = null,
    val aiStylingNote: String = "",
    val fitScore: Long = 92L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val safeIds = garmentIds.filter { it.isNotBlank() }.take(10).ifEmpty { listOf("default_item") }
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to title.take(100).ifBlank { "오늘의 Ateli 코디" },
            "tpoOccasion" to tpoOccasion.take(50).ifBlank { "데일리" },
            "weatherSummary" to weatherSummary.take(100).ifBlank { "맑음" },
            "garmentIds" to safeIds,
            "aiStylingNote" to aiStylingNote.take(600).ifBlank { "체형과 날씨를 고려한 밸런스 매칭 코디입니다." },
            "fitScore" to fitScore.coerceIn(0L, 100L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!outerGarmentId.isNullOrBlank()) base["outerGarmentId"] = outerGarmentId.take(128)
        if (!topGarmentId.isNullOrBlank()) base["topGarmentId"] = topGarmentId.take(128)
        if (!bottomGarmentId.isNullOrBlank()) base["bottomGarmentId"] = bottomGarmentId.take(128)
        if (!shoesGarmentId.isNullOrBlank()) base["shoesGarmentId"] = shoesGarmentId.take(128)
        return base
    }
}

data class SampleReceiptTemplate(
    val title: String,
    val storeName: String,
    val rawReceiptText: String
)

/**
 * CuratedWardrobePresets
 *
 * 실물 의류 누끼 스튜디오 컷아웃 에셋(Transparent RGBA WebP) 및 전자영수증 프리셋 관리 객체.
 *
 * Verified Cutout Image References:
 * - Beige Trench Coat: https://pngimg.com/uploads/coat/coat_PNG33.png
 * - Tailored Wool Blazer: https://www.pngall.com/wp-content/uploads/2016/09/Blazer-PNG.png
 * - Sky Blue Oxford Shirt: https://www.pngall.com/wp-content/uploads/24/Dress-Shirt-Light-Blue-Buttoned-Long-Sleeve-PNG.png
 * - Cream Knit Pullover: https://www.pngall.com/wp-content/uploads/24/Sweater-Beige-Ribbed-Knit-Long-Sleeve-PNG.png
 * - Sage Utility Field Jacket: https://pngimg.com/uploads/jacket/jacket_PNG8037.png
 * - Charcoal Tailored Slacks: https://www.pngall.com/wp-content/uploads/2016/09/Trouser-PNG-Image.png
 * - Straight Indigo Denim: https://pngimg.com/uploads/jeans/jeans_PNG5745.png
 * - Espresso Leather Dress Shoes: https://pngimg.com/uploads/men_shoes/men_shoes_PNG7491.png
 * - Heritage Grey Running Sneakers (993): https://pngimg.com/uploads/running_shoes/running_shoes_PNG5792.png
 */
object CuratedWardrobePresets {
    private val PRESET_ASSET_RULES: List<Pair<List<String>, String>> = listOf(
        listOf("트렌치", "맥코트", "코트") to "asset://garments/garment_trench_beige.webp",
        listOf("블레이저", "수트", "울 자켓") to "asset://garments/garment_blazer_charcoal.webp",
        listOf("윈드브레이커", "바람막이", "필드", "유틸리티") to "asset://garments/garment_jacket_sage.webp",
        listOf("자켓", "재킷", "아우터") to "asset://garments/garment_blazer_charcoal.webp",
        listOf("셔츠", "옥스포드", "포플린") to "asset://garments/garment_shirt_skyblue.webp",
        listOf("니트", "스웨터", "풀오버", "캐시미어") to "asset://garments/garment_sweater_cream.webp",
        listOf("데님", "청바지", "진", "셀비지") to "asset://garments/garment_jeans_indigo.webp",
        listOf("슬랙스", "팬츠", "트라우저", "치노") to "asset://garments/garment_slacks_charcoal.webp",
        listOf("로퍼", "더비", "구두") to "asset://garments/garment_loafer_brown.webp",
        listOf("스니커즈", "러닝", "운동화", "993") to "asset://garments/garment_sneaker_grey.webp"
    )

    private val CATEGORY_FALLBACK_ASSETS: Map<String, String> = mapOf(
        "OUTER" to "asset://garments/garment_trench_beige.webp",
        "TOP" to "asset://garments/garment_shirt_skyblue.webp",
        "BOTTOM" to "asset://garments/garment_slacks_charcoal.webp",
        "SHOES" to "asset://garments/garment_sneaker_grey.webp"
    )

    fun resolvePresetImageAsset(
        name: String,
        subCategory: String = "",
        category: String = ""
    ): String? {
        val text = "$name $subCategory".lowercase()
        val matchedAsset = PRESET_ASSET_RULES
            .firstOrNull { (keywords, _) -> keywords.any { text.contains(it) } }
            ?.second
        return matchedAsset ?: CATEGORY_FALLBACK_ASSETS[category]
    }

    val sampleReceiptTemplates = listOf(
        SampleReceiptTemplate(
            title = "무신사 스토어 가을 아우터/하의 결제 영수증",
            storeName = "MUSINSA",
            rawReceiptText = """
                [무신사 스토어 전자영수증]
                주문번호: MSS-20261028-9941
                1. [무신사 스탠다드] 발마칸 오버핏 맥코트 (더스티 베이지 / L) - 139,000원 (생활방수 발수 코팅, 봄/가을 간절기용)
                2. [드로우핏] 원턱 와이드 히든밴딩 슬랙스 (차콜 그레이 / 31) - 68,000원 (사계절 출근/하객룩 정장 팬츠)
            """.trimIndent()
        ),
        SampleReceiptTemplate(
            title = "29CM 주말 데이트 니트 & 스니커즈 영수증",
            storeName = "29CM",
            rawReceiptText = """
                [29CM 주문 확인 이메일]
                결제 완료 내역:
                1. [던스트] 유니섹스 케이블 크루넥 니트 풀오버 (버터 크림) - 89,000원 (도톰한 봄/가을·겨울 데이트룩)
                2. [뉴발란스] 993 메이드 인 USA 스웨이드 러닝화 (헤리티지 그레이) - 259,000원 (사계절 데일리·운동·주말 나들이)
            """.trimIndent()
        ),
        SampleReceiptTemplate(
            title = "W컨셉 오피스 셔츠 & 레더 크로스백 영수증",
            storeName = "W컨셉",
            rawReceiptText = """
                [W컨셉 모바일 결제 영수증]
                1. [프론트로우] 크리스피 포플린 스트라이프 셔츠 (스카이 블루) - 118,000원 (가벼운 봄/여름/가을 출근·미팅룩)
                2. [아카이브앱크] 플링 미니멀 레더 숄더백 (딥 블랙) - 189,000원 (생활방수, 출근·하객룩·데이트)
            """.trimIndent()
        )
    )

    fun starterWardrobeItems(): List<Garment> = listOf(
        Garment(
            id = "starter_01_trench",
            name = "발마칸 오버핏 트렌치 맥코트",
            brand = "무신사 스탠다드",
            category = "OUTER",
            subCategory = "트렌치코트",
            colorName = "더스티 베이지",
            colorHex = "#D2B48C",
            season = "SPRING_FALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = true,
            tpoTags = listOf("출근", "데이트", "하객룩"),
            purchasePrice = 139000L,
            wearCount = 14L,
            imageUrl = "asset://garments/garment_trench_beige.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "MUSINSA"
        ),
        Garment(
            id = "starter_02_blazer",
            name = "테일러드 싱글 울 블레이저",
            brand = "쿠어 (COOR)",
            category = "OUTER",
            subCategory = "블레이저",
            colorName = "미드나잇 차콜",
            colorHex = "#22252A",
            season = "SPRING_FALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = false,
            tpoTags = listOf("출근", "하객룩", "발표"),
            purchasePrice = 248000L,
            wearCount = 2L,
            imageUrl = "asset://garments/garment_blazer_charcoal.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "29CM"
        ),
        Garment(
            id = "starter_03_shirt",
            name = "크리스피 옥스포드 포플린 셔츠",
            brand = "폴로 랄프로렌",
            category = "TOP",
            subCategory = "셔츠",
            colorName = "스카이 블루",
            colorHex = "#9AC3EC",
            season = "ALL",
            pattern = "솔리드",
            thickness = "LIGHT",
            waterResistant = false,
            tpoTags = listOf("출근", "데이트", "하객룩"),
            purchasePrice = 169000L,
            wearCount = 22L,
            imageUrl = "asset://garments/garment_shirt_skyblue.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "SSF샵"
        ),
        Garment(
            id = "starter_04_knit",
            name = "캐시미어 블렌드 립 니트",
            brand = "던스트 (Dunst)",
            category = "TOP",
            subCategory = "니트 풀오버",
            colorName = "버터 크림",
            colorHex = "#EBD4A7",
            season = "SPRING_FALL",
            pattern = "골지 립",
            thickness = "MEDIUM",
            waterResistant = false,
            tpoTags = listOf("데이트", "주말 나들이", "출근"),
            purchasePrice = 98000L,
            wearCount = 9L,
            imageUrl = "asset://garments/garment_sweater_cream.webp",
            sourceType = "CAMERA_AI",
            receiptStore = "직접 촬영 (AI 누끼)"
        ),
        Garment(
            id = "starter_05_jacket",
            name = "에어리 테크 필드 유틸리티 자켓",
            brand = "아크테릭스",
            category = "OUTER",
            subCategory = "유틸리티 자켓",
            colorName = "세이지 그린",
            colorHex = "#6E735E",
            season = "SPRING_FALL",
            pattern = "솔리드",
            thickness = "LIGHT",
            waterResistant = true,
            tpoTags = listOf("운동", "주말 나들이"),
            purchasePrice = 320000L,
            wearCount = 0L,
            imageUrl = "asset://garments/garment_jacket_sage.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "EQL"
        ),
        Garment(
            id = "starter_06_slacks",
            name = "원턱 와이드 테일러드 슬랙스",
            brand = "드로우핏",
            category = "BOTTOM",
            subCategory = "와이드 슬랙스",
            colorName = "차콜 그레이",
            colorHex = "#2E2E30",
            season = "ALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = false,
            tpoTags = listOf("출근", "하객룩", "데이트"),
            purchasePrice = 68000L,
            wearCount = 19L,
            imageUrl = "asset://garments/garment_slacks_charcoal.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "MUSINSA"
        ),
        Garment(
            id = "starter_07_jeans",
            name = "셀비지 스트레이트 인디고 데님",
            brand = "리바이스",
            category = "BOTTOM",
            subCategory = "데님 팬츠",
            colorName = "인디고 블루",
            colorHex = "#35537A",
            season = "ALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = false,
            tpoTags = listOf("주말 나들이", "데이트", "출근"),
            purchasePrice = 129000L,
            wearCount = 11L,
            imageUrl = "asset://garments/garment_jeans_indigo.webp",
            sourceType = "CAMERA_AI",
            receiptStore = "직접 촬영 (AI 누끼)"
        ),
        Garment(
            id = "starter_08_loafer",
            name = "미니멀 번치드 레더 더비슈즈",
            brand = "로맨틱무브",
            category = "SHOES",
            subCategory = "더비 로퍼",
            colorName = "에스프레소 브라운",
            colorHex = "#3D2B1F",
            season = "ALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = false,
            tpoTags = listOf("출근", "하객룩", "데이트"),
            purchasePrice = 158000L,
            wearCount = 16L,
            imageUrl = "asset://garments/garment_loafer_brown.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "29CM"
        ),
        Garment(
            id = "starter_09_sneaker",
            name = "993 메이드 인 USA 스웨이드 러닝화",
            brand = "뉴발란스",
            category = "SHOES",
            subCategory = "스니커즈",
            colorName = "스톤 그레이",
            colorHex = "#9E9E9E",
            season = "ALL",
            pattern = "솔리드",
            thickness = "MEDIUM",
            waterResistant = true,
            tpoTags = listOf("운동", "주말 나들이", "출근"),
            purchasePrice = 219000L,
            wearCount = 0L,
            imageUrl = "asset://garments/garment_sneaker_grey.webp",
            sourceType = "RECEIPT_AUTO",
            receiptStore = "크림 (KREAM)"
        )
    )
}
