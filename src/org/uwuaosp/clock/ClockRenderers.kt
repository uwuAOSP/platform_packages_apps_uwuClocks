package org.uwuaosp.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PathEffect
import android.graphics.Typeface
import android.icu.util.Calendar
import android.view.View
import android.widget.FrameLayout
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import kotlin.math.max
import kotlin.math.min

object ClockRendererFactory {
    fun create(context: Context, style: ClockStyleSpec, isLargeClock: Boolean): ClockRenderer {
        return when (style.rendererKind) {
            ClockRendererKind.ESSENTIAL -> EssentialRenderer(context, isLargeClock)
            ClockRendererKind.SPLIT -> SplitRenderer(context, isLargeClock)
            ClockRendererKind.RAIL -> RailRenderer(context, isLargeClock)
            ClockRendererKind.ORBITAL -> OrbitalRenderer(context, isLargeClock)
            ClockRendererKind.PIXEL -> PixelRenderer(context, isLargeClock)
            ClockRendererKind.WEATHER -> WeatherRenderer(context, isLargeClock)
        }
    }
}

private abstract class BaseRenderer(
    private val rendererContext: Context,
    private val isLargeClock: Boolean,
) : FrameLayout(rendererContext), ClockRenderer {
    protected var state: ClockRenderState? = null
    protected var fontSizePx = defaultFontSizePx()
    protected var primaryColor = Color.WHITE
    protected var secondaryColor = Color.WHITE
    protected var accentColor = Color.WHITE
    private var dozeFraction = 0f

    protected val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isSubpixelText = true
            isDither = true
        }

    override val view: View
        get() = this

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    override fun render(state: ClockRenderState) {
        this.state = state
        dozeFraction = state.dozeFraction
        updatePalette(state.theme)
        contentDescription = state.contentDescription
        alpha = 1f - dozeFraction * 0.16f
        invalidate()
        requestLayout()
    }

    override fun onFontSizeChanged(fontSizePx: Float) {
        this.fontSizePx = fontSizePx.coerceAtLeast(1f)
        requestLayout()
        invalidate()
    }

    override fun onThemeChanged(theme: ThemeConfig) {
        updatePalette(theme)
        invalidate()
    }

    override fun onDoze(fraction: Float) {
        dozeFraction = fraction.coerceIn(0f, 1f)
        alpha = 1f - dozeFraction * 0.16f
        invalidate()
    }

    override fun onFold(fraction: Float) {
        scaleX = 1f - fraction.coerceIn(0f, 1f) * 0.02f
        scaleY = 1f - fraction.coerceIn(0f, 1f) * 0.02f
    }

    override fun onPositionAnimated(animation: ClockPositionAnimationArgs) {
        translationX = (animation.direction * animation.fraction * 4f)
    }

    override fun onPickerCarouselSwiping(fraction: Float) {
        scaleX = 1f + fraction.coerceIn(0f, 1f) * 0.04f
        scaleY = scaleX
    }

    override fun onFidgetTap(point: VPointF) {
        pivotX = point.x
        pivotY = point.y
    }

    override fun onFontAxesChanged(style: ClockAxisStyle) {
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = desiredSize()
        setMeasuredDimension(
            resolveSize(desired.first, widthMeasureSpec),
            resolveSize(desired.second, heightMeasureSpec),
        )
    }

    protected abstract fun desiredSize(): Pair<Int, Int>

    protected fun drawText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        size: Float,
        color: Int,
        weight: Int = Typeface.NORMAL,
        family: String = "google-sans-flex",
    ) {
        if (text.isEmpty()) return
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.create(family, Typeface.NORMAL), weight, false)
        val metrics = paint.fontMetrics
        val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, centerX - paint.measureText(text) / 2f, baseline, paint)
    }

    protected fun drawLine(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        width: Float,
        pathEffect: PathEffect? = null,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = color
        paint.pathEffect = pathEffect
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.pathEffect = null
    }

    protected fun updatePalette(theme: ThemeConfig) {
        val baseColor = theme.seedColor ?: theme.getDefaultColor(rendererContext)
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val dark = theme.isDarkTheme
        primaryColor =
            Color.HSVToColor(
                if (dark) 255 else 235,
                floatArrayOf(hsv[0], (hsv[1] * 0.45f).coerceIn(0f, 1f), if (dark) 0.96f else 0.18f),
            )
        secondaryColor =
            Color.HSVToColor(
                if (dark) 220 else 210,
                floatArrayOf(hsv[0], (hsv[1] * 0.75f).coerceIn(0f, 1f), if (dark) 0.84f else 0.28f),
            )
        accentColor =
            if (dozeFraction > 0.5f) theme.getAodColor(rendererContext)
            else Color.HSVToColor(255, floatArrayOf(hsv[0], hsv[1].coerceIn(0f, 1f), if (dark) 1f else 0.36f))
    }

    protected fun centerX(): Float = width / 2f

    protected fun defaultFontSizePx(): Float {
        val sizeSp = if (isLargeClock) 96f else 42f
        return sizeSp * resources.displayMetrics.scaledDensity
    }

    protected fun contentWidth(): Float = max(width.toFloat(), fontSizePx * 3.4f)
}

