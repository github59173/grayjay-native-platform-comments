package com.futo.platformplayer.api.media

import com.futo.platformplayer.api.media.models.IPlatformChannelContent
import com.futo.platformplayer.api.media.models.PlatformAuthorLink
import com.futo.platformplayer.api.media.models.ResultCapabilities
import com.futo.platformplayer.api.media.models.channels.IPlatformChannel
import com.futo.platformplayer.api.media.models.chapters.IChapter
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationResult
import com.futo.platformplayer.api.media.models.contents.IPlatformContent
import com.futo.platformplayer.api.media.models.contents.IPlatformContentDetails
import com.futo.platformplayer.api.media.models.live.ILiveChatWindowDescriptor
import com.futo.platformplayer.api.media.models.live.IPlatformLiveEvent
import com.futo.platformplayer.api.media.models.playback.IPlaybackTracker
import com.futo.platformplayer.api.media.models.playlists.IPlatformPlaylist
import com.futo.platformplayer.api.media.models.playlists.IPlatformPlaylistDetails
import com.futo.platformplayer.api.media.models.video.IPlatformVideo
import com.futo.platformplayer.api.media.models.video.PlatformVideoReaction
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionResult
import com.futo.platformplayer.api.media.models.video.PlatformVideoReactionState
import com.futo.platformplayer.api.media.structures.IPager
import com.futo.platformplayer.models.ImageVariable

/**
 * A client for a specific platform
 */
interface IPlatformClient {
    val id: String;
    val name: String;

    val icon: ImageVariable?;
    /** Optional opaque ARGB accent supplied by the source plugin for platform-owned UI. */
    val accentColor: Int? get() = null;

    //Capabilities
    val capabilities: PlatformClientCapabilities;

    fun initialize();
    fun disable();

    /**
     * Gets the home recommendations
     */
    fun getHome(): IPager<IPlatformContent>

    /**
     * Gets the shorts feed
     */
    fun getShorts(): IPager<IPlatformVideo>

    //Search
    /**
     * Gets search suggestion for the provided query string
     */
    fun searchSuggestions(query: String): Array<String>;
    /**
     * Describes what the plugin is capable on filtering/sorting search results
     */
    fun getSearchCapabilities(): ResultCapabilities;
    /**
     * Searches for content and returns a search pager with results
     */
    fun search(query: String, type: String? = null, order: String? = null, filters: Map<String, List<String>>? = null): IPager<IPlatformContent>;



    /**
     * Describes what the plugin is capable on filtering/sorting search results on channels
     */
    fun getSearchChannelContentsCapabilities(): ResultCapabilities;
    /**
     * Searches for content on a channel and returns a video pager
     */
    fun searchChannelContents(channelUrl: String, query: String, type: String? = null, order: String? = null, filters: Map<String, List<String>>? = null): IPager<IPlatformContent>;


    /**
     * Searches for channels and returns a channel pager
     */
    fun searchChannels(query: String): IPager<PlatformAuthorLink>;

    /**
     * Searches for channels and returns a content pager
     */
    fun searchChannelsAsContent(query: String): IPager<IPlatformContent>;


    //Video Pages
    /**
     * Determines if the provided url is a valid url for getting channel from this client
     */
    fun isChannelUrl(url: String): Boolean;
    /**
     * Gets channel details, might also fetch videos which is then obtained by IPlatformChannel.getVideos. Otherwise might fall back to getChannelVideos
     */
    fun getChannel(channelUrl: String): IPlatformChannel;
    /**
     * Describes what the plugin is capable on filtering/sorting channel results
     */
    fun getChannelCapabilities(): ResultCapabilities;
    /**
     * Gets all videos of a channel, ideally in upload time descending
     */
    fun getChannelContents(channelUrl: String, type: String? = null, order: String? = null, filters: Map<String, List<String>>? = null): IPager<IPlatformContent>;

    /**
     * Describes what the plugin is capable on peek channel results
     */
    fun getPeekChannelTypes(): List<String>;
    /**
     * Peeks contents of a channel, upload time descending
     */
    fun peekChannelContents(channelUrl: String, type: String? = null): List<IPlatformContent>

    /**
     * Gets all playlists of a channel
     */
    fun getChannelPlaylists(channelUrl: String): IPager<IPlatformPlaylist>

    /**
     * Gets the channel url associated with a claimType
     */
    fun getChannelUrlByClaim(claimType: Int, claimValues: Map<Int, String>): String?;

    //Video
    /**
     * Determines if the provided url is a valid url for getting details from this client
     */
    fun isContentDetailsUrl(url: String): Boolean;
    /**
     * Gets the video details for a given url, including video/audio streams
     */
    fun getContentDetails(url: String): IPlatformContentDetails;

    fun getContentChapters(url: String): List<IChapter>;

    /**
     * Gets the playback tracker for a piece of content
     */
    fun getPlaybackTracker(url: String): IPlaybackTracker?;

    /**
     * Get content recommendations
     */
    fun getContentRecommendations(url: String): IPager<IPlatformContent>?;

    /** Optional native-platform reaction state and mutation support for a video. */
    fun getVideoReactionState(contentUrl: String): PlatformVideoReactionState =
        PlatformVideoReactionState.unsupported();
    fun setVideoReaction(
        contentUrl: String,
        reaction: PlatformVideoReaction
    ): PlatformVideoReactionResult = PlatformVideoReactionResult.unsupported();


    //Comments
    /**
     * Gets the comments underneath a video
     */
    fun getComments(url: String): IPager<IPlatformComment>;
    /**
     * Gets the replies to a comment
     */
    fun getSubComments(comment: IPlatformComment): IPager<IPlatformComment>;

    /** Optional comment mutations. Default implementations preserve old clients. */
    fun createComment(contentUrl: String, message: String): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();
    fun replyToComment(comment: IPlatformComment, message: String): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();
    fun editComment(comment: IPlatformComment, message: String): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();
    fun deleteComment(comment: IPlatformComment): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();
    fun likeComment(comment: IPlatformComment, enabled: Boolean): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();
    fun dislikeComment(comment: IPlatformComment, enabled: Boolean): PlatformCommentMutationResult =
        PlatformCommentMutationResult.unsupported();

    /** Optional user-visible source identity, for example the selected channel. */
    fun getCommentingIdentity(): String? = null;

    /**
     * Gets the live events of a livestream
     */
    fun getLiveChatWindow(url: String): ILiveChatWindowDescriptor?;
    /**
     * Gets the live events of a livestream
     */
    fun getLiveEvents(url: String): IPager<IPlatformLiveEvent>?


    //Playlists
    /**
     * Search for Playlists and returns a Playlist pager
     */
    fun searchPlaylists(query: String, type: String? = null, order: String? = null, filters: Map<String, List<String>>? = null): IPager<IPlatformContent>;
    /**
     * Gets a playlist from a url
     */
    fun isPlaylistUrl(url: String): Boolean;
    /**
     * Gets a playlist from a url
     */
    fun getPlaylist(url: String): IPlatformPlaylistDetails;

    //Migration
    /**
     * Retrieves the playlists of the currently logged in user
     */
    fun getUserPlaylists(): Array<String>;
    /**
     * Retrieves the subscriptions of the currently logged in user
     */
    fun getUserSubscriptions(): Array<String>;
    /**
     * Retrieves the history of the currently logged in user
     */
    fun getUserHistory(): IPager<IPlatformContent>;


    fun isClaimTypeSupported(claimType: Int): Boolean;
}
