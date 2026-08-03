package com.futo.platformplayer.api.media.models.comments

import com.futo.platformplayer.api.media.IPlatformClient
import com.futo.platformplayer.api.media.models.PlatformAuthorLink
import com.futo.platformplayer.api.media.models.ratings.IRating
import com.futo.platformplayer.api.media.structures.IPager
import java.time.OffsetDateTime

interface IPlatformComment {
    /** Source plugin identity assigned by the host while parsing the comment. */
    val sourcePluginId: String? get() = null;
    val contextUrl: String;
    val author : PlatformAuthorLink;
    val message : String;
    val rating : IRating;
    val date : OffsetDateTime?;

    val replyCount : Int?;

    /** Stable source identity when supplied by a newer plugin. */
    val stableId: String? get() = null;
    /** True only when the authenticated source identity owns this comment. */
    val isOwnedByUser: Boolean get() = false;
    val isEdited: Boolean get() = false;
    val userReaction: PlatformCommentReaction get() = PlatformCommentReaction.NONE;
    /** Per-comment authorization; absent fields default to no mutation access. */
    val capabilities: Set<PlatformCommentCapability> get() = emptySet();
    val visibility: PlatformCommentVisibility get() = PlatformCommentVisibility.UNKNOWN;
    /** Opaque, non-secret plugin data required to reconstruct an action. */
    val context: Map<String, String> get() = emptyMap();

    fun getReplies(client: IPlatformClient) : IPager<IPlatformComment>?;
}
