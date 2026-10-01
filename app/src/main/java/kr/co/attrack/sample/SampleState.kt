package kr.co.attrack.sample

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How the current screen was reached from a link, shown on the destination screen. */
data class LinkArrival(
    val deferred: Boolean,
    val linkId: String,
    val parameters: Map<String, String>,
    /** Deferred links only: whether the SDK accepted the acknowledgement. */
    val acknowledged: Boolean,
)

/** In-app navigation. Deep links and the UI both go through [open]. */
object SampleNavigator {
    private val mutableDestination = MutableStateFlow<Destination>(Destination.Home)
    val destination: StateFlow<Destination> = mutableDestination.asStateFlow()

    private val mutableArrival = MutableStateFlow<LinkArrival?>(null)
    val arrival: StateFlow<LinkArrival?> = mutableArrival.asStateFlow()

    fun open(destination: Destination, arrival: LinkArrival? = null) {
        mutableArrival.value = arrival
        mutableDestination.value = destination
    }

    fun recordArrival(arrival: LinkArrival) {
        mutableArrival.value = arrival
    }
}
