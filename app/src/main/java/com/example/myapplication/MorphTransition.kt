package com.example.myapplication

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * A [Shape] that renders a [Morph] at whatever progress [progressState] currently holds.
 *
 * [progressState] is a [State] reference (not a raw Float) so the value is read
 * lazily inside [createOutline] on every draw call rather than captured once at
 * construction time. Without this, the shape would be frozen at the progress value
 * from the first composition.
 *
 * The morph polygon lives in -1..1 coordinate space. [createOutline] scales it to
 * the composable's pixel bounds via a Matrix on every call.
 */
class MorphableShape(
    private val morph: Morph,
    private val progressState: State<Float>
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val matrix = Matrix().also {
            it.setRectToRect(
                RectF(-1f, -1f, 1f, 1f),
                RectF(0f, 0f, size.width, size.height),
                Matrix.ScaleToFit.FILL
            )
        }
        val nativePath = android.graphics.Path()
        // Read state here — inside the draw system, not in composition
        morph.toPath(progress = progressState.value, path = nativePath)
        nativePath.transform(matrix)
        return Outline.Generic(nativePath.asComposePath())
    }
}

/**
 * Applies [SharedTransitionScope.sharedBounds] with a clip shape that morphs
 * between [startShape] and [endShape] in sync with the nav entry's enter/exit
 * transition.
 *
 * The morph progress is a child of [animatedVisibilityScope]'s [Transition],
 * so it shares the same timeline as the bounds animation and is automatically
 * seekable during predictive back.
 *
 * Wire up on the source (nav icon) and destination (screen root) with the same [key]:
 *
 * ```kotlin
 * // On the Camera nav icon:
 * Icon(
 *     modifier = Modifier.sharedBoundsWithMorphableShape(
 *         key = "camera_reveal",
 *         sharedTransitionScope = LocalSharedTransitionScope.current!!,
 *         animatedVisibilityScope = LocalNavAnimatedContentScope.current,
 *         startShape = RoundedPolygon.circle(numVertices = 8),
 *         endShape   = RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.05f))
 *     )
 * )
 *
 * // On the CameraScreen root Box:
 * Box(
 *     modifier = Modifier.sharedBoundsWithMorphableShape(
 *         key = "camera_reveal",
 *         sharedTransitionScope = LocalSharedTransitionScope.current!!,
 *         animatedVisibilityScope = LocalNavAnimatedContentScope.current,
 *         startShape = RoundedPolygon.circle(numVertices = 8),
 *         endShape   = RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.05f))
 *     )
 * )
 * ```
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsWithMorphableShape(
    key: Any,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    startShape: RoundedPolygon,
    endShape: RoundedPolygon,
    animationSpec: FiniteAnimationSpec<Float> = spring(),
    // Default None on both ends so only the destination content is rendered
    // during the transition — no crossfade bleed from the source composable.
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
): Modifier = with(sharedTransitionScope) {

    val morph = remember(startShape, endShape) {
        Morph(start = startShape, end = endShape)
    }

    // Capture the State<Float> object, not the delegated Float value.
    // The State reference is stable across recompositions; progressState.value
    // is read lazily inside MorphableShape.createOutline during the draw pass.
    val progressState: State<Float> = animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { animationSpec },
        label = "morph_clip_$key"
    ) { enterExitState ->
        when (enterExitState) {
            EnterExitState.PreEnter -> 1f
            EnterExitState.Visible  -> 0f
            EnterExitState.PostExit -> 1f
        }
    }

    // remember(morph) so the shape instance is stable — recreating it every
    // recomposition would break OverlayClip identity checks inside SharedTransitionScope.
    val clipShape = remember(morph) {
        MorphableShape(morph = morph, progressState = progressState)
    }
        this@sharedBoundsWithMorphableShape.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = enter,
            exit = exit,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(clipShape),
        ).skipToLookaheadSize().skipToLookaheadPosition()
}
