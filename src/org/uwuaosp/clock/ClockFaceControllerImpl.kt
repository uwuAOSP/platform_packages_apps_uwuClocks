package org.uwuaosp.clock

import android.content.Context
import android.icu.util.TimeZone
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.android.systemui.customization.clocks.DigitalDateFormatter
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.TimeKeeper
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRect
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.AodClockBurnInModel
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings
import com.android.systemui.plugins.keyguard.ui.clocks.ClockTickRate
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale

class ClockFaceControllerImpl(
    private val pluginContext: Context,
    private val settings: ClockSettings,
    private val messageBuffer: MessageBuffer,
    private val timeKeeper: TimeKeeper,
    private val style: ClockStyleSpec,
    private val isLargeClock: Boolean,
) : ClockFaceController {
    private val timeFormatter =
        DigitalTimeFormatter(
            pattern = "h:mm",
            timeKeeper = timeKeeper,
            enableContentDescription = true,
        )
    private val dateFormatter =
        DigitalDateFormatter(
            pattern = "MMM d EEEE",
            timeKeeper = timeKeeper,
            enableContentDescription = false,
        )
    private val renderer = ClockRendererFactory.create(pluginContext, style, isLargeClock)
    private val root =
        FrameLayout(pluginContext).apply {
            id =
                if (isLargeClock) ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE
                else ClockViewIds.LOCKSCREEN_CLOCK_VIEW_SMALL
            clipChildren = false
            clipToPadding = false
            addView(
                renderer.view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }

    private var locale = Locale.getDefault()
    private var themeState = ThemeConfig(isDarkTheme = true, settings.seedColor)
    private var dozeFraction = 0f
    private var fontSizePx = defaultFontSizePx()
    private var weatherData: WeatherData? = null
    private var alarmData: AlarmData? = null
    private var zenData: ZenData? = null

    override val view: View
        get() = root

    override val layout = ClockFaceLayoutImpl(root)

    override val config =
        ClockFaceConfig(
            tickRate = ClockTickRate.PER_MINUTE,
            hasCustomWeatherDataDisplay = style.supportsWeather,
        )

    override var theme: ThemeConfig
        get() = themeState
        set(value) {
            themeState = value
        }

    override val events =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                timeKeeper.updateTime()
                render()
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                themeState = theme
                renderer.onThemeChanged(theme)
                render()
            }

            override fun onFontSettingChanged(fontSizePx: Float) {
                this@ClockFaceControllerImpl.fontSizePx = fontSizePx
                renderer.onFontSizeChanged(fontSizePx)
                render()
            }

            override fun onTargetRegionChanged(targetRegion: VRect) {
                renderer.view.requestLayout()
            }

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {
                renderer.view.requestLayout()
            }
        }

    override val animations =
        object : ClockAnimations {
            override fun enter() {
                renderer.view.alpha = 1f
                renderer.view.visibility = View.VISIBLE
            }

            override fun doze(fraction: Float) {
                dozeFraction = fraction.coerceIn(0f, 1f)
                renderer.onDoze(dozeFraction)
                render()
            }

            override fun fold(fraction: Float) {
                renderer.onFold(fraction.coerceIn(0f, 1f))
            }

            override fun charge() {
                renderer.onCharge()
            }

            override fun onPositionAnimated(animation: ClockPositionAnimationArgs) {
                renderer.onPositionAnimated(animation)
            }

            override fun onPickerCarouselSwiping(fraction: Float) {
                renderer.onPickerCarouselSwiping(fraction)
            }

            override fun onFidgetTap(x: Float, y: Float) {
                renderer.onFidgetTap(VPointF(x, y))
            }

            override fun onFontAxesChanged(style: ClockAxisStyle) {
                renderer.onFontAxesChanged(style)
            }
        }

    init {
        timeFormatter.locale = locale
        dateFormatter.locale = locale
        timeFormatter.formatKind = TimeFormatKind.getFromContext(pluginContext)
        renderer.onFontSizeChanged(fontSizePx)
        renderer.onThemeChanged(themeState)
        render()
    }

    fun onTimeZoneChanged() {
        render()
    }

    fun onTimeFormatChanged(formatKind: TimeFormatKind) {
        timeFormatter.formatKind = formatKind
        render()
    }

    fun onLocaleChanged(locale: Locale) {
        this.locale = locale
        timeFormatter.locale = locale
        dateFormatter.locale = locale
        render()
    }

    fun onWeatherDataChanged(data: WeatherData) {
        weatherData = data
        render()
    }

    fun onAlarmDataChanged(data: AlarmData) {
        alarmData = data
        render()
    }

    fun onZenDataChanged(data: ZenData) {
        zenData = data
        render()
    }

    private fun render() {
        val dateText = dateFormatter.getText()
        val contentDescription =
            listOfNotNull(timeFormatter.getContentDescription(), dateText.takeIf { it.isNotEmpty() })
                .joinToString(", ")
                .ifEmpty { null }
        val state =
            ClockRenderState(
                timeText = timeFormatter.getText(),
                dateText = dateText,
                contentDescription = contentDescription,
                nowMillis = timeKeeper.time.time,
                locale = locale,
                timeZone = timeKeeper.timeZone,
                theme = themeState,
                dozeFraction = dozeFraction,
                fontSizePx = fontSizePx,
                isLargeClock = isLargeClock,
                weatherData = weatherData,
                alarmData = alarmData,
                zenData = zenData,
            )
        root.contentDescription = contentDescription
        renderer.render(state)
        root.requestLayout()
    }

    private fun defaultFontSizePx(): Float {
        val sizeSp = if (isLargeClock) 96f else 42f
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sizeSp,
            pluginContext.resources.displayMetrics,
        )
    }
}
