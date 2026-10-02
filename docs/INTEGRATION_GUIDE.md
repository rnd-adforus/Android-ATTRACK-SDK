# 내 Android 앱에 ATTRACK 연동하기

ATTRACK은 앱 설치를 자동으로 기록하고 앱에서 발생한 이벤트를 전송합니다. **의존성을 추가하고 초기화 함수를 한 번 호출하면 기본 연동이 끝납니다.** 딥링크와 결과 리스너는 필요할 때만 추가하세요.

[English](INTEGRATION_GUIDE.en.md)

## 시작 전 준비

ATTRACK에 **실제로 연동할 앱**을 등록하고 파트너 콘솔에서 **앱 ID**(`app_…`)와 **클라이언트 키**를 받으세요. 등록한 패키지 이름과 서명 인증서는 기기에 설치할 앱과 일치해야 합니다. Google Play 배포 앱은 업로드 인증서가 아닌 **앱 서명 인증서**를 등록합니다.

패키지 이름은 앱 모듈의 `applicationId`입니다. 예를 들어 `com.yourcompany.yourapp`이며 ATTRACK 앱 ID와는 다릅니다. 아래 `YOUR_`로 시작하는 값은 모두 설명용 자리 표시자이므로 본인 앱의 값으로 바꾸세요.

지원 환경: Android 5.0 이상(`minSdk 21`), `compileSdk 35` 이상, Android Gradle Plugin 8.6 이상, JDK 17. `compileSdk 36` 사용 시 AGP 8.9 이상이 필요합니다.

## 1. SDK 의존성 추가

프로젝트의 **settings.gradle.kts**에 있는 저장소 목록에 Maven 주소를 추가하세요.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://nexus.adforus.com/repository/attrack/") }
    }
}
```

앱 모듈의 **build.gradle.kts**에 다음 의존성을 추가하고 Gradle Sync를 실행하세요.

```kotlin
dependencies {
    implementation("com.adforus.sdk:attrack:1.0.0")
}
```

Gradle이 SDK와 Install Referrer, 광고 ID, WorkManager 라이브러리를 함께 내려받습니다. 저장소 로그인은 필요하지 않습니다. `INTERNET`, `com.google.android.gms.permission.AD_ID` 권한도 SDK manifest에서 자동 병합됩니다. 앱에 동일한 권한이 이미 선언되어 있어도 충돌하지 않습니다.

## 2. 앱 시작 시 한 번 초기화

앱의 `Application.onCreate()`에서 다음 함수를 호출하세요.

```kotlin
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
```

아직 `Application` 클래스가 없다면 아래처럼 만드세요.

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

기존 `AndroidManifest.xml`의 application 요소에 클래스를 등록하세요.

```xml
<application android:name=".MyApplication">
    <!-- 기존 액티비티 등 앱 선언 -->
