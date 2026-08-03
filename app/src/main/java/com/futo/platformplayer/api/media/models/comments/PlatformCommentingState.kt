package com.futo.platformplayer.api.media.models.comments

/** Per-page commenting state discovered while loading the platform's comments. */
enum class PlatformCommentingAvailability {
    UNKNOWN,
    AVAILABLE,
    LOCKED
}

data class PlatformCommentingState(
    val availability: PlatformCommentingAvailability = PlatformCommentingAvailability.UNKNOWN,
    val reason: String? = null
) {
    val isLocked: Boolean get() = availability == PlatformCommentingAvailability.LOCKED

    companion object {
        val UNKNOWN = PlatformCommentingState()
        val AVAILABLE = PlatformCommentingState(PlatformCommentingAvailability.AVAILABLE)
    }
}

/** Optional pager metadata; older clients remain compatible and resolve to UNKNOWN. */
interface IPlatformCommentingPager {
    val commentingState: PlatformCommentingState
}
