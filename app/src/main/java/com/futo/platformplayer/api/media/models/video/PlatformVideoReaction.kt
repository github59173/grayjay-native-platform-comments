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
