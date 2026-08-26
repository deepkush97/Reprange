package com.deepkush.reprange.ui.screens.exercise

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.content.ContextWrapper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.data.repo.Difficulty
import com.deepkush.reprange.ui.component.DynamicImageBackground
import com.deepkush.reprange.ui.theme.extractThemeColor
import com.deepkush.reprange.ui.theme.LocalThemeSeedController
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hero surface per §3: content-driven dynamic background + adaptive foreground trio +
 * status-bar icon flip + optional live theme re-seeding from the artwork.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    viewModel: com.deepkush.reprange.viewmodels.ExerciseDetailViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val themeSeed = LocalThemeSeedController.current

    val (backgroundStyle, _) = com.deepkush.reprange.utils.rememberEnumPreference(
        PreferenceKeys.BACKGROUND_STYLE,
        com.deepkush.reprange.constants.BackgroundStyle.GRADIENT,
    )
    val (showGifs, _) = rememberPreference(PreferenceKeys.SHOW_GIFS, true)
    val (dynamicFromContent, _) = rememberPreference(PreferenceKeys.DYNAMIC_FROM_CONTENT, true)

    val dynamic = backgroundStyle != com.deepkush.reprange.constants.BackgroundStyle.THEMED
    val scheme = MaterialTheme.colorScheme

    // §3.5 adaptive foreground trio.
    val adaptivePrimary by animateColorAsState(if (dynamic) Color.White else scheme.onSurface, tween(400), label = "afP")
    val adaptiveSecondary by animateColorAsState(
        if (dynamic) Color.White.copy(alpha = 0.7f) else scheme.onSurfaceVariant,
        tween(400),
        label = "afS",
    )
    val adaptiveSurface by animateColorAsState(
        if (dynamic) Color.White.copy(alpha = 0.2f) else scheme.surfaceVariant,
        tween(400),
        label = "afB",
    )

    // Status-bar icons flip over dark atmospheres (§3.5).
    val baseContext = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(baseContext) {
        var ctx: android.content.Context? = baseContext
        while (ctx != null && ctx !is android.app.Activity) {
            ctx = (ctx as? android.content.ContextWrapper)?.baseContext
        }
        ctx
    }
    DisposableEffect(dynamic) {
        val window = activity?.window
        val controller = window?.decorView?.let { v -> WindowInsetsControllerCompat(window, v) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    // §2.2 content-derived live seeding.
    LaunchedEffect(exercise?.id, dynamicFromContent) {
        val ex = exercise ?: return@LaunchedEffect
        if (!dynamicFromContent) return@LaunchedEffect
        val url = ex.imageUrl
        withContext(Dispatchers.IO) {
            runCatching {
                val result = SingletonImageLoader.get(context).execute(
                    ImageRequest.Builder(context).data(url)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("seed_${ex.id}")
                        .build(),
                )
                val bitmap = result.image?.toBitmap() ?: return@runCatching
                withContext(Dispatchers.Main) {
                    themeSeed.value = bitmap.extractThemeColor()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        DynamicImageBackground(
            imageUrl = exercise?.let { if (showGifs) it.gifUrl else it.imageUrl },
            contentId = exercise?.mediaId,
            style = backgroundStyle,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (dynamic) Color.White.copy(alpha = 0.15f) else scheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    IconButton(onClick = { AppHaptics.tap(view); onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = adaptivePrimary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hero animation/thumbnail.
            exercise?.let { ex ->
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(context)
                        .data(if (showGifs) ex.gifUrl else ex.imageUrl).build(),
                    contentDescription = ex.name,
                    imageLoader = SingletonImageLoader.get(context),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(200.dp)
                        .clip(listItemShape(0, 1, radius = 28.dp)),
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    ex.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = adaptivePrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${ex.target.replaceFirstChar { it.uppercase() }} · ${ex.equipment.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = adaptiveSecondary,
                )
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(ex.category.replaceFirstChar { it.uppercase() }, adaptiveSurface, adaptivePrimary)
                    MetaChip(Difficulty.fromName(ex.difficulty).label, adaptiveSurface, adaptivePrimary)
                    if (ex.secondaryMuscles.isNotEmpty()) {
                        MetaChip(
                            "+" + ex.secondaryMuscles.size.toString() + " muscles",
                            adaptiveSurface,
                            adaptivePrimary,
                        )
                    }
                }

                if (ex.instructionsEn.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    Text("How to", style = MaterialTheme.typography.titleMedium, color = adaptivePrimary)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = listItemShape(0, 1, radius = 20.dp),
                        color = if (dynamic) Color.Black.copy(alpha = 0.35f)
                        else scheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            ex.instructionsEn.forEachIndexed { i, step ->
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        "${i + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = adaptiveSecondary,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Text(
                                        step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = adaptivePrimary.copy(alpha = 0.95f),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ex.attribution,
                        style = MaterialTheme.typography.labelSmall,
                        color = adaptiveSecondary.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 28.dp),
                ) {
                    Button(
                        onClick = {
                            AppHaptics.primaryTap(view)
                            viewModel.startNow { id -> if (id > 0) onStartWorkout(id) }
                        },
                        shape = listItemShape(0, 1, radius = 18.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Start now")
                    }
                    if (viewModel.hasActiveSession()) {
                        OutlinedButton(
                            onClick = {
                                AppHaptics.confirm(view)
                                viewModel.addToLiveSession { }
                                onBack()
                            },
                            shape = listItemShape(0, 1, radius = 18.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, container: Color, content: Color) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
