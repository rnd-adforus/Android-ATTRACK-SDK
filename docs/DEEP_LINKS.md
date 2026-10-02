# 딥링크 — ATTRACK Android SDK 1.0.0

ATTRACK 링크는 앱을 열고 링크에 정의된 파라미터를 앱에 전달합니다.

- **앱이 설치된 경우:** Android가 앱을 바로 열고(검증된 App Link) SDK가 링크를
  시작 Activity에 전달합니다.
- **앱이 설치되지 않은 경우:** 사용자가 Google Play에서 앱을 설치합니다. 첫 실행 시
  SDK가 같은 링크를 전달합니다(**지연** 링크).

먼저 [시작하기](INTEGRATION_GUIDE.md)를 완료하세요. 링크는 `Tracker.initialize`
없이도 동작하지만 SDK 의존성은 필요합니다.

[English](DEEP_LINKS.en.md)

## 1. 링크 Activity 선언

`TrackerLinkActivity`는 SDK에 포함되어 있습니다. `<application>` 안에 실제
패키지명으로 intent filter를 추가합니다.

**“내 패키지 이름”은 설치되는 앱의 `applicationId`입니다.** 앱 모듈의
`build.gradle.kts`에서 `defaultConfig { applicationId = "…" }`를 확인하세요.
Google Play 앱 주소의 `id`와도 같은 값입니다. SDK 패키지 `kr.co.attrack.tracker`,
Kotlin/Java 코드의 `namespace`, ATTRACK App ID(`app_…`)와는 다릅니다.

