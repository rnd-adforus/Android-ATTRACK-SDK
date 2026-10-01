# ATTRACK Android SDK

Android 앱을 위한 설치 귀속, 앱 내 이벤트 측정, 딥링크 SDK입니다.

- Google Play 설치를 귀속합니다(Install Referrer, 자동 전송)
- 앱에 등록된 이벤트 스키마로 검사한 앱 내 이벤트를 기록합니다
- 링크로 앱을 엽니다. 설치 직후 첫 실행(지연 딥링크)도 지원합니다
- 안정적으로 전송합니다. 이벤트는 기기에 저장되어 오프라인, 재시작, 프로세스
  종료 후에도 재시도됩니다

이 저장소에는 **문서**와 **샘플 앱**이 있습니다. SDK 자체는 라이브러리
(`com.adforus.sdk:attrack`)로 배포됩니다.

[English README](README.en.md)

## 문서

| | 한국어 | English |
|---|---|---|
| 시작하기: 설치, 이벤트, 결과, API 레퍼런스 | [시작하기](docs/INTEGRATION_GUIDE.md) | [Get started](docs/INTEGRATION_GUIDE.en.md) |
| 딥링크와 지연 딥링크 | [딥링크](docs/DEEP_LINKS.md) | [Deep links](docs/DEEP_LINKS.en.md) |

## 지원 환경

| | |
|---|---|
| `minSdk` | 21 (Android 5.0) |
| `compileSdk` | 35 이상 (36 권장) |
| Android Gradle Plugin | 8.6.0 이상 (compileSdk 36은 8.9.0 이상) |
| JDK | 17 |
| 언어 | Kotlin, Java |

