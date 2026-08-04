package com.futo.platformplayer.views.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.futo.platformplayer.R
import com.futo.platformplayer.api.media.models.chapters.IChapter
import com.futo.platformplayer.api.media.models.chapters.TimelineSegment
import com.futo.platformplayer.api.media.models.chapters.TimelineSegments
import kotlin.math.max

@OptIn(UnstableApi::class)
class SegmentedTimeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DefaultTimeBar(context, attrs, defStyleAttr) {
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val segmentOverlayEnabled: Boolean
    private val barHeight: Int
    private val barGravity: Int
    private val scrubberPadding: Int
    private val minimumSegmentWidth = resources.displayMetrics.density
    private var scrubberPaddingDisabled = false
    private var scrubberVisible = true
    private var durationMs: Long = C.TIME_UNSET
    private var positionMs: Long = 0L
    private var scrubX: Float? = null
    private var segments: List<TimelineSegment> = emptyList()

    init {
        val segmentStyle = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SegmentedTimeBar,
            defStyleAttr,
            0
        )
        try {
            segmentOverlayEnabled = segmentStyle.getBoolean(
                R.styleable.SegmentedTimeBar_segmentOverlayEnabled,
                true
            )
        } finally {
            segmentStyle.recycle()
        }

        val density = resources.displayMetrics.density
        val defaultBarHeight = dpToPx(density, DEFAULT_BAR_HEIGHT_DP)
        val defaultScrubberEnabledSize = dpToPx(density, DEFAULT_SCRUBBER_ENABLED_SIZE_DP)
        val defaultScrubberDisabledSize = dpToPx(density, DEFAULT_SCRUBBER_DISABLED_SIZE_DP)
        val defaultScrubberDraggedSize = dpToPx(density, DEFAULT_SCRUBBER_DRAGGED_SIZE_DP)
        val styled = context.theme.obtainStyledAttributes(
            attrs,
            androidx.media3.ui.R.styleable.DefaultTimeBar,
            defStyleAttr,
            0
        )
        try {
            val scrubberDrawable = styled.getDrawable(androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_drawable)
            barHeight = styled.getDimensionPixelSize(androidx.media3.ui.R.styleable.DefaultTimeBar_bar_height, defaultBarHeight)
            barGravity = styled.getInt(
                androidx.media3.ui.R.styleable.DefaultTimeBar_bar_gravity,
                BAR_GRAVITY_CENTER
            )
            val enabledSize = styled.getDimensionPixelSize(
                androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_enabled_size,
                defaultScrubberEnabledSize
            )
            val disabledSize = styled.getDimensionPixelSize(
                androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_disabled_size,
                defaultScrubberDisabledSize
            )
            val draggedSize = styled.getDimensionPixelSize(
                androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_dragged_size,
                defaultScrubberDraggedSize
            )
            scrubberPadding = if(scrubberDrawable != null) {
                (scrubberDrawable.minimumWidth + 1) / 2
            } else {
                (max(disabledSize, max(enabledSize, draggedSize)) + 1) / 2
            }
        } finally {
            styled.recycle()
        }
    }

    fun setChapters(chapters: List<IChapter>?) {
        segments = TimelineSegments.fromChapters(chapters)
        postInvalidate()
    }

    fun getSegments(): List<TimelineSegment> = segments.toList()

    fun isSegmentOverlayEnabled(): Boolean = segmentOverlayEnabled

    override fun setDuration(duration: Long) {
        durationMs = duration
        super.setDuration(duration)
        invalidate()
    }

    override fun setPosition(position: Long) {
        positionMs = position
        super.setPosition(position)
        invalidate()
    }

    override fun showScrubber() {
        scrubberVisible = true
        scrubberPaddingDisabled = false
        super.showScrubber()
    }

    override fun showScrubber(showAnimationDurationMs: Long) {
        scrubberVisible = true
        scrubberPaddingDisabled = false
        super.showScrubber(showAnimationDurationMs)
    }

    override fun hideScrubber(disableScrubberPadding: Boolean) {
        scrubberVisible = false
        scrubberPaddingDisabled = disableScrubberPadding
        super.hideScrubber(disableScrubberPadding)
    }

    override fun hideScrubber(hideAnimationDurationMs: Long) {
        scrubberVisible = false
        super.hideScrubber(hideAnimationDurationMs)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        if(handled) {
            scrubX = when(event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> event.x
                else -> null
            }
            invalidate()
        }
        return handled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if(!segmentOverlayEnabled || segments.isEmpty() || durationMs <= 0L || durationMs == C.TIME_UNSET) return

        val activeScrubberPadding = if(scrubberPaddingDisabled) 0 else scrubberPadding
        val barLeft = paddingLeft + activeScrubberPadding
        val barRight = width - paddingRight - activeScrubberPadding
        val barWidth = barRight - barLeft
        if(barWidth <= 0) return

        val progressBarY = if(barGravity == BAR_GRAVITY_BOTTOM) {
            height - paddingBottom - barHeight - max(activeScrubberPadding - barHeight / 2, 0)
        } else {
            (height - barHeight) / 2
        }
        val barTop = progressBarY.toFloat()
        val barBottom = (progressBarY + barHeight).toFloat()
        val exclusionCenter = if(scrubberVisible) {
            scrubX?.coerceIn(barLeft.toFloat(), barRight.toFloat())
                ?: (barLeft + barWidth * (positionMs.coerceIn(0L, durationMs).toDouble() / durationMs)).toFloat()
        } else null
        val exclusionHalfWidth = max(1f, activeScrubberPadding.toFloat())

        for(segment in TimelineSegments.clipToDuration(segments, durationMs)) {
            if((segment.color ushr 24) == 0) continue

            var left = barLeft + (barWidth * (segment.startMs.toDouble() / durationMs)).toFloat()
            var right = barLeft + (barWidth * (segment.endMs.toDouble() / durationMs)).toFloat()
            if(right - left < minimumSegmentWidth) {
                val center = (left + right) / 2f
                left = max(barLeft.toFloat(), center - minimumSegmentWidth / 2f)
                right = minOf(barRight.toFloat(), left + minimumSegmentWidth)
                left = max(barLeft.toFloat(), right - minimumSegmentWidth)
            }
            left = left.coerceIn(barLeft.toFloat(), barRight.toFloat())
            right = right.coerceIn(barLeft.toFloat(), barRight.toFloat())
            if(right <= left) continue

            segmentPaint.color = segment.color
            if(exclusionCenter != null && right > exclusionCenter - exclusionHalfWidth && left < exclusionCenter + exclusionHalfWidth) {
                val beforeRight = minOf(right, exclusionCenter - exclusionHalfWidth)
                if(beforeRight > left)
                    canvas.drawRect(RectF(left, barTop, beforeRight, barBottom), segmentPaint)
                val afterLeft = max(left, exclusionCenter + exclusionHalfWidth)
                if(right > afterLeft)
                    canvas.drawRect(RectF(afterLeft, barTop, right, barBottom), segmentPaint)
            } else {
                canvas.drawRect(RectF(left, barTop, right, barBottom), segmentPaint)
            }
        }
    }

    companion object {
        private fun dpToPx(density: Float, dp: Int): Int = (density * dp + 0.5f).toInt()
    }
}
