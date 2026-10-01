# ATTRACK Android SDK API


모든 메서드는 `kr.co.attrack.tracker.Tracker`의 static 메서드이며, release
빌드에서는 예외를 던지지 않습니다.

Maven 배포본은 SDK의 release variant입니다. 앱을 debug로 빌드해도 SDK가 debug variant로 바뀌지는 않습니다.

| 메서드 | 반환 | 설명 |
|---|---|---|
| `initialize(context, appId, clientKey)` | — | SDK 시작. debug 빌드는 설정 오류 시 예외 |
| `initializeWithResult(context, appId, clientKey)` | `TrackerResult` | SDK를 시작하고 즉시 결과 반환 |
| `setResultListener(listener)` | — | 선택 기능. 로컬 결과는 호출 스레드, 네트워크 결과는 작업 스레드에서 수신. `null`이면 해제 |
| `logEvent(name, params)` | `String?` | 이벤트 저장, `transaction_id` 반환(거부 시 `null`) |
| `logEventWithResult(name, params)` | `TrackerResult` | 엄격한 검사 후 저장, `EVENT_QUEUED` 또는 오류 코드 |
| `status()` | `TrackerStatus` | 현재 SDK 상태 |
| `eventSchema()` | `List<TrackerEventDefinition>` | 앱이 보낼 수 있는 이벤트 |
| `eventDefinition(name)` | `TrackerEventDefinition?` | 한 이벤트의 파라미터, 없으면 `null` |
| `describeEventSchema()` | `String` | 전체 이벤트를 한 줄씩 |
| `eventSchemaVersion()` | `String` | 캐시된 스키마 버전 |
| `installAttribution()` | `InstallAttribution?` | 이 설치의 Play Install Referrer |
| `handleDeepLink(context, intent, callback)` | — | 링크·지연 링크 해석, callback은 메인 스레드 |
| `acknowledgeDeepLink(context, data)` | `Boolean` | 지연 링크 처리 완료 표시, `false`면 대기 유지 |

`params`는 선택입니다(기본값: 빈 map).

**`TrackerStatus`**

| 값 | 의미 |
|---|---|
| `NOT_STARTED` | 초기화 전이거나 기기에서 초기화 실패 |
| `INITIALIZING` | 서버와 연결 중 |
| `ACTIVE` | 실행 중, 이벤트 전송 |
| `DISABLED` | 앱의 추적이 비활성화됨. 이벤트는 다음 시작을 위해 보관 |
| `BLOCKED` | 식별 정보 불일치 또는 정책 차단. 다음 시작까지 전송 중단 |

**`InstallAttribution`**: `status`(`ok` 또는 `unavailable`), `clickId`, `utmSource`,
`utmMedium`, `utmCampaign`, `utmContent`, `utmTerm`, `sender`, `publisherId`,
`referrerClickSeconds`, `installBeginSeconds`, `raw`, `params`.

**`DynamicLinkResult`**: `code`, `data`(`PendingDynamicLinkData?`), `message`,
`retryable`.

**`PendingDynamicLinkData`**: `parameters`(링크 파라미터), `linkId`, `isDeferred`,
`handoffId`(지연 링크 하나의 고정 키), `clickId`·`sender`(이 링크로 이어진 클릭,
참고용), `link`.

**`TrackerLinkActivity`**: 검증된 링크를 받는 SDK Activity. 딥링크 가이드에 따라 Manifest에 선언하며
직접 실행하지 마세요.


[연동 가이드](INTEGRATION_GUIDE.md)
