package com.basic.base.ktx

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basic.base.ui.LocalHazeScaffoldState
import dev.materii.pullrefresh.DragRefreshLayout
import dev.materii.pullrefresh.PullRefreshDefaults
import dev.materii.pullrefresh.rememberPullRefreshState

@Composable
fun RefreshLazyGrid(
    modifier: Modifier,
    columns: GridCells,
    dataSize: () -> Int,
    state: RefreshState,
    childScrollState: LazyGridState = rememberLazyGridState(),
    canPullDownRefresh: () -> Boolean = { true },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
    backdropColor: Color = Color.Black.copy(alpha = 0.3f),
    refreshHeader: @Composable (state: RefreshState) -> Unit,
    refreshFooter: @Composable (state: RefreshState) -> Unit,
    stickyHeader: @Composable ColumnScope.() -> Unit = {},
    content: LazyGridScope.() -> Unit,
) {
    val refreshState = state.state.collectAsState().value
    val scaffoldState = LocalHazeScaffoldState.current
    val coordinatorLayoutState = LocalCoordinatorLayoutState.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.enablePullDown && refreshState == RefreshState.State.PULL_DOWN_REFRESHING,
        refreshingOffset = refreshingOffset,
        refreshThreshold = refreshThreshold,
        onRefresh = {
            if (state.enablePullDown && refreshState != RefreshState.State.PULL_DOWN_REFRESHING && refreshState != RefreshState.State.PULL_UP_REFRESHING) {
                state.state.value = RefreshState.State.PULL_DOWN_REFRESHING
            }
        })
    val isPullDownEnable = state.enablePullDown && canPullDownRefresh() && (scaffoldState?.isExpanded != false) && refreshState != RefreshState.State.PULL_UP_REFRESHING
    val hasData by remember {
        derivedStateOf { dataSize() > 0 }
    }
    val refreshOverscrollEffect = rememberRefreshOverscrollEffect(
        isAtTop = { !childScrollState.canScrollBackward },
        base = overscrollEffect
    )
    DragRefreshLayout(
        modifier = Modifier.fillMaxWidth().coordinatorMainScroll(coordinatorLayoutState, childScrollState),
        state = pullRefreshState,
        flipped = false,
        enabled = isPullDownEnable,
        indicator = {
            refreshHeader(state)
        },
        backdropColor = backdropColor,
        content = {
            Column(modifier = modifier.fillMaxWidth()) {
                Column(modifier = Modifier.coordinatorDragProxy(coordinatorLayoutState)) {
                    stickyHeader()
                }
                LazyVerticalGrid(
                    modifier = modifier.fillMaxWidth(),
                    columns = columns,
                    contentPadding = contentPadding,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                    flingBehavior = flingBehavior,
                    overscrollEffect = refreshOverscrollEffect,
                    state = childScrollState) {
                    content()
                    if (state.enablePullUp && hasData) {
                        item { refreshFooter(state) }
                    }
                }
            }
        }
    )
    LaunchedEffect(pullRefreshState.progress){
        state.progress.value = pullRefreshState.progress
    }
    val noMore = state.noMore.collectAsState().value
    if (state.enablePullUp && !noMore) {
        LaunchedEffect(childScrollState.canScrollForward) {
            if (!childScrollState.canScrollForward && childScrollState.canScrollBackward) {
                if (refreshState != RefreshState.State.PULL_DOWN_REFRESHING && refreshState != RefreshState.State.PULL_UP_REFRESHING) {
                    state.state.value = RefreshState.State.PULL_UP_REFRESHING
                }
            }
        }
    }
}