전체 요구 사항은 [시작하기](docs/INTEGRATION_GUIDE.md#시작하기-전에)를 참고하세요.

## 빠른 시작

**1. Maven 저장소와 의존성 추가**

ATTRACK SDK는 아래 Nexus Maven 저장소에서 Gradle이 자동으로 다운로드합니다.
먼저 프로젝트의 `settings.gradle.kts`에 저장소를 추가하세요. 기존 저장소 설정이
있다면 그 `repositories` 블록에 `maven` 항목만 추가합니다.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://nexus.adforus.com/repository/attrack/") }
    }
}
```

앱 모듈의 `app/build.gradle.kts`에 의존성을 추가하세요.

```kotlin
dependencies {
    implementation("com.adforus.sdk:attrack:1.0.1")
}
```

본인 앱에서 Android Studio의 Gradle Sync를 실행하거나 아래 명령으로 빌드합니다.
Gradle이 SDK와 필요한 의존성(Install Referrer, 광고 ID, WorkManager)을 다운로드하므로 파일을 따로 받을 필요가 없습니다.
다운로드에는 Nexus 로그인이나 SDK client key가 필요하지 않습니다.

```bash
./gradlew :app:assembleDebug
```

**2. 내 앱의 App ID와 현재 client key 준비**

키를 개인 빌드 설정 또는 앱의 런타임 설정에서 가져오세요. 아래 예시는 앱의
`BuildConfig.ATTRACK_APP_ID`, `BuildConfig.ATTRACK_CLIENT_KEY` 필드를 사용합니다.
[시작하기](docs/INTEGRATION_GUIDE.md#3-sdk-초기화)에 필드 설정 방법이 있습니다.

**3. `Application`에서 초기화**

```kotlin
Tracker.setResultListener { result -> Log.d("ATTRACK", "${result.code}") }
Tracker.initializeWithResult(this, BuildConfig.ATTRACK_APP_ID, BuildConfig.ATTRACK_CLIENT_KEY)
```

**4. 이벤트 기록**

```kotlin
Tracker.logEventWithResult("purchase", mapOf("sku" to "pro_monthly", "revenue" to 9.99, "currency" to "KRW"))
```

설치는 자동으로 측정됩니다. 이어서 [시작하기](docs/INTEGRATION_GUIDE.md)를 참고하세요.

## 샘플 앱

실행해 보고 그대로 가져다 쓸 수 있는 전체 연동 예시입니다.

| 지연 링크 (설치 후 첫 실행) | 직접 링크 (앱 설치됨) |
|---|---|
| <img src="docs/images/deferred-link.png" width="260" alt="지연 링크로 열리고 확인된 Offer 화면"> | <img src="docs/images/direct-link.png" width="260" alt="직접 링크로 열린 Product 화면"> |

### 실행 방법

1. 본인 앱의 `applicationId`(설치 패키지)를 사용하세요. 이 샘플은 본인 앱에
   맞게 바꿔 사용하는 템플릿입니다. 해당 패키지의 App ID와 client key를 사용하고,
   설치할 빌드의 서명 인증서가 등록되어 있어야 합니다(`./gradlew signingReport`).
   Play 설치는 업로드 키가 아닌 Play 앱 서명 인증서를 등록하세요.
   공용 테스트 앱, 공용 키, 서버의 샘플용 앱 등록은 제공하지 않습니다.
   다음 이벤트를 본인 앱에 등록하세요.

   | 이벤트 | 집계 | 파라미터 |
   |---|---|---|
   | `sign_up` | 설치당 1회 | `method: string` |
   | `tutorial_complete` | 설치당 1회 | — |
   | `level_complete` | 매번 | `level: number`, `score: number`, `perfect: bool` |
   | `purchase` | 매번 | `sku: string`, `revenue: number`, `currency: string`, `order_id: string` |

2. `local.properties`(git 제외)에 값을 추가합니다.

   ```properties
   attrack.packageName=com.yourcompany.yourapp
   attrack.appId=app_xxxxxxxxxxxxxxxxxxxxxxxx
   attrack.clientKey=your-client-key
   ```

   Gradle `-P` 옵션이나 환경 변수 `ATTRACK_PACKAGE_NAME`, `ATTRACK_APP_ID`,
   `ATTRACK_CLIENT_KEY`로도 지정할 수 있습니다. 패키지 설정이 없으면 빌드가 중단됩니다.

3. 템플릿에는 Nexus Maven 저장소와 `com.adforus.sdk:attrack:1.0.1` 의존성이
   이미 설정되어 있습니다. 저장소 URL은 바꿀 필요가 없습니다. Gradle Sync 또는
   빌드 시 SDK와 의존성이 자동으로 다운로드됩니다.
   `attrack.packageName`은 본인 앱에 등록된 정확한 패키지로 바꾸세요.
   `namespace`와 소스 코드의 `kr.co.attrack.sample`은 템플릿 코드 위치이며,
   설치되는 앱의 패키지는 `attrack.packageName`으로 결정됩니다.
   `AndroidManifest.xml`의 `${applicationId}`는 해당 값으로 자동 치환됩니다.

4. 빌드 및 설치:

   ```bash
   ./gradlew :app:installDebug
   ```

App ID나 client key가 없어도 앱은 실행되며 SDK가 시작되지 않은 이유
(`INVALID_APP_ID` 또는 `INVALID_CONFIGURATION`)를 보여 줍니다.

### 화면 구성

| 화면 | 내용 |
|---|---|
| **Overview** | `Tracker.status()`, 시작 결과, 이벤트 스키마, `Tracker.installAttribution()` |
| **Events** | 이벤트별 버튼과 SDK가 기기에서 거부하는 잘못된 예시 |
| **Links** | 최근 링크 결과, 링크 서비스에 연결할 수 없을 때의 재시도 버튼 |
| **Offer / Product** | 링크로 열린 화면과 도착 경로: 직접/지연, 링크 ID, 파라미터, 확인 여부 |
| **Data** | SDK가 수집하는 기기 식별자 |
| **Log** | 모든 SDK 결과, 최신순(logcat: `adb logcat -s AttrackSample`) |

### 연동 코드 위치

| 파일 | 내용 |
|---|---|
| [`SampleApplication.kt`](app/src/main/java/kr/co/attrack/sample/SampleApplication.kt) | Result listener 설정 후 `Application.onCreate`에서 `initializeWithResult` 1회 호출 |
| [`SampleEvents.kt`](app/src/main/java/kr/co/attrack/sample/SampleEvents.kt) | 이벤트별 `logEventWithResult` |
| [`MainActivity.kt`](app/src/main/java/kr/co/attrack/sample/MainActivity.kt) | `onCreate`/`onNewIntent`의 `handleDeepLink`, 지연 링크 확인, `handoffId`, 다음 foreground 재시도 |
| [`DeepLinkRouter.kt`](app/src/main/java/kr/co/attrack/sample/DeepLinkRouter.kt) | 링크 파라미터의 허용 목록 기반 화면 이동(단위 테스트 포함) |
| [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) | 백업 규칙, 내 applicationId 범위의 링크 필터 |

### 링크 테스트

```bash
APP_PACKAGE="com.yourcompany.yourapp"  # 본인 앱에 등록된 applicationId
LINK_ID="YOUR_LINK_ID"
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/$APP_PACKAGE/$LINK_ID"
```

본인 앱에 생성된 링크 ID를 사용하세요. 지연 링크는 Google Play 설치(예: 내부
테스트 트랙)가 필요하며, 직접 설치한 APK는 `NO_LINK`를 반환합니다.

## 라이선스

이 저장소의 샘플 코드와 문서는 [MIT License](LICENSE)로 공개됩니다. ATTRACK SDK
라이브러리(`com.adforus.sdk:attrack`)는 이 저장소에 포함되지 않으며 별도의
조건으로 배포됩니다.
