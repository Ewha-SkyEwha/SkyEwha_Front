# Trendie (Android)

초보 여행 숏폼 유튜버를 위한 **트렌드 반영 맞춤형 가이드 어플리케이션** (Android / Kotlin)

- 백엔드 API와 통신하여 트렌드 분석 대시보드/피드백 리포트 등을 제공하는 안드로이드 클라이언트입니다.
- 기본 API Base URL: `https://skyewha-trendie.kr/` (`ApiConfig.BASE_URL`)

---

## 주요 기능

### 로그인
- **Google OAuth 로그인**
  - 서버에서 로그인 URL을 받아 브라우저로 로그인 페이지를 열고, 딥링크로 앱에 복귀합니다.
  - 딥링크 콜백: `trendie://oauth/callback`
- **Kakao 로그인**
  - Kakao SDK 로그인 연동

### 트렌드 대시보드
- 트렌드 해시태그 Top10 **BarChart** 표시
  - 차트 막대 선택 시 해당 키워드로 YouTube 검색을 엽니다.
- 주간 급상승 해시태그 Top10 표시
- 인기 영상 목록 표시 (클릭 시 영상 URL로 이동)

### 키워드 기반 영상 검색
- 키워드로 관련 영상 목록 조회
- 영상 북마크 추가/해제

### 사용자 영상 피드백 보고서
- 동영상 업로드 기반 분석 요청
- 텍스트 입력 기반 분석 요청
- 피드백 리포트 화면에서 추천 해시태그/제목/연관 영상 등 결과 표시
- 피드백 리포트 화면을 PDF로 저장(다운로드 폴더)

### 피드백 보고서 내역
- 서버에서 내 피드백 목록을 조회하여 리스트로 표시합니다.

### 북마크
- 북마크한 영상 목록 조회

### 마이페이지 / 설정
- 닉네임 수정
- 프로필 이미지 선택 및 크롭(uCrop)
- 설정 화면에서 로그아웃
  - 서버 로그아웃 API를 호출한 뒤(성공/실패 무관), 로컬 저장소를 정리하고 로그인 화면으로 이동합니다.

---

## 기술 스택

- Kotlin, Android (Activity/Fragment 기반)
- XML Layout + ViewBinding
- Retrofit2 / OkHttp3 (Interceptor/Authenticator 구성 포함)
- Moshi (JSON)
- Kotlin Coroutines / Flow
- Kakao SDK
- Coil / Glide (이미지 로딩)
- MPAndroidChart (차트)
- AndroidX Security Crypto (EncryptedSharedPreferences / MasterKey)
- AndroidX DataStore (Preferences)
- AndroidX Room
- uCrop (이미지 크롭)
- Material Components (MaterialToolbar, BottomSheet 등)
- (참고) `ui/theme`에 Compose(Material3) 기반 테마 코드가 포함되어 있습니다.

---

## 프로젝트 구조 (요약)

- `com/h/trendie/`
  - `ApiConfig.kt` : API Base URL 및 SharedPreferences 키 등 상수
  - `LoginActivity.kt` : Google/Kakao 로그인 및 딥링크 콜백 처리
  - `HomeFragment.kt` : 트렌드/인기영상 UI 및 차트 클릭 시 YouTube 이동

- `com/h/trendie/network/`
  - `ApiClient.kt` : Retrofit/OkHttp 설정 및 JWT 적용
  - `Interceptor.kt` : Authorization 헤더 처리 등

- `com/h/trendie/data/`
  - `ApiService.kt` : 백엔드 API 인터페이스 정의
  - `UserPrefs.kt` : DataStore 기반 닉네임 저장/조회

- `com/h/trendie/data/auth/`
  - `TokenProvider.kt` : EncryptedSharedPreferences 기반 토큰 저장/조회
  - `TokenRefereshAuthenticator.kt` : 인증 갱신 처리 로직

- `AndroidManifest.xml`
  - OAuth 딥링크 인텐트필터: `trendie://oauth/callback`
  - Kakao `AuthCodeHandlerActivity` 등록
  - `FileProvider` 등록

---

## 설치 및 실행 방법

