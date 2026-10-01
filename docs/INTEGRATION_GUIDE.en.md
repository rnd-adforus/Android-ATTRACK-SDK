# ATTRACK Android SDK — Get started

The ATTRACK Android SDK measures app installs and in-app events, attributes
installs from Google Play, and opens your app from links, including the first
open after an install. This guide covers SDK `0.5.0`.

[한국어](INTEGRATION_GUIDE.ko.md) · [Deep links in detail](DEEP_LINKS.md)

## Before you begin

| Requirement | Value |
|---|---|
| `minSdk` | 21 or higher (Android 5.0) |
| `compileSdk` | **35 or higher**, 36 recommended (required by the bundled WorkManager 2.10.5) |
| `targetSdk` | Your choice |
| Android Gradle Plugin | **8.6.0 or higher** for compileSdk 35; **8.9.0 or higher** for compileSdk 36 |
| Gradle | The version your AGP requires (for example AGP 8.9 → Gradle 8.11.1 or higher) |
| JDK (build) | 17 or higher |
| Java / Kotlin | The SDK is Java 8 bytecode; works from Java apps and Kotlin 1.9+ apps (built with Kotlin 2.0.21) |
| Included dependencies | Google Play Install Referrer 2.2, AndroidX WorkManager 2.10.5 |
| Advertising ID library | `play-services-ads-identifier` 18.3.0 (`minSdk` 23+); apps on `minSdk` 21–22 use 18.2.0 |
| Network | HTTPS |

You also need, for your app:

- the **App ID** (`app_` followed by 24 hexadecimal characters),
- the **client key**,
- the **event names** and parameters registered for your app,
- for links, the **link IDs** created for your app.

## 1. Add the SDK

Add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("kr.co.attrack:attrack-tracker:0.5.0")

    // Lets the SDK read the Google Advertising ID. Required for paid attribution.
    // Apps on minSdk 21–22: use 18.2.0 (18.3.0 requires minSdk 23).
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}
```

If you received the SDK as an AAR file, place `attrack-tracker-0.5.0.aar` in
`app/libs` and add its dependencies yourself (an AAR does not carry them):

```kotlin
dependencies {
    implementation(files("libs/attrack-tracker-0.5.0.aar"))
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("androidx.work:work-runtime:2.10.5")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}
```

## 2. Update your manifest

Add the client key as `<meta-data>` and reference the SDK's backup rules, which
stop an installation's tracking state from being restored onto another
installation.

```xml
<application
    android:dataExtractionRules="@xml/attrack_tracker_backup_rules"
    android:fullBackupContent="@xml/attrack_tracker_backup_rules_legacy">

    <meta-data
        android:name="kr.co.attrack.sdk.CLIENT_KEY"
        android:value="${attrackClientKey}" />
</application>
```

Keep the key out of source control and inject it at build time:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["attrackClientKey"] =
            providers.gradleProperty("ATTRACK_CLIENT_KEY").get()
    }
}
```

The SDK's own manifest adds the `INTERNET` and `AD_ID` permissions.

## 3. Initialize the SDK

Initialize once, in your `Application.onCreate`, in the main process. Set the
result listener first so no result is missed.

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

- `INITIALIZATION_STARTED` is returned immediately. `INITIALIZED` arrives later on
  the listener, once the server has accepted your app.
- The **install event is sent automatically**. You do not log it.
- `Tracker.initialize(context, appId)` is the same call without a return value;
  in debug builds it throws on a configuration error so mistakes surface early.
- Listener callbacks run on the SDK's worker thread, not the main thread.

## 4. Log events

```kotlin
val result = Tracker.logEventWithResult(
    "purchase",
    mapOf("sku" to "pro_monthly", "revenue" to 9.99, "currency" to "KRW"),
)
if (result.code == TrackerResultCode.EVENT_QUEUED) {
    // Saved on the device. Delivery happens in the background.
} else {
    Log.w("MyApp", "Rejected: ${result.code} ${result.fieldErrors}")
}
```

- `logEventWithResult` checks the event against its registered definition and
  returns a `TrackerResult`. It never throws.
- `Tracker.logEvent(name, params)` returns only the `transaction_id` (or `null` if
  rejected). In debug builds it throws on a definition mistake.
- Every declared parameter is required; undeclared parameters are rejected.
  Types: `string` → `String`, `number` → finite `Number`, `bool` → `Boolean`.
- Event names: 1–40 characters, start with a letter, letters/digits/`_` only, and
  not starting with `firebase_`, `google_` or `ga_`. Parameters: up to 16 KiB.
