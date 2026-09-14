package org.uwuaosp.clock

import android.content.Context
import android.graphics.drawable.Drawable
import com.android.internal.annotations.Keep
import com.android.systemui.customization.clocks.TimeKeeperImpl
import com.android.systemui.log.LogcatOnlyMessageBuffer
import com.android.systemui.log.core.LogLevel
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMetadata
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPickerConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings

@Keep
@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
class ClockProviderImpl : ClockProviderPlugin {
    private lateinit var pluginContext: Context
    private var messageBuffers: ClockMessageBuffers? = null

    override fun onCreate(hostCtx: Context, pluginCtx: Context) {
        pluginContext = pluginCtx
    }

    override fun initialize(buffers: ClockMessageBuffers?) {
        messageBuffers =
            buffers
                ?: ClockMessageBuffers(
                    LogcatOnlyMessageBuffer(LogLevel.DEBUG),
                )
    }

    override fun getClocks(): List<ClockMetadata> {
        return ClockStyles.all().map { ClockMetadata(it.id) }
    }

    override fun createClock(ctx: Context, settings: ClockSettings): ClockController {
        val style = ClockStyles.forId(settings.clockId)
        return ClockControllerImpl(
            pluginContext = pluginContext,
            settings = settings,
            messageBuffers = messageBuffers
                ?: ClockMessageBuffers(LogcatOnlyMessageBuffer(LogLevel.DEBUG)),
            timeKeeper = TimeKeeperImpl(),
            style = style,
        )
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        val style = ClockStyles.forId(settings.clockId)
        return ClockPickerConfig(
            id = style.id,
            name = pluginContext.getString(style.nameResId),
            description = pluginContext.getString(style.descriptionResId),
            thumbnail = loadDrawable(style.thumbnailResId),
        )
    }

    private fun loadDrawable(resourceId: Int): Drawable {
        return pluginContext.resources.getDrawable(resourceId, pluginContext.theme)
    }
}
