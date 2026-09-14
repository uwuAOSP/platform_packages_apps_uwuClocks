package org.uwuaosp.clock

import android.view.View
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig

interface ClockRenderer {
    val view: View

    fun render(state: ClockRenderState)

    fun onFontSizeChanged(fontSizePx: Float)

    fun onThemeChanged(theme: ThemeConfig)

    fun onDoze(fraction: Float)

    fun onFold(fraction: Float) {}

    fun onCharge() {}

    fun onPositionAnimated(animation: ClockPositionAnimationArgs) {}

    fun onPickerCarouselSwiping(fraction: Float) {}

    fun onFidgetTap(point: VPointF) {}

    fun onFontAxesChanged(style: ClockAxisStyle) {}
}
