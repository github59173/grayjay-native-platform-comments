package com.futo.platformplayer.views.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.futo.platformplayer.R
import com.futo.platformplayer.Settings
import com.futo.platformplayer.UIDialogs
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.LazyComment
import com.futo.platformplayer.api.media.models.comments.PolycentricPlatformComment
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingState
import com.futo.platformplayer.api.media.models.comments.PlatformCommentReaction
import com.futo.platformplayer.api.media.models.comments.PlatformCommentUiPolicy
import com.futo.platformplayer.api.media.models.ratings.RatingLikeDislikes
import com.futo.platformplayer.api.media.models.ratings.RatingLikes
import com.futo.platformplayer.constructs.Event1
import com.futo.platformplayer.dialogs.resolveReplyMention
import com.futo.platformplayer.fixHtmlLinks
import com.futo.platformplayer.resolvePlatformAccentColor
import com.futo.platformplayer.setPlatformPlayerLinkMovementMethod
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StatePolycentric
import com.futo.platformplayer.states.StatePlatform
import com.futo.platformplayer.toHumanNowDiffString
import com.futo.platformplayer.toHumanNumber
import com.futo.platformplayer.views.LoaderView
import com.futo.platformplayer.views.others.CreatorThumbnail
import com.futo.platformplayer.views.pills.PillButton
import com.futo.platformplayer.views.pills.PillRatingLikesDislikes
import com.futo.polycentric.core.ApiMethods
import com.futo.polycentric.core.Opinion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentViewHolder : ViewHolder {
    private val _creatorThumbnail: CreatorThumbnail;
    private val _textAuthor: TextView;
    private val _textMetadata: TextView;
    private val _textBody: TextView;
    private val _imageLikeIcon: ImageView;
    private val _textLikes: TextView;
    private val _imageDislikeIcon: ImageView;
    private val _textDislikes: TextView;
    private val _buttonReplies: PillButton;
    private val _layoutRating: LinearLayout;
    private val _pillRatingLikesDislikes: PillRatingLikesDislikes;
    private val _layoutComment: ConstraintLayout;
    private val _buttonOptions: ImageButton;

    private val _containerComments: ConstraintLayout;
    private val _loader: LoaderView;

    private var reactionJob: Job? = null
    @Volatile private var desiredPlatformReaction = PlatformCommentReaction.NONE
    @Volatile private var confirmedPlatformReaction = PlatformCommentReaction.NONE
    private var reactionGeneration = 0
    private var replyThreadParent: IPlatformComment? = null
    private var replyThreadState = PlatformCommentingState.UNKNOWN

    var onRepliesClick = Event1<IPlatformComment>();
    var onReply = Event1<IPlatformComment>();
    var onDelete = Event1<IPlatformComment>();
    var onEdit = Event1<IPlatformComment>();
    var onAuthorClick = Event1<IPlatformComment>();
    var comment: IPlatformComment? = null
        private set;

    constructor(viewGroup: ViewGroup) : super(LayoutInflater.from(viewGroup.context).inflate(R.layout.list_comment, viewGroup, false)) {
        _layoutComment = itemView.findViewById(R.id.layout_comment);
        _creatorThumbnail = itemView.findViewById(R.id.image_thumbnail);
        _textAuthor = itemView.findViewById(R.id.text_author);
        _textMetadata = itemView.findViewById(R.id.text_metadata);
        _textBody = itemView.findViewById(R.id.text_body);
        _imageLikeIcon = itemView.findViewById(R.id.image_like_icon);
        _textLikes = itemView.findViewById(R.id.text_likes);
        _imageDislikeIcon = itemView.findViewById(R.id.image_dislike_icon);
        _textDislikes = itemView.findViewById(R.id.text_dislikes);
        _buttonReplies = itemView.findViewById(R.id.button_replies);
        _layoutRating = itemView.findViewById(R.id.layout_rating);
        _pillRatingLikesDislikes = itemView.findViewById(R.id.rating);
        _buttonOptions = itemView.findViewById(R.id.button_comment_options);

        _containerComments = itemView.findViewById(R.id.comment_container);
        _loader = itemView.findViewById(R.id.loader);

        _pillRatingLikesDislikes.onLikeDislikeUpdated.subscribe { args ->
            val c = comment ?: return@subscribe
            if (c !is PolycentricPlatformComment)
                return@subscribe

            val newOpinion: Opinion = if (args.hasLiked) {
                Opinion.like
            } else if (args.hasDisliked) {
                Opinion.dislike
            } else {
                Opinion.neutral
            }

            _layoutComment.alpha = if (args.dislikes > 2 && args.dislikes.toFloat() / (args.likes + args.dislikes).toFloat() >= 0.7f) 0.5f else 1.0f;

            StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                ApiMethods.setOpinion(args.processHandle, c.reference, newOpinion)
            }

            StatePolycentric.instance.updateLikeMap(c.reference, args.hasLiked, args.hasDisliked)
        };

        _creatorThumbnail.onClick.subscribe {
            val c = comment ?: return@subscribe;
            onAuthorClick.emit(c);
        }

        _creatorThumbnail.setOnClickListener {
            val c = comment ?: return@setOnClickListener;
            onAuthorClick.emit(c);
        }
        _textAuthor.setOnClickListener {
            val c = comment ?: return@setOnClickListener;
            onAuthorClick.emit(c);
        }
        _buttonReplies.onClick.subscribe {
            val c = comment ?: return@subscribe;
            onRepliesClick.emit(c);
        }

        _buttonOptions.setOnClickListener { showCommentOptions() }

        _textBody.setPlatformPlayerLinkMovementMethod(viewGroup.context)
    }

    private fun showCommentOptions() {
        val c = comment ?: return
        val ownsPolycentricComment = c is PolycentricPlatformComment &&
            StatePolycentric.instance.processHandle?.system == c.eventPointer.system
        val canEdit = c !is PolycentricPlatformComment && PlatformCommentUiPolicy.canEdit(c)
        val canDelete = ownsPolycentricComment ||
            (c !is PolycentricPlatformComment && PlatformCommentUiPolicy.canDelete(c))
        val canReply = c !is PolycentricPlatformComment &&
            PlatformCommentUiPolicy.canReplyToOtherUser(c, replyThreadParent, replyThreadState) &&
            resolveReplyMention(c.author.name, c.author.url).isNotEmpty()
        val replyLocked = c !is PolycentricPlatformComment &&
            PlatformCommentUiPolicy.isReplyLockedInThread(c, replyThreadParent, replyThreadState)
        PopupMenu(itemView.context, _buttonOptions).apply {
            if (canReply) {
                menu.add(MENU_GROUP_COMMENT, MENU_REPLY, 0, R.string.reply)
            } else if (replyLocked) {
                menu.add(MENU_GROUP_COMMENT, MENU_REPLY_LOCKED, 0, R.string.reply).apply {
                    setIcon(R.drawable.ic_lock_18)
                    isEnabled = false
                }
                setForceShowIcon(true)
            }
            menu.add(MENU_GROUP_COMMENT, MENU_COPY, 1, R.string.copy)
            if (canEdit)
                menu.add(MENU_GROUP_COMMENT, MENU_EDIT, 2, R.string.edit_comment)
            if (canDelete)
                menu.add(MENU_GROUP_COMMENT, MENU_DELETE, 3, R.string.delete)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_REPLY -> onReply.emit(c)
                    MENU_COPY -> {
                        val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Comment", c.message))
                        UIDialogs.toast(itemView.context, "Copied", false)
                    }
                    MENU_EDIT -> onEdit.emit(c)
                    MENU_DELETE -> onDelete.emit(c)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun queuePlatformReaction(c: IPlatformComment, desired: PlatformCommentReaction) {
        desiredPlatformReaction = desired
        if (reactionJob?.isActive == true)
            return

        val generation = reactionGeneration
        reactionJob = StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            while (generation == reactionGeneration && this@CommentViewHolder.comment === c) {
                val target = desiredPlatformReaction
                val previous = confirmedPlatformReaction
                if (target == previous) break
                val result = when (target) {
                    PlatformCommentReaction.LIKE -> StatePlatform.instance.likeComment(c, true)
                    PlatformCommentReaction.DISLIKE -> StatePlatform.instance.dislikeComment(c, true)
                    PlatformCommentReaction.NONE -> if (previous == PlatformCommentReaction.LIKE)
                        StatePlatform.instance.likeComment(c, false)
                    else
                        StatePlatform.instance.dislikeComment(c, false)
                }
                if (result.success) {
                    confirmedPlatformReaction = result.reaction
                } else {
                    desiredPlatformReaction = confirmedPlatformReaction
                    withContext(Dispatchers.Main) {
                        if (generation != reactionGeneration || this@CommentViewHolder.comment !== c)
                            return@withContext
                        _pillRatingLikesDislikes.setRating(
                            c.rating,
                            confirmedPlatformReaction == PlatformCommentReaction.LIKE,
                            confirmedPlatformReaction == PlatformCommentReaction.DISLIKE,
                            PlatformCommentUiPolicy.canDislike(c)
                        )
                        UIDialogs.toast(itemView.context, result.message ?: itemView.context.getString(R.string.failed_to_update_comment_reaction))
                    }
                    break
                }
            }
        }
    }

    fun bind(
        comment: IPlatformComment,
        readonly: Boolean,
        replyThreadParent: IPlatformComment? = null,
        replyThreadState: PlatformCommentingState = PlatformCommentingState.UNKNOWN
    ) {
        this.replyThreadParent = replyThreadParent
        this.replyThreadState = replyThreadState

        reactionJob?.cancel()
        reactionJob = null
        _pillRatingLikesDislikes.setPlatformMutationHandler()
        reactionGeneration += 1
        confirmedPlatformReaction = comment.userReaction
        desiredPlatformReaction = comment.userReaction

        if(comment is LazyComment){
            if(comment.isAvailable)
            {
                comment.getUnderlyingComment()?.let {
                    bind(it, readonly, replyThreadParent, replyThreadState);
                }
                return;
            }
            else {
                _loader.visibility = View.VISIBLE;
                _loader.start();
                _containerComments.visibility = View.GONE;
                comment.setUIHandler {
                    StateApp.instance.scopeOrNull?.launch(Dispatchers.Main) {
                        if (it.isAvailable && it == this@CommentViewHolder.comment)
                            bind(it, readonly, replyThreadParent, replyThreadState);
                    }
                }
            }
        }
        else {
            _loader.stop();
            _loader.visibility = View.GONE;
            _containerComments.visibility = View.VISIBLE;
        }

        _creatorThumbnail.setThumbnail(comment.author.thumbnail, false);
        val polycentricComment = if (comment is PolycentricPlatformComment) comment else null
        _creatorThumbnail.setHarborAvailable(polycentricComment != null,false, polycentricComment?.eventPointer?.system?.toProto());
        _textAuthor.text = comment.author.name;

        val date = comment.date;
        if (date != null) {
            _textMetadata.visibility = View.VISIBLE;
            _textMetadata.text = if (comment.isEdited)
                itemView.context.getString(R.string.comment_metadata_age_edited, date.toHumanNowDiffString())
            else
                itemView.context.getString(R.string.comment_metadata_age, date.toHumanNowDiffString())
        } else if (comment.isEdited) {
            _textMetadata.visibility = View.VISIBLE
            _textMetadata.setText(R.string.comment_metadata_edited)
        } else {
            _textMetadata.visibility = View.GONE;
        }

        val rating = comment.rating;
        if (rating is RatingLikeDislikes) {
            _layoutComment.alpha = if (Settings.instance.comments.badReputationCommentsFading &&
                rating.dislikes > 2 && rating.dislikes.toFloat() / (rating.likes + rating.dislikes).toFloat() >= 0.7f) 0.5f else 1.0f;
        } else {
            _layoutComment.alpha = 1.0f;
        }

        _textBody.text = comment.message.fixHtmlLinks();

        val sourceReactionColor = if (comment is PolycentricPlatformComment) {
            null
        } else {
            comment.sourcePluginId
                ?.let(StatePlatform.instance::getClientOrNull)
                ?.resolvePlatformAccentColor(itemView.context)
        }
        _pillRatingLikesDislikes.setReactionColor(sourceReactionColor)

        val hasPlatformRating = PlatformCommentUiPolicy.canReact(comment)
        if (!readonly && comment !is PolycentricPlatformComment && hasPlatformRating) {
            _pillRatingLikesDislikes.setPlatformMutationHandler(
                canLike = PlatformCommentUiPolicy.canLike(comment),
                canDislike = PlatformCommentUiPolicy.canDislike(comment)
            ) { hasLiked, hasDisliked ->
                val desired = if (hasLiked) {
                    PlatformCommentReaction.LIKE
                } else if (hasDisliked) {
                    PlatformCommentReaction.DISLIKE
                } else {
                    PlatformCommentReaction.NONE
                }
                queuePlatformReaction(comment, desired)
            }
        }
        if (readonly || (comment !is PolycentricPlatformComment && !hasPlatformRating)) {
            _layoutRating.visibility = View.VISIBLE;
            _pillRatingLikesDislikes.visibility = View.GONE;

            when (comment.rating) {
                is RatingLikeDislikes -> {
                    val r = comment.rating as RatingLikeDislikes;
                    _textLikes.visibility = View.VISIBLE;
                    _imageLikeIcon.visibility = View.VISIBLE;
                    _textLikes.text = r.likes.toHumanNumber();

                    _imageDislikeIcon.visibility = View.VISIBLE;
                    _textDislikes.visibility = View.VISIBLE;
                    _textDislikes.text = r.dislikes.toHumanNumber();
                }
                is RatingLikes -> {
                    val r = comment.rating as RatingLikes;
                    _textLikes.visibility = View.VISIBLE;
                    _imageLikeIcon.visibility = View.VISIBLE;
                    _textLikes.text = r.likes.toHumanNumber();

                    _imageDislikeIcon.visibility = View.GONE;
                    _textDislikes.visibility = View.GONE;
                }
                else -> {
                    _textLikes.visibility = View.GONE;
                    _imageLikeIcon.visibility = View.GONE;
                    _imageDislikeIcon.visibility = View.GONE;
                    _textDislikes.visibility = View.GONE;
                }
            }
        } else {
            _layoutRating.visibility = View.GONE;
            _pillRatingLikesDislikes.visibility = View.VISIBLE;

            if (comment is PolycentricPlatformComment) {
                val hasLiked = StatePolycentric.instance.hasLiked(comment.reference.toByteArray());
                val hasDisliked = StatePolycentric.instance.hasDisliked(comment.reference.toByteArray());
                _pillRatingLikesDislikes.setRating(comment.rating, hasLiked, hasDisliked);
            } else {
                _pillRatingLikesDislikes.setRating(
                    comment.rating,
                    comment.userReaction == PlatformCommentReaction.LIKE,
                    comment.userReaction == PlatformCommentReaction.DISLIKE,
                    PlatformCommentUiPolicy.canDislike(comment)
                );
            }
        }

        val replies = comment.replyCount ?: 0;
        val canReply = if (comment is PolycentricPlatformComment)
            !readonly
        else
            PlatformCommentUiPolicy.canReply(comment)
        val replyLocked = comment !is PolycentricPlatformComment && PlatformCommentUiPolicy.isReplyLocked(comment)
        if (canReply || replies > 0) {
            _buttonReplies.visibility = View.VISIBLE;
            _buttonReplies.icon.setImageResource(if (replyLocked) R.drawable.ic_lock_18 else R.drawable.ic_forum)
            _buttonReplies.alpha = if (replyLocked) 0.65f else 1.0f
            _buttonReplies.text.text = itemView.context.resources.getQuantityString(R.plurals.reply_count, replies, replies)
        } else {
            _buttonReplies.visibility = View.GONE;
            _buttonReplies.alpha = 1.0f
        }

        _buttonOptions.visibility = View.VISIBLE

        this.comment = comment;
    }

    companion object {
        private const val TAG = "CommentViewHolder";
        private const val MENU_GROUP_COMMENT = 1
        private const val MENU_COPY = 1
        private const val MENU_EDIT = 2
        private const val MENU_DELETE = 3
        private const val MENU_REPLY = 4
        private const val MENU_REPLY_LOCKED = 5
    }
}
