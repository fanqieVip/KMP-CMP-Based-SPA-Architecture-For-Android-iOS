package com.basic.base.ktx

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

/**
 * 一个只负责“头部折叠/展开”的协调者组件。
 *
 * 它不关心子内容具体是什么滚动实现，也不直接操作子组件的 scroll state。
 * 只要子组件支持 nested scroll（LazyColumn、verticalScroll、pullRefresh 等），
 * 都可以和这里的头部折叠逻辑自然协作。
 * 悬停效果有两种方式(可根据实际情况选择合适的方式)：
 * 1.minHeaderHeight = header区域需要悬停的那部分高度
 * 2.minHeaderHeight = 0dp（允许全部折叠Header），content中使用Column包裹需要悬停的部分+Lazy列表组件
 * @param minHeaderHeight 滑动时Header最小高度。Dp.Unspecified：header不允许折叠  其他：可最大折叠到指定高度
 * @param scrollUpPriority 上划时消费优先级
 * @param scrollDownPriority 下滑时消费优先级
 * @param canConsumeScrollUp 是否消费上划事件
 * @param canConsumeScrollDown 是否消费下滑事件
 * @param header expandedProgress: 折叠进度（0f~1f）
 * @param content 子内容
 */
@Composable
fun CoordinatorLayout(
    modifier: Modifier = Modifier,
    state: CoordinatorLayoutState = rememberCoordinatorLayoutState(),
    minHeaderHeight: Dp = Dp.Unspecified,
    scrollUpPriority: CoordinatorPriority = CoordinatorPriority.PARENT_FIRST,
    scrollDownPriority: CoordinatorPriority = CoordinatorPriority.CHILD_FIRST,
    canConsumeScrollUp: () -> Boolean = { true },
    canConsumeScrollDown: () -> Boolean = { true },
    header: @Composable (expandedProgress: Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val minHeaderHeightPx = remember(minHeaderHeight, density) {
        if (minHeaderHeight.isSpecified) {
            with(density) { minHeaderHeight.toPx() }
        } else {
            null
        }
    }

    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    LaunchedEffect(state, nestedScrollDispatcher) {
        state.dispatcher = nestedScrollDispatcher
    }

    val nestedScrollConnection = remember(state, scrollUpPriority, scrollDownPriority, canConsumeScrollUp, canConsumeScrollDown) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return when {
                    available.y < 0f && scrollUpPriority == CoordinatorPriority.PARENT_FIRST && canConsumeScrollUp() ->
                        Offset(0f, state.dispatchScrollDelta(available.y))

                    available.y > 0f && scrollDownPriority == CoordinatorPriority.PARENT_FIRST && canConsumeScrollDown() ->
                        Offset(0f, state.dispatchScrollDelta(available.y))

                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return when {
                    available.y < 0f && scrollUpPriority == CoordinatorPriority.CHILD_FIRST && canConsumeScrollUp() ->
                        Offset(0f, state.dispatchScrollDelta(available.y))

                    available.y > 0f && scrollDownPriority == CoordinatorPriority.CHILD_FIRST && canConsumeScrollDown() ->
                        Offset(0f, state.dispatchScrollDelta(available.y))

                    else -> Offset.Zero
                }
            }
        }
    }

    CompositionLocalProvider(LocalCoordinatorLayoutState provides state) {
        SubcomposeLayout(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
        ) { constraints ->
            val headerPlaceables = subcompose(CoordinatorSlot.Header) {
                // Header 默认不转发给子级
                Box(modifier = Modifier.coordinatorDragProxy(state, forwardToContent = false)) {
                    header(state.expandedFraction)
                }
            }.map {
                it.measure(constraints.copy(minWidth = 0, minHeight = 0))
            }
            val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
            state.updateHeaderHeight(headerHeight.toFloat(), minHeaderHeightPx)
            val collapseOffset = state.collapsedOffsetPx.roundToInt()
            val contentY = (headerHeight - collapseOffset).coerceAtLeast(0)
            val contentHeight = (constraints.maxHeight - contentY).coerceAtLeast(0)

            val bodyPlaceables = subcompose(CoordinatorSlot.Content) {
                Box(modifier = Modifier.coordinatorDragProxy(state)){
                    content()
                }
            }.map {
                it.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = contentHeight,
                    )
                )
            }

            val layoutWidth = constraints.maxWidth
            val layoutHeight = constraints.maxHeight

            layout(layoutWidth, layoutHeight) {
                headerPlaceables.forEach { placeable ->
                    placeable.placeRelative(x = 0, y = -collapseOffset)
                }

                bodyPlaceables.forEach { placeable ->
                    placeable.placeRelative(x = 0, y = contentY)
                }
            }
        }
    }
}