- Put your own IDs (order ID, payment ID) in parameters. The SDK creates the
  `transaction_id` itself.

**Delivery.** Events are written to disk before sending and survive app restarts,
reboots and offline periods. The SDK retries network errors, server errors, rate
limits and authentication-clock errors with backoff, and resumes in the
background after the process is killed (Android WorkManager). A retried event
keeps its ID, so it is never counted twice. After a user force-stops the app,
Android pauses background work until the app is opened again.

## 5. Read the event schema

After `INITIALIZED`, the SDK holds the events your app may send:

```kotlin
Tracker.eventSchema()                // List<TrackerEventDefinition>
Tracker.eventDefinition("purchase")  // purchase(sku: string, revenue: number, …) or null
Tracker.describeEventSchema()        // all events, one per line
Tracker.eventSchemaVersion()         // empty before the first successful initialization
```

## 6. Handle results

Every `TrackerResult` has:

| Property | Meaning |
|---|---|
| `operation` | `INITIALIZATION`, `EVENT_QUEUE` or `EVENT_DELIVERY` |
| `code` | Stable `TrackerResultCode` to branch on |
| `success` | Whether this operation succeeded |
| `message` | Human-readable detail. Do not branch on it |
| `retryable` | Whether the SDK will retry by itself |
| `eventName`, `eventId`, `transactionId` | The event concerned |
| `ingestId` | Server reference for this attempt, when available |
| `httpStatus` | HTTP status; `0` means no response |
| `fieldErrors` | Parameter → reason, for example `score → required` |
| `cause` | Underlying exception, when there is one |

**Success codes**

| Code | Meaning |
|---|---|
| `INITIALIZATION_STARTED` | Initialization started on the device |
| `INITIALIZED` | The server accepted your app; the event schema is cached |
| `EVENT_QUEUED` | The event is saved on the device and waiting to be sent |
| `EVENT_DELIVERED` | The server accepted the event |
| `EVENT_DUPLICATE` | The server already had this event (a retry); nothing is lost |

**Error codes**

| Code | Cause | What to do |
|---|---|---|
| `INVALID_ARGUMENT` | A `null` context or other invalid argument | Fix the call |
| `INVALID_APP_ID` | Missing or malformed App ID | Use the App ID issued for your app |
| `INVALID_CONFIGURATION` | Client key metadata missing, or a second `initialize` with a different App ID | Check the manifest and initialize once |
| `WRONG_PROCESS` | Initialized outside the main process | Initialize only in the main process |
| `NOT_INITIALIZED` | Event logged before initialization | Initialize first |
| `INVALID_EVENT_NAME` | Name breaks the naming rules | Fix the name |
| `EVENT_NOT_REGISTERED` | Event not registered for your app | Use a registered name |
| `PARAM_NOT_REGISTERED` | Parameter not in the event definition | Remove it |
| `PARAM_REQUIRED` | A declared parameter is missing or `null` | Add it |
| `PARAM_TYPE_MISMATCH` | Wrong value type | Send the declared type |
| `PARAMS_INVALID` | Parameters cannot be encoded | Use strings, numbers and booleans |
| `PAYLOAD_TOO_LARGE` | Parameters exceed 16 KiB | Send less data |
| `QUEUE_WRITE_FAILED` | The device could not save the event | Check free storage |
| `SIGNING_CERTIFICATE_UNAVAILABLE` | Android could not read the app's signature | Reinstall the app |
| `IDENTITY_MISMATCH` | Package name or signing certificate not registered for your app | Register the certificate that signs this build |
| `TRACKER_DISABLED` | Tracking is disabled for your app | Contact ATTRACK support |
| `TRACKER_BLOCKED` | Blocked by server policy | Contact ATTRACK support with the `ingestId` |
| `INVALID_PAYLOAD` | The server rejected the request format | Update the SDK; contact support with the `ingestId` |
| `AUTHENTICATION_FAILED` | Wrong client key or device clock | Check the key; the SDK retries |
| `RATE_LIMITED` | Too many requests | None; the SDK retries |
| `NETWORK_ERROR` | No response | None; the SDK retries |
| `SERVER_ERROR` | Server error (HTTP 5xx) | None; the SDK retries |
| `UNKNOWN_ERROR` | Unexpected response | Contact support with `httpStatus` and `ingestId` |

Events rejected permanently (invalid, not registered, blocked) are not retried.

## 7. Deep links

The SDK opens your app from ATTRACK links (`https://api.attrack.co.kr/l/<package>/<linkId>`)
and hands you the parameters defined for the link. If the app is not installed,
the user installs it from Google Play and the SDK delivers the same link on the
first open (a **deferred** link).

