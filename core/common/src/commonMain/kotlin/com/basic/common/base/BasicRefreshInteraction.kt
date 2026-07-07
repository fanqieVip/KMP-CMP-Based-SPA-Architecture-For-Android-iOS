package com.basic.common.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.RefreshLazyGrid
import com.basic.base.ktx.RefreshLazyList
import com.basic.base.ktx.RefreshLazyStaggeredGrid
import com.basic.base.ktx.RefreshState
import com.basic.common.R_com_basic_common
import com.basic.common.common_usr_classic_arrow
import com.basic.common.common_usr_classic_spinner
import dev.materii.pullrefresh.PullRefreshDefaults
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 业务层通用列表刷新组件
 * @param modifier 修饰符
 * @param dataSize 数据长度
 * @param state 刷新状态
 * @param childScrollState 列表滚动状态
 * @param canPullDownRefresh 是否允许下拉刷新
 * @param contentPadding 内容内边距
 * @param horizontalAlignment 水平对齐方式
 * @param verticalArrangement 垂直排列方式
 * @param flingBehavior 滚动行为
 * @param overscrollEffect 越界效果
 * @param refreshThreshold 刷新阈值
 * @param refreshingOffset 刷新中偏移量
 * @param backdropColor 背景颜色
 * @param stickyHeader 吸顶头部
 * @param refreshHeader 刷新头部
 * @param refreshFooter 刷新底部
 * @param content 列表项
 */
@Composable
fun BasicRefreshLazyListInteraction(
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
    refreshingOffset: Dp = PullRefreshDefaults.RefreshThreshold,
    backdropColor: Color = Color.Transparent,
    stickyHeader: @Composable ColumnScope.() -> Unit = {},
    refreshHeader: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshHeader(state)
    },
    refreshFooter: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshFooter(state)
    },
    content: LazyListScope.() -> Unit
) {
    RefreshLazyList(
        modifier = modifier.fillMaxSize(),
        state = state,
        dataSize = dataSize,
        childScrollState = childScrollState,
        canPullDownRefresh = canPullDownRefresh,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        overscrollEffect = overscrollEffect,
        refreshHeader = refreshHeader,
        refreshFooter = refreshFooter,
        refreshThreshold = refreshThreshold,
        refreshingOffset = refreshingOffset,
        backdropColor = backdropColor,
        stickyHeader = stickyHeader,
        content = content
    )
}

/**
 * 业务层通用瀑布流刷新组件
 * @param modifier 修饰符
 * @param columns 列数配置
 * @param dataSize 数据长度
 * @param state 刷新状态
 * @param childScrollState 列表滚动状态
 * @param canPullDownRefresh 是否允许下拉刷新
 * @param contentPadding 内容内边距
 * @param horizontalArrangement 水平排列方式
 * @param verticalItemSpacing 垂直项间距
 * @param flingBehavior 滚动行为
 * @param overscrollEffect 越界效果
 * @param refreshThreshold 刷新阈值
 * @param refreshingOffset 刷新中偏移量
 * @param backdropColor 背景颜色
 * @param refreshHeader 刷新头部
 * @param refreshFooter 刷新底部
 * @param stickyHeader 吸顶头部
 * @param content 列表项
 */
@Composable
fun BasicRefreshStaggeredGridInteraction(
    modifier: Modifier,
    columns: StaggeredGridCells,
    dataSize: () -> Int,
    state: RefreshState,
    childScrollState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    canPullDownRefresh: () -> Boolean = { true },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalItemSpacing: Dp = 0.dp,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
    backdropColor: Color = Color.Transparent,
    refreshHeader: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshHeader(state)
    },
    refreshFooter: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshFooter(state)
    },
    stickyHeader: @Composable ColumnScope.() -> Unit = {},
    content: LazyStaggeredGridScope.() -> Unit,
) {
    RefreshLazyStaggeredGrid(
        modifier = modifier,
        columns = columns,
        dataSize = dataSize,
        state = state,
        childScrollState = childScrollState,
        canPullDownRefresh = canPullDownRefresh,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalItemSpacing = verticalItemSpacing,
        flingBehavior = flingBehavior,
        overscrollEffect = overscrollEffect,
        refreshThreshold = refreshThreshold,
        refreshingOffset = refreshingOffset,
        backdropColor = backdropColor,
        refreshHeader = refreshHeader,
        refreshFooter = refreshFooter,
        stickyHeader = stickyHeader,
        content = content
    )
}

/**
 * 业务层通用网格刷新组件
 * @param modifier 修饰符
 * @param columns 列数配置
 * @param dataSize 数据长度
 * @param state 刷新状态
 * @param childScrollState 列表滚动状态
 * @param canPullDownRefresh 是否允许下拉刷新
 * @param contentPadding 内容内边距
 * @param horizontalArrangement 水平排列方式
 * @param verticalArrangement 垂直排列方式
 * @param flingBehavior 滚动行为
 * @param overscrollEffect 越界效果
 * @param refreshThreshold 刷新阈值
 * @param refreshingOffset 刷新中偏移量
 * @param backdropColor 背景颜色
 * @param refreshHeader 刷新头部
 * @param refreshFooter 刷新底部
 * @param stickyHeader 吸顶头部
 * @param content 列表项
 */
