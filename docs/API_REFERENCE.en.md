# ATTRACK Android SDK API


All methods are static on `kr.co.attrack.tracker.Tracker`. None throws in a
published Maven release. This does not depend on whether your host app is debug or release.

| Method | Returns | Description |
|---|---|---|
| `initialize(context, appId, clientKey)` | — | Starts the SDK. The SDK source debug variant throws on configuration errors; the Maven release reports errors |
| `initializeWithResult(context, appId, clientKey)` | `TrackerResult` | Starts the SDK and returns the immediate outcome |
| `setResultListener(listener)` | — | Optional observer; caller thread for local results, worker for network results; `null` removes it |
| `logEvent(name, params)` | `String?` | Queues an event; returns its `transaction_id`, or `null` if rejected |
| `logEventWithResult(name, params)` | `TrackerResult` | Queues an event with strict checks; `EVENT_QUEUED` or an error code |
| `status()` | `TrackerStatus` | Current SDK state |
| `eventSchema()` | `List<TrackerEventDefinition>` | Events this app may send |
| `eventDefinition(name)` | `TrackerEventDefinition?` | One event's parameters, or `null` |
| `describeEventSchema()` | `String` | All events, one per line |
| `eventSchemaVersion()` | `String` | Version of the cached schema |
| `installAttribution()` | `InstallAttribution?` | The Play Install Referrer for this installation |
| `handleDeepLink(context, intent, callback)` | — | Resolves a link or deferred link; callback on the main thread |
| `acknowledgeDeepLink(context, data)` | `Boolean` | Marks a deferred link as handled; `false` keeps it pending |

`params` is optional (default: empty map).

**`TrackerStatus`**

| Value | Meaning |
|---|---|
| `NOT_STARTED` | Not initialized, or initialization failed on the device |
| `INITIALIZING` | Connecting to the server |
| `ACTIVE` | Running; events are delivered |
| `DISABLED` | Tracking is disabled for the app. Events stay saved for a later start |
| `BLOCKED` | Identity mismatch or blocked by policy. Delivery stops until the next start |

**`InstallAttribution`**: `status` (`ok` or `unavailable`), `clickId`, `utmSource`,
`utmMedium`, `utmCampaign`, `utmContent`, `utmTerm`, `sender`, `publisherId`,
`referrerClickSeconds`, `installBeginSeconds`, `raw`, `params`.

**`DynamicLinkResult`**: `code`, `data` (`PendingDynamicLinkData?`), `message`,
`retryable`.

**`PendingDynamicLinkData`**: `parameters` (the link's parameters), `linkId`,
`isDeferred`, `handoffId` (stable key for one deferred link), `clickId` and `sender`
(the click that led here, informational), `link`.

**`TrackerLinkActivity`**: the SDK Activity that receives verified links. Declare
it as described in the deep-link guide; do not start it yourself.


## Usage examples

Start with the [integration guide](INTEGRATION_GUIDE.en.md). These examples use your own app configuration; no sample account is required. Kotlin snippets use `import kr.co.attrack.tracker.*`; Java snippets use `import kr.co.attrack.tracker.*;`. Android types below are fully qualified. Logging is for development; remove verbose diagnostics from production.

### 1. Initialize once

Place this class in your app and register it as the manifest’s Application class. If you already have one, add the call to its `onCreate()`. Replace both placeholders with your own app’s console values. Choose either `initialize` or `initializeWithResult`; you do not need both.

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

### 2. Send an event

Use event and parameter names registered for your app. `purchase`, `order_id`, and `amount` below are examples, not built-in events. A returned transaction ID means queued locally, not delivered to the server.

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

### 3. Inspect a rejected event

Use this alternative when you want a result code and field errors. It checks the cached event schema when available; the server still validates every event. Do not also call `logEvent` for the same action, or you will submit two events.

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

### 4. Observe initialization and delivery (optional)

Place this in Application `onCreate()` as an alternative to the first example. Register the listener before initialization to observe startup. Initialization stays outside the callback. `INITIALIZATION_STARTED` is a local start; `INITIALIZED` confirms the server response. Keep the callback short. Dispatch to the main thread before touching UI, and avoid capturing an Activity in this global listener. Pass `null` to remove it.

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

### 5. Read status, event definitions, and attribution

These are snapshots, not blocking network calls. Schema data can be empty before initialization succeeds. A null attribution means unknown or unavailable; it does not prove an organic install. Read again later when your app needs it. Attribution fields are diagnostic inputs, not authorization or reward proof.

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

### 6. Receive and acknowledge a deep link

Call this from your launch Activity’s `onCreate()` and `onNewIntent()` after the UI is ready. `routeToScreen` is your app’s navigation function: validate the parameters, perform navigation, and return true only after it succeeds. If navigation waits for login, acknowledge in its eventual completion handler. A failed acknowledgement leaves the deferred link pending; use `handoffId` to avoid navigating twice. See the deep-link guide for the complete Activity and intent-filter setup.

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

[Deep-link setup](DEEP_LINKS.en.md) · [Integration guide](INTEGRATION_GUIDE.en.md)
