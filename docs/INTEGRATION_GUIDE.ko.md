# ATTRACK Android SDK — 시작하기

ATTRACK Android SDK는 앱 설치와 앱 내 이벤트를 측정하고, Google Play 설치를
귀속하며, 링크로 앱을 엽니다(설치 직후 첫 실행 포함). 이 문서는 SDK `0.5.0` 기준입니다.

[English](INTEGRATION_GUIDE.en.md) · [딥링크 상세](DEEP_LINKS.ko.md)

## 시작하기 전에

| 항목 | 요구 사항 |
|---|---|
| `minSdk` | 21 이상 (Android 5.0) |
| `compileSdk` | **35 이상**, 36 권장 (포함된 WorkManager 2.10.5 요구 사항) |
| `targetSdk` | 앱에서 결정 |
| Android Gradle Plugin | **8.6.0 이상** (compileSdk 35), compileSdk 36은 **8.9.0 이상** |
| Gradle | 사용하는 AGP 버전이 요구하는 버전 (예: AGP 8.9 → Gradle 8.11.1 이상) |
| JDK (빌드) | 17 이상 |
| Java / Kotlin | SDK는 Java 8 bytecode입니다. Java 앱과 Kotlin 1.9 이상 앱 모두 사용 가능 (SDK는 Kotlin 2.0.21로 빌드) |
| 함께 설치되는 의존성 | Google Play Install Referrer 2.2, AndroidX WorkManager 2.10.5 |
| Google 광고 ID 라이브러리 | `play-services-ads-identifier` 18.3.0 (`minSdk` 23 이상), `minSdk` 21~22 앱은 18.2.0 |
| 네트워크 | HTTPS |

앱별로 다음 값이 필요합니다.

- **App ID** (`app_` + 16진수 24자)
- **Client key**
- 앱에 등록된 **이벤트 이름**과 파라미터
- 링크를 사용할 경우 앱에 생성된 **링크 ID**

## 1. SDK 추가

앱 모듈의 `build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("kr.co.attrack:attrack-tracker:0.5.0")

    // SDK가 Google 광고 ID를 읽기 위해 필요합니다. 유료 귀속에 필수입니다.
    // minSdk 21~22 앱은 18.2.0을 사용하세요(18.3.0은 minSdk 23 이상).
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}
```

SDK를 AAR 파일로 받은 경우 `attrack-tracker-0.5.0.aar`를 `app/libs`에 넣고,
AAR에는 의존성이 포함되지 않으므로 직접 추가합니다.

```kotlin
dependencies {
    implementation(files("libs/attrack-tracker-0.5.0.aar"))
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("androidx.work:work-runtime:2.10.5")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}
```

## 2. Manifest 설정

Client key를 `<meta-data>`로 추가하고, 설치의 추적 상태가 다른 설치로 복원되지
않도록 SDK 백업 규칙을 지정합니다.

```xml
<application
    android:dataExtractionRules="@xml/attrack_tracker_backup_rules"
    android:fullBackupContent="@xml/attrack_tracker_backup_rules_legacy">

    <meta-data
        android:name="kr.co.attrack.sdk.CLIENT_KEY"
        android:value="${attrackClientKey}" />
</application>
```

키는 소스 코드에 넣지 말고 빌드 시 주입합니다.

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["attrackClientKey"] =
            providers.gradleProperty("ATTRACK_CLIENT_KEY").get()
    }
}
```

`INTERNET`, `AD_ID` 권한은 SDK manifest가 자동으로 추가합니다.

## 3. SDK 초기화

`Application.onCreate`에서 main process로 한 번 초기화합니다. 결과를 놓치지
않도록 result listener를 먼저 설정합니다.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Tracker.setResultListener { result ->
            if (!result.success) {
                Log.w("MyApp", "ATTRACK ${result.code}: ${result.message}")
            }
        }

        val start = Tracker.initializeWithResult(this, "app_611b8e8624cfc117300f4712")
        if (!start.success) {
            Log.e("MyApp", "ATTRACK could not start: ${start.code}")
        }
    }
}
```

- `INITIALIZATION_STARTED`는 즉시 반환됩니다. 서버가 앱을 승인하면 이후 listener로
  `INITIALIZED`가 전달됩니다.
