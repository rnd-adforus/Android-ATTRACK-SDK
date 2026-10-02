# Deep links — ATTRACK Android SDK 1.0.0

ATTRACK links open your app and hand it the parameters defined for the link.

- **App installed:** Android opens the app directly (a verified App Link) and the
  SDK delivers the link to your launch Activity.
- **App not installed:** the user installs the app from Google Play. On the first
  open the SDK delivers the same link (a **deferred** link).

Complete [Get started](INTEGRATION_GUIDE.en.md) first. Links do not require
`Tracker.initialize`, but the SDK dependency must be in place.

[한국어](DEEP_LINKS.md)

## 1. Declare the link Activity

`TrackerLinkActivity` is part of the SDK. Add this intent filter for it inside
`<application>`, using your package name:

**“Your package name” means the installed app's `applicationId`** from your app
module's `build.gradle.kts` (`defaultConfig { applicationId = "…" }`). It is also
the `id` in your Google Play listing URL. It is not the SDK package
`kr.co.attrack.tracker`, your Kotlin/Java `namespace`, or an ATTRACK App ID
(`app_…`).

For example, if your own registered `applicationId` is `com.yourcompany.yourapp`,
Gradle expands `/l/${applicationId}/` to `/l/com.yourcompany.yourapp/`. Your link
then looks like `https://api.attrack.co.kr/l/com.yourcompany.yourapp/LINK_ID`.
Use the exact package registered for your app, including any build variant
suffix; a different package needs its own matching registration and credentials.
Keep `kr.co.attrack.tracker.TrackerLinkActivity` unchanged: that is an SDK class.
See [Android application IDs](https://developer.android.com/build/configure-app-module).

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
            android:pathPrefix="/l/${applicationId}/" />
    </intent-filter>
</activity>
```

- Keep the host `api.attrack.co.kr` and the path `/l/<your package>/`.
- The SDK already sets `noHistory`, an empty `taskAffinity` and a no-display
  theme on this Activity; do not override them.
- For verification to succeed, the certificate that signs your published app must
  be registered for your app. For Google Play builds that is the **Play app
  signing** certificate, not your upload key.

`TrackerLinkActivity` checks the link and forwards it to your app's launch
Activity (the one with `MAIN`/`LAUNCHER`). Links for another app or another host
are ignored.

## 2. Handle links in your launch Activity

Call `Tracker.handleDeepLink` from both `onCreate` and `onNewIntent`, after your
UI is ready. Declaring the Activity `android:launchMode="singleTop"` makes a link
that arrives while the app is open go to `onNewIntent`.

**Kotlin**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Set up your UI first.
    if (savedInstanceState == null) handleLink(intent)
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
            val routed = routeToScreen(data.parameters)
            if (routed && data.isDeferred) Tracker.acknowledgeDeepLink(this, data)
        }
    }
}
```

**Java**

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Set up your UI first.
    if (savedInstanceState == null) handleLink(getIntent());
}

@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleLink(intent);
}