</application>
```

클래스가 앱 namespace 밖에 있으면 전체 클래스 이름을 입력하세요. 이미 `Application` 클래스를 사용 중이라면 해당 클래스에 초기화 호출만 추가하고 기존 manifest 설정을 유지하면 됩니다.

**기본 연동은 여기까지입니다.** SDK가 설치 기록, 이벤트 정의 조회, 대기 중인 이벤트 재전송을 처리합니다. 초기화 콜백이나 리스너를 기다릴 필요가 없습니다. SDK 상태는 Android의 비공개 백업 제외 영역에 저장되므로 SDK를 위한 백업 규칙도 추가할 필요가 없습니다.

클라이언트 키는 manifest가 아닌 **초기화 인자**로 전달합니다. 실제 앱에서는 비공개 빌드 설정이나 런타임 설정에서 키를 가져와 위 자리 표시자를 대체하세요. 실제 키를 공개 저장소에 커밋하지 마세요. 키를 교체하면 다음 앱 프로세스 시작 때 새 키를 전달합니다. 앱에 포함된 클라이언트 키는 추출될 수 있으므로 관리자 인증 정보나 기기 무결성 증명으로 취급하지 않습니다.

## 3. 이벤트가 발생할 때 전송

파트너 콘솔에 등록한 이벤트 이름과 파라미터를 사용하세요. 예를 들어 `sign_up` 이벤트에 문자열 `method`를 등록했다면:

```kotlin
Tracker.logEvent("sign_up", mapOf("method" to "email"))
```

`initialize()` 호출 뒤부터 사용할 수 있으며 서버 응답을 기다릴 필요는 없습니다. 배포 SDK는 이벤트를 기기에 저장하고 서버가 등록된 이벤트 정의에 맞는지 검증합니다. `install`은 SDK가 자동 전송하므로 직접 보내지 마세요.

반환된 트랜잭션 ID는 **기기에 저장됐다는 뜻**이며 서버 수신 완료를 뜻하지 않습니다. `null`이면 로컬에서 거부된 것입니다. 서버 수신 여부는 파트너 콘솔의 로그에서 확인하세요. 디버그 서명으로 전송한 데이터는 로그에는 표시되지만 운영 통계와 포스트백에는 포함되지 않습니다.

## 선택 사항: 초기화·전송 결과 확인

결과 리스너는 문제를 확인할 때 쓰는 관찰 기능입니다. 리스너가 SDK를 시작하거나 이벤트 수집을 허용하는 것은 아닙니다. 초기 결과까지 확인하려면 초기화보다 먼저 등록하세요.

```kotlin
Tracker.setResultListener { result ->
    // result.code, result.message로 결과를 확인합니다.
    // 짧게 처리하고, 화면 갱신은 메인 스레드로 전달하세요.
}
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY")
```

두 함수는 별도로 호출합니다. **리스너 안에서 `initialize()`를 호출하지 마세요.** 로컬 검증 결과는 호출 스레드에서, 네트워크 결과는 SDK 작업 스레드에서 전달될 수 있습니다. 리스너를 해제할 때는 `Tracker.setResultListener(null)`을 호출합니다.

즉시 검증 결과가 필요한 경우 `initializeWithResult(...)`를 사용할 수 있습니다. `INITIALIZATION_STARTED`는 로컬 시작 완료이고, 서버가 초기화를 승인하면 나중에 `INITIALIZED`가 전달됩니다. `logEventWithResult(...)`는 캐시된 이벤트 정의를 엄격하게 검증하고 싶을 때 사용하는 선택 API입니다. 일반적인 연동은 앞의 간단한 함수만으로 충분합니다.

## 선택 사항: 딥링크

링크를 눌렀을 때 앱의 특정 화면을 열려면 [딥링크 가이드](DEEP_LINKS.md)를 따르세요. Manifest 설정과 `Tracker.handleDeepLink(...)` 사용법을 설명합니다. 기본 설치·이벤트 수집과는 별도 기능입니다.

## Java

```java
import kr.co.attrack.tracker.Tracker;

// Application.onCreate()에서 super.onCreate() 호출 뒤:
Tracker.initialize(this, "YOUR_APP_ID", "YOUR_CLIENT_KEY");

// 등록한 이벤트 발생 시:
Tracker.logEvent("sign_up", java.util.Collections.singletonMap("method", "email"));
```

## 전송 방식과 문제 확인

- 오프라인 이벤트는 일반적인 프로세스 재시작과 앱 업데이트 후에도 유지됩니다. Android가 네트워크 작업을 허용하면 재전송합니다. 사용자가 앱을 강제 종료한 경우 다시 열기 전까지 백그라운드 작업이 중단됩니다.
- 사용자 이벤트는 최대 500개까지 보관합니다. 가득 차면 가장 오래된 이벤트를 제거하고 제한된 진단 큐에 기록합니다. 서버가 영구 거부한 이벤트는 무한 재시도하지 않습니다. 디스크 장애나 앱 삭제까지 포함하는 무손실 보장은 아닙니다.
- `Tracker.installAttribution()`이 `null`이면 아직 Play 응답이 없거나 referrer를 얻지 못한 상태입니다. **미확인**이며 자연 유입으로 단정할 수 없습니다.
- 앱 식별 오류는 등록된 `applicationId` 및 **실제로 설치한 빌드**의 서명 인증서를 확인하세요. 디버그 빌드는 인증서가 다를 수 있습니다.
- 이벤트 오류는 이름, 필수 필드와 타입을, 인증 오류는 현재 키와 앱 활성 상태를 확인하세요.
- Maven SDK는 `https://api.attrack.co.kr`을 사용합니다. 샘플 앱의 주소 설정으로 배포 SDK의 서버 주소가 바뀌지는 않습니다.

전체 메서드와 상태·결과 코드는 [API 레퍼런스](API_REFERENCE.ko.md)를 참고하세요.
