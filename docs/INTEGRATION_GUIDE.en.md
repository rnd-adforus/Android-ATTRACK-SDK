# Add ATTRACK to your Android app

## Version 1.1.0 threading and acceptance contract

Initialization schedules background setup and returns without waiting for storage, package-manager calls, or network access. `INITIALIZATION_STARTED` confirms admission; later setup failures and `INITIALIZED` arrive through the result listener. Events called immediately after initialization wait behind setup in the same bounded storage queue.

`logEvent` returns a transaction ID after in-memory admission. `logEventWithResult` returns `EVENT_ACCEPTED` after bounded parameter and cached-schema checks. Neither return value confirms persistence. The listener's later `EVENT_QUEUED` confirms the outbox write; `QUEUE_WRITE_FAILED` reports a failed write with the same transaction ID. Process exit before the write can lose an accepted event. There are 128 waiting storage tasks; overload is rejected without blocking or executing disk work on the caller. Parameters are copied with a 16 KiB encoded limit, depth 12 and 1024 visited values; supported values are null, strings, booleans, standard primitive numbers, maps and lists. Arbitrary object conversion is rejected.

Result listeners run on a separate SDK thread. They must dispatch UI updates to main. A slow listener can fill the 64-slot notification queue; subsequent notifications are dropped while persistence and delivery continue. Listener notifications are best-effort diagnostics, not a durable event ledger.

`acknowledgeDeepLink` is nonblocking: `true` means the background acknowledgement task was admitted, not that its write completed. A write failure or process exit before completion can redeliver the handoff; route idempotently using `handoffId`. Deep-link result callbacks still run on main.

The wire contract is unchanged. HTTP 202 means the backend durably accepted the receipt for later processing; `EVENT_DELIVERED` describes that acknowledgement, not completion of attribution, reporting or partner postbacks. HTTP 429/5xx/network failures retain the same event IDs for retry.

ATTRACK tracks installs automatically and lets you send your app's events. Basic integration takes one dependency and one initialization call. Deep links and result listeners are optional.

[한국어](INTEGRATION_GUIDE.md)

## Before you start

Register **your own app** with ATTRACK and get its **App ID** (`app_…`) and **client key** from the partner console. The registered package name and signing certificate must match the app installed on the device. For Google Play, use the Play **app signing** certificate, not the upload certificate.

Your package name is your app module's `applicationId`, such as `com.yourcompany.yourapp`. It is different from the ATTRACK App ID. Every value beginning with `YOUR_` below is a placeholder—replace it with your app's value.

Requires Android 5.0+ (`minSdk 21`), `compileSdk 35` or higher, Android Gradle Plugin 8.6+ and JDK 17. For `compileSdk 36`, use AGP 8.9+.

## 1. Add the dependency

Add this Maven entry to the existing repositories in **settings.gradle.kts**:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://nexus.adforus.com/repository/attrack/") }
    }
}
```

Then add this line in your app module's **build.gradle.kts** and sync Gradle:

```kotlin
dependencies {
    implementation("com.adforus.sdk:attrack:1.1.0")
}
```

Gradle downloads the SDK and its Install Referrer, Advertising ID and WorkManager dependencies automatically. No repository login is needed. The SDK also adds `INTERNET` and `com.google.android.gms.permission.AD_ID` through manifest merging; an existing identical permission declaration does not conflict.

## 2. Initialize once when your app starts

In your `Application.onCreate()`, call:

```kotlin
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
```

If you do not have an `Application` class yet, create one:

```kotlin
import android.app.Application
import kr.co.attrack.tracker.Tracker

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
    }
}
```

Register that class in your existing `AndroidManifest.xml` application element:

```xml
<application android:name=".MyApplication">
    <!-- Your existing activities and other declarations -->
</application>
```

Use the full class name if `MyApplication` is outside your app's namespace. If your app already has an `Application` class, add the call there and keep its existing manifest entry.

**That completes basic integration.** The SDK records the install, fetches your event definitions and retries queued work automatically. You do not need a listener, an initialization callback, or SDK-specific backup rules. SDK state stays in Android's private no-backup storage.

Pass the client key as an argument—never as manifest metadata. In your real app, obtain it from your private build or runtime configuration; do not commit a real key to a public repository. Replace the placeholders above with that configuration. After a key rotation, supply the new key at the next process start. An embedded client key is extractable from an app; it is not a server administrator credential or device attestation.

## 3. Send an event when it happens

Use an event name and parameters registered for your app in the partner console. For example, if you registered `sign_up` with a string parameter named `method`:

```kotlin
Tracker.logEvent("sign_up", mapOf("method" to "email"))
```

Call this after `initialize()`; you do not need to wait for a network response. The published SDK queues it locally and the server validates it against your registered event definition. Do not send `install` yourself—the SDK owns that event.

The returned transaction ID means the event was **queued**, not yet accepted by the server. A `null` return means local rejection. Check the partner console's logs to confirm server receipt. Debug-signed traffic appears in logs but is excluded from production statistics and postbacks.

## Optional: see initialization and delivery results

A listener lets you observe results for troubleshooting. It does not start the SDK or gate event collection. Register it before initialization only if you want to observe the earliest results:

```kotlin
Tracker.setResultListener { result ->
    // Observe result.code and result.message here.
    // Keep this callback short; post UI changes to the main thread.
}
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
```

The two calls are separate. **Do not put `initialize()` inside the listener.** Result callbacks run on the SDK result thread. Remove the listener with `Tracker.setResultListener(null)`.

`initializeWithResult(...)` is an alternative if you need an immediate validation result. `INITIALIZATION_STARTED` means background setup was accepted; `INITIALIZED` arrives later if the server accepts initialization. `logEventWithResult(...)` adds strict validation against the cached event schema; use it when you deliberately need that check. The simple calls above are sufficient for normal integration.

## Optional: deep links

Follow the [deep-link guide](DEEP_LINKS.md) if links should open a particular screen in your app. It explains the manifest entry and `Tracker.handleDeepLink(...)`. Deep-link setup is separate from install and event tracking.

## Java

```java
import kr.co.attrack.tracker.Tracker;

// In your Application.onCreate(), after super.onCreate():
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY");

// When your registered event happens:
Tracker.logEvent("sign_up", java.util.Collections.singletonMap("method", "email"));
```

## Delivery and troubleshooting

- Offline events survive ordinary process restarts and app updates. Delivery resumes when Android allows network work. Force-stopping an app prevents background work until it is opened again.
- The outbox holds 500 custom events. If it fills, the oldest are removed and recorded in a bounded diagnostic queue. Permanent server rejections are not retried indefinitely. This is bounded retry delivery, not a guarantee against disk failure or uninstall.
- `Tracker.installAttribution()` may be `null` before Play responds or when no referrer is available. Missing attribution is **unknown**, not proof of an organic install.
- For identity errors, check `applicationId` and the certificate of the **installed build** against your registration. A debug build often has a different certificate.
- For event errors, check the registered event name, required fields and types. For authentication errors, check the current client key and app status.
- The Maven SDK uses `https://api.attrack.co.kr`. Changing the sample application's endpoint setting does not change the published SDK.

See the [API reference](API_REFERENCE.en.md) for statuses, result codes and optional methods.
