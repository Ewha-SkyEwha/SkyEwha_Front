# Trendie (Android)

초보 여행 숏폼 유튜버를 위한 **트렌드 반영 맞춤형 가이드 앱** (Android / Kotlin)

- 백엔드 API와 통신하여 트렌드/키워드 분석/피드백 리포트 등을 제공하는 안드로이드 클라이언트입니다.
- 기본 API Base URL: `https://skyewha-trendie.kr/` (`ApiConfig.BASE_URL`)

## 주요 기능
- 로그인
  - Google OAuth 연동 (딥링크 콜백: `trendie://oauth/callback`)
  - Kakao SDK 로그인 연동
- 트렌드 / 콘텐츠 조회
  - 주간 트렌드 해시태그 조회
  - YouTube 인기 영상 조회
- 분석 / 피드백
  - 영상 업로드 및 키워드 처리
  - 텍스트 키워드 처리
  - 피드백 리포트 조회
  - 내 피드백 목록 조회
- 북마크
  - 영상 북마크 추가/해제
  - 내 북마크 목록 조회
- 마이페이지
  - 닉네임 수정

## 기술 스택
- Kotlin, Android (Activity/Fragment)
- ViewBinding
- Retrofit2 / OkHttp
- Moshi
- Kotlin Coroutines
- Kakao SDK
- Coil
- AndroidX Security (EncryptedSharedPreferences)

## 프로젝트 구조 (요약)
- `main/java/com/h/trendie/`
  - `ApiConfig.kt` : API Base URL
  - `network/` : `ApiClient`, `ApiService`
  - `data/auth/TokenProvider.kt` : 토큰 저장/관리
- `main/AndroidManifest.xml`
  - OAuth 딥링크 인텐트필터 (`trendie://oauth/callback`)
  - Kakao `AuthCodeHandlerActivity` 등록
  - FileProvider 등록

## 실행 방법
1. 레포 클론

    ```bash
    git clone https://github.com/Ewha-SkyEwha/SkyEwha_Front/
    ```

2. Android Studio로 열기 → Gradle Sync
3. 에뮬레이터/실기기에서 실행

## 관련 레포 / 문서
- Backend: https://github.com/Ewha-SkyEwha/SkyEwha_Backend
- API 문서: https://skyewha-trendie.kr/docs

## 주의사항
- 로그인/분석 기능은 백엔드 API 연결이 필요합니다.
- 토큰은 EncryptedSharedPreferences 기반으로 저장됩니다.
