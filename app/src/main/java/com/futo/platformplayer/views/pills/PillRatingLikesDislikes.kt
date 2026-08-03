package com.futo.platformplayer.views.pills

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.futo.platformplayer.R
import com.futo.platformplayer.api.media.models.ratings.IRating
import com.futo.platformplayer.api.media.models.ratings.RatingLikeDislikes
import com.futo.platformplayer.api.media.models.ratings.RatingLikes
import com.futo.platformplayer.api.media.models.video.PlatformVideoReaction
import com.futo.platformplayer.constructs.Event1
import com.futo.platformplayer.states.StatePolycentric
import com.futo.platformplayer.toHumanNumber
import com.futo.platformplayer.views.LoaderView
import com.futo.polycentric.core.ProcessHandle

data class OnLikeDislikeUpdatedArgs(
    val processHandle: ProcessHandle,
    val likes: Long,
    val hasLiked: Boolean,
    val dislikes: Long,
    val hasDisliked: Boolean,
)

enum class VideoReactionNetwork {
    POLYCENTRIC,
    PLATFORM
}

class PillRatingLikesDislikes : LinearLayout {
    private val _layoutLike: LinearLayout
    private val _layoutDislike: LinearLayout
    private val _pillRoot: LinearLayout
    private val _layoutPlatformRow: LinearLayout
    private val _layoutPlatformLike: LinearLayout
    private val _layoutPlatformDislike: LinearLayout
    private val _platformRowSeparator: View
    private val _textLikes: TextView
    private val _textDislikes: TextView
    private val _textPlatformLikes: TextView
    private val _textPlatformDislikes: TextView
    private val _loaderViewLikes: LoaderView
    private val _loaderViewDislikes: LoaderView
    private val _loaderViewPlatformLikes: LoaderView
    private val _loaderViewPlatformDislikes: LoaderView
    private val _separator: View
    private val _platformSeparator: View
    private val _iconLikes: ImageView
    private val _iconDislikes: ImageView
    private val _iconPlatformLikes: ImageView
    private val _iconPlatformDislikes: ImageView
    private val _normalLikeLayoutParams: LayoutParams
    private val _normalDislikeLayoutParams: LayoutParams
    private val _normalSeparatorLayoutParams: LayoutParams
    private var _normalTopMargin: Int? = null
    private var _isLoading = false

    private var _likes = 0L
    private var _hasLiked = false
    private var _dislikes = 0L
    private var _hasDisliked = false
    private var _platformCanLike = false
    private var _platformCanDislike = false
    private var _reactionColor: Int? = null
    private var _platformMutationHandler: ((Boolean, Boolean) -> Unit)? = null

    private var _dualMode = false
    private var _dualPolycentricLoaded = false
    private var _dualPlatformLoaded = false
    private var _dualPlatformCanLike = false
    private var _dualPlatformCanDislike = false
    private var _dualPlatformDislikeCountAvailable = false
    private var _dualPlatformColor = 0
    private var _dualPlatformName = ""
    private var _dualPolycentricRating = RatingLikeDislikes(0, 0)
    private var _dualPlatformRating = RatingLikeDislikes(0, 0)
    private var _dualPolycentricReaction = PlatformVideoReaction.NONE
    private var _dualPlatformReaction = PlatformVideoReaction.NONE
    private var _dualMutationHandler: ((VideoReactionNetwork, PlatformVideoReaction) -> Unit)? = null

