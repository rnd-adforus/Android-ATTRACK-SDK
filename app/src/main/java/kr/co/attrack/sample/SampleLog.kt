package kr.co.attrack.sample

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kr.co.attrack.tracker.DynamicLinkCode
import kr.co.attrack.tracker.DynamicLinkResult
import kr.co.attrack.tracker.TrackerResult
import org.json.JSONObject

/** One line in the on-screen log. */
data class LogEntry(val atMillis: Long, val title: String, val detail: String, val ok: Boolean)

/**
 * Everything the SDK reports, shown on the Log screen and written to logcat as
 * one JSON object per line under the `AttrackSample` tag.
 *
 * The JSON lines let a script follow the app without screen scraping:
 * `adb logcat -s AttrackSample -v raw`. Nothing secret is written — no client
 * key, GAID, raw referrer, link token or full link URL.
 */
object SampleLog {
    const val TAG = "AttrackSample"
    private const val MAX_ENTRIES = 200

    private val mutableEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = mutableEntries

    /** Called from the SDK result listener, usually on the SDK worker thread. */
    fun result(result: TrackerResult) {
        val json = JSONObject()
            .put("operation", result.operation.name)
            .put("code", result.code.name)
            .put("success", result.success)
            .put("retryable", result.retryable)
            .put("event", result.eventName ?: "")
            .put("transaction_id", result.transactionId ?: "")
            .put("ingest_id", result.ingestId ?: "")
            .put("http", result.httpStatus)
            .put("field_errors", JSONObject(result.fieldErrors))
            .put("message", result.message.take(300))
        emit("result", json)
        val subject = listOfNotNull(result.eventName?.takeIf { it.isNotEmpty() }, result.transactionId)
            .joinToString(" · ")
        add(
            title = result.code.name,
            detail = listOf(subject, result.message, fieldErrors(result.fieldErrors))
                .filter { it.isNotEmpty() }.joinToString("\n"),
            ok = result.success,
        )
    }

    fun link(result: DynamicLinkResult, destination: Destination?, acknowledged: Boolean, duplicate: Boolean) {
        val data = result.data
        emit(
            "link",
            JSONObject()
                .put("code", result.code.name)
                .put("retryable", result.retryable)
                .put("link_id", data?.linkId ?: "")
                .put("deferred", data?.isDeferred ?: false)
                // Presence only: click IDs and handoff tokens are not written to logs.
                .put("click_context", !data?.clickId.isNullOrEmpty())
                .put("sender", data?.sender ?: "")
                .put("parameters", JSONObject(data?.parameters ?: emptyMap<String, String>()))
                .put("destination", destination?.toString() ?: "")
                .put("acknowledged", acknowledged)
                .put("duplicate_handoff", duplicate)
                .put("message", result.message.take(300)),
        )
        add(
            title = "Link ${result.code.name}",
            detail = listOfNotNull(
                data?.let { "${if (it.isDeferred) "deferred" else "direct"} ${it.linkId} ${it.parameters}" },
                destination?.let { "→ $it" },
                result.message.takeIf { it.isNotEmpty() },
            ).joinToString("\n"),
            ok = result.code == DynamicLinkCode.RESOLVED || result.code == DynamicLinkCode.NO_LINK,
        )
    }

    fun note(title: String, detail: String = "", ok: Boolean = true) = add(title, detail, ok)

    /** One machine-readable line. `kind` separates results, links and debug commands. */
    fun emit(kind: String, json: JSONObject) {
        Log.i(TAG, json.put("kind", kind).put("at", System.currentTimeMillis()).toString())
    }

    private fun add(title: String, detail: String, ok: Boolean) {
        val entry = LogEntry(System.currentTimeMillis(), title, detail, ok)
        mutableEntries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    private fun fieldErrors(errors: Map<String, String>): String =
        if (errors.isEmpty()) "" else errors.entries.joinToString(", ", prefix = "fields: ") { "${it.key}=${it.value}" }
}
