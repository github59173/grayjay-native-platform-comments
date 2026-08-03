package com.futo.platformplayer

import com.futo.platformplayer.api.media.PlatformClientCapabilities
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionError
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionResult
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionRetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformVideoReactionTests {
    @Test
    fun videoReactionCapabilityRequiresStateAndMutationMethods() {
        assertFalse(PlatformClientCapabilities().hasVideoReactions)
        assertFalse(PlatformClientCapabilities(hasVideoReactionState = true).hasVideoReactions)
        assertFalse(PlatformClientCapabilities(hasVideoReactionMutation = true).hasVideoReactions)
        assertTrue(
            PlatformClientCapabilities(
                hasVideoReactionState = true,
                hasVideoReactionMutation = true
            ).hasVideoReactions
        )
    }

    @Test
    fun transientVideoReactionFailuresReceiveThreeTotalAttempts() {
        val transient = PlatformVideoReactionResult(
            success = false,
            retryable = true,
            error = PlatformVideoReactionError.NETWORK_ERROR
        )

        assertTrue(PlatformVideoReactionRetryPolicy.shouldRetry(transient, 1))
        assertTrue(PlatformVideoReactionRetryPolicy.shouldRetry(transient, 2))
        assertFalse(PlatformVideoReactionRetryPolicy.shouldRetry(transient, 3))
        assertEquals(250L, PlatformVideoReactionRetryPolicy.delayAfter(1))
        assertEquals(750L, PlatformVideoReactionRetryPolicy.delayAfter(2))
    }

    @Test
    fun videoReactionRetriesStopForLoginAndUnsupportedActions() {
        assertFalse(
            PlatformVideoReactionRetryPolicy.shouldRetry(
                PlatformVideoReactionResult(
                    success = false,
                    error = PlatformVideoReactionError.AUTH_REQUIRED
                ),
                1
            )
        )
        assertFalse(
            PlatformVideoReactionRetryPolicy.shouldRetry(
                PlatformVideoReactionResult(
                    success = false,
                    error = PlatformVideoReactionError.ACTION_NOT_SUPPORTED
                ),
                1
            )
        )
    }

    @Test
    fun untypedSourceRejectionsAreRetriedBeforeRollback() {
        assertTrue(
            PlatformVideoReactionRetryPolicy.shouldRetry(
                PlatformVideoReactionResult(success = false, message = "Rejected upstream"),
                1
            )
        )
    }
}
