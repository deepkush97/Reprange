package com.deepkush.reprange.ui.component

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.deepkush.reprange.constants.BackgroundStyle
import com.deepkush.reprange.ui.theme.ColorExtractor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import androidx.palette.graphics.Palette
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Content-driven dynamic background engine per ANDROID_M3_EXPRESSIVE_STANDARDS §3.
 * Crossfades atmospheres via AnimatedContent; opacity tied to sheet/drag progress.
 */
@Composable
fun DynamicImageBackground(
    imageUrl: String?,
    contentId: String?,
    style: BackgroundStyle,
    modifier: Modifier = Modifier,
    progressAlpha: Float = 1f,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val cache = remember { mutableMapOf<String, List<Color>>() }
    val fallback = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgbCompat()

    LaunchedEffect(contentId, style, imageUrl) {
        if (imageUrl == null || contentId == null ||
            style !in setOf(BackgroundStyle.GRADIENT, BackgroundStyle.GLOW_ANIMATED)
        ) {
            gradientColors = emptyList()
            return@LaunchedEffect
        }
        val key = "${style.name}-$contentId"
        cache[key]?.let {
            gradientColors = it
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = imageLoader.execute(
                    ImageRequest.Builder(context).data(imageUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .crossfade(false)
                        .build(),
                ).image?.toBitmap() ?: return@runCatching

                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).maximumColorCount(8)
                        .resizeBitmapArea(100 * 100).generate()
                }
                val colors = when (style) {
                    BackgroundStyle.GRADIENT ->
                        ColorExtractor.extractGradientTriple(palette, fallback)
                    else ->
                        ColorExtractor.extractGlowColors(palette, fallback)
                }
                cache[key] = colors
                withContext(Dispatchers.Main) { gradientColors = colors }
            }
        }
    }

    val crossfadeMs = when (style) {
        BackgroundStyle.GRADIENT, BackgroundStyle.BLUR, BackgroundStyle.THEMED -> 800
        BackgroundStyle.GLOW_ANIMATED, BackgroundStyle.HERO_LAYERED -> 1200
        BackgroundStyle.LIVE_MESH -> 1500
    }

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = style to contentId,
            transitionSpec = {
                fadeIn(tween(crossfadeMs)) togetherWith fadeOut(tween(crossfadeMs))
            },
            label = "dynamicBackground",
        ) { (currentStyle, currentId) ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) },
            ) {
                when (currentStyle) {
                    BackgroundStyle.THEMED -> {}
                    BackgroundStyle.GRADIENT -> GradientLayer(gradientColors)
                    BackgroundStyle.BLUR ->
                        BlurLayer(imageUrl.takeIf { currentId != null })
                    BackgroundStyle.GLOW_ANIMATED -> GlowLayer(gradientColors)
                    BackgroundStyle.HERO_LAYERED ->
                        HeroLayered(imageUrl.takeIf { currentId != null })
                    BackgroundStyle.LIVE_MESH ->
                        LiveMesh(imageUrl.takeIf { currentId != null })
                }
            }
        }
    }
}

private fun Color.toArgbCompat(): Int =
    AndroidColor.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
    )

/** GRADIENT - extracted-color vertical stops + contrast scrim (§3.3). */
@Composable
private fun GradientLayer(colors: List<Color>) {
    val c0 = colors.getOrNull(0) ?: Color.Transparent
    val c1 = colors.getOrNull(1) ?: c0.copy(alpha = 0.6f)
    val c2 = colors.getOrNull(2) ?: Color.Black
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(0.0f to c0, 0.5f to c1, 1.0f to c2),
            )
            .background(Color.Black.copy(alpha = 0.2f)),
    )
}

/** Soft theme-fused variant for detail surfaces (§3.4). */
@Composable
fun ThemedGradientOverlay(colors: List<Color>, surface: Color, modifier: Modifier = Modifier) {
    val c0 = animateColorAsState(
        colors.getOrNull(0)?.copy(alpha = 0.5f) ?: surface,
        tween(800),
        label = "c0",
    ).value
    val c1 = animateColorAsState(
        colors.getOrNull(1)?.copy(alpha = 0.3f) ?: surface,
        tween(800),
        label = "c1",
    ).value
    Box(
        modifier.background(
            Brush.verticalGradient(0f to c0, 0.5f to c1, 1f to surface),
        ),
    )
}

