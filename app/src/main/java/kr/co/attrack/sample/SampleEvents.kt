package kr.co.attrack.sample

import kr.co.attrack.tracker.Tracker
import kr.co.attrack.tracker.TrackerResult
import java.util.UUID

/**
 * The events this sample sends. These names and parameter types must be
 * registered for your app before they are sent; every declared parameter is
 * required, and an unregistered name or parameter is refused.
 *
 * | Event              | Conversion            | Parameters                                   |
 * |--------------------|-----------------------|----------------------------------------------|
 * | sign_up            | once_per_installation | method: string                               |
 * | tutorial_complete  | once_per_installation | (none)                                       |
 * | level_complete     | every_event           | level: number, score: number, perfect: bool  |
 * | purchase           | every_event           | sku: string, revenue: number, currency: string, order_id: string |
 */
object SampleEvents {

    fun signUp(method: String = "email"): TrackerResult =
        Tracker.logEventWithResult("sign_up", mapOf("method" to method))

    fun tutorialComplete(): TrackerResult = Tracker.logEventWithResult("tutorial_complete")

    fun levelComplete(level: Int, score: Int, perfect: Boolean): TrackerResult =
        Tracker.logEventWithResult(
            "level_complete",
            mapOf("level" to level, "score" to score, "perfect" to perfect),
        )

    /**
     * Your own order ID is an ordinary parameter. The SDK generates the tracker
     * transaction ID itself and returns it on the result.
     */
    fun purchase(sku: String = "pro_monthly", revenue: Double = 9.99, currency: String = "USD"): TrackerResult =
        Tracker.logEventWithResult(
            "purchase",
            mapOf(
                "sku" to sku,
                "revenue" to revenue,
                "currency" to currency,
                "order_id" to "order_" + UUID.randomUUID().toString().take(8),
            ),
        )

    /**
     * Deliberate mistakes, so the Events screen can show what each local
     * validation code looks like. None of these reach the network.
     */
    val mistakes: List<Pair<String, () -> TrackerResult>> = listOf(
        "Invalid name (spaces)" to { Tracker.logEventWithResult("level complete") },
        "Reserved prefix (google_)" to { Tracker.logEventWithResult("google_purchase") },
        "Not registered" to { Tracker.logEventWithResult("level_start", mapOf("level" to 1)) },
        "Unknown parameter" to {
            Tracker.logEventWithResult("sign_up", mapOf("method" to "email", "referrer" to "friend"))
        },
        "Missing parameter" to { Tracker.logEventWithResult("level_complete", mapOf("level" to 3)) },
        "Wrong type" to {
            Tracker.logEventWithResult(
                "level_complete",
                mapOf("level" to "three", "score" to 900, "perfect" to true),
            )
        },
    )
}
