# 샘플 템플릿 실행하기

실행해 보고 그대로 가져다 쓸 수 있는 전체 연동 예시입니다.

| 지연 링크 (설치 후 첫 실행) | 직접 링크 (앱 설치됨) |
|---|---|
| <img src="images/deferred-link.png" width="260" alt="지연 링크로 열리고 확인된 Offer 화면"> | <img src="images/direct-link.png" width="260" alt="직접 링크로 열린 Product 화면"> |

### 실행 방법

1. 본인 앱의 `applicationId`(설치 패키지)를 사용하세요. 이 샘플은 본인 앱에
   맞게 바꿔 사용하는 템플릿입니다. 해당 패키지의 App ID와 client key를 사용하고,
   설치할 빌드의 서명 인증서가 등록되어 있어야 합니다(`./gradlew signingReport`).
   Play 설치는 업로드 키가 아닌 Play 앱 서명 인증서를 등록하세요.
   공용 테스트 앱, 공용 키, 서버의 샘플용 앱 등록은 제공하지 않습니다.
   다음 이벤트를 본인 앱에 등록하세요.

   | 이벤트 | 집계 | 파라미터 |
   |---|---|---|
   | `sign_up` | 설치당 1회 | `method: string` |
   | `tutorial_complete` | 설치당 1회 | — |
   | `level_complete` | 매번 | `level: number`, `score: number`, `perfect: bool` |
   | `purchase` | 매번 | `sku: string`, `revenue: number`, `currency: string`, `order_id: string` |

2. `local.properties`(git 제외)에 값을 추가합니다.

   ```properties
   attrack.packageName=com.yourcompany.yourapp
   attrack.appId=app_xxxxxxxxxxxxxxxxxxxxxxxx
   attrack.clientKey=your-client-key
   ```

   Gradle `-P` 옵션이나 환경 변수 `ATTRACK_PACKAGE_NAME`, `ATTRACK_APP_ID`,
   `ATTRACK_CLIENT_KEY`로도 지정할 수 있습니다. 패키지 설정이 없으면 빌드가 중단됩니다.

3. 템플릿에는 Nexus Maven 저장소와 `com.adforus.sdk:attrack:1.0.0` 의존성이
   이미 설정되어 있습니다. 저장소 URL은 바꿀 필요가 없습니다. Gradle Sync 또는
   빌드 시 SDK와 의존성이 자동으로 다운로드됩니다.
   `attrack.packageName`은 본인 앱에 등록된 정확한 패키지로 바꾸세요.
   `namespace`와 소스 코드의 `kr.co.attrack.sample`은 템플릿 코드 위치이며,
   설치되는 앱의 패키지는 `attrack.packageName`으로 결정됩니다.
   `AndroidManifest.xml`의 `${applicationId}`는 해당 값으로 자동 치환됩니다.

4. 빌드 및 설치:

   ```bash
   ./gradlew :app:installDebug
   ```

App ID나 client key가 없어도 앱은 실행되며 SDK가 시작되지 않은 이유
(`INVALID_APP_ID` 또는 `INVALID_CONFIGURATION`)를 보여 줍니다.

### 화면 구성

| 화면 | 내용 |
|---|---|
| **Overview** | `Tracker.status()`, 시작 결과, 이벤트 스키마, `Tracker.installAttribution()` |
| **Events** | 이벤트별 버튼과 SDK가 기기에서 거부하는 잘못된 예시 |
| **Links** | 최근 링크 결과, 링크 서비스에 연결할 수 없을 때의 재시도 버튼 |
| **Offer / Product** | 링크로 열린 화면과 도착 경로: 직접/지연, 링크 ID, 파라미터, 확인 여부 |
| **Data** | SDK가 수집하는 기기 식별자 |
| **Log** | 모든 SDK 결과, 최신순(logcat: `adb logcat -s AttrackSample`) |

### 연동 코드 위치

| 파일 | 내용 |
|---|---|
| [`SampleApplication.kt`](../app/src/main/java/kr/co/attrack/sample/SampleApplication.kt) | Result listener 설정 후 `Application.onCreate`에서 `initializeWithResult` 1회 호출 |
| [`SampleEvents.kt`](../app/src/main/java/kr/co/attrack/sample/SampleEvents.kt) | 이벤트별 `logEventWithResult` |
| [`MainActivity.kt`](../app/src/main/java/kr/co/attrack/sample/MainActivity.kt) | `onCreate`/`onNewIntent`의 `handleDeepLink`, 지연 링크 확인, `handoffId`, 다음 foreground 재시도 |
| [`DeepLinkRouter.kt`](../app/src/main/java/kr/co/attrack/sample/DeepLinkRouter.kt) | 링크 파라미터의 허용 목록 기반 화면 이동(단위 테스트 포함) |
| [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | Application 등록, 내 applicationId 범위의 링크 필터 |

### 링크 테스트

```bash
APP_PACKAGE="com.yourcompany.yourapp"  # 본인 앱에 등록된 applicationId
LINK_ID="YOUR_LINK_ID"
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://api.attrack.co.kr/l/$APP_PACKAGE/$LINK_ID"
```

본인 앱에 생성된 링크 ID를 사용하세요. 지연 링크는 Google Play 설치(예: 내부
테스트 트랙)가 필요하며, 직접 설치한 APK는 `NO_LINK`를 반환합니다.
