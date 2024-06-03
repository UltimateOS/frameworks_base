package com.android.systemui.communal.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.CommunalSwipeDetector
import com.android.compose.animation.scene.DefaultSwipeDetector
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ElementMatcher
import com.android.compose.animation.scene.FixedSizeEdgeDetector
import com.android.compose.animation.scene.LowestZIndexScenePicker
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.SwipeDirection
import com.android.compose.animation.scene.observableTransitionState
import com.android.compose.animation.scene.transitions
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.systemui.Flags
import com.android.systemui.Flags.glanceableHubFullscreenSwipe
import com.android.systemui.communal.shared.model.CommunalBackgroundType
import com.android.systemui.communal.shared.model.CommunalScenes
import com.android.systemui.communal.shared.model.CommunalTransitionKeys
import com.android.systemui.communal.ui.compose.extensions.allowGestures
import com.android.systemui.communal.ui.viewmodel.CommunalViewModel
import com.android.systemui.communal.util.CommunalColors
import com.android.systemui.res.R
import com.android.systemui.scene.shared.model.SceneDataSourceDelegator
import com.android.systemui.scene.ui.composable.SceneTransitionLayoutDataSource

object Communal {
    object Elements {
        val Scrim = ElementKey("Scrim", scenePicker = LowestZIndexScenePicker)
        val Grid = ElementKey("CommunalContent")
        val LockIcon = ElementKey("CommunalLockIcon")
        val IndicationArea = ElementKey("CommunalIndicationArea")
    }
}

object AllElements : ElementMatcher {
    override fun matches(key: ElementKey, scene: SceneKey) = true
}

val sceneTransitions = transitions {
    to(CommunalScenes.Communal, key = CommunalTransitionKeys.SimpleFade) {
        spec = tween(durationMillis = 250)
        fade(AllElements)
    }
    to(CommunalScenes.Communal) {
        spec = tween(durationMillis = 1000)
        translate(Communal.Elements.Grid, Edge.Right)
        timestampRange(startMillis = 167, endMillis = 334) { fade(AllElements) }
    }
    to(CommunalScenes.Blank) {
        spec = tween(durationMillis = 1000)
        translate(Communal.Elements.Grid, Edge.Right)
        timestampRange(endMillis = 167) {
            fade(Communal.Elements.Grid)
            fade(Communal.Elements.IndicationArea)
            fade(Communal.Elements.LockIcon)
        }
        timestampRange(startMillis = 167, endMillis = 334) { fade(Communal.Elements.Scrim) }
    }
}

/**
 * View containing a [SceneTransitionLayout] that shows the communal UI and handles transitions.
 *
 * This is a temporary container to allow the communal UI to use [SceneTransitionLayout] for gesture
 * handling and transitions before the full Flexiglass layout is ready.
 */
