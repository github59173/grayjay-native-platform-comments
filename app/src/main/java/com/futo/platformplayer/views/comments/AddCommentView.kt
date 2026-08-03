package com.futo.platformplayer.views.comments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import com.futo.platformplayer.R
import com.futo.platformplayer.UIDialogs
import com.futo.platformplayer.dp
import com.futo.platformplayer.api.media.models.comments.CommentDestination
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.PlatformCommentingState
import com.futo.platformplayer.api.media.models.comments.PlatformCommentUiPolicy
import com.futo.platformplayer.constructs.Event1
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.states.StatePolycentric
import com.futo.platformplayer.states.StatePlatform
import userpackage.Protocol

class AddCommentView : LinearLayout {
    private val _textComment: TextView;

    private var _contextUrl: String? = null
    private var _ref: Protocol.Reference? = null
    private var _parentPlatformComment: IPlatformComment? = null
    private var _preferredDestination: CommentDestination? = null
    private var _restrictToPreferredDestination = false
    private var _commentingState = PlatformCommentingState.UNKNOWN
    private var _lastClickTime = 0L

    val onCommentAdded = Event1<IPlatformComment>();

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_add_comment, this, true);

        _textComment = findViewById(R.id.edit_comment);
        _textComment.setOnClickListener {
            val cu = _contextUrl ?: return@setOnClickListener
            val ref = _ref

            val now = System.currentTimeMillis()
            if (now - _lastClickTime > 3000) {
                val client = StatePlatform.instance.getContentClientOrNull(cu)
                val platformAvailable = if (_parentPlatformComment != null)
                    _commentingState.availability == com.futo.platformplayer.api.media.models.comments.PlatformCommentingAvailability.AVAILABLE ||
                        (!_commentingState.isLocked && PlatformCommentUiPolicy.canReply(_parentPlatformComment))
                else
                    PlatformCommentUiPolicy.canCreate(client?.capabilities)
                val polycentricAvailable = ref != null && StatePolycentric.instance.processHandle != null
                val showDialog = {
                    try {
                        UIDialogs.showCommentDialog(
                            context,
                            cu,
                            ref,
                            client,
                            _parentPlatformComment,
                            _preferredDestination,
                            _restrictToPreferredDestination
                        ) { onCommentAdded.emit(it) };
                    } catch (e: Throwable) {
                        Logger.w(TAG, "Failed to post comment", e);
                        UIDialogs.toast(context, context.getString(R.string.failed_to_post_comment) + " ${e.message}");
                    }
                }
                when (_preferredDestination) {
                    CommentDestination.PLATFORM -> {
                        if (platformAvailable) showDialog()
                        else UIDialogs.toast(context, context.getString(R.string.no_comment_destination_available))
                    }
                    CommentDestination.POLYCENTRIC -> {
                        when {
                            ref == null -> UIDialogs.toast(context, context.getString(R.string.no_comment_destination_available))
                            polycentricAvailable -> showDialog()
                            else -> StatePolycentric.instance.requireLogin(
                                context,
                                context.getString(R.string.please_login_to_post_a_comment)
                            ) { showDialog() }
                        }
                    }
                    null -> {
                        if (platformAvailable) showDialog()
                        else StatePolycentric.instance.requireLogin(
                            context,
                            context.getString(R.string.please_login_to_post_a_comment)
                        ) { showDialog() }
                    }
                }

                _lastClickTime = now
            }
        }
    }

    fun setContext(
        contextUrl: String?,
        ref: Protocol.Reference?,
        parentPlatformComment: IPlatformComment? = null,
        preferredDestination: CommentDestination? = null,
        restrictToPreferredDestination: Boolean = false
    ) {
        if (_contextUrl != contextUrl)
            _commentingState = PlatformCommentingState.UNKNOWN
        _contextUrl = contextUrl;
        _ref = ref;
        _parentPlatformComment = parentPlatformComment;
        setDestination(preferredDestination, restrictToPreferredDestination)
    }

    fun setDestination(destination: CommentDestination?, restrictToDestination: Boolean = false) {
        _preferredDestination = destination
        _restrictToPreferredDestination = restrictToDestination
        renderState()
    }

    fun setCommentingState(state: PlatformCommentingState) {
        _commentingState = state
        renderState()
    }

    private fun renderState() {
        val platformDestination = _preferredDestination == CommentDestination.PLATFORM ||
            _parentPlatformComment != null
        val locked = platformDestination && _commentingState.isLocked
        val destinationLabel = when (_preferredDestination) {
            CommentDestination.PLATFORM -> _contextUrl?.let {
                StatePlatform.instance.getContentClientOrNull(it)?.name
                    ?.substringBefore(" — ")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            } ?: context.getString(R.string.platform)
            CommentDestination.POLYCENTRIC -> context.getString(R.string.polycentric)
            null -> null
        }
        _textComment.text = when {
            locked && _parentPlatformComment != null -> context.getString(R.string.replies_are_locked)
            locked && destinationLabel != null -> context.getString(R.string.destination_comments_are_locked, destinationLabel)
            locked -> context.getString(R.string.comments_are_locked)
            destinationLabel != null -> context.getString(R.string.add_a_destination_comment, destinationLabel)
            else -> context.getString(R.string.add_a_comment)
        }
        _textComment.isEnabled = !locked
        _textComment.alpha = if (locked) 0.55f else 1.0f
        _textComment.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (locked) R.drawable.ic_lock_18 else 0,
            0,
            0,
            0
        )
        _textComment.compoundDrawablePadding = if (locked) 8.dp(resources) else 0
        _textComment.contentDescription = if (locked)
            _commentingState.reason ?: _textComment.text
        else
            _textComment.text
    }

    companion object {
        const val TAG = "AddCommentView"
    }
}
