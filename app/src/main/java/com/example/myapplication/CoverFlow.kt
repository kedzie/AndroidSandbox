package com.example.myapplication


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Configuration ────────────────────────────────────────────────────────────

private const val SIDE_ITEMS       = 4      // items rendered on each side of center
private const val ROTATION_Y       = 55f    // max rotationY in degrees
private const val SCALE_PER_ITEM   = 0.10f  // scale reduction per item from center
private const val Z_DEPTH          = 60f    // dp pushed back per item from center
private const val SPACING_FRACTION = 0.1f  // fraction of item width between item centers


// ─── State ────────────────────────────────────────────────────────────────────

/**
 * Continuous float position where integers are settled indices.
 * 0.0 = item 0 centered, 1.5 = halfway between items 1 and 2.
 *
 * [isAnimating] is true during both the decay phase and the spring snap phase.
 * Callers use this to switch item content between full and placeholder rendering.
 *
 * [targetIndex] is set before animation begins — callers can use it to
 * prefetch content at the destination before the animation completes.
 */
class CoverFlowState(initialIndex: Int = 0) {
    val position = Animatable(initialIndex.toFloat())

    // Set to true for the full fling + snap sequence, false once settled.
    // Internal set so only LazyCoverFlow drives it.
    var isAnimating by mutableStateOf(false)
        internal set

    // Set to the projected landing index before animation begins.
    // Fires before the first frame of animation — ideal for prefetching.
    var targetIndex by mutableIntStateOf(initialIndex)
        internal set

    val currentIndex: Int get() = position.value.roundToInt()
}

@Composable
fun rememberCoverFlowState(initialIndex: Int = 0): CoverFlowState =
    remember(initialIndex) { CoverFlowState(initialIndex) }

// ─── Item Provider ────────────────────────────────────────────────────────────

private class CoverFlowItemProvider(
    override val itemCount: Int,
    private val content: @Composable (index: Int) -> Unit
) : LazyLayoutItemProvider {
    @Composable
    override fun Item(index: Int, key: Any) = content(index)
    override fun getKey(index: Int): Any = index
}

// ─── Layout ───────────────────────────────────────────────────────────────────

