package com.futo.platformplayer.api.media.models.video

enum class PlatformVideoReaction {
    NONE,
    LIKE,
    DISLIKE
}

enum class PlatformVideoReactionError {
    AUTH_REQUIRED,
    ACTION_NOT_SUPPORTED,
    NETWORK_ERROR,
    RATE_LIMITED,
    UPSTREAM_RESPONSE_CHANGED,
    UNKNOWN
}

data class PlatformVideoReactionState(
    val available: Boolean = false,
    val reaction: PlatformVideoReaction = PlatformVideoReaction.NONE,
    val canLike: Boolean = false,
    val canDislike: Boolean = false,
    val message: String? = null,
    val error: PlatformVideoReactionError? = null
) {
    companion object {
        fun unsupported() = PlatformVideoReactionState(
            error = PlatformVideoReactionError.ACTION_NOT_SUPPORTED
        )
    }
}

data class PlatformVideoReactionResult(
    val success: Boolean,
    val reaction: PlatformVideoReaction = PlatformVideoReaction.NONE,
    val retryable: Boolean = false,
    val message: String? = null,
    val error: PlatformVideoReactionError? = null
) {
    companion object {
        fun unsupported() = PlatformVideoReactionResult(
            success = false,
            error = PlatformVideoReactionError.ACTION_NOT_SUPPORTED
        )
    }
}

/**
 * Keeps transient native-platform reaction failures off the UI thread while avoiding
 * repeated requests for failures that require user action or source support.
 */
object PlatformVideoReactionRetryPolicy {
    const val MAX_ATTEMPTS = 3
    private val RETRY_DELAYS_MS = longArrayOf(250L, 750L)

    /** [completedAttempts] is one-based and includes the result supplied here. */
    fun shouldRetry(result: PlatformVideoReactionResult, completedAttempts: Int): Boolean {
        if (result.success || completedAttempts >= MAX_ATTEMPTS) return false
        if (
            result.error == PlatformVideoReactionError.AUTH_REQUIRED ||
            result.error == PlatformVideoReactionError.ACTION_NOT_SUPPORTED
        ) return false

        return result.retryable || result.error == null || result.error in setOf(
            PlatformVideoReactionError.NETWORK_ERROR,
            PlatformVideoReactionError.RATE_LIMITED,
            PlatformVideoReactionError.UPSTREAM_RESPONSE_CHANGED,
            PlatformVideoReactionError.UNKNOWN
        )
    }

    fun delayAfter(completedAttempts: Int): Long =
        RETRY_DELAYS_MS.getOrElse((completedAttempts - 1).coerceAtLeast(0)) {
            RETRY_DELAYS_MS.last()
        }
}
