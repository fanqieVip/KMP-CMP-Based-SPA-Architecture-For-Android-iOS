package com.basic.base.ktx

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basic.base.ui.LocalHazeScaffoldState

/**
 * 相对于原版能更好的兼容ios/android上嵌入不同控件时的声明周期
 * @param autoResetHazeScaffoldOnPageSettled 当搭配Scaffold脚手架使用时，是否在切换Pager后自动Center归位
 */
@Composable
fun HorizontalPagerLifecycle(
    state: PagerState,
    modifier: Modifier = Modifier,
    autoResetHazeScaffoldOnPageSettled: (settledPage: Int) -> Boolean = { false },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val hazeScaffoldState = LocalHazeScaffoldState.current
    val shouldAutoResetOnPageSettled by rememberUpdatedState(autoResetHazeScaffoldOnPageSettled)
    var lastSettledPage by remember(state) { mutableIntStateOf(state.settledPage) }

    LaunchedEffect(state, hazeScaffoldState) {
        if (hazeScaffoldState == null) return@LaunchedEffect
        snapshotFlow { state.settledPage }.collect { settledPage ->
            if (settledPage == lastSettledPage) return@collect
            if (shouldAutoResetOnPageSettled(settledPage)) {
                hazeScaffoldState.animateToExpanded()
            }
            lastSettledPage = settledPage
        }
    }

    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = overscrollEffect,
        pageContent = {
            PageContentLifecycle(state, it, pageContent)
        }
    )
}

/**
 * 相对于原版能更好的兼容ios/android上嵌入不同控件时的声明周期
 */
@Composable
fun VerticalPagerLifecycle(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Vertical),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    VerticalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = overscrollEffect,
        pageContent = {
            PageContentLifecycle(state, it, pageContent)
        }
    )
}
@Composable
private fun PagerScope.PageContentLifecycle(state: PagerState, index: Int, pageContent: @Composable PagerScope.(page: Int) -> Unit){
    val visible = state.currentPage == index
    CompositionLocalProvider(LocalPageLifecycleVisible provides visible) {
        pageContent(index)
    }
}
val LocalPageLifecycleVisible = staticCompositionLocalOf<Boolean> { true }
