package com.futo.platformplayer

import com.futo.platformplayer.api.media.PlatformClientCapabilities
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
}
