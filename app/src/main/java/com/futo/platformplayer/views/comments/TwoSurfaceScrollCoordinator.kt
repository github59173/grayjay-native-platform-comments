package com.futo.platformplayer.views.comments

/**
 * Applies one scroll delta across the outer Grayjay list and the inner comments
 * surface without dropping the unconsumed portion at their shared boundary.
 */
internal object TwoSurfaceScrollCoordinator {
    fun consume(
        scrollDeltaY: Int,
        consumeOuter: (Int) -> Int,
        consumeInner: (Int) -> Int
    ): Int {
        if (scrollDeltaY == 0) return 0

        return if (scrollDeltaY > 0) {
            consumeInOrder(scrollDeltaY, consumeOuter, consumeInner)
        } else {
            consumeInOrder(scrollDeltaY, consumeInner, consumeOuter)
        }
    }

    private fun consumeInOrder(
        scrollDeltaY: Int,
        consumeFirst: (Int) -> Int,
        consumeSecond: (Int) -> Int
    ): Int {
        val consumedFirst = boundedConsumption(scrollDeltaY, consumeFirst(scrollDeltaY))
        val remaining = scrollDeltaY - consumedFirst
        if (remaining == 0) return consumedFirst

        val consumedSecond = boundedConsumption(remaining, consumeSecond(remaining))
        return consumedFirst + consumedSecond
    }

    private fun boundedConsumption(requested: Int, reported: Int): Int {
        return if (requested > 0) {
            reported.coerceIn(0, requested)
        } else {
            reported.coerceIn(requested, 0)
        }
    }
}
