# Ateli (아틀리에)

> **AI 기반 스마트 디지털 옷장 & 3D 가상 피팅 아틀리에**  
> AI 영수증/사진 자동 옷장 등록, 5대 체형 맞춤 3D 가상 피팅, 실시간 날씨·TPO 코디 추천 및 CPW(착용당 비용) 가성비 분석 네이티브 안드로이드 앱입니다.

---

## 🌟 주요 기능 (Key Features)

1. **스마트 옷장 (Digital Wardrobe)**
   - 쇼핑몰 구매 영수증(텍스트) 및 의류 사진 자동 인식 및 등록 (Google Gemini 구조화된 출력 파이프라인)
   - 카테고리(아우터, 상의, 하의, 신발, 잡화), 색상(Hex), 계절, 패턴, 방수 여부 자동 분류
   - 의류 자동 누끼(배경 분리) 처리 및 프리셋 라이브러리 제공

2. **3D 가상 피팅 아틀리에 (3D Virtual Fitting Canvas)**
   - 5대 체형(역삼각형, 모래시계, 직사각형, 어반 피어, 타원형) 파라메트릭 신체 치수 맞춤 피팅
   - Three.js WebGL PBR 스튜디오(12개 파츠 마네킹, 실시간 원단 질감 조명) 및 Jetpack Compose Canvas 기반 2D/2.5D 레이어 피팅 지원
   - Room DB 기반 로컬 피팅 히스토리 타임라인 자동 기록

3. **실시간 날씨 & TPO 맞춤 스타일링 (Weather & TPO Engine)**
   - 기상청/Open-Meteo 실시간 기온, 강수, 습도 데이터와 옷장 의류 속성 매칭
   - 출근, 데이트, 하객룩, 캐주얼 등 상황(TPO)별 최적의 코디 큐레이션 및 점수 산출

4. **수석 AI 퍼스널 스타일리스트 (AI Stylist Chat)**
   - 대화형 실시간 스트리밍 AI 패션 어드바이저 (Gemini Flash / Pro)
   - 개인 옷장 보유 아이템 및 체형 특성을 고려한 일대일 패션 컨설팅

5. **패션 가성비 분석 (CPW Analytics & B2B Suite)**
   - CPW(Cost Per Wear: 착용당 비용) 분석, 옷장 수면 아이템(Closet Sleeper) 식별
   - 글로벌 패션 테크 알고리즘(Acloset, Whering, Stylebook, Stitch Fix) 벤치마크 엔진 탑재
   - 패션 바이어/브랜드 관점의 PRO 엔터프라이즈 모드 지원

---

## 🛠 기술 스택 (Tech Stack)

- **Platform & Language**: Android (Min SDK 24, Target SDK 36), Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3 BOM), Three.js WebGL Engine (HTML5 WebView Canvas)
- **Architecture**: MVVM + Clean Architecture + Repository Pattern, Coroutines & StateFlow
- **Backend & Database**: Firebase Authentication, Cloud Firestore (Zero-Trust Security Rules), Room Local DB
- **AI & Vision**: Firebase AI (Google Gemini API - Structured Output & Streaming Chat)
- **Networking & Hardware**: Retrofit2 + Moshi, CameraX, Play Services Location, Coil
- **Testing**: Robolectric, JUnit4, Firestore Rules Unit Testing

---

## 🚀 로컬 실행 가이드 (Getting Started)

### 사전 준비사항
- **Android Studio** 최신 버전 (Ladybug / Meerkat 권장)
- **JDK 17** 이상

### 프로젝트 설정 및 실행
1. Android Studio를 실행하고 **Open**을 눌러 본 프로젝트 폴더(`16. Ateli`)를 선택합니다.
2. Gradle 동기화(Sync Project with Gradle Files)가 완료될 때까지 대기합니다.
3. 프로젝트 루트에 `.env` 파일을 생성하고 Gemini API 키를 입력합니다. (필요 시 `.env.example` 참고)
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
4. 안드로이드 에뮬레이터 또는 실기기를 연결한 후 **Run (`Shift + F10`)**을 눌러 실행합니다.
