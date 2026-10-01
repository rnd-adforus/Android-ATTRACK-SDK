package kr.co.attrack.sample

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkRouterTest {

    @Test
    fun emptyParametersOpenHome() {
        assertEquals(Destination.Home, DeepLinkRouter.route(emptyMap()))
        assertEquals(Destination.Home, DeepLinkRouter.route(mapOf("screen" to "home")))
    }

    @Test
    fun knownScreensRouteWithTheirId() {
        assertEquals(
            Destination.Offer("summer-42"),
            DeepLinkRouter.route(mapOf("screen" to "offer", "offer_id" to "summer-42")),
        )
        assertEquals(
            Destination.Product("sku_1"),
            DeepLinkRouter.route(mapOf("screen" to "product", "product_id" to "sku_1")),
        )
    }

    @Test
    fun untrustedValuesFallBackToHome() {
        assertEquals(Destination.Home, DeepLinkRouter.route(mapOf("screen" to "admin")))
        assertEquals(Destination.Home, DeepLinkRouter.route(mapOf("screen" to "offer")))
        assertEquals(
            Destination.Home,
            DeepLinkRouter.route(mapOf("screen" to "offer", "offer_id" to "../../etc")),
        )
        assertEquals(
            Destination.Home,
            DeepLinkRouter.route(mapOf("screen" to "product", "product_id" to "x".repeat(65))),
        )
    }
}
