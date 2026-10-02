# ATTRACK Android SDK API


모든 메서드는 `kr.co.attrack.tracker.Tracker`의 static 메서드이며, release
빌드에서는 예외를 던지지 않습니다.

Maven 배포본은 SDK의 release variant입니다. 앱을 debug로 빌드해도 SDK가 debug variant로 바뀌지는 않습니다.

| 메서드 | 반환 | 설명 |
|---|---|---|
| `initialize(context, appId, clientKey)` | — | SDK 시작. debug 빌드는 설정 오류 시 예외 |
| `initializeWithResult(context, appId, clientKey)` | `TrackerResult` | SDK를 시작하고 즉시 결과 반환 |
| `setResultListener(listener)` | — | 선택 기능. 로컬 결과는 호출 스레드, 네트워크 결과는 작업 스레드에서 수신. `null`이면 해제 |
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
참고용), `link`.

**`TrackerLinkActivity`**: 검증된 링크를 받는 SDK Activity. 딥링크 가이드에 따라 Manifest에 선언하며
직접 실행하지 마세요.


## 사용 예제

먼저 [연동 가이드](INTEGRATION_GUIDE.md)를 따라 설정하세요. 아래 예제에는 내 앱의 값을 사용하며 샘플 계정은 필요하지 않습니다. Kotlin은 `import kr.co.attrack.tracker.*`, Java는 `import kr.co.attrack.tracker.*;`를 추가하세요. Android 타입은 전체 이름으로 표시했습니다. 로그는 개발 확인용이며 운영 앱에서는 불필요한 진단 로그를 제거하세요.

### 1. 한 번 초기화하기

앱의 Application 클래스에 추가하세요. 기존 클래스가 있다면 `onCreate()`에 호출만 넣으면 됩니다. 두 값은 내 앱 콘솔의 값으로 바꾸세요. `initialize`와 `initializeWithResult` 중 하나만 사용합니다.

**Kotlin**

```kotlin
class MyApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
    }
}

// Alternative inside onCreate(), when you need the immediate result:
// val result = Tracker.initializeWithResult(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
// android.util.Log.d("ATTRACK", result.code.name)
```

**Java**

```java
public final class MyApplication extends android.app.Application {
    @Override public void onCreate() {
        super.onCreate();
        Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY");
    }
}
```

### 2. 이벤트 보내기

내 앱에 등록한 이벤트와 파라미터 이름을 사용하세요. 아래 `purchase`, `order_id`, `amount`는 예시이며 기본 제공 이벤트가 아닙니다. transaction ID 반환은 기기에 저장되었다는 뜻이며 서버 전송 완료를 의미하지 않습니다.

**Kotlin**

```kotlin
fun recordPurchase() {
    val transactionId = Tracker.logEvent(
        "purchase", mapOf("order_id" to "ORDER_123", "amount" to 9900)
    )
    if (transactionId == null) {
        android.util.Log.w("ATTRACK", "Event was not queued")
    }
    // For a registered event with no parameters: Tracker.logEvent("tutorial_complete")
}
```

**Java**

```java
void recordPurchase() {
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("order_id", "ORDER_123");
    params.put("amount", 9900);
    String transactionId = Tracker.logEvent("purchase", params);
    if (transactionId == null) {
        android.util.Log.w("ATTRACK", "Event was not queued");
    }
}
```

### 3. 거부된 이벤트 확인하기

결과 코드와 필드별 오류가 필요하면 이 메서드를 사용하세요. 사용 가능한 캐시 스키마로 검사하며 서버에서도 검증합니다. 같은 동작에 `logEvent`까지 호출하면 이벤트를 두 번 보내게 됩니다.

**Kotlin**

```kotlin
fun recordPurchaseWithResult() {
    val result = Tracker.logEventWithResult(
        "purchase", mapOf("order_id" to "ORDER_123", "amount" to 9900)
    )
    when (result.code) {
        TrackerResultCode.EVENT_QUEUED ->
            android.util.Log.d("ATTRACK", "Queued: ${result.transactionId}")
        else -> android.util.Log.w("ATTRACK", "${result.code}: ${result.fieldErrors}")
    }
}
```

**Java**

```java
void recordPurchaseWithResult() {
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("order_id", "ORDER_123");
    params.put("amount", 9900);
    TrackerResult result = Tracker.logEventWithResult("purchase", params);
    if (result.getCode() == TrackerResultCode.EVENT_QUEUED) {
        android.util.Log.d("ATTRACK", "Queued: " + result.getTransactionId());
    } else {
        android.util.Log.w("ATTRACK", result.getCode() + ": " + result.getFieldErrors());
    }
}
```

### 4. 초기화·전송 결과 구독하기 (선택)

첫 예시 대신 Application의 `onCreate()`에서 사용할 수 있습니다. 초기화 전에 리스너를 등록하면 시작 결과도 받습니다. 초기화 호출은 콜백 밖에 둡니다. `INITIALIZATION_STARTED`는 시작 접수, `INITIALIZED`는 서버 응답 확인입니다. 콜백은 짧게 유지하고 UI 변경은 메인 스레드로 전달하세요. 전역 리스너에서 Activity를 참조하지 마세요. 해제할 때는 `null`을 전달합니다.

**Kotlin**