### 1) 레포 클론
```bash
git clone https://github.com/Ewha-SkyEwha/SkyEwha_Front/
```

### 2) Android Studio에서 프로젝트 열기
- Android Studio 실행 → **Open** → 클론한 폴더 선택
- Gradle Sync가 자동으로 시작되며, 필요 시 상단 배너의 **Sync Now**를 눌러 동기화합니다.

### 3) 필수 설정 (Kakao AppKey)
- `AndroidManifest.xml`에서 `com.kakao.sdk.AppKey`가 `@string/kakao_native_app_key`를 참조합니다.
- 따라서 빌드/실행을 위해 `kakao_native_app_key` string 리소스가 프로젝트에 포함되어 있어야 합니다.

### 4) 실행
- AVD(에뮬레이터) 또는 실기기 선택
- **Run** 버튼으로 설치 및 실행

> 참고: 로그인/트렌드/분석 기능은 백엔드 서버(`https://skyewha-trendie.kr/`) 연결이 필요합니다.

---

## Required configuration

- `AndroidManifest.xml`에서 `com.kakao.sdk.AppKey`가 `@string/kakao_native_app_key`를 참조합니다.  
  따라서 빌드/실행을 위해 해당 string 리소스가 프로젝트에 포함되어 있어야 합니다.

---

## How to test (Manual)

백엔드 서버(`https://skyewha-trendie.kr/`)가 실행 중이라는 가정 하에, 다음 순서로 기본 기능을 수동 테스트할 수 있습니다.

1. **로그인**
   - 앱 실행 → Google 또는 Kakao 로그인
   - 로그인 성공 후 메인 화면(`MainActivity`)으로 이동하는지 확인

2. **홈 화면 (트렌드)**
   - `HomeFragment`에서 트렌드 해시태그 차트/주간 급상승 해시태그/인기 영상 목록이 로드되는지 확인
   - 차트 막대 선택 시 YouTube 검색으로 이동하는지 확인
   - 인기 영상 클릭 시 영상 URL로 이동하는지 확인

3. **키워드 영상 검색**
   - 키워드 검색 후 관련 영상 목록이 표시되는지 확인
   - 관심 영상 북마크 추가/해제 동작 확인

4. **사용자 영상 피드백 보고서**
   - 동영상 업로드 또는 텍스트 입력으로 분석 요청
   - 피드백 리포트 화면에서 추천 해시태그/제목/연관 영상이 표시되는지 확인
   - “PDF 저장” 버튼으로 다운로드 폴더에 PDF가 저장되는지 확인

5. **피드백 보고서 내역**
   - 피드백 생성 후 피드백 보고서 내역 화면에서 서버 목록이 조회되는지 확인

6. **설정 / 로그아웃**
   - 설정 화면에서 로그아웃 클릭 시 로그인 화면으로 이동하는지 확인

---

## Data / sample data

- 트렌드 해시태그, 영상 정보, 분석 결과 등 실제 데이터는 모두 백엔드 서버 DB에서 조회합니다.
- 이 레포에는 별도의 CSV/SQL 샘플 데이터 파일을 포함하지 않습니다.
- 데이터 스키마/샘플 데이터는 백엔드 레포 및 API 문서를 참고합니다.

---

## 관련 레포 / 문서

- Backend: https://github.com/Ewha-SkyEwha/SkyEwha_Backend
- API 문서: https://skyewha-trendie.kr/docs

---

## Open Source / Third-party Libraries

본 프로젝트는 아래의 오픈소스/서드파티 라이브러리를 사용합니다.  
(라이선스 및 고지 의무는 각 라이브러리의 LICENSE 및 배포물의 고지 내용을 따릅니다.)

- Retrofit2
- OkHttp3 (+ HttpLoggingInterceptor) / Okio
- Moshi (+ Converter)
- Kotlin Coroutines / Flow
- Kakao SDK (Android)
- Coil
- Glide
- MPAndroidChart
- AndroidX Security Crypto (EncryptedSharedPreferences / MasterKey)
- AndroidX DataStore (Preferences)
- AndroidX Room
- uCrop
- Material Components for Android
- AndroidX / Jetpack libraries
