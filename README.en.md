# ATTRACK Android SDK

Install attribution, in-app event tracking and deep links for Android apps.

- Attributes installs from Google Play (Install Referrer), sent automatically
- Logs in-app events checked against your app's registered event schema
- Opens your app from links, including the first open after an install (deferred links)
- Delivers reliably: events are saved on the device and retried after offline
  periods, restarts and process death

This repository contains the **documentation** and a **sample app**. The SDK
itself is distributed as a library (`com.adforus.sdk:attrack`).

[한국어 README](README.md)

## Documentation

| | English | 한국어 |
|---|---|---|
| Get started: setup, events, results, API reference | [Get started](docs/INTEGRATION_GUIDE.en.md) | [시작하기](docs/INTEGRATION_GUIDE.md) |
| Deep links and deferred deep links | [Deep links](docs/DEEP_LINKS.en.md) | [딥링크](docs/DEEP_LINKS.md) |

## Requirements

| | |
|---|---|
| `minSdk` | 21 (Android 5.0) |
| `compileSdk` | 35 or higher (36 recommended) |
| Android Gradle Plugin | 8.6.0+ (8.9.0+ for compileSdk 36) |
| JDK | 17 |
| Languages | Kotlin and Java |

See [Get started](docs/INTEGRATION_GUIDE.en.md#before-you-begin) for the full list.

## Quick start

**1. Add the Maven repository and dependency**

Gradle downloads the ATTRACK SDK directly from the Nexus Maven repository below.
Add the repository to your project's `settings.gradle.kts`. If you already have a
`repositories` block, add the `maven` entry to that block.

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

Add the dependency in your app module's `app/build.gradle.kts`.

```kotlin
dependencies {
    implementation("com.adforus.sdk:attrack:1.0.1")
}
```

For your existing app, run Gradle Sync in Android Studio or build with the command below. Gradle
downloads the SDK and its dependencies (Install Referrer, Advertising ID,
WorkManager) automatically; there is no manual file
download step. Downloading requires no Nexus login or SDK client key.

```bash
./gradlew :app:assembleDebug
```

**2. Supply your app's App ID and current client key**

Load the key from private build configuration or your app's runtime
configuration. This example uses your app's `BuildConfig.ATTRACK_APP_ID` and
`BuildConfig.ATTRACK_CLIENT_KEY`; see [Get started](docs/INTEGRATION_GUIDE.en.md#3-initialize-the-sdk)
for the field definitions.

**3. Initialize in your `Application`**

```kotlin
Tracker.setResultListener { result -> Log.d("ATTRACK", "${result.code}") }
Tracker.initializeWithResult(this, BuildConfig.ATTRACK_APP_ID, BuildConfig.ATTRACK_CLIENT_KEY)
```

**4. Log an event**

```kotlin
Tracker.logEventWithResult("purchase", mapOf("sku" to "pro_monthly", "revenue" to 9.99, "currency" to "KRW"))
```

The install is tracked automatically. Continue with [Get started](docs/INTEGRATION_GUIDE.en.md).

## Sample app

A complete integration you can run and copy from.

| Deferred link (first open after install) | Direct link (app installed) |
|---|---|
| <img src="docs/images/deferred-link.png" width="260" alt="Offer page opened by a deferred link and acknowledged"> | <img src="docs/images/direct-link.png" width="260" alt="Product page opened by a direct link"> |

### Run it

1. Use **your own app's `applicationId` (installed package)**. This sample is
   a template to adapt to your app. Use that package's App ID and client key,
   and register the certificate that signs your installed build
   (`./gradlew signingReport`). Play installs use the Play app-signing certificate.
   There is no shared demo app, shared key, or sample registration on the server.
   Register these events for your own app:

   | Event | Counted | Parameters |
   |---|---|---|
   | `sign_up` | once per installation | `method: string` |
   | `tutorial_complete` | once per installation | — |
   | `level_complete` | every event | `level: number`, `score: number`, `perfect: bool` |
   | `purchase` | every event | `sku: string`, `revenue: number`, `currency: string`, `order_id: string` |

2. Add your values to `local.properties` (git-ignored):

   ```properties
   attrack.packageName=com.yourcompany.yourapp
   attrack.appId=app_xxxxxxxxxxxxxxxxxxxxxxxx
   attrack.clientKey=your-client-key
   ```

   They also work as `-P` Gradle flags or as the environment variables
   `ATTRACK_PACKAGE_NAME`, `ATTRACK_APP_ID` and `ATTRACK_CLIENT_KEY`.
   A package name is required before building.

3. The template already configures the Nexus Maven repository and the
   `com.adforus.sdk:attrack:1.0.1` dependency. Keep the repository URL as configured.
   Gradle downloads the SDK and its dependencies when you sync or build. Set `attrack.packageName` to your exact registered package.
   The source `namespace` and `kr.co.attrack.sample` folders describe the template's
   code; the installed package is set by `attrack.packageName`.
   Gradle automatically expands `${applicationId}` in `AndroidManifest.xml`.

4. Build and install:

   ```bash
   ./gradlew :app:installDebug
   ```

Without an App ID or client key the app still runs and shows why the SDK did not
start (`INVALID_APP_ID` or `INVALID_CONFIGURATION`).

### What it shows

| Screen | Shows |
|---|---|
| **Overview** | `Tracker.status()`, the start result, the event schema and `Tracker.installAttribution()` |
| **Events** | One button per event, plus deliberate mistakes the SDK rejects on the device |
| **Links** | Recent link results, and a retry button when the link service was unreachable |
| **Offer / Product** | The page a link opens, with how you arrived: direct or deferred, link ID, parameters, acknowledgement |
| **Data** | The device identifiers the SDK collects |
| **Log** | Every SDK result, newest first (also in logcat: `adb logcat -s AttrackSample`) |

### Where the integration lives

| File | What it shows |
|---|---|
| [`SampleApplication.kt`](app/src/main/java/kr/co/attrack/sample/SampleApplication.kt) | Result listener, then `initializeWithResult` once in `Application.onCreate` |
| [`SampleEvents.kt`](app/src/main/java/kr/co/attrack/sample/SampleEvents.kt) | `logEventWithResult` for each event |
| [`MainActivity.kt`](app/src/main/java/kr/co/attrack/sample/MainActivity.kt) | `handleDeepLink` in `onCreate`/`onNewIntent`, acknowledging deferred links, `handoffId`, retry on the next foreground |
| [`DeepLinkRouter.kt`](app/src/main/java/kr/co/attrack/sample/DeepLinkRouter.kt) | Allow-listed routing of link parameters (unit-tested) |
| [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) | Backup rules and a link filter scoped to your applicationId |

### Try a link

```bash
APP_PACKAGE="com.yourcompany.yourapp"  # your registered applicationId
LINK_ID="YOUR_LINK_ID"
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/$APP_PACKAGE/$LINK_ID"
```

Use a link ID created for your own registered app. Deferred links need an install from Google
Play (for example an internal testing track); a sideloaded APK reports `NO_LINK`.

## License

The sample code and documentation in this repository are released under the
[MIT License](LICENSE). The ATTRACK SDK library (`com.adforus.sdk:attrack`)
is not part of this repository and is distributed under its own terms.