private class EssentialRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    override fun desiredSize(): Pair<Int, Int> {
        val width = (fontSizePx * 3.7f).toInt()
        val height = (fontSizePx * 1.55f).toInt()
        return width to height
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val timeSize = fontSizePx * 1.14f
        drawText(canvas, current.timeText, centerX(), height * 0.43f, timeSize, primaryColor, 500, "google-sans-flex-clock")
        drawText(canvas, current.dateText, centerX(), height * 0.82f, fontSizePx * 0.22f, secondaryColor, 500)
    }
}

private class SplitRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    override fun desiredSize(): Pair<Int, Int> {
        return (fontSizePx * 3.8f).toInt() to (fontSizePx * 2.55f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val x = centerX()
        drawText(canvas, current.timeText.substringBefore(":"), x, height * 0.25f, fontSizePx * 1.42f, primaryColor, 700, "google-sans-flex-clock")
        drawText(canvas, current.dateText, x, height * 0.51f, fontSizePx * 0.24f, secondaryColor, 500)
        drawText(canvas, current.timeText.substringAfter(":", current.timeText), x, height * 0.78f, fontSizePx * 1.42f, accentColor, 700, "google-sans-flex-clock")
    }
}

private class RailRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    override fun desiredSize(): Pair<Int, Int> {
        return (fontSizePx * 4.1f).toInt() to (fontSizePx * 1.65f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val hour = current.timeText.substringBefore(":")
        val minute = current.timeText.substringAfter(":", current.timeText)
        val timeSize = fontSizePx * 0.92f
        val gap = fontSizePx * 0.12f
        paint.typeface = Typeface.create(Typeface.create("google-sans-flex-clock", Typeface.NORMAL), 700, false)
        paint.textSize = timeSize
        val hourWidth = paint.measureText(hour)
        val minuteWidth = paint.measureText(minute)
        val colonWidth = paint.measureText(":")
        val totalWidth = hourWidth + minuteWidth + colonWidth + gap * 2f
        var x = centerX() - totalWidth / 2f
        drawText(canvas, hour, x + hourWidth / 2f, height * 0.42f, timeSize, primaryColor, 700, "google-sans-flex-clock")
        x += hourWidth + gap
        drawText(canvas, ":", x + colonWidth / 2f, height * 0.42f, timeSize, accentColor, 500, "google-sans-flex-clock")
        x += colonWidth + gap
        drawText(canvas, minute, x + minuteWidth / 2f, height * 0.42f, timeSize, primaryColor, 700, "google-sans-flex-clock")
        drawText(canvas, current.dateText, centerX(), height * 0.82f, fontSizePx * 0.2f, secondaryColor, 500)
    }
}

