package kr.co.attrack.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.flow.MutableStateFlow
import kr.co.attrack.sample.ui.SampleApp
import kr.co.attrack.tracker.DynamicLinkCode
import kr.co.attrack.tracker.Tracker

/**
 * The app's real launch Activity. Links reach it three ways:
 *  - cold start from a link: [onCreate] with a VIEW intent (via the SDK bridge),
 *  - a link while the app is open: [onNewIntent],
 *  - the first open after a Google Play install from a link: [onCreate] with the
 *    normal launcher intent; the SDK reads the Play referrer and replays the link.
 */
class MainActivity : ComponentActivity() {

    /** A link that failed with a retryable error, retried on the next foreground. */
    private val pendingRetry = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampleApp(
                app = application as SampleApplication,
                pendingLinkRetry = pendingRetry,
                onRetryLink = { pendingRetry.value?.let { retry(it) } },
            )
        }
        // Not after a configuration change: the link was already handled.
        if (savedInstanceState == null) handleLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLink(intent)
    }

    override fun onStart() {
        super.onStart()
        // Retry a temporarily unavailable link once per return to the
        // foreground. Never loop on errors.
        pendingRetry.value?.let(::retry)
    }

    private fun retry(intent: Intent) {
        pendingRetry.value = null
        handleLink(intent)
    }

    private fun handleLink(intent: Intent?) {
        // No initialization or advertising consent is needed for navigation.
        Tracker.handleDeepLink(this, intent) { result ->
            // Runs on the main thread.
            val data = result.data
            var destination: Destination? = null
            var acknowledged = false
            var duplicate = false
            when (result.code) {
                DynamicLinkCode.RESOLVED -> if (data != null) {
                    destination = DeepLinkRouter.route(data.parameters)
                    // A deferred link repeats until acknowledged. handoffId makes a
                    // repeat idempotent: do not navigate twice for one handoff.
                    duplicate = data.isDeferred && data.handoffId == lastHandoffId()
                    val arrival = LinkArrival(data.isDeferred, data.linkId, data.parameters, acknowledged = false)
                    if (!duplicate) SampleNavigator.open(destination, arrival)
                    if (data.isDeferred) {
                        rememberHandoffId(data.handoffId)
                        // Acknowledge only after navigation succeeded. A false return
                        // keeps it pending; the next launch delivers it again.
                        acknowledged = Tracker.acknowledgeDeepLink(this, data)
                        if (!duplicate) SampleNavigator.recordArrival(arrival.copy(acknowledged = acknowledged))
                    }
                }
                DynamicLinkCode.UNAVAILABLE -> if (result.retryable) {
                    pendingRetry.value = Intent(intent ?: Intent())
                }
                // NO_LINK: a normal launch. INVALID_LINK / EXPIRED: stay on a safe screen.
                DynamicLinkCode.NO_LINK, DynamicLinkCode.INVALID_LINK, DynamicLinkCode.EXPIRED -> Unit
            }
            SampleLog.link(result, destination, acknowledged, duplicate)
        }
    }

    private fun lastHandoffId(): String =
        getSharedPreferences(LINK_PREFS, Context.MODE_PRIVATE).getString(KEY_HANDOFF, "").orEmpty()

    private fun rememberHandoffId(id: String) {
        getSharedPreferences(LINK_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HANDOFF, id).apply()
    }

    private companion object {
        const val LINK_PREFS = "sample_links"
        const val KEY_HANDOFF = "last_handoff_id"
    }
}