/** BLUR - real-time backdrop blur via Haze pair (requires SDK >= S upstream). */
@Composable
private fun BlurLayer(imageUrl: String?) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    if (imageUrl == null) return
    AsyncImage(
        model = ImageRequest.Builder(context).data(imageUrl).size(256).build(),
        contentDescription = null,
        imageLoader = SingletonImageLoader.get(context),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
    )
    Box(
        Modifier
            .fillMaxSize()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.Black,
                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.30f))),
                    blurRadius = 80.dp,
                    noiseFactor = 0.15f,
                ),
            ),
    )
}

/** GLOW_ANIMATED - drifting radial aurora blobs on one cached canvas pass (§3.3). */
@Composable
private fun GlowLayer(colors: List<Color>) {
    val progress by rememberInfiniteTransition("glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "glowProgress",
    )

    fun oscillate(min: Float, max: Float, phase: Float): Float =
        min + (max - min) * ((sin(2f * PI.toFloat() * (progress + phase)) + 1f) * 0.5f)

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val palette = if (colors.isEmpty()) listOf(Color(0xFF101216)) else colors
                fun rotatedColorAt(i: Int): Color {
                    val n = palette.size
                    val pos = progress * n + i
                    val i0 = ((pos.toInt() % n) + n) % n
                    val frac = pos - pos.toInt()
                    return androidx.compose.ui.graphics.lerp(
                        palette[i0 % n],
                        palette[(i0 + 1) % n],
                        frac,
                    )
                }

                onDrawBehind {
                    drawRect(Color(0xFF050505))
                    repeat(6) { i ->
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    rotatedColorAt(i).copy(alpha = 0.85f - i * 0.05f),
                                    Color.Transparent,
                                ),
                                center = Offset(
                                    size.width * oscillate(0f, 1f, i * 0.11f),
                                    size.height * oscillate(0f, 1f, i * 0.11f + 0.05f),
                                ),
                                radius = size.width * oscillate(0.7f, 1.6f, i * 0.09f),
                            ),
                        )
                    }
                }
            },
    )
}

/** HERO_LAYERED - sharp image top + blurred base + masked fade (§3.3). */
@Composable
private fun HeroLayered(imageUrl: String?) {
    val context = LocalContext.current
    if (imageUrl == null) return
    val loader = SingletonImageLoader.get(context)

    AsyncImage(
        model = ImageRequest.Builder(context).data(imageUrl).size(128).build(),
        contentDescription = null,
        imageLoader = loader,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(150.dp),
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            imageLoader = loader,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.999f)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.75f to Color.Black,
                            0.92f to Color.Black.copy(alpha = 0.4f),
                            1.00f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.4f)),
                ),
            ),
    )
}

/** LIVE_MESH - counter-rotating saturated blurred layers (§3.3). */
@Composable
private fun LiveMesh(imageUrl: String?) {
    val context = LocalContext.current
    if (imageUrl == null) return
    val loader = SingletonImageLoader.get(context)

    val t1 by rememberInfiniteTransition("mesh1").animateFloat(
        0f, 360f, infiniteRepeatable(tween(80000, easing = LinearEasing), RepeatMode.Restart), "r1",
    )
    val t2 by rememberInfiniteTransition("mesh2").animateFloat(
        360f, 0f, infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), "r2",
    )
    val t3 by rememberInfiniteTransition("mesh3").animateFloat(
        0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart), "r3",
    )

    val saturation = remember {
        ColorMatrix().apply { setToSaturation(1.8f) }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.8f
                    scaleY = 1.8f
                },
        ) {
            MeshImage(loader, context, imageUrl, rotation = t1, saturation = saturation, alpha = 0.85f)
            MeshImage(loader, context, imageUrl, rotation = t2, saturation = saturation, alpha = 0.7f)
            MeshImage(loader, context, imageUrl, rotation = t3, saturation = saturation, alpha = 0.6f)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                    ),
                ),
        )
    }
}

@Composable
private fun MeshImage(
    loader: coil3.ImageLoader,
    context: android.content.Context,
    url: String,
    rotation: Float,
    saturation: ColorMatrix,
    alpha: Float,
) {
    AsyncImage(
        model = ImageRequest.Builder(context).data(url).size(128).build(),
        contentDescription = null,
        imageLoader = loader,
        contentScale = ContentScale.Crop,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(saturation),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = rotation
            }
            .blur(110.dp),
    )
}
