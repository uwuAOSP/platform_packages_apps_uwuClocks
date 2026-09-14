package org.uwuaosp.clock

import com.android.systemui.plugins.keyguard.ui.clocks.ClockId

enum class ClockRendererKind {
    ESSENTIAL,
    SPLIT,
    RAIL,
    ORBITAL,
    PIXEL,
    WEATHER,
}

data class ClockStyleSpec(
    val id: ClockId,
    val nameResId: Int,
    val descriptionResId: Int,
    val thumbnailResId: Int,
    val rendererKind: ClockRendererKind,
    val supportsWeather: Boolean = false,
)

object ClockStyles {
    const val ESSENTIAL_ID = "CLOCK_ESSENTIAL"
    const val SPLIT_ID = "CLOCK_SPLIT"
    const val RAIL_ID = "CLOCK_RAIL"
    const val ORBITAL_ID = "CLOCK_ORBITAL"
    const val PIXEL_ID = "CLOCK_PIXEL"
    const val WEATHER_ID = "CLOCK_WEATHER"

    private val styles =
        listOf(
            ClockStyleSpec(
                ESSENTIAL_ID,
                R.string.clock_essential_name,
                R.string.clock_essential_description,
                R.drawable.clock_essential_thumbnail,
                ClockRendererKind.ESSENTIAL,
            ),
            ClockStyleSpec(
                SPLIT_ID,
                R.string.clock_split_name,
                R.string.clock_split_description,
                R.drawable.clock_split_thumbnail,
                ClockRendererKind.SPLIT,
            ),
            ClockStyleSpec(
                RAIL_ID,
                R.string.clock_rail_name,
                R.string.clock_rail_description,
                R.drawable.clock_rail_thumbnail,
                ClockRendererKind.RAIL,
            ),
            ClockStyleSpec(
                ORBITAL_ID,
                R.string.clock_orbital_name,
                R.string.clock_orbital_description,
                R.drawable.clock_orbital_thumbnail,
                ClockRendererKind.ORBITAL,
            ),
            ClockStyleSpec(
                PIXEL_ID,
                R.string.clock_pixel_name,
                R.string.clock_pixel_description,
                R.drawable.clock_pixel_thumbnail,
                ClockRendererKind.PIXEL,
            ),
            ClockStyleSpec(
                WEATHER_ID,
                R.string.clock_weather_name,
                R.string.clock_weather_description,
                R.drawable.clock_weather_thumbnail,
                ClockRendererKind.WEATHER,
                supportsWeather = true,
            ),
        )

    fun all(): List<ClockStyleSpec> = styles

    fun forId(id: ClockId?): ClockStyleSpec {
        return styles.firstOrNull { it.id == id } ?: styles.first()
    }
}
