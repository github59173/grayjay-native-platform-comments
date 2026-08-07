package com.futo.platformplayer.views.comments

import org.junit.Assert.assertEquals
import org.junit.Test

class TwoSurfaceScrollCoordinatorTest {
    @Test
    fun enteringCommentsConsumesOuterThenInner() {
        val calls = mutableListOf<String>()

        val consumed = TwoSurfaceScrollCoordinator.consume(
            100,
            consumeOuter = { requested ->
                calls += "outer:$requested"
                40
            },
            consumeInner = { requested ->
                calls += "inner:$requested"
                requested
            }
        )

        assertEquals(100, consumed)
        assertEquals(listOf("outer:100", "inner:60"), calls)
    }

    @Test
    fun returningToMetadataConsumesInnerThenOuter() {
        val calls = mutableListOf<String>()

        val consumed = TwoSurfaceScrollCoordinator.consume(
            -100,
            consumeOuter = { requested ->
                calls += "outer:$requested"
                requested
            },
            consumeInner = { requested ->
                calls += "inner:$requested"
                -30
            }
        )

        assertEquals(-100, consumed)
        assertEquals(listOf("inner:-100", "outer:-70"), calls)
    }

    @Test
    fun reportsOnlyWhatBothSurfacesActuallyConsume() {
        val consumed = TwoSurfaceScrollCoordinator.consume(
            100,
            consumeOuter = { 20 },
            consumeInner = { 30 }
        )

        assertEquals(50, consumed)
    }

    @Test
    fun boundsInvalidConsumerReportsToRequestedDirectionAndDistance() {
        val positive = TwoSurfaceScrollCoordinator.consume(
            100,
            consumeOuter = { -50 },
            consumeInner = { 200 }
        )
        val negative = TwoSurfaceScrollCoordinator.consume(
            -100,
            consumeOuter = { -200 },
            consumeInner = { 50 }
        )

        assertEquals(100, positive)
        assertEquals(-100, negative)
    }
}
