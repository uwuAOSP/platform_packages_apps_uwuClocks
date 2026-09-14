package org.uwuaosp.clock

import android.content.Context
import android.icu.util.TimeZone
import com.android.systemui.customization.clocks.TimeKeeper
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEventListeners
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.io.PrintWriter
import java.util.Locale

class ClockControllerImpl(
    private val pluginContext: Context,
    private val settings: ClockSettings,
    messageBuffers: ClockMessageBuffers,
    timeKeeper: TimeKeeper,
    private val style: ClockStyleSpec,
) : ClockController {
    override val smallClock =
        ClockFaceControllerImpl(
            pluginContext = pluginContext,
            settings = settings,
            messageBuffer = messageBuffers.smallClockMessageBuffer,
            timeKeeper = timeKeeper,
            style = style,
            isLargeClock = false,
        )

    override val largeClock =
        ClockFaceControllerImpl(
            pluginContext = pluginContext,
            settings = settings,
            messageBuffer = messageBuffers.largeClockMessageBuffer,
            timeKeeper = timeKeeper,
            style = style,
            isLargeClock = true,
        )

    override val config =
        ClockConfig(
            id = style.id,
            name = pluginContext.getString(style.nameResId),
            description = pluginContext.getString(style.descriptionResId),
        )

    override val eventListeners = ClockEventListeners()

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                timeKeeper.timeZone = timeZone
                smallClock.onTimeZoneChanged()
                largeClock.onTimeZoneChanged()
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                smallClock.onTimeFormatChanged(formatKind)
                largeClock.onTimeFormatChanged(formatKind)
            }

            override fun onLocaleChanged(locale: Locale) {
                smallClock.onLocaleChanged(locale)
                largeClock.onLocaleChanged(locale)
            }

            override fun onWeatherDataChanged(data: WeatherData) {
                smallClock.onWeatherDataChanged(data)
                largeClock.onWeatherDataChanged(data)
            }

            override fun onAlarmDataChanged(data: AlarmData) {
                smallClock.onAlarmDataChanged(data)
                largeClock.onAlarmDataChanged(data)
            }

            override fun onZenDataChanged(data: ZenData) {
                smallClock.onZenDataChanged(data)
                largeClock.onZenDataChanged(data)
            }
        }

    override fun initialize(isDarkTheme: Boolean, dozeFraction: Float, foldFraction: Float) {
        val formatKind = TimeFormatKind.getFromContext(pluginContext)
        events.onTimeFormatChanged(formatKind)
        listOf(smallClock, largeClock).forEach { face ->
            face.events.onThemeChanged(face.theme.copy(isDarkTheme = isDarkTheme))
            face.animations.doze(dozeFraction)
            face.animations.fold(foldFraction)
            face.events.onTimeTick()
        }
    }

    override fun dump(pw: PrintWriter) {
        pw.println("ClockControllerImpl(clockId=${style.id}, settings=$settings)")
    }
}
