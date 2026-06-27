package com.basic.base.ktx

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity

/**
 * A wrapper for [OverscrollEffect] that allows pull-to-refresh to take priority when at the top.
 * On iOS, the native bounce effect can interfere with pull-to-refresh by consuming the drag gestures.
 * This wrapper ensures that when dragging down at the top, the gesture is passed to the performScroll
 * (and subsequently to nested scroll participants like PullRefresh) before the overscroll effect.
 */
class RefreshOverscrollEffect(
    private val base: OverscrollEffect,
    private val isAtTop: () -> Boolean
) : OverscrollEffect {
    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        // When pulling down (delta.y > 0) at the top during a drag, let performScroll handle it first.
        // This allows PullRefresh to respond immediately without being intercepted by iOS bounce.
        return if (delta.y > 0 && (source == NestedScrollSource.Drag || source == NestedScrollSource.UserInput) && isAtTop()) {
            val consumed = performScroll(delta)
            val left = delta - consumed
            if (left.y > 0) {
                // If there's still delta left, let the base overscroll handle it
                base.applyToScroll(left, source) { Offset.Zero } + consumed
            } else {
                consumed
            }
        } else {
            base.applyToScroll(delta, source, performScroll)
        }
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        base.applyToFling(velocity, performFling)
    }

    override val isInProgress: Boolean
        get() = base.isInProgress

    override val node: DelegatableNode
        get() = base.node
}

@Composable
fun rememberRefreshOverscrollEffect(
    isAtTop: () -> Boolean,
    base: OverscrollEffect? = rememberOverscrollEffect()
): OverscrollEffect? {
    if (base == null) return null
    return remember(base, isAtTop) {
        RefreshOverscrollEffect(base, isAtTop)
    }
}