@Composable
fun rememberCoordinatorLayoutState(): CoordinatorLayoutState = remember {
    CoordinatorLayoutState()
}

@Stable
class CoordinatorLayoutState internal constructor() {
    internal var headerHeightPx by mutableFloatStateOf(0f)
        private set

    internal var minHeaderHeightPx by mutableFloatStateOf(0f)
        private set

    private var hasMinHeaderHeight by mutableStateOf(false)

    internal var collapsedOffsetPx by mutableFloatStateOf(0f)
        private set

    val visibleHeaderHeightPx: Float
        get() = (headerHeightPx - collapsedOffsetPx).coerceAtLeast(0f)

    internal var contentConnection by mutableStateOf<NestedScrollConnection?>(null)
    internal var dispatcher by mutableStateOf<NestedScrollDispatcher?>(null)

    val collapsedFraction: Float
        get() {
            val scrollableHeight = scrollableHeightPx
            return when {
                scrollableHeight <= 0f -> 0f
                else -> collapsedOffsetPx / scrollableHeight
            }
        }

    val expandedFraction: Float
        get() = 1f - collapsedFraction

    val status: State<ExpandStatus> = derivedStateOf {
        val scrollableHeight = scrollableHeightPx
        when {
            collapsedOffsetPx <= 0f -> ExpandStatus.isExpanded
            scrollableHeight > 0f && collapsedOffsetPx >= scrollableHeight -> ExpandStatus.isCollapsed
            else -> ExpandStatus.halfExpanded
        }
    }

    private val scrollableHeightPx: Float
        get() = if (hasMinHeaderHeight) {
            (headerHeightPx - minHeaderHeightPx).coerceAtLeast(0f)
        } else {
            0f
        }

    internal fun updateHeaderHeight(heightPx: Float, minHeightPx: Float?) {
        headerHeightPx = heightPx
        hasMinHeaderHeight = minHeightPx != null
        minHeaderHeightPx = minHeightPx ?: heightPx
        collapsedOffsetPx = collapsedOffsetPx.coerceIn(0f, scrollableHeightPx)
    }

    /**
     * @param deltaY nested scroll 体系里的 y 方向位移。
     * 向上滚动时为负值，向下滚动时为正值。
     * @return 当前容器实际消费掉的位移。
     */
    internal fun dispatchScrollDelta(deltaY: Float): Float {
        val scrollableHeight = scrollableHeightPx
        if (scrollableHeight <= 0f || deltaY == 0f) return 0f

        val previous = collapsedOffsetPx
        val next = (previous - deltaY).coerceIn(0f, scrollableHeight)
        collapsedOffsetPx = next
        return previous - next
    }

