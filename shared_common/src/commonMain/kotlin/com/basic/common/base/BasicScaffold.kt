package com.basic.common.base

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.basic.base.ui.HazeScaffold
import com.basic.base.ui.HazeScaffoldScope
import com.basic.base.ui.HazeScaffoldState
import com.basic.base.ui.defaultHazeScaffoldSurfaceModifier
import com.basic.base.ui.rememberHazeScaffoldState
import dev.chrisbanes.haze.HazeState

@Composable
fun BasicHazeScaffold(
    modifier: Modifier = Modifier,
    state: HazeScaffoldState = rememberHazeScaffoldState(),
    topSurfaceColor: Color = Color.White,
    bottomSurfaceColor: Color = Color.White,
    topSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h, topSurfaceColor) },
    bottomSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h, bottomSurfaceColor) },
    legacyAndroidTopModifier: (Modifier) -> Modifier = { m -> m.background(topSurfaceColor) },
    legacyAndroidBottomModifier: (Modifier) -> Modifier = { m -> m.background(bottomSurfaceColor) },
    canConsumeScrollUp: () -> Boolean = { true },
    canConsumeScrollDown: () -> Boolean = { true },
    maxTopOverlap: Dp = Dp.Unspecified,
    minTopHeight: Dp = Dp.Unspecified,
    topAlignment: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.Bottom,
    top: @Composable HazeScaffoldScope.() -> Unit = {},
    center: @Composable HazeScaffoldScope.() -> Unit,
    bottom: @Composable HazeScaffoldScope.() -> Unit = {},
) {
    HazeScaffold(
        modifier = modifier,
        state = state,
        topSurfaceModifier = topSurfaceModifier,
        bottomSurfaceModifier = bottomSurfaceModifier,
        legacyAndroidTopModifier = legacyAndroidTopModifier,
        legacyAndroidBottomModifier = legacyAndroidBottomModifier,
        canConsumeScrollUp = canConsumeScrollUp,
        canConsumeScrollDown = canConsumeScrollDown,
        maxTopOverlap = maxTopOverlap,
        minTopHeight = minTopHeight,
        topAlignment = topAlignment,
        top = top,
        center = center,
        bottom = bottom,
    )
}