private class OrbitalRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    override fun desiredSize(): Pair<Int, Int> {
        val size = (fontSizePx * 3.15f).toInt()
        return size to size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val radius = min(width, height) * 0.42f
        val cx = centerX()
        val cy = height * 0.47f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, fontSizePx * 0.045f)
        paint.color = secondaryColor
        canvas.drawCircle(cx, cy, radius, paint)
        val calendar = Calendar.getInstance(current.timeZone).apply { timeInMillis = current.nowMillis }
        val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val progress = minutes / (24f * 60f)
        paint.color = accentColor
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, progress * 360f, false, paint)
        drawText(canvas, current.timeText, cx, cy - fontSizePx * 0.04f, fontSizePx * 0.72f, primaryColor, 600, "google-sans-flex-clock")
        drawText(canvas, current.dateText, cx, cy + fontSizePx * 0.47f, fontSizePx * 0.18f, secondaryColor, 500)
    }
}

private class PixelRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    private val patterns =
        mapOf(
            '0' to listOf("11111", "10001", "10011", "10101", "11001", "10001", "11111"),
            '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
            '2' to listOf("11110", "00001", "00001", "01110", "10000", "10000", "11111"),
            '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
            '4' to listOf("10010", "10010", "10010", "11111", "00010", "00010", "00010"),
            '5' to listOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
            '6' to listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
            '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
            '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
            '9' to listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
            ':' to listOf("0", "1", "0", "0", "1", "0", "0"),
        )

    override fun desiredSize(): Pair<Int, Int> {
        return (fontSizePx * 3.8f).toInt() to (fontSizePx * 1.35f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val cell = max(2f, fontSizePx * 0.105f)
        val gap = cell * 0.38f
        val charWidth = cell * 5f + gap * 4f
        val charGap = cell * 1.8f
        val text = current.timeText.map { if (Character.isDigit(it)) Character.forDigit(Character.digit(it, 10), 10) else it }
        val totalWidth = text.size * charWidth + (text.size - 1) * charGap
        var startX = centerX() - totalWidth / 2f
        val startY = height * 0.16f
        for (character in text) {
            val pattern = patterns[character]
            if (pattern == null) {
                drawText(canvas, current.timeText, centerX(), height * 0.42f, fontSizePx * 0.9f, primaryColor, 700, "monospace")
                break
            }
            for (row in pattern.indices) {
                for (column in pattern[row].indices) {
                    if (pattern[row][column] != '1') continue
                    paint.style = Paint.Style.FILL
                    paint.color = if (character == ':') accentColor else primaryColor
                    val left = startX + column * (cell + gap)
                    val top = startY + row * (cell + gap)
                    canvas.drawRoundRect(left, top, left + cell, top + cell, cell / 3f, cell / 3f, paint)
                }
            }
            startX += charWidth + charGap
        }
        drawText(canvas, current.dateText, centerX(), height * 0.86f, fontSizePx * 0.18f, secondaryColor, 500)
    }
}

private class WeatherRenderer(context: Context, isLargeClock: Boolean) :
    BaseRenderer(context, isLargeClock) {
    override fun desiredSize(): Pair<Int, Int> {
        return (fontSizePx * 4.2f).toInt() to (fontSizePx * 2.05f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = state ?: return
        val weather = current.weatherData
        drawText(canvas, current.timeText, centerX(), height * 0.31f, fontSizePx * 1.15f, primaryColor, 600, "google-sans-flex-clock")
        drawText(canvas, current.dateText, centerX(), height * 0.57f, fontSizePx * 0.2f, secondaryColor, 500)
        if (weather == null) {
            drawText(canvas, "--°", centerX(), height * 0.82f, fontSizePx * 0.27f, accentColor, 600)
        } else {
            val unit = if (weather.useCelsius) "C" else "F"
            drawText(canvas, "${weather.temperature}°$unit", centerX(), height * 0.79f, fontSizePx * 0.27f, accentColor, 600)
            if (weather.description.isNotEmpty()) {
                drawText(canvas, weather.description, centerX(), height * 0.94f, fontSizePx * 0.14f, secondaryColor, 500)
            }
        }
    }
}
