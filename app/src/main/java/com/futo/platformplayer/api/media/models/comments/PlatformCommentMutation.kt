package com.futo.platformplayer.api.media.models.comments

import com.futo.platformplayer.api.media.PlatformClientCapabilities

/** Independently detectable platform comment actions. */
enum class PlatformCommentCapability {
    COMMENTS_CREATE,
    COMMENTS_REPLY,
    COMMENTS_EDIT,
    COMMENTS_DELETE,
    COMMENTS_LIKE,
    COMMENTS_DISLIKE
}

enum class PlatformCommentReaction {
    NONE,
    LIKE,
    DISLIKE
}

/** Centralized fail-closed policy for exposing platform comment controls. */
object PlatformCommentUiPolicy {
    fun canCreate(capabilities: PlatformClientCapabilities?): Boolean =
        capabilities?.hasCommentsCreate == true

    fun canReply(comment: IPlatformComment?): Boolean =
        comment != null && PlatformCommentCapability.COMMENTS_REPLY in comment.capabilities

    fun canReplyToOtherUser(comment: IPlatformComment?): Boolean =
        comment?.isOwnedByUser == false && canReply(comment)

    fun replyState(comment: IPlatformComment?): PlatformCommentingState = when {
        comment == null -> PlatformCommentingState.UNKNOWN
        isReplyLocked(comment) -> PlatformCommentingState(
            PlatformCommentingAvailability.LOCKED,
            comment.context["replyLockReason"]
        )
        canReply(comment) -> PlatformCommentingState.AVAILABLE
        else -> PlatformCommentingState.UNKNOWN
    }

    fun canReplyToOtherUser(
        selectedComment: IPlatformComment?,
        threadParent: IPlatformComment?,
        threadState: PlatformCommentingState
    ): Boolean {
        if (selectedComment == null || selectedComment.isOwnedByUser)
            return false
        val effectiveState = if (threadState.availability != PlatformCommentingAvailability.UNKNOWN)
            threadState
        else
            replyState(threadParent ?: selectedComment)
        return effectiveState.availability == PlatformCommentingAvailability.AVAILABLE
    }

    fun isReplyLockedInThread(
        selectedComment: IPlatformComment?,
        threadParent: IPlatformComment?,
        threadState: PlatformCommentingState
    ): Boolean {
        if (selectedComment == null || selectedComment.isOwnedByUser)
            return false
        val effectiveState = if (threadState.availability != PlatformCommentingAvailability.UNKNOWN)
            threadState
        else
            replyState(threadParent ?: selectedComment)
        return effectiveState.isLocked
    }

    fun isReplyLocked(comment: IPlatformComment?): Boolean =
        comment != null && comment.context["replyLocked"].equals("true", ignoreCase = true)

    fun canLike(comment: IPlatformComment?): Boolean =
        comment != null && PlatformCommentCapability.COMMENTS_LIKE in comment.capabilities

    fun canDislike(comment: IPlatformComment?): Boolean =
        comment != null && PlatformCommentCapability.COMMENTS_DISLIKE in comment.capabilities

    fun canEdit(comment: IPlatformComment?): Boolean =
        comment?.isOwnedByUser == true && PlatformCommentCapability.COMMENTS_EDIT in comment.capabilities

    fun canDelete(comment: IPlatformComment?): Boolean =
        comment?.isOwnedByUser == true && PlatformCommentCapability.COMMENTS_DELETE in comment.capabilities

    fun canReact(comment: IPlatformComment?): Boolean =
        canLike(comment) || canDislike(comment)
}

/** Drops bridge values that cannot be represented by the public string map contract. */
fun sanitizeCommentContext(context: Map<*, *>?): Map<String, String> =
    context?.entries?.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }?.toMap() ?: emptyMap()

enum class PlatformCommentVisibility {
    UNKNOWN,
    ACKNOWLEDGED,
    VISIBLE,
    HELD_FOR_REVIEW,
    DELETED
}

enum class PlatformCommentMutationError {
    AUTH_REQUIRED,
    SESSION_EXPIRED,
    ACCOUNT_OR_CHANNEL_NOT_SELECTED,
    COMMENTS_DISABLED,
    ACTION_NOT_SUPPORTED,
    NOT_AUTHORIZED,
    COMMENT_NOT_FOUND,
    INVALID_TEXT,
    TEXT_TOO_LONG,
    RATE_LIMITED,
    MODERATION_REJECTED,
    HELD_FOR_REVIEW,
    NETWORK_ERROR,
    UPSTREAM_RESPONSE_CHANGED,
    UNKNOWN
}

/**
 * Normalized result returned by every comment mutation. A successful transport
 * acknowledgement is distinct from confirmed public visibility.
 */
data class PlatformCommentMutationResult(
    val success: Boolean,
    val comment: IPlatformComment? = null,
    val deleted: Boolean = false,
    val reaction: PlatformCommentReaction = PlatformCommentReaction.NONE,
    val retryable: Boolean = false,
    val message: String? = null,
    val error: PlatformCommentMutationError? = null,
    val visibility: PlatformCommentVisibility = PlatformCommentVisibility.UNKNOWN
) {
    companion object {
        fun unsupported() = PlatformCommentMutationResult(
            success = false,
            error = PlatformCommentMutationError.ACTION_NOT_SUPPORTED
        )
    }
}
