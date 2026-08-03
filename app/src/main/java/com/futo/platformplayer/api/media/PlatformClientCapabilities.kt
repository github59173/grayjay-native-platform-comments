package com.futo.platformplayer.api.media

data class PlatformClientCapabilities(
    val hasChannelSearch: Boolean = false,
    val hasGetComments: Boolean = false,
    val hasGetUserSubscriptions: Boolean = false,
    val hasSearchPlaylists: Boolean = false,
    val hasGetPlaylist: Boolean = false,
    val hasGetUserPlaylists: Boolean = false,
    val hasSearchChannelContents: Boolean = false,
    val hasSaveState: Boolean = false,
    val hasGetPlaybackTracker: Boolean = false,
    val hasGetChannelUrlByClaim: Boolean = false,
    val hasGetChannelTemplateByClaimMap: Boolean = false,
    val hasGetSearchCapabilities: Boolean = false,
    val hasGetSearchChannelContentsCapabilities: Boolean = false,
    val hasGetChannelCapabilities: Boolean = false,
    val hasGetLiveEvents: Boolean = false,
    val hasGetLiveChatWindow: Boolean = false,
    val hasGetContentChapters: Boolean = false,
    val hasPeekChannelContents: Boolean = false,
    val hasGetChannelPlaylists: Boolean = false,
    val hasGetContentRecommendations: Boolean = false,
    val hasGetUserHistory: Boolean = false,
    val hasCommentsCreate: Boolean = false,
    val hasCommentsReply: Boolean = false,
    val hasCommentsEdit: Boolean = false,
    val hasCommentsDelete: Boolean = false,
    val hasCommentsLike: Boolean = false,
    val hasCommentsDislike: Boolean = false,
    val hasGetCommentingIdentity: Boolean = false,
    val hasVideoReactionState: Boolean = false,
    val hasVideoReactionMutation: Boolean = false
) {
    val hasVideoReactions: Boolean
        get() = hasVideoReactionState && hasVideoReactionMutation

    fun supports(capability: com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability): Boolean =
        when (capability) {
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_CREATE -> hasCommentsCreate
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_REPLY -> hasCommentsReply
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_EDIT -> hasCommentsEdit
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_DELETE -> hasCommentsDelete
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_LIKE -> hasCommentsLike
            com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability.COMMENTS_DISLIKE -> hasCommentsDislike
        }
}
