package com.basic.common.base

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.basic.base.ui.HazeRule
import com.basic.base.ui.HazeScaffold
import com.basic.base.ui.HazeScaffoldScope
import com.basic.base.ui.HazeScaffoldState
import com.basic.base.ui.defaultHazeScaffoldSurfaceModifier
import com.basic.base.ui.rememberHazeScaffoldState
import dev.chrisbanes.haze.HazeState

/**
 * 业务层脚手架组件
 * @param topSurfaceColor top区背景颜色
 * @param bottomSurfaceColor bottom区背景颜色
 * @param topSurfaceModifier top区毛玻璃效果
 * @param bottomSurfaceModifier bottom区毛玻璃效果
 * @param legacyTopModifier top 不支持或禁止毛玻璃效果的兜底修饰器
 * @param legacyBottomModifier bottom 不支持或禁止毛玻璃效果的兜底修饰器
 * @param hazeRule 毛玻璃效果启用规则
 */
@Composable
fun BasicHazeScaffold(
    modifier: Modifier = Modifier,
    state: HazeScaffoldState = rememberHazeScaffoldState(),
    topSurfaceColor: Color = Color.White,
    bottomSurfaceColor: Color = Color.White,
    topSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h, topSurfaceColor) },
    bottomSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h, bottomSurfaceColor) },
    legacyTopModifier: (Modifier) -> Modifier = { m -> m.background(topSurfaceColor) },
    legacyBottomModifier: (Modifier) -> Modifier = { m -> m.background(bottomSurfaceColor) },
    hazeRule: HazeRule = HazeRule.ALL,
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
        legacyTopModifier = legacyTopModifier,
        legacyBottomModifier = legacyBottomModifier,
        hazeRule = hazeRule,
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