@Composable
fun BasicRefreshLazyGridInteraction(
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
    refreshingOffset: Dp = PullRefreshDefaults.RefreshThreshold,
    backdropColor: Color = Color.Transparent,
    refreshHeader: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshHeader(state)
    },
    refreshFooter: @Composable (state: RefreshState) -> Unit = { state ->
        RefreshFooter(state)
    },
    stickyHeader: @Composable ColumnScope.() -> Unit = {},
    content: LazyGridScope.() -> Unit,
) {
    RefreshLazyGrid(
        modifier = modifier,
        columns = columns,
        dataSize = dataSize,
        state = state,
        childScrollState = childScrollState,
        canPullDownRefresh = canPullDownRefresh,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        overscrollEffect = overscrollEffect,
        refreshThreshold = refreshThreshold,
        refreshingOffset = refreshingOffset,
        backdropColor = backdropColor,
        refreshHeader = refreshHeader,
        refreshFooter = refreshFooter,
        stickyHeader = stickyHeader,
        content = content
    )
}

private const val AnimationDurationMs = 150 //箭头动画时间
private val animationSpec = tween<Float>(durationMillis = AnimationDurationMs)

/**
 * 自定义补零函数
 * @param value 输入数值
 * @return 补零后的字符串
 */
private fun padZero(value: Int): String {
    return if (value < 10) "0$value" else value.toString()
}

/**
 * 获取刷新时间
 * @param time 刷新时间
 * @return 格式化后的刷新时间字符串
 */
private fun getRefreshTime(time: Instant): String {
    val instantFromMillis = Instant.fromEpochMilliseconds(time.toEpochMilliseconds())
    val localDateTime = instantFromMillis.toLocalDateTime(TimeZone.currentSystemDefault())

    //格式化为 MM-dd HH:mm
    val month = padZero(localDateTime.month.number)     // 月份补零
    val day = padZero(localDateTime.day)                // 日期补零
    val hour = padZero(localDateTime.hour)              // 小时补零
    val minute = padZero(localDateTime.minute)          // 分钟补零
    val formattedTime = "$month-$day $hour:$minute"

    return "上次刷新：$formattedTime"
}

/**
 * 刷新头部组件
 * @param refreshRefreshState 刷新状态
 */
@Composable
private fun RefreshHeader(refreshRefreshState: RefreshState) {
    val state = refreshRefreshState.state.collectAsState().value
    val progress = refreshRefreshState.progress.collectAsState().value

    val arrowDegrees = remember { Animatable(initialValue = 0f) }
    val tipContent = if (state == RefreshState.State.PULL_DOWN_REFRESHING) {
        "正在刷新…"
    } else {
        if (progress < 1) "下拉可以刷新" else "释放立即刷新"
    }
    val arrowChange by derivedStateOf { progress < 1 }
    LaunchedEffect(arrowChange) {
        arrowDegrees.animateTo(
            targetValue = if (arrowChange) 0f else 180f,
            animationSpec = animationSpec
        )
    }
    var lastRefreshTime by remember("lastRefreshTime") { mutableStateOf(getRefreshTime(Clock.System.now())) }
    LaunchedEffect(state) {
        if (state == RefreshState.State.IDLE) {
            lastRefreshTime = getRefreshTime(Clock.System.now())
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = state != RefreshState.State.IDLE, transitionSpec = {
                    (fadeIn(animationSpec) + scaleIn(animationSpec)).togetherWith(
                        fadeOut(animationSpec) + scaleOut(animationSpec)
                    )
                },
                label = ""
            ) {
                if (it) {
                    val transition = rememberInfiniteTransition(label = "InfiniteTransition")
                    val rotate by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing)
                        ),
                        label = "RotateAnimation"
                    )
                    Image(
                        painter = painterResource(R_com_basic_common.drawable.common_usr_classic_spinner),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotate),
                        colorFilter = null,
                    )

                } else {
                    Image(
                        painter = painterResource(R_com_basic_common.drawable.common_usr_classic_arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowDegrees.value),
                        colorFilter = null,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .widthIn(min = 96.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Crossfade(targetState = tipContent, animationSpec = animationSpec) {
                    BasicText(
                        text = it, style = TextStyle.Default.copy(
                            fontSize = 15.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }

                Spacer(modifier = Modifier.size(2.dp))
                Crossfade(targetState = lastRefreshTime, animationSpec = animationSpec) {
                    BasicText(
                        text = it, style = TextStyle.Default.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    )
                }
            }
        }
    }
}

/**
 * 刷新底部组件
 * @param refreshRefreshState 刷新状态
 */
@Composable
private fun RefreshFooter(refreshRefreshState: RefreshState) {
    val state = refreshRefreshState.state.collectAsState().value
    val noMore = refreshRefreshState.noMore.collectAsState().value
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (state == RefreshState.State.PULL_UP_REFRESHING) {
            Text(
                text = "加载中...",
                color = Color(0xFFCCCCCC),
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            if (noMore) {
                Text(
                    text = "没有更多了",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Text(
                    text = "点击加载更多",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 24.dp).clickable {
                        refreshRefreshState.state.value = RefreshState.State.PULL_UP_REFRESHING
                    }
                )
            }
        }

    }
}