    val onLikeDislikeUpdated = Event1<OnLikeDislikeUpdatedArgs>()

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.rating_likesdislikes, this, true)
        _pillRoot = findViewById(R.id.layout_rating_pill_root)
        _layoutLike = findViewById(R.id.layout_like)
        _layoutDislike = findViewById(R.id.layout_dislike)
        _layoutPlatformRow = findViewById(R.id.layout_platform_row)
        _layoutPlatformLike = findViewById(R.id.layout_platform_like)
        _layoutPlatformDislike = findViewById(R.id.layout_platform_dislike)
        _platformRowSeparator = findViewById(R.id.pill_platform_row_separator)
        _textLikes = findViewById(R.id.pill_likes)
        _textDislikes = findViewById(R.id.pill_dislikes)
        _textPlatformLikes = findViewById(R.id.pill_platform_likes)
        _textPlatformDislikes = findViewById(R.id.pill_platform_dislikes)
        _separator = findViewById(R.id.pill_seperator)
        _platformSeparator = findViewById(R.id.pill_platform_seperator)
        _iconDislikes = findViewById(R.id.pill_dislike_icon)
        _iconLikes = findViewById(R.id.pill_like_icon)
        _iconPlatformLikes = findViewById(R.id.pill_platform_like_icon)
        _iconPlatformDislikes = findViewById(R.id.pill_platform_dislike_icon)
        _loaderViewLikes = findViewById(R.id.loader_likes)
        _loaderViewDislikes = findViewById(R.id.loader_dislikes)
        _loaderViewPlatformLikes = findViewById(R.id.loader_platform_likes)
        _loaderViewPlatformDislikes = findViewById(R.id.loader_platform_dislikes)
        _normalLikeLayoutParams = LayoutParams(_layoutLike.layoutParams as LayoutParams)
        _normalDislikeLayoutParams = LayoutParams(_layoutDislike.layoutParams as LayoutParams)
        _normalSeparatorLayoutParams = LayoutParams(_separator.layoutParams as LayoutParams)

        _layoutLike.setOnClickListener {
            if (_isLoading) return@setOnClickListener
            if (_dualMode) {
                if (!_dualPolycentricLoaded) return@setOnClickListener
                _dualMutationHandler?.invoke(
                    VideoReactionNetwork.POLYCENTRIC,
                    toggledReaction(_dualPolycentricReaction, PlatformVideoReaction.LIKE)
                )
                return@setOnClickListener
            }
            val platformHandler = _platformMutationHandler
            if (platformHandler != null) {
                if (!_platformCanLike) return@setOnClickListener
                updateLike()
                platformHandler(_hasLiked, _hasDisliked)
            } else {
                StatePolycentric.instance.requireLogin(context, context.getString(R.string.please_login_to_like)) { like(it) }
            }
        }
        _layoutDislike.setOnClickListener {
            if (_isLoading) return@setOnClickListener
            if (_dualMode) {
                if (!_dualPolycentricLoaded) return@setOnClickListener
                _dualMutationHandler?.invoke(
                    VideoReactionNetwork.POLYCENTRIC,
                    toggledReaction(_dualPolycentricReaction, PlatformVideoReaction.DISLIKE)
                )
                return@setOnClickListener
            }
            val platformHandler = _platformMutationHandler
            if (platformHandler != null) {
                if (!_platformCanDislike) return@setOnClickListener
                updateDislike()
                platformHandler(_hasLiked, _hasDisliked)
            } else {
                StatePolycentric.instance.requireLogin(context, context.getString(R.string.please_login_to_dislike)) { dislike(it) }
            }
        }
        _layoutPlatformLike.setOnClickListener {
            if (!_dualMode || _isLoading || !_dualPlatformLoaded) return@setOnClickListener
            val desired = toggledReaction(_dualPlatformReaction, PlatformVideoReaction.LIKE)
            if (desired == PlatformVideoReaction.LIKE && !_dualPlatformCanLike) return@setOnClickListener
            _dualMutationHandler?.invoke(VideoReactionNetwork.PLATFORM, desired)
        }
        _layoutPlatformDislike.setOnClickListener {
            if (!_dualMode || _isLoading || !_dualPlatformLoaded) return@setOnClickListener
            if (!_dualPlatformDislikeCountAvailable) return@setOnClickListener
            val desired = toggledReaction(_dualPlatformReaction, PlatformVideoReaction.DISLIKE)
            if (desired == PlatformVideoReaction.DISLIKE && !_dualPlatformCanDislike) return@setOnClickListener
            _dualMutationHandler?.invoke(VideoReactionNetwork.PLATFORM, desired)
        }
    }

    fun setPlatformMutationHandler(
        canLike: Boolean = false,
        canDislike: Boolean = false,
        handler: ((Boolean, Boolean) -> Unit)? = null
    ) {
        clearDualReactionMode(update = false)
        _platformCanLike = canLike
        _platformCanDislike = canDislike
        _platformMutationHandler = handler
    }

    /** Sets the selected-state color for a single-network reaction pill. */
    fun setReactionColor(color: Int?) {
        if (_reactionColor == color) return
        _reactionColor = color
        if (!_dualMode) updateColors()
    }

    fun setDualReactionState(
        polycentricRating: RatingLikeDislikes,
        polycentricReaction: PlatformVideoReaction,
        polycentricLoaded: Boolean,
        platformRating: RatingLikeDislikes,
        platformReaction: PlatformVideoReaction,
        platformLoaded: Boolean,
        platformColor: Int,
        platformName: String,
        platformCanLike: Boolean,
        platformCanDislike: Boolean,
        platformDislikeCountAvailable: Boolean,
        handler: (VideoReactionNetwork, PlatformVideoReaction) -> Unit
    ) {
        _platformMutationHandler = null
        _platformCanLike = false
        _platformCanDislike = false
        _dualMode = true
        _dualPolycentricRating = polycentricRating
        _dualPolycentricReaction = polycentricReaction
        _dualPolycentricLoaded = polycentricLoaded
        _dualPlatformRating = platformRating
        _dualPlatformReaction = platformReaction
        _dualPlatformLoaded = platformLoaded
        _dualPlatformColor = platformColor
        _dualPlatformName = platformName
        _dualPlatformCanLike = platformCanLike
        _dualPlatformCanDislike = platformCanDislike
        _dualPlatformDislikeCountAvailable = platformDislikeCountAvailable
        _dualMutationHandler = handler
        configureDualLayout(true)
        renderDualState()
    }

    fun clearDualReactionMode(update: Boolean = true) {
        if (!_dualMode && _dualMutationHandler == null) return
        _dualMode = false
        _dualPolycentricLoaded = false
        _dualPlatformLoaded = false
        _dualMutationHandler = null
        configureDualLayout(false)
        if (update) updateColors()
    }

    fun setLoading(loading: Boolean) {
        if (_isLoading == loading) return
        _isLoading = loading
        if (_dualMode) {
            alpha = if (loading) 0.65f else 1f
            return
        }

        if (loading) {
            _textLikes.visibility = View.GONE
            _loaderViewLikes.visibility = View.VISIBLE
            _textDislikes.visibility = View.GONE
            _loaderViewDislikes.visibility = View.VISIBLE
            _loaderViewLikes.start()
            _loaderViewDislikes.start()
        } else {
            _loaderViewLikes.stop()
            _loaderViewDislikes.stop()
            _textLikes.visibility = View.VISIBLE
            _loaderViewLikes.visibility = View.GONE
            _textDislikes.visibility = View.VISIBLE
            _loaderViewDislikes.visibility = View.GONE
        }
    }

    fun setRating(
        rating: IRating,
        hasLiked: Boolean = false,
        hasDisliked: Boolean = false,
        showDislikeAction: Boolean = false
    ) {
        clearDualReactionMode(update = false)
        setLoading(false)
        when (rating) {
            is RatingLikeDislikes -> setRating(rating, hasLiked, hasDisliked)
            is RatingLikes -> setRating(rating, hasLiked, hasDisliked, showDislikeAction)
            else -> throw Exception("Unknown rating type")
        }
    }

    fun setRating(rating: RatingLikeDislikes, hasLiked: Boolean = false, hasDisliked: Boolean = false) {
        clearDualReactionMode(update = false)
        setLoading(false)
        _textLikes.text = rating.likes.toHumanNumber()
        _textDislikes.text = rating.dislikes.toHumanNumber()
        _textLikes.visibility = View.VISIBLE
        _textDislikes.visibility = View.VISIBLE
        _separator.visibility = View.VISIBLE
        _iconDislikes.visibility = View.VISIBLE
        _likes = rating.likes
        _dislikes = rating.dislikes
        _hasLiked = hasLiked
        _hasDisliked = hasDisliked
        updateColors()
    }

    fun setRating(
        rating: RatingLikes,
        hasLiked: Boolean = false,
        hasDisliked: Boolean = false,
        showDislikeAction: Boolean = false
    ) {
        clearDualReactionMode(update = false)
        setLoading(false)
        _textLikes.text = rating.likes.toHumanNumber()
        _textLikes.visibility = View.VISIBLE
        _textDislikes.visibility = View.GONE
        _separator.visibility = if (showDislikeAction) View.VISIBLE else View.GONE
        _iconDislikes.visibility = if (showDislikeAction) View.VISIBLE else View.GONE
        _likes = rating.likes
        _dislikes = if (hasDisliked) 1 else 0
        _hasLiked = hasLiked
        _hasDisliked = hasDisliked
        updateColors()
    }

    fun like(processHandle: ProcessHandle) {
        updateLike()
        onLikeDislikeUpdated.emit(OnLikeDislikeUpdatedArgs(processHandle, _likes, _hasLiked, _dislikes, _hasDisliked))
    }

    private fun updateLike() {
        if (_hasDisliked) {
            _dislikes--
            _hasDisliked = false
            _textDislikes.text = _dislikes.toHumanNumber()
        }
        if (_hasLiked) {
            _likes--
            _hasLiked = false
        } else {
            _likes++
            _hasLiked = true
        }
        _textLikes.text = _likes.toHumanNumber()
        updateColors()
    }

    fun dislike(processHandle: ProcessHandle) {
        updateDislike()
        onLikeDislikeUpdated.emit(OnLikeDislikeUpdatedArgs(processHandle, _likes, _hasLiked, _dislikes, _hasDisliked))
    }

    private fun updateDislike() {
        if (_hasLiked) {
            _likes--
            _hasLiked = false
            _textLikes.text = _likes.toHumanNumber()
        }
        if (_hasDisliked) {
            _dislikes--
            _hasDisliked = false
        } else {
            _dislikes++
            _hasDisliked = true
        }
        _textDislikes.text = _dislikes.toHumanNumber()
        updateColors()
    }

    private fun configureDualLayout(enabled: Boolean) {
        _layoutPlatformRow.visibility = if (enabled) View.VISIBLE else View.GONE
        _platformRowSeparator.visibility = if (enabled) View.VISIBLE else View.GONE
        alpha = 1f
        (layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            if (enabled && _normalTopMargin == null) _normalTopMargin = params.topMargin
            params.width = if (enabled) dualVideoReactionWidth() else ViewGroup.LayoutParams.WRAP_CONTENT
            params.topMargin = if (enabled) 0 else (_normalTopMargin ?: params.topMargin)
            layoutParams = params
        }
        if (enabled) {
            _pillRoot.setBackgroundResource(R.drawable.background_video_action)
            val dividerWidth = dp(3)
            val dividerHeight = dp(25)
            minimumWidth = dualVideoReactionWidth()
            _layoutLike.layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            _layoutDislike.layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            _layoutLike.setPadding(dp(3), dp(6), dp(3), dp(7))
            _layoutDislike.setPadding(dp(3), dp(6), dp(3), dp(7))
            _iconLikes.layoutParams = _iconLikes.layoutParams.apply { width = dp(22) }
            _iconDislikes.layoutParams = _iconDislikes.layoutParams.apply { width = dp(22) }
            _iconPlatformLikes.layoutParams = _iconPlatformLikes.layoutParams.apply { width = dp(22) }
            _iconPlatformDislikes.layoutParams = _iconPlatformDislikes.layoutParams.apply { width = dp(22) }
            _separator.layoutParams = LayoutParams(dividerWidth, dividerHeight).apply { gravity = Gravity.CENTER_VERTICAL }
            _platformSeparator.layoutParams = LayoutParams(dividerWidth, dividerHeight).apply { gravity = Gravity.CENTER_VERTICAL }
            _separator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
        } else {
            _pillRoot.setBackgroundResource(R.drawable.background_pill)
            minimumWidth = 0
            _layoutLike.layoutParams = LayoutParams(_normalLikeLayoutParams)
            _layoutDislike.layoutParams = LayoutParams(_normalDislikeLayoutParams)
            _layoutLike.setPadding(dp(7), dp(6), dp(8), dp(7))
            _layoutDislike.setPadding(0, dp(6), dp(12), dp(7))
            _iconLikes.layoutParams = _iconLikes.layoutParams.apply { width = dp(30) }
            _iconDislikes.layoutParams = _iconDislikes.layoutParams.apply { width = dp(30) }
            _separator.layoutParams = LayoutParams(_normalSeparatorLayoutParams)
            _separator.setBackgroundColor(0xFF808080.toInt())
        }
        requestLayout()
    }

    private fun renderDualState() {
        val neutralColor = ContextCompat.getColor(context, R.color.white)
        val polycentricColor = ContextCompat.getColor(context, R.color.colorPrimary)
        _separator.setBackgroundColor(polycentricColor)
        _platformSeparator.setBackgroundColor(_dualPlatformColor)

        if (_dualPolycentricLoaded) {
            stopLoading(_loaderViewLikes, _textLikes)
            stopLoading(_loaderViewDislikes, _textDislikes)
            _textLikes.text = _dualPolycentricRating.likes.toHumanNumber()
            _textDislikes.text = _dualPolycentricRating.dislikes.toHumanNumber()
        } else {
            startLoading(_loaderViewLikes, _textLikes)
            startLoading(_loaderViewDislikes, _textDislikes)
        }

        stopLoading(_loaderViewPlatformLikes, _textPlatformLikes)
        _textPlatformLikes.text = _dualPlatformRating.likes.toHumanNumber()
        _textPlatformDislikes.text = _dualPlatformRating.dislikes.toHumanNumber()
        _textPlatformDislikes.visibility = if (_dualPlatformDislikeCountAvailable) View.VISIBLE else View.GONE
        _loaderViewPlatformDislikes.stop()
        _loaderViewPlatformDislikes.visibility = View.GONE

        updateReactionColor(
            _iconLikes,
            _textLikes,
            R.drawable.ic_thumb_up,
            _dualPolycentricReaction == PlatformVideoReaction.LIKE,
            polycentricColor,
            context.getString(R.string.video_reaction_like),
            context.getString(R.string.polycentric),
            neutralColor
        )
        updateReactionColor(
            _iconDislikes,
            _textDislikes,
            R.drawable.ic_thumb_down,
            _dualPolycentricReaction == PlatformVideoReaction.DISLIKE,
            polycentricColor,
            context.getString(R.string.video_reaction_dislike),
            context.getString(R.string.polycentric),
            neutralColor
        )
        updateReactionColor(
            _iconPlatformLikes,
            _textPlatformLikes,
            R.drawable.ic_thumb_up,
            _dualPlatformReaction == PlatformVideoReaction.LIKE,
            _dualPlatformColor,
            context.getString(R.string.video_reaction_like),
            _dualPlatformName,
            neutralColor
        )
        val platformDislikeInteractive = _dualPlatformDislikeCountAvailable &&
            _dualPlatformLoaded &&
            _dualPlatformCanDislike
        val platformDislikeVisuallyAvailable = _dualPlatformDislikeCountAvailable &&
            (!_dualPlatformLoaded || _dualPlatformCanDislike)
        _layoutPlatformDislike.isEnabled = platformDislikeInteractive
        _layoutPlatformDislike.alpha = if (platformDislikeVisuallyAvailable) 1f else 0.55f
        updateReactionColor(
            _iconPlatformDislikes,
            _textPlatformDislikes,
            R.drawable.ic_thumb_down,
            platformDislikeVisuallyAvailable && _dualPlatformReaction == PlatformVideoReaction.DISLIKE,
            _dualPlatformColor,
            context.getString(R.string.video_reaction_dislike),
            _dualPlatformName,
            if (platformDislikeVisuallyAvailable) neutralColor else ContextCompat.getColor(context, R.color.gray_67)
        )
        if (!_dualPlatformDislikeCountAvailable) {
            _iconPlatformDislikes.contentDescription = context.getString(R.string.platform_video_dislike_unavailable)
        }
    }

    private fun updateReactionColor(
        icon: ImageView,
        text: TextView,
        iconResource: Int,
        selected: Boolean,
        selectedColor: Int,
        actionName: String,
        networkName: String,
        neutralColor: Int
    ) {
        icon.setImageResource(iconResource)
        val color = if (selected) selectedColor else neutralColor
        icon.setColorFilter(color)
        text.setTextColor(color)
        icon.contentDescription = context.getString(
            R.string.video_reaction_accessibility,
            actionName,
            if (selected) networkName else context.getString(R.string.video_reaction_not_selected)
        )
    }

    private fun startLoading(loader: LoaderView, text: TextView) {
        text.visibility = View.GONE
        loader.visibility = View.VISIBLE
        loader.start()
    }

    private fun stopLoading(loader: LoaderView, text: TextView) {
        loader.stop()
        loader.visibility = View.GONE
        text.visibility = View.VISIBLE
    }

    private fun toggledReaction(
        current: PlatformVideoReaction,
        selected: PlatformVideoReaction
    ): PlatformVideoReaction = if (current == selected) PlatformVideoReaction.NONE else selected

    private fun updateColors() {
        if (_dualMode) {
            renderDualState()
            return
        }
        _iconLikes.setImageResource(R.drawable.ic_thumb_up)
        _iconDislikes.setImageResource(R.drawable.ic_thumb_down)
        _iconLikes.contentDescription = context.getString(R.string.video_reaction_like)
        _iconDislikes.contentDescription = context.getString(R.string.video_reaction_dislike)
        val selectedColor = _reactionColor ?: ContextCompat.getColor(context, R.color.colorPrimary)
        val neutralColor = ContextCompat.getColor(context, R.color.white)
        _textLikes.setTextColor(if (_hasLiked) selectedColor else neutralColor)
        _iconLikes.setColorFilter(if (_hasLiked) selectedColor else neutralColor)
        _textDislikes.setTextColor(if (_hasDisliked) selectedColor else neutralColor)
        _iconDislikes.setColorFilter(if (_hasDisliked) selectedColor else neutralColor)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dualVideoReactionWidth(): Int =
        resources.getDimensionPixelSize(R.dimen.dual_video_reaction_width)
}