- **설치 이벤트는 자동으로 전송**되므로 직접 기록하지 않습니다.
- `Tracker.initialize(context, appId)`는 반환값이 없는 같은 호출이며, debug
  빌드에서는 설정 오류 시 예외를 던져 문제를 빨리 알려 줍니다.
- Listener callback은 메인 스레드가 아닌 SDK worker thread에서 실행됩니다.

## 4. 이벤트 기록

```kotlin
val result = Tracker.logEventWithResult(
    "purchase",
    mapOf("sku" to "pro_monthly", "revenue" to 9.99, "currency" to "KRW"),
)
if (result.code == TrackerResultCode.EVENT_QUEUED) {
    // 기기에 저장됨. 전송은 백그라운드에서 진행됩니다.
} else {
    Log.w("MyApp", "Rejected: ${result.code} ${result.fieldErrors}")
}
```

- `logEventWithResult`는 등록된 이벤트 정의로 검사한 뒤 `TrackerResult`를 반환하며
  예외를 던지지 않습니다.
- `Tracker.logEvent(name, params)`는 `transaction_id`만 반환합니다(거부 시 `null`).
  debug 빌드에서는 정의 오류 시 예외를 던집니다.
- 선언된 파라미터는 모두 필수이며, 선언되지 않은 파라미터는 거부됩니다.
  타입: `string` → `String`, `number` → 유한한 `Number`, `bool` → `Boolean`.
- 이벤트 이름: 1~40자, 영문자로 시작, 영문자·숫자·`_`만 사용, `firebase_`·
  `google_`·`ga_`로 시작할 수 없음. 파라미터 크기: 최대 16 KiB.
- 주문 ID·결제 ID 같은 자체 ID는 파라미터에 넣습니다. `transaction_id`는 SDK가 생성합니다.

**전송.** 이벤트는 전송 전에 디스크에 저장되어 앱 재시작, 재부팅, 오프라인에도
유지됩니다. 네트워크 오류, 서버 오류, 요청 제한, 인증 시간 오류는 backoff로 자동
재시도하며, 프로세스가 종료된 뒤에도 백그라운드(Android WorkManager)에서 이어서
전송합니다. 재시도한 이벤트는 같은 ID를 유지하므로 두 번 집계되지 않습니다.
사용자가 앱을 강제 종료하면 Android가 다음 실행 전까지 백그라운드 작업을 멈춥니다.

## 5. 이벤트 스키마 조회

`INITIALIZED` 이후 SDK는 앱이 보낼 수 있는 이벤트를 알고 있습니다.

```kotlin
Tracker.eventSchema()                // List<TrackerEventDefinition>
Tracker.eventDefinition("purchase")  // purchase(sku: string, revenue: number, …) 또는 null
Tracker.describeEventSchema()        // 전체 이벤트를 한 줄씩
Tracker.eventSchemaVersion()         // 첫 초기화 성공 전에는 빈 문자열
```

## 6. 결과 처리

모든 `TrackerResult`는 다음 값을 가집니다.

| 속성 | 의미 |
|---|---|
| `operation` | `INITIALIZATION`, `EVENT_QUEUE`, `EVENT_DELIVERY` |
| `code` | 분기에 사용하는 고정 `TrackerResultCode` |
| `success` | 이 작업의 성공 여부 |
| `message` | 사람이 읽는 설명. 분기에 사용하지 마세요 |
| `retryable` | SDK가 자동으로 재시도하는지 여부 |
| `eventName`, `eventId`, `transactionId` | 해당 이벤트 |
| `ingestId` | 이 시도의 서버 참조값(있는 경우) |
| `httpStatus` | HTTP 상태, `0`은 응답 없음 |
| `fieldErrors` | 파라미터 → 사유, 예: `score → required` |
| `cause` | 원인 예외(있는 경우) |

**성공 코드**

| 코드 | 의미 |
|---|---|
| `INITIALIZATION_STARTED` | 기기에서 초기화 시작 |
| `INITIALIZED` | 서버가 앱을 승인하고 이벤트 스키마를 캐시함 |
| `EVENT_QUEUED` | 이벤트가 기기에 저장되어 전송 대기 중 |
| `EVENT_DELIVERED` | 서버가 이벤트를 수락함 |
| `EVENT_DUPLICATE` | 서버에 이미 있는 이벤트(재시도). 유실 없음 |

