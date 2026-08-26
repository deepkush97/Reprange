package com.deepkush.reprange.ui.theme

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.Color

/**
 * Port of the Material palette scoring algorithm (the same scoring used for wallpaper accents,
 * wrapped by materialKolor's ecosystem as `Score.score`). Weighted dominance x vibrancy pick.
 */
internal object Score {

    private const val WEIGHT_SATURATION = 3f
    private const val WEIGHT_BRIGHTNESS = 0.6f
    private const val WEIGHT_POPULATION = 0.1f

    private data class Target(
        val saturation: Float,
        val brightness: Float,
        val saturationWeight: Float = 0.24f,
        val brightnessWeight: Float = 0.52f,
        val populationWeight: Float = 0.24f,
    )

    private val TARGETS = listOf(
        Target(saturation = 1f, brightness = 0.5f),   // vibrant
        Target(saturation = 1f, brightness = 0.74f),  // light vibrant
        Target(saturation = 1f, brightness = 0.26f),  // dark vibrant
        Target(saturation = 0.3f, brightness = 0.5f), // muted
        Target(saturation = 0.3f, brightness = 0.74f),// light muted
        Target(saturation = 0.3f, brightness = 0.26f),// dark muted
    )

    /** Returns candidate colors sorted best-first. */
    fun score(colors: Map<Int, Int>): List<Int> {
        if (colors.isEmpty()) return emptyList()
        val maxPopulation = colors.values.max()

        data class Scored(val score: Float, val rgb: Int)

        val scored = colors.map { (rgb, population) ->
            val hsv = FloatArray(3)
            AndroidColor.RGBToHSV(
                AndroidColor.red(rgb),
                AndroidColor.green(rgb),
                AndroidColor.blue(rgb),
                hsv,
            )
            var best = Float.MIN_VALUE
            for (t in TARGETS) {
                val sDiff = 1f - kotlin.math.abs(hsv[1] - t.saturation)
                val bDiff = 1f - kotlin.math.abs(hsv[2] - t.brightness)
                val value = WEIGHT_SATURATION * t.saturationWeight * sDiff +
                    WEIGHT_BRIGHTNESS * t.brightnessWeight * bDiff +
                    WEIGHT_POPULATION * t.populationWeight * (population.toFloat() / maxPopulation)
                if (value > best) best = value
            }
            Scored(best, rgb)
        }.sortedByDescending { it.score }

        return scored.map { it.rgb }
    }
}

/** Dominance x vibrancy extraction via Material Score - never raw swatch picking. */
fun Bitmap.extractThemeColor(): Color {
    val populations = Palette.from(this).maximumColorCount(8).generate()
        .swatches.associate { it.rgb to it.population }
    return Color(Score.score(populations).first())
}

object ColorExtractor {

    /**
     * Weighted swatch -> vivid triple per §3.2:
     * [primary, primary * 0.6, Black].
     */
    fun extractGradientTriple(palette: Palette, fallbackColor: Int): List<Color> {
        val candidates = listOfNotNull(
            palette.dominantSwatch, palette.vibrantSwatch, palette.darkVibrantSwatch,
            palette.lightVibrantSwatch, palette.mutedSwatch, palette.darkMutedSwatch,
            palette.lightMutedSwatch,
        )

        fun weight(s: Palette.Swatch?): Float {
            if (s == null) return 0f
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(s.rgb, hsv)
            val vibrancyBonus = if (hsv[1] > 0.3f && hsv[2] > 0.3f) 1.5f else 1f
            return s.population.toFloat() * 2f * vibrancyBonus * (hsv[1] + hsv[2]) / 2f
        }

        val best = candidates.maxByOrNull(::weight)
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))

        fun vibrant(c: Color): Boolean {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(c.toArgb(), hsv)
            return hsv[1] > 0.25f && hsv[2] > 0.2f && hsv[2] < 0.9f
        }

        fun enhance(c: Color, satFactor: Float): Color {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(c.toArgb(), hsv)
            hsv[1] = (hsv[1] * satFactor).coerceAtMost(1f)
            hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
            return Color(AndroidColor.HSVToColor(hsv))
        }

        val primary = best?.rgb?.let { Color(it) }
            ?.takeIf(::vibrant)?.let { enhance(it, 1.3f) } ?: enhance(dominant, 1.1f)
        return listOf(
            primary,
            primary.copy(red = primary.red * 0.6f, green = primary.green * 0.6f, blue = primary.blue * 0.6f),
            Color.Black,
        )
    }

    /** Six distinct swatches for GLOW_ANIMATED aurora blobs. */
    fun extractGlowColors(palette: Palette, fallbackColor: Int): List<Color> {
        val raw = listOfNotNull(
            palette.vibrantSwatch, palette.lightVibrantSwatch, palette.darkVibrantSwatch,
            palette.mutedSwatch, palette.lightMutedSwatch, palette.darkMutedSwatch,
            palette.dominantSwatch,
        ).map { Color(it.rgb) }.distinct()
        if (raw.isNotEmpty()) return raw.take(6)
        return listOf(Color(fallbackColor))
    }

    private fun Color.toArgb(): Int = AndroidColor.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
    )
}