/**
 * Cover Flow layout — center item faces forward, items to each side rotated
 * to face inward and recede in Z (like iOS Finder / iTunes).
 *
 * ## Fling feel — spring with initial velocity:
 *
 * On release, [splineBasedDecay.calculateTargetValue] is used as pure math
 * to identify the target card index — it never runs as an animation.
 * The fling velocity is handed directly to a spring targeting that card.
 *
 * The spring starts with your momentum, overshoots proportional to velocity,
 * then pulls back and settles — like being attached to a bungee cord anchored
 * at the nearest card center. Hard fling = wide overshoot + visible bounce.
 * Gentle nudge = barely overshoots, snaps cleanly.
 *
 * ## Prefetch:
 *   [onFlingTarget] fires with the projected target index BEFORE animation starts.
 *   Use this to trigger page loads (e.g. pagedItems[targetIndex]) for the
 *   destination without triggering loads for every intermediate index.
 *
 * ## Content during animation:
 *   [state.isAnimating] is true throughout the entire fling+snap sequence.
 *   Callers can use this to render cheap placeholders for non-center items
 *   rather than triggering full composition and data loads mid-fling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyCoverFlow(
    state: CoverFlowState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 200.dp,
    itemHeight: Dp = 280.dp,
    // Fires before animation begins — use to prefetch content at destination
    onFlingTarget: ((targetIndex: Int) -> Unit)? = null,
    itemContent: @Composable (index: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val updatedContent by rememberUpdatedState(itemContent)
    val updatedOnFlingTarget by rememberUpdatedState(onFlingTarget)

    // Clamp the Animatable to valid index bounds whenever itemCount changes.
    // animateDecay respects these bounds and will not overshoot them.
    LaunchedEffect(itemCount) {
        state.position.updateBounds(
            lowerBound = 0f,
            upperBound = (itemCount-1f).coerceAtLeast(0f)
        )
    }

    LazyLayout(
        itemProvider = { CoverFlowItemProvider(itemCount) { updatedContent(it) } },
        modifier = modifier
            .pointerInput(itemCount) {
                val itemWidthPx = itemWidth.toPx()
                val velocityTracker = VelocityTracker()
                // PointerInputScope IS a Density — splineBasedDecay uses it for
                // friction coefficients calibrated to the physical screen density.
                val decay = splineBasedDecay<Float>(this)

                // Velocity the animation was moving at when a new touch interrupted it.
                // Captured in onDragStart before stop() kills the animation state,
                // then blended into the spring's initialVelocity on the next release.
                var interruptedVelocity = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        // Capture animation velocity BEFORE stopping — if the user
                        // grabs the deck mid-fling we carry that momentum forward.
                        interruptedVelocity = state.position.velocity
                        scope.launch { state.position.stop() }
                        velocityTracker.resetTracking()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPointerInputChange(change)
                        scope.launch {
                            val delta = -dragAmount / itemWidthPx
                            state.position.snapTo(
                                (state.position.value + delta)
                                    .coerceIn(0f, (itemCount - 1).toFloat())
                            )
                        }
                    },
                    onDragEnd = {
                        val gestureVelocity = velocityTracker.calculateVelocity()
                        // Actual finger velocity at release
                        val velocityItems = -gestureVelocity.x / itemWidthPx

                        // If a slow drag interrupted a fast fling, the finger velocity
                        // may be near zero but the animation had meaningful momentum.
                        // Blend them — finger velocity wins if significant, otherwise
                        // the interrupted animation carries through.
                        // Use whichever is larger in magnitude so a deliberate slow
                        // drag to a specific card always wins over residual momentum.
                        val effectiveVelocity = if (abs(velocityItems) > abs(interruptedVelocity))
                            velocityItems else interruptedVelocity
                        interruptedVelocity = 0f  // consumed

                        scope.launch {
                            val projectedEnd = decay.calculateTargetValue(
                                state.position.value, effectiveVelocity
                            ).coerceIn(0f, (itemCount - 1).toFloat())

                            val target = projectedEnd.roundToInt()
                            state.targetIndex = target
                            state.isAnimating = true
                            updatedOnFlingTarget?.invoke(target)

                            state.position.animateTo(
                                targetValue = target.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                initialVelocity = effectiveVelocity
                            )

                            state.isAnimating = false
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            state.isAnimating = true
                            state.position.animateTo(
                                state.currentIndex.toFloat(),
                                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                            )
                            state.isAnimating = false
                        }
                    }
                )
            }

    ) { constraints ->
        val itemWidthPx  = itemWidth.roundToPx()
        val itemHeightPx = itemHeight.roundToPx()
        val itemConstraints = Constraints.fixed(itemWidthPx, itemHeightPx)

        // Reading position.value here subscribes the Layout phase to Animatable.
        // Every animation frame triggers re-layout — positions update each frame.
        // The graphicsLayer transforms inside placeWithLayer (rotation, scale, Z)
        // are Draw-phase operations on the RenderNode and do not cause re-layout.
        val visualPos = state.position.value

        val first = (visualPos.toInt() - SIDE_ITEMS).coerceAtLeast(0)
        val last  = (visualPos.toInt() + SIDE_ITEMS + 1).coerceAtMost(itemCount - 1)

        val measuredItems = (first..last).mapNotNull { index ->
            measure(index, itemConstraints).firstOrNull()?.let { index to it }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val centerX   = constraints.maxWidth  / 2 - itemWidthPx  / 2
            val centerY   = constraints.maxHeight / 2 - itemHeightPx / 2
            val spacingPx = (itemWidthPx * SPACING_FRACTION).toInt()


            // Back-to-front ordering so center item paints over side items.
            measuredItems
                .sortedByDescending { (index, _) -> abs(index - visualPos) }
                .forEach { (index, placeable) ->
                    val offset    = index - visualPos
                    val absOffset = abs(offset)

                    placeable.placeWithLayer(
                        x = centerX + (offset.coerceIn(-1f, 1f) * (itemWidthPx/2) + offset * spacingPx).roundToInt(),
                        y = centerY
                    ) {
                        rotationY = (-offset * ROTATION_Y).coerceIn(-ROTATION_Y, ROTATION_Y)
                        transformOrigin = TransformOrigin(.5f, 1f)
                        val scale = (1f - absOffset * SCALE_PER_ITEM).coerceAtLeast(0.4f)
                        scaleX = scale
                        scaleY = scale
                        cameraDistance = 8 * density
                    }
                }
        }
    }
}
