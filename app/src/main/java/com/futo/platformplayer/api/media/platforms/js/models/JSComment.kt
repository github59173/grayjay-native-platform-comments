package com.futo.platformplayer.api.media.platforms.js.models

import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.api.media.IPlatformClient
import com.futo.platformplayer.api.media.models.PlatformAuthorLink
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability
import com.futo.platformplayer.api.media.models.comments.PlatformCommentReaction
import com.futo.platformplayer.api.media.models.comments.PlatformCommentVisibility
import com.futo.platformplayer.api.media.models.comments.sanitizeCommentContext
import com.futo.platformplayer.api.media.models.ratings.IRating
import com.futo.platformplayer.api.media.platforms.js.JSClient
import com.futo.platformplayer.api.media.platforms.js.SourcePluginConfig
import com.futo.platformplayer.api.media.structures.IPager
import com.futo.platformplayer.engine.V8Plugin
import com.futo.platformplayer.getOrDefault
import com.futo.platformplayer.getOrThrow
import com.futo.platformplayer.getOrThrowNullable
import com.futo.platformplayer.invokeV8
import com.futo.platformplayer.requireSourcePlugin
import com.futo.platformplayer.serializers.OffsetDateTimeNullableSerializer
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@kotlinx.serialization.Serializable
class JSComment : IPlatformComment {
    @kotlinx.serialization.Transient
    private var _hasGetReplies: Boolean = false;

    @kotlinx.serialization.Transient
    private var _config: SourcePluginConfig? = null;
    @kotlinx.serialization.Transient
    private var _comment: V8ValueObject? = null;
    @kotlinx.serialization.Transient
    private var _plugin: V8Plugin? = null;

    @kotlinx.serialization.Transient
    override var sourcePluginId: String? = null
        private set

    override val contextUrl: String;
    override val author: PlatformAuthorLink;
    override var message: String
        private set
    override val rating: IRating;
    @kotlinx.serialization.Serializable(with = OffsetDateTimeNullableSerializer::class)
    override val date: OffsetDateTime?;
    override val replyCount: Int?;
    override val stableId: String?;
    override val isOwnedByUser: Boolean;
    override var isEdited: Boolean
        private set
    override var userReaction: PlatformCommentReaction
        private set
    override val capabilities: Set<PlatformCommentCapability>;
    override val visibility: PlatformCommentVisibility;

    override val context: Map<String, String>;


    constructor(config: SourcePluginConfig, plugin: V8Plugin, obj: V8ValueObject) {
        _config = config;
        _comment = obj;
        _plugin = plugin;
        sourcePluginId = config.id;

        var parsedContextUrl: String? = null;
        var parsedAuthor: PlatformAuthorLink? = null;
        var parsedMessage: String? = null;
        var parsedRating: IRating? = null;
        var parsedDate: OffsetDateTime? = null;
        var parsedReplyCount: Int? = null;
        var parsedContext: Map<String, String>? = null;
        var parsedStableId: String? = null;
        var parsedOwned = false;
        var parsedEdited = false;
        var parsedReaction = PlatformCommentReaction.NONE;
        var parsedCapabilities: Set<PlatformCommentCapability> = emptySet();
        var parsedVisibility = PlatformCommentVisibility.UNKNOWN;
        var parsedHasGetReplies = false;

        plugin.busy {
            val contextName = "Comment";
            parsedContextUrl = _comment!!.getOrThrow(config, "contextUrl", contextName);
            parsedAuthor = PlatformAuthorLink.fromV8(_config!!, _comment!!.getOrThrow(config, "author", contextName));
            parsedMessage = _comment!!.getOrThrow(config, "message", contextName);
            parsedRating = IRating.fromV8(config, _comment!!.getOrThrow(config, "rating", contextName));
            parsedDate = _comment!!.getOrThrowNullable<Int>(config, "date", contextName)?.let { OffsetDateTime.of(LocalDateTime.ofEpochSecond(it.toLong(), 0, ZoneOffset.UTC), ZoneOffset.UTC) };
            parsedReplyCount = _comment!!.getOrThrowNullable(config, "replyCount", contextName);
            parsedContext = _comment!!.getOrDefault(config, "context", contextName, hashMapOf()) ?: hashMapOf();
            parsedStableId = _comment!!.getOrDefault(config, "id", contextName, null);
            parsedOwned = _comment!!.getOrDefault(config, "isOwnedByUser", contextName, false) ?: false;
            parsedEdited = _comment!!.getOrDefault(config, "isEdited", contextName, false) ?: false;
            parsedReaction = enumValueOrDefault(
                _comment!!.getOrDefault(config, "userReaction", contextName, "NONE"),
                PlatformCommentReaction.NONE
            );
            parsedCapabilities = (_comment!!.getOrDefault<List<String>>(config, "capabilities", contextName, emptyList()) ?: emptyList())
                .mapNotNull { value -> enumValueOrNull<PlatformCommentCapability>(value) }
                .toSet();
            parsedVisibility = enumValueOrDefault(
                _comment!!.getOrDefault(config, "visibility", contextName, "UNKNOWN"),
                PlatformCommentVisibility.UNKNOWN
            );
            parsedHasGetReplies = _comment!!.has("getReplies");
        }

        contextUrl = parsedContextUrl ?: "";
        author = parsedAuthor ?: PlatformAuthorLink.UNKNOWN;
        message = parsedMessage ?: "";
        rating = parsedRating ?: throw IllegalStateException("Missing comment rating");
        date = parsedDate;
        replyCount = parsedReplyCount;
        stableId = parsedStableId;
        isOwnedByUser = parsedOwned;
        isEdited = parsedEdited;
        userReaction = parsedReaction;
        capabilities = parsedCapabilities;
        visibility = parsedVisibility;
        // V8 is dynamically typed and can return null values through a Java
        // platform type even though the public contract is Map<String, String>.
        // Sanitize before this object can be serialized back into a mutation.
        context = sanitizeCommentContext(parsedContext);
        _hasGetReplies = parsedHasGetReplies;
    }

    override fun getReplies(client: IPlatformClient): IPager<IPlatformComment>? {
        if(!_hasGetReplies)
            return null;

        val plugin = if(client is JSClient) client else throw NotImplementedError("Only implemented for JSClient");
        return _comment!!.requireSourcePlugin("Comment.getReplies").busy {
            val obj = _comment!!.invokeV8<V8ValueObject>("getReplies", arrayOf<Any>());
            return@busy JSCommentPager(_config!!, plugin, obj);
        }
    }

    internal fun updateUserReaction(reaction: PlatformCommentReaction) {
        userReaction = reaction;
    }

    internal fun updateAfterAcknowledgedEdit(updatedMessage: String) {
        message = updatedMessage
        isEdited = true
    }

    companion object {
        private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
            value?.let { raw -> enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } };

        private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
            enumValueOrNull<T>(value) ?: default;
    }
}