예를 들어 본인 앱의 등록된 `applicationId`가 `com.yourcompany.yourapp`이면
Gradle이 `/l/${applicationId}/`를 `/l/com.yourcompany.yourapp/`로 바꿉니다.
실제 링크는 `https://api.attrack.co.kr/l/com.yourcompany.yourapp/LINK_ID` 형태입니다.
빌드 variant의 suffix까지 포함하여 앱에 등록된 정확한 패키지를 사용하세요.
패키지가 다르면 그 패키지에 맞는 별도 등록과 자격 증명이 필요합니다.
`kr.co.attrack.tracker.TrackerLinkActivity`는 SDK 클래스이므로 바꾸지 않습니다.
[Android application ID 안내](https://developer.android.com/build/configure-app-module)

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

- 호스트 `api.attrack.co.kr`와 경로 `/l/<패키지명>/`을 유지하세요.
- SDK가 이 Activity에 `noHistory`, 빈 `taskAffinity`, 화면 없는 테마를 이미
  지정하므로 덮어쓰지 마세요.
- 검증에 성공하려면 배포되는 앱의 서명 인증서가 앱에 등록되어 있어야 합니다.
  Google Play 빌드는 업로드 키가 아닌 **Play 앱 서명** 인증서입니다.

`TrackerLinkActivity`는 링크를 확인한 뒤 앱의 시작 Activity(`MAIN`/`LAUNCHER`)로
전달합니다. 다른 앱이나 다른 호스트의 링크는 무시합니다.

## 2. 시작 Activity에서 링크 처리

UI를 준비한 뒤 `onCreate`와 `onNewIntent` 모두에서 `Tracker.handleDeepLink`를
호출합니다. Activity를 `android:launchMode="singleTop"`으로 선언하면 앱이 열려 있을
때 도착한 링크가 `onNewIntent`로 전달됩니다.

**Kotlin**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // UI를 먼저 준비합니다.
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
    // UI를 먼저 준비합니다.
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

`savedInstanceState == null` 조건은 화면 회전 시 같은 링크를 다시 처리하지 않게
합니다. Callback은 메인 스레드에서 실행됩니다.

## 3. 링크 파라미터로 화면 이동

`routeToScreen`은 앱 코드입니다. `data.parameters`는 링크에 정의된 값의
`Map<String, String>`입니다(예: `screen=offer`, `offer_id=summer-42`). SDK는
key에 별도 의미를 부여하지 않습니다.

- 파라미터는 신뢰할 수 없는 입력입니다. 허용된 화면만 열고 모든 값을 검증하며,
  알 수 없는 값은 홈 화면으로 보냅니다.
- 파라미터가 없는 링크는 일반 홈 화면을 엽니다.
- 파라미터만으로 보상을 지급하거나 로그인·권한 확인을 건너뛰지 마세요.
- 실제로 화면 이동이 끝난 뒤에만 `true`를 반환합니다. 비동기 이동이나 로그인 대기가
  있으면 완료 시점에 acknowledge합니다.

링크 파라미터는 최대 20개이며, 이름은 URL-safe 문자 64자, 값은 256자까지입니다.

## 4. 지연 링크 확인(acknowledge)

지연 링크는 확인하기 전까지 매 실행마다 다시 전달됩니다.

- **지연** 링크(`data.isDeferred == true`)로 화면 이동에 성공한 뒤에만
  `Tracker.acknowledgeDeepLink(context, data)`를 호출합니다. 직접 열린 링크는
  확인이 필요 없습니다.
- `false`는 확인이 저장되지 않았다는 뜻이며 링크가 다시 전달됩니다.
- `data.handoffId`를 고정 키로 사용하세요. 이미 이동한 handoff라면 다시 이동하지
  말고 acknowledge만 합니다.
- 대기 중인 링크는 프로세스 종료와 재부팅 후에도 유지되고, 앱 삭제 시 지워집니다.
  클릭 후 일정 기간(기본 30일) 동안 유효합니다.

## 5. 결과 코드

| `DynamicLinkCode` | 의미 | 조치 |
|---|---|---|
| `RESOLVED` | 링크 확인됨 | `data.parameters`로 이동, `isDeferred`면 acknowledge |
| `NO_LINK` | 링크 없는 일반 실행 | 없음 |
| `INVALID_LINK` | 알 수 없거나 비활성화되었거나 다른 앱의 링크 | 안전한 화면 유지 |
| `EXPIRED` | 만료된 링크 | 안전한 화면 유지 |
| `UNAVAILABLE` | 네트워크 또는 서비스 사용 불가 | `retryable`이면 다음 foreground에서 `handleDeepLink` 재호출. 반복 호출 금지 |

`DynamicLinkResult`에는 `message`(log용, 분기 금지)와 `retryable`도 있습니다.
`PendingDynamicLinkData`에는 `parameters`, `linkId`, `isDeferred`, `handoffId`,
`clickId`, `sender`, `link`가 있습니다.

## 6. 브라우저와 Google Play

- 검증된 App Link는 대부분의 앱과 브라우저에서 앱을 바로 엽니다.
- 링크가 브라우저에서 열리면 링크 페이지가 앱 실행을 시도하고, 실패하면 Google
  Play로 이동합니다. 일부 브라우저와 인앱 브라우저는 자동 실행을 막으며, 이때
  페이지에 앱 열기·스토어 버튼이 표시됩니다.
- Play 설치 후 사용자가 앱을 한 번 열어야 SDK가 링크를 전달합니다. 설치 후 SDK가
  앱을 스스로 열 수는 없습니다.
- 지연 링크는 Google Play 설치에서만 동작합니다. 직접 설치한 APK는 첫 실행에서
  `NO_LINK`를 반환합니다.

## 7. 테스트

```bash
# App Link 도메인 검증을 다시 요청하고 결과를 확인합니다.
APP_PACKAGE="com.yourcompany.yourapp"  # 본인 앱에 등록된 applicationId
LINK_ID="YOUR_LINK_ID"
adb shell pm verify-app-links --re-verify "$APP_PACKAGE"
adb shell pm get-app-links "$APP_PACKAGE"      # 기대값: api.attrack.co.kr: verified

# 링크를 엽니다(앱의 실제 링크 ID 사용).
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/$APP_PACKAGE/$LINK_ID"
```

확인 항목:

- 앱이 닫힌 상태(최초 실행)와 열린 상태(`onNewIntent`로 전달)
- 파라미터가 있는 링크와 없는 링크
- 이동 후 화면 회전(두 번 이동하지 않음)
- 네트워크 없음 후 다음 foreground에서 재시도(`UNAVAILABLE`)
- 지연 링크: 링크를 통해 Google Play(예: 내부 테스트 트랙)에서 설치 → 한 번 실행 →
  acknowledge 후 다시 전달되지 않는지 확인

## 8. 문제 해결

| 증상 | 확인 |
|---|---|
| 앱 대신 브라우저나 Google Play가 열림 | `pm get-app-links`가 `verified`여야 합니다. `pathPrefix`의 패키지명, 호스트, 설치된 빌드의 서명 인증서가 앱에 등록되었는지 확인 |
| 앱은 열리지만 callback이 없음 | 시작 Activity의 `onCreate`와 `onNewIntent` **모두**에서 `handleDeepLink` 호출 |
| `parameters`가 비어 있음 | 홈 링크라면 정상. 아니라면 링크에 정의된 파라미터 확인 |
| 지연 링크가 계속 다시 옴 | 이동 성공 후 `acknowledgeDeepLink` 호출, `handoffId`로 중복 이동 방지 |
| `INVALID_LINK` | 링크 ID·패키지·호스트 오류 또는 비활성화된 링크 |
| `EXPIRED` | 만료된 링크 |
| `UNAVAILABLE` | 네트워크 없음 또는 서비스 접근 불가, 다음 foreground에서 재시도 |

- [Android App Links](https://developer.android.com/training/app-links)
- [Google Play Install Referrer](https://developer.android.com/google/play/installreferrer)
