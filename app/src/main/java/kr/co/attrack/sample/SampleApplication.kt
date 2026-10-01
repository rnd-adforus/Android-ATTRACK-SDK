package kr.co.attrack.sample

import android.app.Application
import kr.co.attrack.tracker.Tracker
import kr.co.attrack.tracker.TrackerResult

class SampleApplication : Application() {

    /** The immediate local outcome of initialization, for the Overview screen. */
    @Volatile
    var initStart: TrackerResult? = null
        private set

    override fun onCreate() {
        super.onCreate()

        // Optional: this showcase displays results on its Overview screen.
        // Real apps can omit the listener and just call Tracker.initialize().
        // Local callbacks may use the caller thread; SampleLog is thread-safe.
        Tracker.setResultListener(SampleLog::result)

        // 2. Initialize once, in the main process, with the App ID and current client key issued for your app.
        //    The install event is collected and sent automatically, together with
        //    the Google Advertising ID and secure ID (collected on every event).
        //    INITIALIZATION_STARTED is local; INITIALIZED arrives on the listener
        //    once the server has accepted this app's identity.
        initStart = Tracker.initializeWithResult(this, BuildConfig.ATTRACK_APP_ID, BuildConfig.ATTRACK_CLIENT_KEY)
    }
}
