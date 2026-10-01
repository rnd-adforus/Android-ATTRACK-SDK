# ATTRACK Android SDK

앱 설치를 자동 기록하고 앱에서 발생한 이벤트를 전송하는 Android SDK입니다.

[English](README.en.md)

본인 앱의 패키지와 서명 인증서를 등록하고 파트너 콘솔에서 앱 ID와 클라이언트 키를 받으세요. 아래 `YOUR_APP_ID`, `YOUR_CLIENT_KEY`를 본인 값으로 바꿉니다. Android 5.0 이상, compileSdk 35 이상, AGP 8.6 이상, JDK 17이 필요합니다.

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
    implementation("com.adforus.sdk:attrack:1.0.2")
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

## 더 알아보기

- [연동 가이드](docs/INTEGRATION_GUIDE.md): 지원 환경, 결과 확인, 전송 방식과 문제 해결
- [딥링크](docs/DEEP_LINKS.md): 링크로 앱의 특정 화면 열기
- [API 레퍼런스](docs/API_REFERENCE.ko.md): 전체 공개 메서드
- [샘플 템플릿 실행](docs/RUN_SAMPLE.ko.md): 필요한 경우에만 사용하세요. 본인 앱의 패키지, 인증서와 키로 설정하며 공용 데모 계정은 제공하지 않습니다.

## 라이선스

이 저장소의 샘플 코드와 문서는 [MIT License](LICENSE)로 공개됩니다. ATTRACK SDK
라이브러리(`com.adforus.sdk:attrack`)는 이 저장소에 포함되지 않으며 별도의
조건으로 배포됩니다.
