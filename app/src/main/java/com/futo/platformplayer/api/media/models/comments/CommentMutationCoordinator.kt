package com.futo.platformplayer.api.media.models.comments

enum class CommentDestination {
    PLATFORM,
    POLYCENTRIC
}

object CommentDestinationSelection {
    fun restrictAvailable(
        available: List<CommentDestination>,
        preferred: CommentDestination?,
        restrictToPreferred: Boolean
    ): List<CommentDestination> = if (restrictToPreferred && preferred != null) {
        available.filter { it == preferred }
    } else {
        available
    }

    /**
     * The active comments tab wins over a remembered dialog choice. This keeps the
     * add-comment affordance and the dialog pointed at the same destination while
     * still retaining the user's last explicit choice when no tab supplied a hint.
     */
    fun resolve(
        available: List<CommentDestination>,
        preferred: CommentDestination?,
        remembered: CommentDestination?
    ): CommentDestination? = preferred?.takeIf { it in available }
        ?: remembered?.takeIf { it in available }
        ?: available.firstOrNull()
}

object CommentReactionStateMachine {
    /** Active reaction taps clear; opposite reaction taps switch directly. */
    fun next(current: PlatformCommentReaction, tapped: PlatformCommentReaction): PlatformCommentReaction =
        if (current == tapped) PlatformCommentReaction.NONE else tapped
}

/** Prevents identical create/reply submissions from racing in the same view. */
class CommentSubmissionGuard {
    private val active = mutableSetOf<String>()

    @Synchronized
    fun tryAcquire(key: String): Boolean = active.add(key)

    @Synchronized
    fun release(key: String) {
        active.remove(key)
    }
}
