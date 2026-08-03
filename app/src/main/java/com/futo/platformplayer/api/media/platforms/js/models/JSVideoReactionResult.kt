package com.futo.platformplayer.api.media.platforms.js.models

import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.api.media.models.video.PlatformVideoReaction
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionError
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionResult
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionState
import com.futo.platformplayer.api.media.platforms.js.JSClient
import com.futo.platformplayer.getOrDefault

object JSVideoReactionResult {
    fun stateFromV8(client: JSClient, obj: V8ValueObject): PlatformVideoReactionState {
        val config = client.config
        val context = "VideoReactionState"
        return PlatformVideoReactionState(
            available = obj.getOrDefault(config, "available", context, false) ?: false,
            reaction = enumValueOrDefault<PlatformVideoReaction>(
                obj.getOrDefault<String>(config, "reaction", context, "NONE"),
                PlatformVideoReaction.NONE
            ),
            canLike = obj.getOrDefault(config, "canLike", context, false) ?: false,
            canDislike = obj.getOrDefault(config, "canDislike", context, false) ?: false,
            message = obj.getOrDefault<String>(config, "message", context, null),
            error = enumValueOrNull<PlatformVideoReactionError>(
                obj.getOrDefault<String>(config, "errorCode", context, null)
            )
        )
    }

    fun mutationFromV8(client: JSClient, obj: V8ValueObject): PlatformVideoReactionResult {
        val config = client.config
        val context = "VideoReactionResult"
        return PlatformVideoReactionResult(
            success = obj.getOrDefault(config, "success", context, false) ?: false,
            reaction = enumValueOrDefault<PlatformVideoReaction>(
                obj.getOrDefault<String>(config, "reaction", context, "NONE"),
                PlatformVideoReaction.NONE
            ),
            retryable = obj.getOrDefault(config, "retryable", context, false) ?: false,
            message = obj.getOrDefault<String>(config, "message", context, null),
            error = enumValueOrNull<PlatformVideoReactionError>(
                obj.getOrDefault<String>(config, "errorCode", context, null)
            )
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { raw -> enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValueOrNull<T>(value) ?: default
}
