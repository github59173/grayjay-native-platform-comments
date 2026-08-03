package com.futo.platformplayer.api.media.platforms.js.models

import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.IPlatformCommentingPager
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingAvailability
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingState
import com.futo.platformplayer.api.media.platforms.js.JSClient
import com.futo.platformplayer.api.media.platforms.js.SourcePluginConfig
import com.futo.platformplayer.api.media.structures.IPager
import com.futo.platformplayer.getOrDefault
import com.futo.platformplayer.requireSourcePlugin

class JSCommentPager : JSPager<IPlatformComment>, IPager<IPlatformComment>, IPlatformCommentingPager {

    override val commentingState: PlatformCommentingState

    constructor(config: SourcePluginConfig, plugin: JSClient, pager: V8ValueObject) : super(config, plugin, pager) {
        commentingState = requirePagerPluginV8("commentingState").busy {
            val rawAvailability = pager.getOrDefault(
                config,
                "commentingAvailability",
                "CommentPager",
                PlatformCommentingAvailability.UNKNOWN.name
            ) ?: PlatformCommentingAvailability.UNKNOWN.name
            val availability = PlatformCommentingAvailability.entries.firstOrNull {
                it.name.equals(rawAvailability, ignoreCase = true)
            } ?: PlatformCommentingAvailability.UNKNOWN
            val reason = pager.getOrDefault<String?>(
                config,
                "commentingLockReason",
                "CommentPager",
                null
            )
            PlatformCommentingState(availability, reason)
        }
    }

    override fun convertResult(obj: V8ValueObject): IPlatformComment {
        return JSComment(config, obj.requireSourcePlugin("JSCommentPager.convertResult"), obj);
    }
}