**오류 코드**

| 코드 | 원인 | 조치 |
|---|---|---|
| `INVALID_ARGUMENT` | `null` context 등 잘못된 인자 | 호출 코드 수정 |
| `INVALID_APP_ID` | App ID 누락 또는 형식 오류 | 앱에 발급된 App ID 사용 |
| `INVALID_CONFIGURATION` | Client key metadata 누락, 또는 다른 App ID로 두 번째 초기화 | Manifest 확인, 초기화는 한 번만 |
| `WRONG_PROCESS` | main process가 아닌 곳에서 초기화 | main process에서만 초기화 |
| `NOT_INITIALIZED` | 초기화 전에 이벤트 기록 | 먼저 초기화 |
| `INVALID_EVENT_NAME` | 이름 규칙 위반 | 이름 수정 |
| `EVENT_NOT_REGISTERED` | 앱에 등록되지 않은 이벤트 | 등록된 이름 사용 |
| `PARAM_NOT_REGISTERED` | 이벤트 정의에 없는 파라미터 | 제거 |
| `PARAM_REQUIRED` | 선언된 파라미터 누락 또는 `null` | 추가 |
| `PARAM_TYPE_MISMATCH` | 값 타입 불일치 | 선언된 타입으로 전송 |
| `PARAMS_INVALID` | 파라미터를 인코딩할 수 없음 | 문자열·숫자·boolean 사용 |
| `PAYLOAD_TOO_LARGE` | 파라미터가 16 KiB 초과 | 데이터 축소 |
| `QUEUE_WRITE_FAILED` | 기기에 이벤트 저장 실패 | 저장 공간 확인 |
| `SIGNING_CERTIFICATE_UNAVAILABLE` | 앱 서명을 읽을 수 없음 | 앱 재설치 |
| `IDENTITY_MISMATCH` | 패키지명 또는 서명 인증서가 앱에 등록되지 않음 | 이 빌드의 서명 인증서 등록 |
| `TRACKER_DISABLED` | 앱의 추적이 비활성화됨 | ATTRACK 지원팀 문의 |
| `TRACKER_BLOCKED` | 서버 정책으로 차단됨 | `ingestId`와 함께 지원팀 문의 |
| `INVALID_PAYLOAD` | 서버가 요청 형식을 거부 | SDK 업데이트, `ingestId`와 함께 문의 |
| `AUTHENTICATION_FAILED` | Client key 또는 기기 시간 오류 | 키 확인, SDK가 재시도 |
| `RATE_LIMITED` | 요청 과다 | 조치 불필요, SDK가 재시도 |
| `NETWORK_ERROR` | 응답 없음 | 조치 불필요, SDK가 재시도 |
| `SERVER_ERROR` | 서버 오류(HTTP 5xx) | 조치 불필요, SDK가 재시도 |
| `UNKNOWN_ERROR` | 예상하지 못한 응답 | `httpStatus`, `ingestId`와 함께 문의 |

영구 거부된 이벤트(형식 오류, 미등록, 차단)는 재시도하지 않습니다.

## 7. 딥링크

SDK는 ATTRACK 링크(`https://api.attrack.co.kr/l/<package>/<linkId>`)로 앱을 열고
링크에 정의된 파라미터를 전달합니다. 앱이 설치되어 있지 않으면 사용자가 Google
Play에서 설치한 뒤 첫 실행 시 같은 링크를 전달합니다(**지연** 링크).

Manifest의 `<application>` 안에 **링크 Activity를 추가**합니다. `com.example.app`을
실제 패키지명으로 바꾸세요.

```xml
<activity
    android:name="kr.co.attrack.tracker.TrackerLinkActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="api.attrack.co.kr"
            android:pathPrefix="/l/com.example.app/" />
    </intent-filter>
</activity>
```

Android가 앱을 바로 열려면(검증된 App Link) 배포되는 앱의 서명 인증서가 앱에
등록되어 있어야 합니다. Google Play 빌드는 업로드 키가 아닌 **Play 앱 서명**
인증서입니다.

시작 Activity의 `onCreate`와 `onNewIntent` 모두에서 **링크를 처리**합니다.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // UI를 먼저 준비합니다.
    handleLink(intent)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleLink(intent)
}

