# ATTRACK Android SDK API


All methods are static on `kr.co.attrack.tracker.Tracker`. None throws in a
published Maven release. This does not depend on whether your host app is debug or release.

| Method | Returns | Description |
|---|---|---|
| `initialize(context, appId, clientKey)` | — | Starts the SDK. The SDK source debug variant throws on configuration errors; the Maven release reports errors |
| `initializeWithResult(context, appId, clientKey)` | `TrackerResult` | Starts the SDK and returns the immediate outcome |
| `setResultListener(listener)` | — | Optional observer; caller thread for local results, worker for network results; `null` removes it |
| `logEvent(name, params)` | `String?` | Queues an event; returns its `transaction_id`, or `null` if rejected |
| `logEventWithResult(name, params)` | `TrackerResult` | Queues an event with strict checks; `EVENT_QUEUED` or an error code |
| `status()` | `TrackerStatus` | Current SDK state |
| `eventSchema()` | `List<TrackerEventDefinition>` | Events this app may send |
| `eventDefinition(name)` | `TrackerEventDefinition?` | One event's parameters, or `null` |
| `describeEventSchema()` | `String` | All events, one per line |
| `eventSchemaVersion()` | `String` | Version of the cached schema |
| `installAttribution()` | `InstallAttribution?` | The Play Install Referrer for this installation |
| `handleDeepLink(context, intent, callback)` | — | Resolves a link or deferred link; callback on the main thread |
| `acknowledgeDeepLink(context, data)` | `Boolean` | Marks a deferred link as handled; `false` keeps it pending |

`params` is optional (default: empty map).

**`TrackerStatus`**

| Value | Meaning |
|---|---|
| `NOT_STARTED` | Not initialized, or initialization failed on the device |
| `INITIALIZING` | Connecting to the server |
| `ACTIVE` | Running; events are delivered |
| `DISABLED` | Tracking is disabled for the app. Events stay saved for a later start |
| `BLOCKED` | Identity mismatch or blocked by policy. Delivery stops until the next start |

**`InstallAttribution`**: `status` (`ok` or `unavailable`), `clickId`, `utmSource`,
`utmMedium`, `utmCampaign`, `utmContent`, `utmTerm`, `sender`, `publisherId`,
`referrerClickSeconds`, `installBeginSeconds`, `raw`, `params`.

**`DynamicLinkResult`**: `code`, `data` (`PendingDynamicLinkData?`), `message`,
`retryable`.

**`PendingDynamicLinkData`**: `parameters` (the link's parameters), `linkId`,
`isDeferred`, `handoffId` (stable key for one deferred link), `clickId` and `sender`
(the click that led here, informational), `link`.

**`TrackerLinkActivity`**: the SDK Activity that receives verified links. Declare
it as described in the deep-link guide; do not start it yourself.


[Integration guide](INTEGRATION_GUIDE.en.md)
