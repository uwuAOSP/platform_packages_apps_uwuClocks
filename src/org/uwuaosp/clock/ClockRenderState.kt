package org.uwuaosp.clock

import android.icu.util.TimeZone
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import java.util.Locale

data class ClockRenderState(
    val timeText: String,
    val dateText: String,
    val contentDescription: String?,
    val nowMillis: Long,
    val locale: Locale,
    val timeZone: TimeZone,
    val theme: ThemeConfig,
    val dozeFraction: Float,
    val fontSizePx: Float,
    val isLargeClock: Boolean,
    val weatherData: WeatherData?,
    val alarmData: AlarmData?,
    val zenData: ZenData?,
)
