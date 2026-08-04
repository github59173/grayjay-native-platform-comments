package com.futo.platformplayer.views.fields

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.futo.platformplayer.R
import com.futo.platformplayer.api.media.models.chapters.TimelineColor
import kotlin.math.max

class ColorSwatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
    }
    private var swatchColor: Int = Color.WHITE

    init {
        isClickable = true
        isFocusable = true
    }

    fun setColor(color: Int) {
        if(swatchColor == color) return
        swatchColor = color
        invalidate()
    }

    fun getColor(): Int = swatchColor

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = resources.displayMetrics.density * 7f
        val bounds = RectF(inset, inset, width - inset, height - inset)
        val radius = max(0f, minOf(bounds.width(), bounds.height()) / 2f)
        val path = Path().apply { addCircle(bounds.centerX(), bounds.centerY(), radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(path)
        val checker = resources.displayMetrics.density * 4f
        var row = 0
        var y = bounds.top
        while(y < bounds.bottom) {
            var column = 0
            var x = bounds.left
            while(x < bounds.right) {
                paint.color = if((row + column) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFFFFFFFF.toInt()
                canvas.drawRect(x, y, minOf(x + checker, bounds.right), minOf(y + checker, bounds.bottom), paint)
                x += checker
                column++
            }
            y += checker
            row++
        }
        paint.color = swatchColor
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, paint)
        canvas.restore()

        outlinePaint.color = if(hasFocus()) 0xFF4D83FF.toInt() else 0xFF8A8A8A.toInt()
        outlinePaint.strokeWidth = resources.displayMetrics.density * if(hasFocus()) 2.5f else 1.5f
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), max(0f, radius - outlinePaint.strokeWidth / 2f), outlinePaint)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }
}

