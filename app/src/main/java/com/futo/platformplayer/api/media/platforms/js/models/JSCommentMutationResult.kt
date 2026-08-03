package com.futo.platformplayer.api.media.platforms.js.models

import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationError
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationResult
import com.futo.platformplayer.api.media.models.comments.PlatformCommentReaction
import com.futo.platformplayer.api.media.models.comments.PlatformCommentVisibility
import com.futo.platformplayer.api.media.platforms.js.JSClient
import com.futo.platformplayer.getOrDefault

object JSCommentMutationResult {
    fun fromV8(client: JSClient, obj: V8ValueObject): PlatformCommentMutationResult {
        val config = client.config
        val contextName = "CommentMutationResult"
        val comment = obj.getOrDefault<V8ValueObject>(config, "comment", contextName, null)?.let {
            JSComment(config, client.getUnderlyingPlugin(), it)
        }
        val success = obj.getOrDefault(config, "success", contextName, false) ?: false
        val deleted = obj.getOrDefault(config, "deleted", contextName, false) ?: false
        val retryable = obj.getOrDefault(config, "retryable", contextName, false) ?: false
        val message = obj.getOrDefault<String>(config, "message", contextName, null)
        val reaction = enumValueOrDefault(
            obj.getOrDefault(config, "reaction", contextName, "NONE"),
            PlatformCommentReaction.NONE
        )
        val error = enumValueOrNull<PlatformCommentMutationError>(
            obj.getOrDefault<String>(config, "errorCode", contextName, null)
        )
        val visibility = enumValueOrDefault(
            obj.getOrDefault(config, "visibility", contextName, "UNKNOWN"),
            PlatformCommentVisibility.UNKNOWN
        )

        return PlatformCommentMutationResult(
            success = success,
            comment = comment,
            deleted = deleted,
            reaction = reaction,
            retryable = retryable,
            message = message,
            error = error,
            visibility = visibility
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { raw -> enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValueOrNull<T>(value) ?: default
}