**Add the link Activity** inside `<application>` in your manifest. Replace
`com.example.app` with your package name:

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

For Android to open your app directly (a verified App Link), the certificate that
signs your published app must be registered for your app. For Google Play builds
that is the **Play app signing** certificate, not your upload key.

**Handle the link in your launch Activity**, in both `onCreate` and `onNewIntent`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Set up your UI first.
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
            val routed = routeToScreen(data.parameters) // your navigation
            if (routed && data.isDeferred) {
                Tracker.acknowledgeDeepLink(this, data)
            }
        }
    }
}
```

- The callback runs on the main thread. Links work without `initialize`.
- `routeToScreen` is your code. Treat parameters as untrusted input: allow only
  known screens and validate values. A link with no parameters should open your
  home screen.
- Call `acknowledgeDeepLink` only after a **deferred** link was routed
  successfully; until then it is delivered again on the next launch. Use
  `data.handoffId` to avoid navigating twice for the same link.

| `DynamicLinkCode` | What to do |
|---|---|
| `RESOLVED` | Route using `data.parameters` |
| `NO_LINK` | Normal launch |
| `INVALID_LINK` | Stay on a safe screen (unknown, disabled or foreign link) |
| `EXPIRED` | Stay on a safe screen |
| `UNAVAILABLE` | If `retryable`, call `handleDeepLink` again on the next foreground; do not loop |

**Test** on a device (use your package and a real link ID):

```bash
adb shell pm verify-app-links --re-verify com.example.app
adb shell pm get-app-links com.example.app
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/com.example.app/LINK_ID"
```

A deferred link needs a real install from Google Play (for example, an internal
testing track); a sideloaded APK reports `NO_LINK` on first open. See
[Deep links](DEEP_LINKS.md) for Java code, limits and troubleshooting.

## 8. Install attribution

`Tracker.installAttribution()` returns what Google Play's Install Referrer
reported for this installation, or `null` when it is not known yet or Play had
no referrer (treat as organic):

```kotlin
Tracker.installAttribution()?.let { attribution ->
    Log.d("MyApp", "source=${attribution.utmSource} campaign=${attribution.utmCampaign}")
}
```

The value is kept for the life of the installation. Attribution itself is
decided by the server; this is the device's copy.

## 9. Device identifiers

The SDK collects two device identifiers automatically and sends them with the
install and every event. There is no setting to turn them off.

| Identifier | Source | Used for |
|---|---|---|
| Google Advertising ID | `AdvertisingIdClient` (`play-services-ads-identifier`) | Paid attribution and preventing duplicate rewards |
| Secure ID | `Settings.Secure.ANDROID_ID` | Preventing duplicate rewards after a reinstall or advertising-ID reset |

- The advertising ID is empty when the user deleted or limited it, or when Google
  Play services is unavailable.
- The secure ID is stored on the server only as a hash, never in raw form.
- Declare **Device or other IDs** in your Google Play *Data safety* form and
  describe these identifiers in your privacy policy.

## 10. Java

All methods are static and work the same from Java:

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

## 11. Logging and support

Logcat lines use the tag `attrack` and start with the result code, for example
`[EVENT_DELIVERED]`. Success lines appear in debug builds of the SDK; failures are
logged in all builds. Client keys, request bodies, advertising IDs and referrer
URLs are never logged. When contacting support, include the `transactionId` and,
when present, the `ingestId`.

## 12. API reference

All methods are static on `kr.co.attrack.tracker.Tracker`. None throws in a
release build.

| Method | Returns | Description |
|---|---|---|
| `initialize(context, appId)` | — | Starts the SDK. Debug builds throw on a configuration error |
| `initializeWithResult(context, appId)` | `TrackerResult` | Starts the SDK and returns the immediate outcome |
| `setResultListener(listener)` | — | Receives every result on the SDK worker thread; `null` removes it |
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
(the click that led here, informational), `link`. `trackingToken` is internal: do
not log or change it.

**`TrackerLinkActivity`**: the SDK Activity that receives verified links. Declare
it in your manifest (section 7); do not start it yourself.

`TrackerDynamicLinks` and `DynamicLinkBuilder` exist only for compatibility with
older integrations. Use `handleDeepLink`.

## Migrating from 0.4.x

- Remove calls to `Tracker.setAdvertisingConsent` and
  `Tracker.setDeviceDeduplicationConsent`. Both were removed; identifiers are now
  always collected (section 9).
- Update your Data safety declaration accordingly.