```kotlin
fun startWithDiagnostics(app: android.app.Application) {
    Tracker.setResultListener { result ->
        when (result.code) {
            TrackerResultCode.INITIALIZED -> android.util.Log.d("ATTRACK", "Initialized")
            TrackerResultCode.EVENT_DELIVERED, TrackerResultCode.EVENT_DUPLICATE ->
                android.util.Log.d("ATTRACK", "Acknowledged: ${result.transactionId}")
            else -> android.util.Log.d("ATTRACK", result.code.name)
        }
    }
    Tracker.initialize(app, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
}
// When diagnostics are no longer needed: Tracker.setResultListener(null)
```

**Java**

```java
void startWithDiagnostics(android.app.Application app) {
    Tracker.setResultListener(result -> {
        android.util.Log.d("ATTRACK", result.getCode().name());
    });
    Tracker.initialize(app, "YOUR_APP_ID", "YOUR_CLIENT_KEY");
}
// When diagnostics are no longer needed: Tracker.setResultListener(null);
```

### 5. 상태·이벤트 정의·설치 정보 읽기

아래 메서드는 네트워크 응답을 기다리지 않고 현재 값을 반환합니다. 초기화 성공 전에는 스키마가 비어 있을 수 있습니다. 설치 정보가 null이면 아직 모르거나 조회할 수 없다는 뜻이지 자연 유입이라는 증거는 아닙니다. 필요할 때 다시 읽으세요. 설치 정보로 권한이나 리워드를 부여하지 마세요.

**Kotlin**

```kotlin
fun inspectSdk() {
    android.util.Log.d("ATTRACK", "State: ${Tracker.status()}")
    android.util.Log.d("ATTRACK", "Schema: ${Tracker.eventSchemaVersion()}")
    android.util.Log.d("ATTRACK", Tracker.describeEventSchema())
    for (event in Tracker.eventSchema()) {
        android.util.Log.d("ATTRACK", "Event: ${event.name}")
    }
    val purchase = Tracker.eventDefinition("purchase")
    for (param in purchase?.params.orEmpty()) {
        android.util.Log.d("ATTRACK", "${param.name}: ${param.type}")
    }
    val attribution = Tracker.installAttribution()
    val campaign = attribution?.utmCampaign?.takeIf { it.isNotBlank() }
    android.util.Log.d("ATTRACK", "Campaign: ${campaign ?: "Unknown"}")
}
```

**Java**

```java
void inspectSdk() {
    android.util.Log.d("ATTRACK", "State: " + Tracker.status());
    android.util.Log.d("ATTRACK", "Schema: " + Tracker.eventSchemaVersion());
    android.util.Log.d("ATTRACK", Tracker.describeEventSchema());
    for (TrackerEventDefinition event : Tracker.eventSchema()) {
        android.util.Log.d("ATTRACK", "Event: " + event.getName());
    }
    TrackerEventDefinition purchase = Tracker.eventDefinition("purchase");
    if (purchase != null) {
        for (TrackerEventParam param : purchase.getParams()) {
            android.util.Log.d("ATTRACK", param.getName() + ": " + param.getType());
        }
    }
    InstallAttribution attribution = Tracker.installAttribution();
    String campaign = attribution == null ? "Unknown" : attribution.getUtmCampaign();
    android.util.Log.d("ATTRACK", "Campaign: " + campaign);
}
```

### 6. 딥링크 수신·처리 완료 알리기

UI 준비 후 시작 Activity의 `onCreate()`와 `onNewIntent()`에서 호출하세요. `routeToScreen`은 앱에서 구현할 화면 이동 함수입니다. 파라미터를 검증하고 이동 성공 후에만 true를 반환하세요. 로그인을 기다려야 한다면 실제 이동 완료 시점에 완료를 알립니다. 저장 실패 시 지연 링크가 남으므로 `handoffId`로 중복 이동을 방지하세요. Activity와 intent-filter 전체 설정은 딥링크 가이드를 참고하세요.

**Kotlin**

```kotlin
fun handleLink(
    activity: android.app.Activity,
    intent: android.content.Intent?,
    routeToScreen: (Map<String, String>) -> Boolean
) {
    Tracker.handleDeepLink(activity, intent) { result ->
        val data = result.data
        if (result.code == DynamicLinkCode.RESOLVED && data != null) {
            if (routeToScreen(data.parameters) && data.isDeferred) {
                val saved = Tracker.acknowledgeDeepLink(activity, data)
                if (!saved) android.util.Log.w("ATTRACK", "Link remains pending")
            }
        }
    }
}
```

**Java**

```java
interface LinkRouter {
    boolean routeToScreen(java.util.Map<String, String> parameters);
}
void handleLink(android.app.Activity activity, android.content.Intent intent, LinkRouter router) {
    Tracker.handleDeepLink(activity, intent, result -> {
        PendingDynamicLinkData data = result.getData();
        if (result.getCode() == DynamicLinkCode.RESOLVED && data != null) {
            if (router.routeToScreen(data.getParameters()) && data.isDeferred()) {
                boolean saved = Tracker.acknowledgeDeepLink(activity, data);
                if (!saved) android.util.Log.w("ATTRACK", "Link remains pending");
            }
        }
    });
}
```

[딥링크 설정](DEEP_LINKS.md) · [연동 가이드](INTEGRATION_GUIDE.md)
