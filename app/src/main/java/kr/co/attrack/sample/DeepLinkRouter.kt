package kr.co.attrack.sample

/** Screens a link is allowed to open. Anything else falls back to [Home]. */
sealed interface Destination {
    data object Home : Destination
    data class Offer(val offerId: String) : Destination
    data class Product(val productId: String) : Destination
}

/**
 * Turns the parameters defined for an ATTRACK link into one of this
 * app's own screens.
 *
 * The SDK hands over plain strings; which keys mean what is entirely the app's
 * decision. Treat them as untrusted input: allow-list the screen and validate
 * every value, because anyone can craft a link. Account checks and rewards
 * never follow from a link parameter alone.
 */
object DeepLinkRouter {
    private val ID = Regex("^[A-Za-z0-9_-]{1,64}$")

    fun route(parameters: Map<String, String>): Destination = when (parameters["screen"]) {
        "offer" -> parameters["offer_id"].validId()?.let(Destination::Offer) ?: Destination.Home
        "product" -> parameters["product_id"].validId()?.let(Destination::Product) ?: Destination.Home
        // No screen (an empty-parameter link), "home", or an unknown value.
        else -> Destination.Home
    }

    private fun String?.validId(): String? = this?.takeIf(ID::matches)
}
