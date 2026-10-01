# ATTRACK Android SDK

Install attribution, in-app event tracking and deep links for Android apps.

- Attributes installs from Google Play (Install Referrer), sent automatically
- Logs in-app events checked against your app's registered event schema
- Opens your app from links, including the first open after an install (deferred links)
- Delivers reliably: events are saved on the device and retried after offline
  periods, restarts and process death

This repository contains the **documentation** and a **sample app**. The SDK
itself is distributed as a library (`kr.co.attrack:attrack-tracker`).

[한국어 README](README.ko.md)

## Documentation

| | English | 한국어 |
|---|---|---|
| Get started: setup, events, results, API reference | [Get started](docs/INTEGRATION_GUIDE.en.md) | [시작하기](docs/INTEGRATION_GUIDE.ko.md) |
| Deep links and deferred deep links | [Deep links](docs/DEEP_LINKS.md) | [딥링크](docs/DEEP_LINKS.ko.md) |

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

**1. Add the dependency**

```kotlin
dependencies {
    implementation("kr.co.attrack:attrack-tracker:0.5.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}
```

**2. Add your client key to the manifest**

```xml
<meta-data
    android:name="kr.co.attrack.sdk.CLIENT_KEY"
    android:value="${attrackClientKey}" />
```

**3. Initialize in your `Application`**

```kotlin
Tracker.setResultListener { result -> Log.d("ATTRACK", "${result.code}") }
Tracker.initializeWithResult(this, "app_xxxxxxxxxxxxxxxxxxxxxxxx")
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

1. You need an App ID and client key for the package `kr.co.attrack.sample`, with
   the certificate that signs your build registered for it
   (`./gradlew signingReport` prints it), and these events registered:

   | Event | Counted | Parameters |
   |---|---|---|
   | `sign_up` | once per installation | `method: string` |
   | `tutorial_complete` | once per installation | — |
   | `level_complete` | every event | `level: number`, `score: number`, `perfect: bool` |
   | `purchase` | every event | `sku: string`, `revenue: number`, `currency: string`, `order_id: string` |

2. Add your values to `local.properties` (git-ignored):

   ```properties
   attrack.appId=app_xxxxxxxxxxxxxxxxxxxxxxxx
   attrack.clientKey=your-client-key
   ```

   They also work as `-P` Gradle flags or as the environment variables
   `ATTRACK_APP_ID` and `ATTRACK_CLIENT_KEY`.

3. Provide the SDK in one of two ways:
   - **Maven:** the build uses `kr.co.attrack:attrack-tracker:0.5.0`. If it is
     hosted in your own repository, set `attrack.mavenUrl=https://…`.
   - **AAR file:** place `attrack-tracker-0.5.0.aar` in `app/libs/`.

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
| [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) | Client key, backup rules, the link Activity's intent filter |

### Try a link

```bash
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/kr.co.attrack.sample/LINK_ID"
```

Use a link ID created for the sample. Deferred links need an install from Google
Play (for example an internal testing track); a sideloaded APK reports `NO_LINK`.

## License

The sample code and documentation in this repository are released under the
[MIT License](LICENSE). The ATTRACK SDK library (`kr.co.attrack:attrack-tracker`)
is not part of this repository and is distributed under its own terms.