private fun handleLink(intent: Intent?) {
    Tracker.handleDeepLink(this, intent) { result ->
        val data = result.data
        if (result.code == DynamicLinkCode.RESOLVED && data != null) {
            val routed = routeToScreen(data.parameters) // 앱의 화면 이동 코드
            if (routed && data.isDeferred) {
                Tracker.acknowledgeDeepLink(this, data)
            }
        }
    }
}
```

- Callback은 메인 스레드에서 실행됩니다. 링크는 `initialize` 없이도 동작합니다.
- `routeToScreen`은 앱 코드입니다. 파라미터는 신뢰할 수 없는 입력이므로 허용된
  화면만 열고 값을 검증하세요. 파라미터가 없는 링크는 홈 화면을 엽니다.
- **지연** 링크로 화면 이동에 성공한 뒤에만 `acknowledgeDeepLink`를 호출합니다.
  그 전에는 다음 실행 때 다시 전달됩니다. 같은 링크로 두 번 이동하지 않도록
  `data.handoffId`를 사용하세요.

| `DynamicLinkCode` | 조치 |
|---|---|
| `RESOLVED` | `data.parameters`로 화면 이동 |
| `NO_LINK` | 일반 실행 |
| `INVALID_LINK` | 안전한 화면 유지(알 수 없거나 비활성화되었거나 다른 앱의 링크) |
| `EXPIRED` | 안전한 화면 유지 |
| `UNAVAILABLE` | `retryable`이면 다음 foreground에서 `handleDeepLink` 재호출. 반복 호출 금지 |

기기에서 **테스트**합니다(실제 패키지명과 링크 ID 사용).

```bash
adb shell pm verify-app-links --re-verify com.example.app
adb shell pm get-app-links com.example.app
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/com.example.app/LINK_ID"
```

지연 링크는 Google Play에서 실제로 설치해야 확인할 수 있습니다(예: 내부 테스트
트랙). 직접 설치한 APK는 첫 실행에서 `NO_LINK`를 반환합니다. Java 코드, 제한,
문제 해결은 [딥링크 상세](DEEP_LINKS.ko.md)를 참고하세요.

## 8. 설치 귀속 정보

`Tracker.installAttribution()`은 Google Play Install Referrer가 이 설치에 대해 알려
준 정보를 반환합니다. 아직 모르거나 referrer가 없으면 `null`입니다(오가닉으로 처리).

```kotlin
Tracker.installAttribution()?.let { attribution ->
    Log.d("MyApp", "source=${attribution.utmSource} campaign=${attribution.utmCampaign}")
}
```

이 값은 설치가 유지되는 동안 보관됩니다. 실제 귀속은 서버가 판단하며, 이 값은
기기의 사본입니다.

## 9. 기기 식별자

SDK는 두 기기 식별자를 자동으로 수집하여 설치 이벤트와 모든 이벤트에 함께
전송합니다. 끄는 설정은 없습니다.

| 식별자 | 출처 | 용도 |
|---|---|---|
| Google 광고 ID | `AdvertisingIdClient` (`play-services-ads-identifier`) | 유료 귀속, 보상 중복 방지 |
| Secure ID | `Settings.Secure.ANDROID_ID` | 재설치·광고 ID 재설정 후 보상 중복 방지 |

- 사용자가 광고 ID를 삭제·제한했거나 Google Play 서비스가 없으면 광고 ID는 빈 값입니다.
- Secure ID는 서버에 hash로만 저장되며 원본은 저장하지 않습니다.
- Google Play *데이터 보안* 양식에 **기기 또는 기타 ID**를 신고하고 개인정보처리방침에
  이 식별자를 명시하세요.

## 10. Java

모든 메서드는 static이며 Java에서도 동일하게 사용합니다.

```java
Tracker.setResultListener(result -> {
    if (!result.getSuccess()) {
        Log.w("MyApp", result.getCode() + ": " + result.getMessage());
    }
});
Tracker.initializeWithResult(getApplicationContext(), "app_611b8e8624cfc117300f4712");