private void handleLink(Intent intent) {
    Tracker.handleDeepLink(this, intent, result -> {
        PendingDynamicLinkData data = result.getData();
        if (result.getCode() == DynamicLinkCode.RESOLVED && data != null) {
            boolean routed = routeToScreen(data.getParameters());
            if (routed && data.isDeferred()) Tracker.acknowledgeDeepLink(this, data);
        }
    });
}
```

`savedInstanceState == null` stops a screen rotation from handling the same link
again. The callback runs on the main thread.

## 3. Route with the link's parameters

`routeToScreen` is your own code. `data.parameters` is a `Map<String, String>` of
the values defined for the link, for example `screen=offer` and
`offer_id=summer-42`. The SDK gives the keys no meaning of its own.

- Treat parameters as untrusted input: allow only known screens and validate every
  value. Fall back to your home screen for anything unknown.
- A link with no parameters should open your normal home screen.
- A parameter must never grant a reward or skip your login or authorization checks.
- Return `true` only once navigation has actually happened. If navigation is
  asynchronous or waits for login, acknowledge in its completion handler.

Links carry up to 20 parameters; names are up to 64 URL-safe characters and
values up to 256 characters.

## 4. Acknowledge deferred links

A deferred link is delivered again on every launch until you acknowledge it:

- Call `Tracker.acknowledgeDeepLink(context, data)` only after a **deferred** link
  (`data.isDeferred == true`) was routed successfully. Direct links need no
  acknowledgement.
- `false` means the acknowledgement was not saved; the link will be delivered
  again.
- Use `data.handoffId` as a stable key: if you have already navigated for that
  handoff, do not navigate again, just acknowledge.
- The pending link survives process death and reboots, and is cleared when the
  app is uninstalled. It stays available for a limited time after the click
  (30 days by default).

## 5. Result codes

| `DynamicLinkCode` | Meaning | What to do |
|---|---|---|
| `RESOLVED` | A link was found | Route with `data.parameters`; acknowledge if `isDeferred` |
| `NO_LINK` | Normal launch, no link | Nothing |
| `INVALID_LINK` | Unknown, disabled or foreign link | Stay on a safe screen |
| `EXPIRED` | The link has expired | Stay on a safe screen |
| `UNAVAILABLE` | Network or service unavailable | If `retryable`, call `handleDeepLink` again on the next foreground; never in a loop |

`DynamicLinkResult` also has `message` (for logs, not for branching) and
`retryable`. `PendingDynamicLinkData` has `parameters`, `linkId`, `isDeferred`,
`handoffId`, `clickId`, `sender` and `link`.

## 6. Browsers and Google Play

- A verified App Link opens the app directly from most apps and browsers.
- When a link opens in a browser instead, the link page tries to open the app and
  otherwise sends the user to Google Play. Some browsers, and some in-app browsers,
  block opening apps automatically; the page then shows buttons to open the app or
  the store.
- After a Play install, the user must open the app once; the SDK then delivers the
  link. The SDK cannot open the app by itself after installation.
- Deferred links work only for installs from Google Play. A sideloaded APK reports
  `NO_LINK` on first open.

## 7. Test

```bash
# Ask Android to (re)verify your App Link domain, then check the result.
APP_PACKAGE="com.yourcompany.yourapp"  # your registered applicationId
LINK_ID="YOUR_LINK_ID"
adb shell pm verify-app-links --re-verify "$APP_PACKAGE"
adb shell pm get-app-links "$APP_PACKAGE"      # expect: api.attrack.co.kr: verified

# Open a link (use a real link ID for your app).
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/$APP_PACKAGE/$LINK_ID"
```

Check:

- the app closed (cold start) and open (warm, arrives in `onNewIntent`);
- a link with parameters and one without;
- rotation after routing (no second navigation);
- no network, then retry on the next foreground (`UNAVAILABLE`);
- a deferred link: install from Google Play (for example an internal testing
  track) through a link, open once, and confirm the link is not delivered again
  after acknowledgement.

## 8. Troubleshooting

| Symptom | Check |
|---|---|
| A browser or Google Play opens instead of the app | `pm get-app-links` must show `verified`. Check the package name in `pathPrefix`, the host, and that the signing certificate of the installed build is registered for your app |
| The app opens but no callback arrives | `handleDeepLink` must be called from the launch Activity's `onCreate` **and** `onNewIntent` |
| `parameters` is empty | Valid for a home link. Otherwise check the link's defined parameters |
| A deferred link keeps coming back | Call `acknowledgeDeepLink` after routing succeeds; use `handoffId` to avoid navigating twice |
| `INVALID_LINK` | The link ID, package or host is wrong, or the link is disabled |
| `EXPIRED` | The link has expired |
| `UNAVAILABLE` | No network or the service is unreachable; retry on the next foreground |

- [Android App Links](https://developer.android.com/training/app-links)
- [Google Play Install Referrer](https://developer.android.com/google/play/installreferrer)
