package com.futo.platformplayer

import com.futo.platformplayer.api.media.PlatformClientCapabilities
import com.futo.platformplayer.api.media.IPlatformClient
import com.futo.platformplayer.api.media.models.PlatformAuthorLink
import com.futo.platformplayer.api.media.models.comments.CommentDestination
import com.futo.platformplayer.api.media.models.comments.CommentDestinationSelection
import com.futo.platformplayer.api.media.models.comments.CommentReactionStateMachine
import com.futo.platformplayer.api.media.models.comments.CommentSubmissionGuard
import com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingAvailability
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingState
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationError
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationResult
import com.futo.platformplayer.api.media.models.comments.PlatformCommentReaction
import com.futo.platformplayer.api.media.models.comments.PlatformCommentUiPolicy
import com.futo.platformplayer.api.media.models.comments.sanitizeCommentContext
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.ratings.RatingLikes
import com.futo.platformplayer.api.media.structures.IPager
import com.futo.platformplayer.dialogs.resolveCommentDialogMode
import com.futo.platformplayer.dialogs.resolveReplyMention
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCommentMutationTests {
    @Test
    fun editDialogStartsWithTheCurrentCommentContents() {
        val comment = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "Current comment contents"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = 0
            override val isOwnedByUser = true
            override val capabilities = setOf(PlatformCommentCapability.COMMENTS_EDIT)
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }

        val editMode = resolveCommentDialogMode(comment)
        assertTrue(editMode.isEditing)
        assertEquals("Current comment contents", editMode.initialText)

        val createMode = resolveCommentDialogMode(null)
        assertFalse(createMode.isEditing)
        assertEquals("", createMode.initialText)

        val replyMode = resolveCommentDialogMode(null, "@fixture ")
        assertFalse(replyMode.isEditing)
        assertEquals("@fixture ", replyMode.initialText)

        val editModeWithIgnoredReplyPrefill = resolveCommentDialogMode(comment, "@ignored ")
        assertEquals("Current comment contents", editModeWithIgnoredReplyPrefill.initialText)
    }

    @Test
    fun directedReplyPrefillUsesAValidatedYouTubeHandle() {
        assertEquals(
            "@from_url ",
            resolveReplyMention("Display Name", "https://www.youtube.com/@from_url/videos")
        )
        assertEquals(
            "@encoded_handle ",
            resolveReplyMention("Display Name", "https://www.youtube.com/%40encoded_handle")
        )
        assertEquals("@from_name ", resolveReplyMention("  @from_name  ", "/channel/UC123"))
        assertEquals("", resolveReplyMention("Display Name", "/channel/UC123"))
        assertEquals("", resolveReplyMention("@not a handle", null))
    }

    @Test
    fun oldClientCapabilitiesDefaultToReadOnly() {
        val capabilities = PlatformClientCapabilities(hasGetComments = true)
        PlatformCommentCapability.entries.forEach { assertFalse(capabilities.supports(it)) }
    }

    @Test
    fun eachSourceCapabilityIsIndependent() {
        val capabilities = PlatformClientCapabilities(hasCommentsEdit = true)
        assertTrue(capabilities.supports(PlatformCommentCapability.COMMENTS_EDIT))
        assertFalse(capabilities.supports(PlatformCommentCapability.COMMENTS_DELETE))
        assertFalse(capabilities.supports(PlatformCommentCapability.COMMENTS_CREATE))
    }

    @Test
    fun allSixSourceCapabilitiesCanBeDetectedIndependently() {
        val cases = listOf(
            PlatformClientCapabilities(hasCommentsCreate = true) to PlatformCommentCapability.COMMENTS_CREATE,
            PlatformClientCapabilities(hasCommentsReply = true) to PlatformCommentCapability.COMMENTS_REPLY,
            PlatformClientCapabilities(hasCommentsEdit = true) to PlatformCommentCapability.COMMENTS_EDIT,
            PlatformClientCapabilities(hasCommentsDelete = true) to PlatformCommentCapability.COMMENTS_DELETE,
            PlatformClientCapabilities(hasCommentsLike = true) to PlatformCommentCapability.COMMENTS_LIKE,
            PlatformClientCapabilities(hasCommentsDislike = true) to PlatformCommentCapability.COMMENTS_DISLIKE
        )
        cases.forEach { (capabilities, expected) ->
            PlatformCommentCapability.entries.forEach { capability ->
                assertEquals(capability == expected, capabilities.supports(capability))
            }
        }
    }

    @Test
    fun commentUiPolicyIsCapabilityDrivenAndFailClosed() {
        assertFalse(PlatformCommentUiPolicy.canCreate(null))
        assertFalse(PlatformCommentUiPolicy.canCreate(PlatformClientCapabilities(hasGetComments = true)))
        assertTrue(PlatformCommentUiPolicy.canCreate(PlatformClientCapabilities(hasCommentsCreate = true)))

        fun commentWith(vararg capabilities: PlatformCommentCapability) = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "fixture"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = 0
            override val capabilities = capabilities.toSet()
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }

        val readOnly = commentWith()
        assertFalse(PlatformCommentUiPolicy.canReply(readOnly))
        assertFalse(PlatformCommentUiPolicy.canReplyToOtherUser(readOnly))
        assertFalse(PlatformCommentUiPolicy.canLike(readOnly))
        assertFalse(PlatformCommentUiPolicy.canDislike(readOnly))
        assertFalse(PlatformCommentUiPolicy.canReact(readOnly))
        assertTrue(PlatformCommentUiPolicy.canReply(commentWith(PlatformCommentCapability.COMMENTS_REPLY)))
        assertTrue(PlatformCommentUiPolicy.canReplyToOtherUser(commentWith(PlatformCommentCapability.COMMENTS_REPLY)))
        val likeOnly = commentWith(PlatformCommentCapability.COMMENTS_LIKE)
        val dislikeOnly = commentWith(PlatformCommentCapability.COMMENTS_DISLIKE)
        assertTrue(PlatformCommentUiPolicy.canLike(likeOnly))
        assertFalse(PlatformCommentUiPolicy.canDislike(likeOnly))
        assertTrue(PlatformCommentUiPolicy.canReact(likeOnly))
        assertFalse(PlatformCommentUiPolicy.canLike(dislikeOnly))
        assertTrue(PlatformCommentUiPolicy.canDislike(dislikeOnly))
        assertTrue(PlatformCommentUiPolicy.canReact(dislikeOnly))
        assertFalse(PlatformCommentUiPolicy.canDelete(commentWith(PlatformCommentCapability.COMMENTS_DELETE)))

        val ownedDelete = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "owned fixture"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = 0
            override val isOwnedByUser = true
            override val capabilities = setOf(PlatformCommentCapability.COMMENTS_DELETE)
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }
        assertTrue(PlatformCommentUiPolicy.canDelete(ownedDelete))

        val ownedReply = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "owned fixture"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = 0
            override val isOwnedByUser = true
            override val capabilities = setOf(PlatformCommentCapability.COMMENTS_REPLY)
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }
        assertFalse(PlatformCommentUiPolicy.canReplyToOtherUser(ownedReply))

        val lockedReply = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "locked fixture"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = 2
            override val context = mapOf("replyLocked" to "true")
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }
        assertTrue(PlatformCommentUiPolicy.isReplyLocked(lockedReply))
        assertFalse(PlatformCommentUiPolicy.canReply(lockedReply))

        val availableParent = commentWith(PlatformCommentCapability.COMMENTS_REPLY)
        assertTrue(PlatformCommentUiPolicy.canReplyToOtherUser(
            readOnly,
            availableParent,
            PlatformCommentingState.UNKNOWN
        ))
        val lockedThread = PlatformCommentingState(
            PlatformCommentingAvailability.LOCKED,
            "Replies are locked"
        )
        assertFalse(PlatformCommentUiPolicy.canReplyToOtherUser(readOnly, availableParent, lockedThread))
        assertTrue(PlatformCommentUiPolicy.isReplyLockedInThread(readOnly, availableParent, lockedThread))
    }

    @Test
    fun oldCommentDefaultsToSafeReadOnlyFields() {
        val oldComment = object : IPlatformComment {
            override val contextUrl = "https://fixture.invalid/watch"
            override val author = PlatformAuthorLink.UNKNOWN
            override val message = "fixture"
            override val rating = RatingLikes(0)
            override val date = null
            override val replyCount = null
            override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? = null
        }
        assertNull(oldComment.stableId)
        assertFalse(oldComment.isOwnedByUser)
        assertFalse(oldComment.isEdited)
        assertEquals(PlatformCommentReaction.NONE, oldComment.userReaction)
        assertTrue(oldComment.capabilities.isEmpty())
        assertTrue(oldComment.context.isEmpty())
    }

    @Test
    fun opaqueNonSecretContextRoundTrips() {
        val context = mapOf(
            "commentId" to "SANITIZED_COMMENT_ID",
            "replyCommand" to "{\"endpointKey\":\"createCommentReplyEndpoint\"}"
        )
        val encoded = Json.encodeToString(context)
        assertEquals(context, Json.decodeFromString<Map<String, String>>(encoded))
        assertFalse(encoded.contains("Cookie", ignoreCase = true))
        assertFalse(encoded.contains("Authorization", ignoreCase = true))
    }

    @Test
    fun nullBridgeContextValuesAreDroppedBeforeMutationSerialization() {
        @Suppress("UNCHECKED_CAST")
        val poisoned = mapOf<String, String?>(
            "replyContinuation" to null,
            "likeCommand" to "SANITIZED_LIKE_COMMAND"
        ) as Map<String, String>

        val sanitized = sanitizeCommentContext(poisoned)
        assertEquals(mapOf("likeCommand" to "SANITIZED_LIKE_COMMAND"), sanitized)
        assertEquals(sanitized, Json.decodeFromString<Map<String, String>>(Json.encodeToString(sanitized)))
    }

    @Test
    fun typedErrorsDistinguishLoginAndExpiredSession() {
        val login = PlatformCommentMutationResult(false, error = PlatformCommentMutationError.AUTH_REQUIRED)
        val expired = PlatformCommentMutationResult(false, retryable = false, error = PlatformCommentMutationError.SESSION_EXPIRED)
        assertEquals(PlatformCommentMutationError.AUTH_REQUIRED, login.error)
        assertEquals(PlatformCommentMutationError.SESSION_EXPIRED, expired.error)
        assertFalse(expired.retryable)
    }

    @Test
    fun reactionTransitionsAreMutuallyExclusiveAndClearable() {
        assertTrue(CommentReactionStateMachine.next(PlatformCommentReaction.NONE, PlatformCommentReaction.LIKE) == PlatformCommentReaction.LIKE)
        assertTrue(CommentReactionStateMachine.next(PlatformCommentReaction.LIKE, PlatformCommentReaction.LIKE) == PlatformCommentReaction.NONE)
        assertTrue(CommentReactionStateMachine.next(PlatformCommentReaction.LIKE, PlatformCommentReaction.DISLIKE) == PlatformCommentReaction.DISLIKE)
        assertTrue(CommentReactionStateMachine.next(PlatformCommentReaction.DISLIKE, PlatformCommentReaction.LIKE) == PlatformCommentReaction.LIKE)
    }

    @Test
    fun activeCommentTabOverridesRememberedDialogDestination() {
        val available = listOf(
            CommentDestination.PLATFORM,
            CommentDestination.POLYCENTRIC
        )
        assertEquals(
            CommentDestination.POLYCENTRIC,
            CommentDestinationSelection.resolve(
                available,
                preferred = CommentDestination.POLYCENTRIC,
                remembered = CommentDestination.PLATFORM
            )
        )
        assertEquals(
            CommentDestination.PLATFORM,
            CommentDestinationSelection.resolve(
                available,
                preferred = null,
                remembered = CommentDestination.PLATFORM
            )
        )
        assertEquals(
            listOf(CommentDestination.POLYCENTRIC),
            CommentDestinationSelection.restrictAvailable(
                available,
                preferred = CommentDestination.POLYCENTRIC,
                restrictToPreferred = true
            )
        )
        assertTrue(
            CommentDestinationSelection.restrictAvailable(
                available = listOf(CommentDestination.PLATFORM),
                preferred = CommentDestination.POLYCENTRIC,
                restrictToPreferred = true
            ).isEmpty()
        )
    }

    @Test
    fun duplicateSubmissionIsRejectedUntilReleased() {
        val guard = CommentSubmissionGuard()
        assertTrue(guard.tryAcquire("video|message"))
        assertFalse(guard.tryAcquire("video|message"))
        guard.release("video|message")
        assertTrue(guard.tryAcquire("video|message"))
    }
}