    /**
     * 手动触发一个完整的 Nested Scroll 流程，主要用于在 Header 上滑动时，
     * 将位移同步给 CoordinatorLayout 自身以及内容区域（Content）。
     *
     * @param forwardToContent 是否转发给子内容。对于 Header 这种区域，通常不希望它滚动列表，设为 false。
     */
    internal fun dispatchFullScroll(
        deltaY: Float,
        source: NestedScrollSource,
        forwardToContent: Boolean = true
    ): Float {
        var consumedY = 0f
        val available = Offset(0f, deltaY)

        // 1. Parent Pre-scroll
        val parentPreConsumed = dispatcher?.dispatchPreScroll(available, source) ?: Offset.Zero
        consumedY += parentPreConsumed.y

        val remainingAfterParentPre = available - parentPreConsumed

        // 2. Coordinator Pre-scroll (向上划折叠)
        // Proxy 模式下无视全局优先级，固定 Parent-First 保证 Header 优先响应
        val coordPreConsumed = if (remainingAfterParentPre.y < 0f) {
            Offset(0f, dispatchScrollDelta(remainingAfterParentPre.y))
        } else Offset.Zero
        consumedY += coordPreConsumed.y

        val remainingAfterCoordPre = remainingAfterParentPre - coordPreConsumed

        // 3. Content Pre-scroll (转发给子级)
        val contentConsumed = if (forwardToContent) {
            contentConnection?.onPreScroll(remainingAfterCoordPre, source) ?: Offset.Zero
        } else Offset.Zero
        consumedY += contentConsumed.y

        val remainingAfterContent = remainingAfterCoordPre - contentConsumed

        // 4. Coordinator Post-scroll (向下划展开)
        val coordPostConsumed = if (remainingAfterContent.y > 0f) {
            Offset(0f, dispatchScrollDelta(remainingAfterContent.y))
        } else Offset.Zero
        consumedY += coordPostConsumed.y

        val remainingAfterCoordPost = remainingAfterContent - coordPostConsumed

        // 5. Parent Post-scroll
        val parentPostConsumed = dispatcher?.dispatchPostScroll(
            consumed = Offset(0f, consumedY),
            available = remainingAfterCoordPost,
            source = source
        ) ?: Offset.Zero
        consumedY += parentPostConsumed.y

        return consumedY
    }

    suspend fun animateToExpanded() {
        val scrollableHeight = scrollableHeightPx
        if (scrollableHeight <= 0f) {
            collapsedOffsetPx = 0f
            return
        }
        animate(
            initialValue = collapsedOffsetPx,
            targetValue = 0f,
            animationSpec = spring()
        ) { value, _ ->
            collapsedOffsetPx = value
        }
    }

    suspend fun animateToCollapsed() {
        val scrollableHeight = scrollableHeightPx
        if (scrollableHeight <= 0f) {
            collapsedOffsetPx = 0f
            return
        }
        animate(
            initialValue = collapsedOffsetPx,
            targetValue = scrollableHeight,
            animationSpec = spring()
        ) { value, _ ->
            collapsedOffsetPx = value
        }
    }
}

enum class CoordinatorPriority {
    CHILD_FIRST,
    PARENT_FIRST
}

enum class ExpandStatus {
    isCollapsed,
    isExpanded,
    halfExpanded
}

private enum class CoordinatorSlot {
    Header,
    Content
}

val LocalCoordinatorLayoutState = staticCompositionLocalOf<CoordinatorLayoutState?> {
    null
}

@Composable
fun currentCoordinatorHeaderHeight(): Dp {
    val state = LocalCoordinatorLayoutState.current
    val density = LocalDensity.current
    return with(density) { (state?.visibleHeaderHeightPx ?: 0f).toDp() }
}

/**
 * 将当前组件标记为 CoordinatorLayout 的“主要滚动源”。
 * 这样当在 Header 或者其他代理组件上滑动时，剩余的位移会分发给这个组件。
 */
fun Modifier.coordinatorMainScroll(
    state: CoordinatorLayoutState?,
    scrollableState: ScrollableState,
): Modifier = composed {
    if (state == null) return@composed this

    val connection = remember(state, scrollableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val consumed = scrollableState.dispatchRawDelta(-available.y)
                return Offset(0f, -consumed)
            }
        }
    }

    LaunchedEffect(state, connection) {
        state.contentConnection = connection
    }

    this
}

/**
 * 将当前组件的滑动事件转发给 CoordinatorLayout 处理。
 *
 * @param forwardToContent 是否转发未被消费的位移给子内容组件。
 */
fun Modifier.coordinatorDragProxy(
    state: CoordinatorLayoutState?,
    enabled: Boolean = true,
    forwardToContent: Boolean = false,
): Modifier = composed {
    if (state == null) return@composed this

    val relayState = rememberScrollableState { delta ->
        val consumed = state.dispatchFullScroll(delta, NestedScrollSource.UserInput, forwardToContent)
        consumed
    }

    scrollable(
        state = relayState,
        orientation = Orientation.Vertical,
        enabled = enabled,
    )
}
