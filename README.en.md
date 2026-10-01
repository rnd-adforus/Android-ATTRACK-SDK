# ATTRACK Android SDK

Track installs automatically and send in-app events from your Android app.

[한국어](README.md)

Register your own package and signing certificate with ATTRACK, then get your App ID and client key from the partner console. Replace `YOUR_APP_ID` and `YOUR_CLIENT_KEY` below. Android 5.0+, compileSdk 35+, AGP 8.6+ and JDK 17 are required.

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
    implementation("com.adforus.sdk:attrack:1.0.2")
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

## Next steps

- [Integration guide](docs/INTEGRATION_GUIDE.en.md): requirements, results, delivery and troubleshooting.
- [Deep links](docs/DEEP_LINKS.en.md): open the right screen from a link.
- [API reference](docs/API_REFERENCE.en.md): all public methods.
- [Run the sample template](docs/RUN_SAMPLE.en.md): optional; use your own registered package, certificate and credentials. No shared demo account is provided.

## License

The sample code and documentation in this repository are released under the
[MIT License](LICENSE). The ATTRACK SDK library (`com.adforus.sdk:attrack`)
is not part of this repository and is distributed under its own terms.
