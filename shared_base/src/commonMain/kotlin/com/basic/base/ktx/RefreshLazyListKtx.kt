package com.basic.base.ktx

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basic.base.ui.LocalHazeScaffoldState
import com.basic.base.utils.toastShort
import dev.materii.pullrefresh.DragRefreshLayout
import dev.materii.pullrefresh.PullRefreshDefaults
import dev.materii.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RefreshLazyList(
    modifier: Modifier,
    dataSize: () -> Int,
    state: RefreshState,
    childScrollState: LazyListState = rememberLazyListState(),
    canPullDownRefresh: () -> Boolean = { true },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
    backdropColor: Color = Color.Black.copy(alpha = 0.3f),
    refreshHeader: @Composable (state: RefreshState) -> Unit,
    refreshFooter: @Composable (state: RefreshState) -> Unit,
    stickyHeader: @Composable ColumnScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
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
                LazyColumn(modifier = Modifier.fillMaxWidth(),
                    state = childScrollState,
                    contentPadding = contentPadding,
                    horizontalAlignment = horizontalAlignment,
                    flingBehavior = flingBehavior,
                    verticalArrangement = verticalArrangement,
                    overscrollEffect = refreshOverscrollEffect) {
                    content()
                    if (state.enablePullUp && hasData) {
                        stickyHeader { refreshFooter(state) }
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

@Composable
fun rememberRefreshState(
    enablePullUp: Boolean = true,
    enablePullDown: Boolean = true
): RefreshState {
    return rememberSaveable(saver = RefreshState.Saver) {
        RefreshState(enablePullUp, enablePullDown)
    }
}

data class RefreshState(
    internal val enablePullUp: Boolean = true,
    internal val enablePullDown: Boolean = true
) {
    companion object {
        internal val Saver: Saver<RefreshState, *> =
            listSaver(save = { listOf(it.enablePullUp, it.enablePullDown, it.state, it.progress, it.noMore) }, restore = {
                RefreshState(it[0] as Boolean, it[1] as Boolean).apply {
                    state.value = it[2] as State
                    progress.value = it[3] as Float
                    noMore.value = it[4] as Boolean
                }
            })
    }

    var state = MutableStateFlow(State.IDLE)
    var noMore = MutableStateFlow(true)
    var progress = MutableStateFlow(0f)
    enum class State {
        IDLE,
        PULL_UP_REFRESHING,
        PULL_DOWN_REFRESHING
    }

    enum class Direction {
        IDLE, UP, DOWN
    }
}

/**
 * 分页控制器，仅限与MainScreenModel绑定使用
 */
interface PagingControl {
    val refreshState: RefreshState

    /**
     * 首次加载
     */
    suspend fun pagingFirst()

    /**
     * 分页加载
     */
    suspend fun pagingMore()

    /**
     * 设置是否还有更多分页数据
     */
    suspend fun pagingOver(isOver: Boolean) {
        withContext(Dispatchers.Main) {
            refreshState.noMore.value = isOver
        }
    }
}

/**
 * 初始化分页器
 * 在ScreenModel的init{}代码块中手动调用
 */
internal fun PagingControl.initPagingControl(screenModelScope: CoroutineScope) {
    screenModelScope.launch {
        refreshState.state.distinctUntilChanged { old, new -> old == new }.collectLatest {
            screenModelScope.launchScope {
                when (it) {
                    RefreshState.State.PULL_UP_REFRESHING -> {
                        pagingMore()
                    }

                    RefreshState.State.PULL_DOWN_REFRESHING -> {
                        pagingFirst()
                    }

                    else -> {}
                }
            }.catch { _, error, _ ->
                toastShort("$error")
            }.finally {
                withContext(Dispatchers.Main) {
                    refreshState.state.value = RefreshState.State.IDLE
                }
            }
        }
    }
}