Map<String, Object> params = new HashMap<>();
params.put("level", 3);
params.put("score", 900);
params.put("perfect", true);
TrackerResult result = Tracker.logEventWithResult("level_complete", params);
```

## 11. Log 및 지원

Logcat은 `attrack` 태그를 사용하며 `[EVENT_DELIVERED]`처럼 결과 코드로 시작합니다.
성공 log는 SDK debug 빌드에서, 실패 log는 모든 빌드에서 출력됩니다. Client key,
요청 본문, 광고 ID, referrer URL은 log에 남기지 않습니다. 지원 문의 시
`transactionId`와 (있는 경우) `ingestId`를 함께 전달하세요.

## 12. API 레퍼런스

모든 메서드는 `kr.co.attrack.tracker.Tracker`의 static 메서드이며, release
빌드에서는 예외를 던지지 않습니다.

| 메서드 | 반환 | 설명 |
|---|---|---|
| `initialize(context, appId)` | — | SDK 시작. debug 빌드는 설정 오류 시 예외 |
| `initializeWithResult(context, appId)` | `TrackerResult` | SDK를 시작하고 즉시 결과 반환 |
| `setResultListener(listener)` | — | 모든 결과를 SDK worker thread에서 수신, `null`이면 해제 |
| `logEvent(name, params)` | `String?` | 이벤트 저장, `transaction_id` 반환(거부 시 `null`) |
| `logEventWithResult(name, params)` | `TrackerResult` | 엄격한 검사 후 저장, `EVENT_QUEUED` 또는 오류 코드 |
| `status()` | `TrackerStatus` | 현재 SDK 상태 |
| `eventSchema()` | `List<TrackerEventDefinition>` | 앱이 보낼 수 있는 이벤트 |
| `eventDefinition(name)` | `TrackerEventDefinition?` | 한 이벤트의 파라미터, 없으면 `null` |
| `describeEventSchema()` | `String` | 전체 이벤트를 한 줄씩 |
| `eventSchemaVersion()` | `String` | 캐시된 스키마 버전 |
| `installAttribution()` | `InstallAttribution?` | 이 설치의 Play Install Referrer |
| `handleDeepLink(context, intent, callback)` | — | 링크·지연 링크 해석, callback은 메인 스레드 |
| `acknowledgeDeepLink(context, data)` | `Boolean` | 지연 링크 처리 완료 표시, `false`면 대기 유지 |

`params`는 선택입니다(기본값: 빈 map).

**`TrackerStatus`**

| 값 | 의미 |
|---|---|
| `NOT_STARTED` | 초기화 전이거나 기기에서 초기화 실패 |
| `INITIALIZING` | 서버와 연결 중 |
| `ACTIVE` | 실행 중, 이벤트 전송 |
| `DISABLED` | 앱의 추적이 비활성화됨. 이벤트는 다음 시작을 위해 보관 |
| `BLOCKED` | 식별 정보 불일치 또는 정책 차단. 다음 시작까지 전송 중단 |

**`InstallAttribution`**: `status`(`ok` 또는 `unavailable`), `clickId`, `utmSource`,
`utmMedium`, `utmCampaign`, `utmContent`, `utmTerm`, `sender`, `publisherId`,
`referrerClickSeconds`, `installBeginSeconds`, `raw`, `params`.

**`DynamicLinkResult`**: `code`, `data`(`PendingDynamicLinkData?`), `message`,
`retryable`.

**`PendingDynamicLinkData`**: `parameters`(링크 파라미터), `linkId`, `isDeferred`,
`handoffId`(지연 링크 하나의 고정 키), `clickId`·`sender`(이 링크로 이어진 클릭,
참고용), `link`. `trackingToken`은 내부 값이므로 log에 남기거나 수정하지 마세요.

**`TrackerLinkActivity`**: 검증된 링크를 받는 SDK Activity. Manifest에 선언하며(7절)
직접 실행하지 마세요.

`TrackerDynamicLinks`, `DynamicLinkBuilder`는 기존 연동 호환용입니다.
`handleDeepLink`를 사용하세요.

## 0.4.x에서 마이그레이션

- `Tracker.setAdvertisingConsent`, `Tracker.setDeviceDeduplicationConsent` 호출을
  제거하세요. 두 메서드는 삭제되었으며 식별자는 항상 수집됩니다(9절).
- 이에 맞게 데이터 보안 신고를 갱신하세요.