private class SaturationValueView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }
    var hue: Float = 0f
        set(value) {
            field = ((value % 360f) + 360f) % 360f
            invalidate()
        }
    var saturation: Float = 1f
        private set
    var brightness: Float = 1f
        private set
    var onChanged: ((Float, Float) -> Unit)? = null

    init {
        isFocusable = true
        contentDescription = context.getString(R.string.color_saturation_brightness)
    }

    fun setSelection(saturation: Float, brightness: Float) {
        this.saturation = saturation.coerceIn(0f, 1f)
        this.brightness = brightness.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (180f * resources.displayMetrics.density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(width <= 0 || height <= 0) return
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        paint.shader = null
        paint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        canvas.drawRect(rect, paint)
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(rect, paint)
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
        canvas.drawRect(rect, paint)
        paint.shader = null

        val x = saturation * width
        val y = (1f - brightness) * height
        markerPaint.color = Color.BLACK
        markerPaint.strokeWidth = resources.displayMetrics.density * 4f
        canvas.drawCircle(x, y, resources.displayMetrics.density * 7f, markerPaint)
        markerPaint.color = Color.WHITE
        markerPaint.strokeWidth = resources.displayMetrics.density * 2f
        canvas.drawCircle(x, y, resources.displayMetrics.density * 7f, markerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE && event.action != MotionEvent.ACTION_UP)
            return false
        parent?.requestDisallowInterceptTouchEvent(true)
        updateSelection(event.x, event.y)
        if(event.action == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val step = if(event?.isShiftPressed == true) 0.1f else 0.02f
        val handled = when(keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> setSelectionAndNotify(saturation - step, brightness)
            KeyEvent.KEYCODE_DPAD_RIGHT -> setSelectionAndNotify(saturation + step, brightness)
            KeyEvent.KEYCODE_DPAD_UP -> setSelectionAndNotify(saturation, brightness + step)
            KeyEvent.KEYCODE_DPAD_DOWN -> setSelectionAndNotify(saturation, brightness - step)
            else -> false
        }
        return handled || super.onKeyDown(keyCode, event)
    }

    private fun updateSelection(x: Float, y: Float) {
        setSelectionAndNotify(x / width.coerceAtLeast(1), 1f - y / height.coerceAtLeast(1))
    }

    private fun setSelectionAndNotify(saturation: Float, brightness: Float): Boolean {
        setSelection(saturation, brightness)
        onChanged?.invoke(this.saturation, this.brightness)
        return true
    }
}

private class ColorSliderView(context: Context, private val mode: Mode) : View(context) {
    enum class Mode { HUE, ALPHA }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }
    var value: Float = 1f
        private set
    var baseColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }
    var onChanged: ((Float) -> Unit)? = null

    init {
        isFocusable = true
        contentDescription = context.getString(if(mode == Mode.HUE) R.string.color_hue else R.string.color_opacity)
    }

    fun setValue(value: Float) {
        this.value = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize((40f * resources.displayMetrics.density).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val rect = RectF(density * 7f, density * 13f, width - density * 7f, height - density * 13f)
        if(mode == Mode.ALPHA) drawChecker(canvas, rect, density * 4f)
        paint.shader = if(mode == Mode.HUE) {
            LinearGradient(rect.left, 0f, rect.right, 0f, intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            ), null, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(rect.left, 0f, rect.right, 0f, baseColor and 0x00FFFFFF, baseColor or 0xFF000000.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, density * 3f, density * 3f, paint)
        paint.shader = null

        val x = rect.left + rect.width() * value
        thumbPaint.color = Color.BLACK
        thumbPaint.strokeWidth = density * 4f
        canvas.drawCircle(x, rect.centerY(), density * 7f, thumbPaint)
        thumbPaint.color = Color.WHITE
        thumbPaint.strokeWidth = density * 2f
        canvas.drawCircle(x, rect.centerY(), density * 7f, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE && event.action != MotionEvent.ACTION_UP)
            return false
        parent?.requestDisallowInterceptTouchEvent(true)
        val padding = resources.displayMetrics.density * 7f
        update((event.x - padding) / max(1f, width - padding * 2f))
        if(event.action == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val delta = if(event?.isShiftPressed == true) 0.1f else 0.01f
        return when(keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN -> { update(value - delta); true }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> { update(value + delta); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun update(value: Float) {
        setValue(value)
        onChanged?.invoke(this.value)
    }

    private fun drawChecker(canvas: Canvas, rect: RectF, size: Float) {
        var row = 0
        var y = rect.top
        while(y < rect.bottom) {
            var column = 0
            var x = rect.left
            while(x < rect.right) {
                paint.shader = null
                paint.color = if((row + column) % 2 == 0) 0xFFBBBBBB.toInt() else Color.WHITE
                canvas.drawRect(x, y, minOf(x + size, rect.right), minOf(y + size, rect.bottom), paint)
                x += size
                column++
            }
            y += size
            row++
        }
    }
}

object InlineColorPickerDialog {
    fun show(
        context: Context,
        title: String,
        initialColor: String,
        defaultColor: String,
        allowAlpha: Boolean,
        onApplied: (String) -> Unit
    ) {
        val fallback = TimelineColor.parse(defaultColor) ?: Color.WHITE
        var selectedColor = TimelineColor.parse(initialColor) ?: fallback
        if(!allowAlpha) selectedColor = selectedColor or 0xFF000000.toInt()
        var updatingText = false
        val density = context.resources.displayMetrics.density

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (8 * density).toInt(), (20 * density).toInt(), 0)
        }
        val preview = ColorSwatchView(context).apply {
            isClickable = false
            isFocusable = false
            layoutParams = LinearLayout.LayoutParams((64 * density).toInt(), (64 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * density).toInt()
            }
        }
        val saturationValue = SaturationValueView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (180 * density).toInt())
        }
        val hueLabel = label(context, context.getString(R.string.color_hue))
        val hueSlider = ColorSliderView(context, ColorSliderView.Mode.HUE)
        val alphaLabel = label(context, context.getString(R.string.color_opacity))
        val alphaSlider = ColorSliderView(context, ColorSliderView.Mode.ALPHA)
        val hexInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine(true)
            hint = if(allowAlpha) "#AARRGGBB" else "#RRGGBB"
            contentDescription = context.getString(R.string.color_hex_value)
        }

        root.addView(preview)
        root.addView(saturationValue)
        root.addView(hueLabel)
        root.addView(hueSlider)
        if(allowAlpha) {
            root.addView(alphaLabel)
            root.addView(alphaSlider)
        }
        root.addView(hexInput)

        fun updateControls(color: Int, updateText: Boolean = true) {
            selectedColor = if(allowAlpha) color else color or 0xFF000000.toInt()
            val hsv = FloatArray(3)
            Color.colorToHSV(selectedColor, hsv)
            saturationValue.hue = hsv[0]
            saturationValue.setSelection(hsv[1], hsv[2])
            hueSlider.setValue(hsv[0] / 360f)
            alphaSlider.baseColor = selectedColor or 0xFF000000.toInt()
            alphaSlider.setValue(Color.alpha(selectedColor) / 255f)
            preview.setColor(selectedColor)
            preview.contentDescription = context.getString(R.string.color_preview_value, TimelineColor.formatArgb(selectedColor))
            if(updateText) {
                updatingText = true
                hexInput.setText(if(allowAlpha) TimelineColor.formatArgb(selectedColor) else "#%06X".format(selectedColor and 0xFFFFFF))
                hexInput.setSelection(hexInput.text.length)
                updatingText = false
            }
        }

        fun updateFromHsv() {
            val alpha = if(allowAlpha) Color.alpha(selectedColor) else 255
            updateControls(Color.HSVToColor(alpha, floatArrayOf(saturationValue.hue, saturationValue.saturation, saturationValue.brightness)))
        }

        saturationValue.onChanged = { _, _ -> updateFromHsv() }
        hueSlider.onChanged = {
            saturationValue.hue = it * 360f
            updateFromHsv()
        }
        alphaSlider.onChanged = {
            selectedColor = (selectedColor and 0x00FFFFFF) or ((it * 255f).toInt().coerceIn(0, 255) shl 24)
            updateControls(selectedColor)
        }
        hexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if(updatingText) return
                TimelineColor.parse(s?.toString())?.let {
                    hexInput.error = null
                    updateControls(it, false)
                }
            }
        })
        updateControls(selectedColor)

        val scroll = ScrollView(context).apply { addView(root) }
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scroll)
            .setNeutralButton(R.string.reset, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.apply, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { updateControls(fallback) }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val parsed = TimelineColor.parse(hexInput.text.toString())
                if(parsed == null) {
                    hexInput.error = context.getString(R.string.invalid_color_value)
                    return@setOnClickListener
                }
                val applied = if(allowAlpha) parsed else parsed or 0xFF000000.toInt()
                onApplied(TimelineColor.formatArgb(applied))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun label(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(0xFFACACAC.toInt())
        textSize = 13f
        val density = resources.displayMetrics.density
        setPadding(0, (6 * density).toInt(), 0, 0)
    }
}