@Composable
fun CommunalContainer(
    modifier: Modifier = Modifier,
    viewModel: CommunalViewModel,
    dataSourceDelegator: SceneDataSourceDelegator,
    colors: CommunalColors,
    content: CommunalContent,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentSceneKey: SceneKey by
        viewModel.currentScene.collectAsStateWithLifecycle(CommunalScenes.Blank)
    val touchesAllowed by viewModel.touchesAllowed.collectAsStateWithLifecycle(initialValue = false)
    val showGestureIndicator by
        viewModel.showGestureIndicator.collectAsStateWithLifecycle(initialValue = false)
    val backgroundType by
        viewModel.communalBackground.collectAsStateWithLifecycle(
            initialValue = CommunalBackgroundType.DEFAULT
        )
    val state: MutableSceneTransitionLayoutState = remember {
        MutableSceneTransitionLayoutState(
            initialScene = currentSceneKey,
            canChangeScene = { _ -> viewModel.canChangeScene() },
            transitions = sceneTransitions,
            enableInterruptions = false,
        )
    }

    val detector = remember { CommunalSwipeDetector() }

    DisposableEffect(state) {
        val dataSource = SceneTransitionLayoutDataSource(state, coroutineScope)
        dataSourceDelegator.setDelegate(dataSource)
        onDispose { dataSourceDelegator.setDelegate(null) }
    }

    // This effect exposes the SceneTransitionLayout's observable transition state to the rest of
    // the system, and unsets it when the view is disposed to avoid a memory leak.
    DisposableEffect(viewModel, state) {
        viewModel.setTransitionState(state.observableTransitionState())
        onDispose { viewModel.setTransitionState(null) }
    }

    val swipeSourceDetector =
        if (glanceableHubFullscreenSwipe()) {
            detector
        } else {
            FixedSizeEdgeDetector(dimensionResource(id = R.dimen.communal_gesture_initiation_width))
        }

    val swipeDetector =
        if (glanceableHubFullscreenSwipe()) {
            detector
        } else {
            DefaultSwipeDetector
        }

    SceneTransitionLayout(
        state = state,
        modifier = modifier.fillMaxSize(),
        swipeSourceDetector = swipeSourceDetector,
        swipeDetector = swipeDetector,
    ) {
        scene(
            CommunalScenes.Blank,
            userActions =
                mapOf(
                    Swipe(SwipeDirection.Left, fromSource = Edge.Right) to CommunalScenes.Communal
                )
        ) {
            // This scene shows nothing only allowing for transitions to the communal scene.
            // TODO(b/339667383): remove this temporary swipe gesture handle
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
                if (showGestureIndicator && Flags.glanceableHubGestureHandle()) {
                    Box(
                        modifier =
                            Modifier.height(220.dp)
                                .width(4.dp)
                                .align(Alignment.CenterVertically)
                                .background(color = Color.White, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        scene(
            CommunalScenes.Communal,
            userActions =
                mapOf(Swipe(SwipeDirection.Right, fromSource = Edge.Left) to CommunalScenes.Blank)
        ) {
            CommunalScene(backgroundType, colors, content)
        }
    }

    // Touches on the notification shade in blank areas fall through to the glanceable hub. When the
    // shade is showing, we block all touches in order to prevent this unwanted behavior.
    Box(modifier = Modifier.fillMaxSize().allowGestures(touchesAllowed))
}

/** Scene containing the glanceable hub UI. */
@Composable
private fun SceneScope.CommunalScene(
    backgroundType: CommunalBackgroundType,
    colors: CommunalColors,
    content: CommunalContent,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.element(Communal.Elements.Scrim).fillMaxSize()) {
        when (backgroundType) {
            CommunalBackgroundType.DEFAULT -> DefaultBackground(colors = colors)
            CommunalBackgroundType.STATIC_GRADIENT -> StaticLinearGradient()
            CommunalBackgroundType.ANIMATED -> AnimatedLinearGradient()
        }
    }
    with(content) { Content(modifier = modifier) }
}

/** Default background of the hub, a single color */
@Composable
private fun BoxScope.DefaultBackground(
    colors: CommunalColors,
) {
    val backgroundColor by colors.backgroundColor.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier.matchParentSize().background(Color(backgroundColor.toArgb())),
    )
}

/** Experimental hub background, static linear gradient */
@Composable
private fun BoxScope.StaticLinearGradient() {
    val colors = LocalAndroidColorScheme.current
    Box(
        Modifier.matchParentSize()
            .background(
                Brush.linearGradient(colors = listOf(colors.primary, colors.primaryContainer)),
            )
    )
    BackgroundTopScrim()
}

/** Experimental hub background, animated linear gradient */
@Composable
private fun BoxScope.AnimatedLinearGradient() {
    val colors = LocalAndroidColorScheme.current
    Box(
        Modifier.matchParentSize()
            .animatedGradientBackground(colors = listOf(colors.primary, colors.primaryContainer))
    )
    BackgroundTopScrim()
}

/** Scrim placed on top of the background in order to dim/bright colors */
@Composable
private fun BoxScope.BackgroundTopScrim() {
    val darkTheme = isSystemInDarkTheme()
    val scrimOnTopColor = if (darkTheme) Color.Black else Color.White
    Box(Modifier.matchParentSize().alpha(0.34f).background(scrimOnTopColor))
}

/** Modifier which sets the background of a composable to an animated gradient */
@Composable
private fun Modifier.animatedGradientBackground(colors: List<Color>): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "scrim background")
    val startOffsetX by
        transition.animateFloat(
            initialValue = -size.width.toFloat(),
            targetValue = size.width.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 5_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scrim start offset"
        )
    background(
            brush =
                Brush.linearGradient(
                    colors = colors,
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
                )
        )
        .onGloballyPositioned { size = it.size }
}
