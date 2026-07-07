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
 * @param modifier 基础修饰符
 * @param state 脚手架状态
 * @param topSurfaceColor top区背景颜色
 * @param bottomSurfaceColor bottom区背景颜色
 * @param topSurfaceModifier top区毛玻璃效果
 * @param bottomSurfaceModifier bottom区毛玻璃效果
 * @param legacyTopModifier top 不支持或禁止毛玻璃效果的兜底修饰器
 * @param legacyBottomModifier bottom 不支持或禁止毛玻璃效果的兜底修饰器
 * @param hazeRule 毛玻璃效果启用规则
 * @param canConsumeScrollUp 是否可以向上消费滚动
 * @param canConsumeScrollDown 是否可以向下消费滚动
 * @param maxTopOverlap 顶部最大重叠距离
 * @param minTopHeight 顶部最小高度
 * @param topAlignment 顶部对齐方式
 * @param top 顶部内容
 * @param center 中间内容
 * @param bottom 底部内容
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
