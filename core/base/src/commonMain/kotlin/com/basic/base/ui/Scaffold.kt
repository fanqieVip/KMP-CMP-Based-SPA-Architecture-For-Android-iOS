package com.basic.base.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.basic.base.Os
import com.basic.base.getPlatform
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

/**
 * 毛玻璃启用规则
 */
enum class HazeRule {
    /** 全部启用 */
    ALL,
    /** 全部禁用 */
    NONE,
    /** 仅安卓启用 */
    ANDROID_ONLY,
    /** 仅 iOS 启用 */
    IOS_ONLY
}

/**
 * Top 与 center 的顶部对齐方式
 */
enum class HazeScaffoldTopCenterAlignment {
    /** Center 的顶部与 Top 的顶部对齐 */
    TOP,
    /** Center 的顶部与 Top 的底部对齐 */
    BOTTOM
}

/**
 * 脚手架组件
 * @param topSurfaceModifier top区毛玻璃效果
 * @param bottomSurfaceModifier bottom 区毛玻璃效果
 * @param legacyTopModifier top 不支持或禁止毛玻璃效果的兜底修饰器
 * @param legacyBottomModifier bottom 不支持或禁止毛玻璃效果的兜底修饰器
 * @param hazeRule 毛玻璃效果启用规则
 * @param canConsumeScrollUp 是否允许向上滑动
 * @param canConsumeScrollDown 是否允许向下滑动
 * @param maxTopOverlap 最大允许的顶部重叠高度(Dp.Unspecified时为全重叠)
 * @param minTopHeight 最小允许的顶部高度(Dp.Unspecified时为保持高度不变)
 * @param topAlignment top区对齐方式
 * @param topCenterAlignment top 与 center 的顶部对齐方式
 * @param topAlignedHazeEffectDistance 顶部对齐时，毛玻璃跟随上滑距离生效的距离；无效时取 top 实际高度
 */
@Composable
fun HazeScaffold(
    modifier: Modifier = Modifier,
    state: HazeScaffoldState = rememberHazeScaffoldState(),
    topSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h) },
    bottomSurfaceModifier: (Modifier, HazeState) -> Modifier = { m, h -> defaultHazeScaffoldSurfaceModifier(m, h) },
    legacyTopModifier: (Modifier) -> Modifier = { m -> defaultLegacyTopModifier(m) },
    legacyBottomModifier: (Modifier) -> Modifier = { m -> defaultLegacyBottomModifier(m) },
    hazeRule: HazeRule = HazeRule.ALL,
    canConsumeScrollUp: () -> Boolean = { true },
    canConsumeScrollDown: () -> Boolean = { true },
    maxTopOverlap: Dp = Dp.Unspecified,
    minTopHeight: Dp = Dp.Unspecified,
    topAlignment: Alignment.Vertical = Alignment.Bottom,
    topCenterAlignment: HazeScaffoldTopCenterAlignment = HazeScaffoldTopCenterAlignment.BOTTOM,
    topAlignedHazeEffectDistance: Dp = Dp.Unspecified,
    top: @Composable HazeScaffoldScope.() -> Unit = {},
    center: @Composable HazeScaffoldScope.() -> Unit,
    bottom: @Composable HazeScaffoldScope.() -> Unit = {},
) {
    val hazeState = rememberHazeState()
    val platform = getPlatform()
    val useLegacySurfaceModifiers = remember(hazeRule, platform.os, platform.systemVersion) {
        val isLowVersionAndroid = platform.os == Os.ANDROID && (platform.systemVersion.toIntOrNull() ?: 0) <= 30
        when (hazeRule) {
            HazeRule.ALL -> isLowVersionAndroid
            HazeRule.NONE -> true
            HazeRule.ANDROID_ONLY -> platform.os != Os.ANDROID || isLowVersionAndroid
            HazeRule.IOS_ONLY -> platform.os != Os.IOS
        }
    }
    val resolvedTopSurfaceModifier = if (useLegacySurfaceModifiers) {
        { m: Modifier, _: HazeState -> legacyTopModifier(m) }
    } else {
        topSurfaceModifier
    }
    val resolvedBottomSurfaceModifier = if (useLegacySurfaceModifiers) {
        { m: Modifier, _: HazeState -> legacyBottomModifier(m) }
    } else {
        bottomSurfaceModifier
    }
    val density = LocalDensity.current
    androidx.compose.runtime.SideEffect {
        state.updateMaxTopOverlap(
            if (maxTopOverlap.isSpecified) with(density) { maxTopOverlap.toPx() } else Float.MAX_VALUE
        )
        state.updateMinTopHeight(
            if (minTopHeight.isSpecified) with(density) { minTopHeight.toPx() } else Float.NaN
        )
    }
    val bottomOverlayHeightPx = state.bottomOverlayHeightPx
    val centerContentPadding = with(density) {
        remember(bottomOverlayHeightPx) {
            PaddingValues(
                bottom = bottomOverlayHeightPx.toDp()
            )
        }
    }
    val topAlignedHazeEffectDistancePx = with(density) {
        if (topAlignedHazeEffectDistance.isSpecified && topAlignedHazeEffectDistance > 0.dp) {
            topAlignedHazeEffectDistance.toPx()
        } else {
            if (topCenterAlignment == HazeScaffoldTopCenterAlignment.BOTTOM && !state.minTopHeightPx.isNaN() && state.minTopHeightPx > 0f) {
                state.minTopHeightPx
            } else {
                state.topMeasuredHeightPx
            }
        }
    }
    val topTravelPx = state.topTravelPx
    val topSurfaceAlpha = when (topCenterAlignment) {
        HazeScaffoldTopCenterAlignment.TOP -> {
            if (topAlignedHazeEffectDistancePx > 0f) {
                (state.topScrollOffsetPx / topAlignedHazeEffectDistancePx).coerceIn(0f, 1f)
            } else {
                if (state.topScrollOffsetPx > 0f) 1f else 0f
            }
        }
        HazeScaffoldTopCenterAlignment.BOTTOM -> {
            if (useLegacySurfaceModifiers) {
                1f
            } else {
                if (topAlignedHazeEffectDistancePx > 0f && topTravelPx > 0f && topTravelPx < Float.MAX_VALUE) {
                    val startOffset = (topTravelPx - topAlignedHazeEffectDistancePx).coerceAtLeast(0f)
                    ((state.topScrollOffsetPx - startOffset) / topAlignedHazeEffectDistancePx).coerceIn(0f, 1f)
                } else {
                    if (topAlignedHazeEffectDistancePx > 0f) {
                        (state.topOverlapPx / topAlignedHazeEffectDistancePx).coerceIn(0f, 1f)
                    } else {
                        if (state.topOverlapPx > 0f) 1f else 0f
                    }
                }
            }
        }
    }
    val nestedScrollConnection = remember(state, canConsumeScrollUp, canConsumeScrollDown, topCenterAlignment) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (topCenterAlignment == HazeScaffoldTopCenterAlignment.TOP) {
                    return Offset.Zero
                }
                val consumedY = when {
                    // 向上滑动时，外层优先消费（先压缩/重叠）
                    available.y < 0f && canConsumeScrollUp() -> state.dispatchScrollDelta(available.y)
                    else -> 0f
                }
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (topCenterAlignment == HazeScaffoldTopCenterAlignment.TOP) {
                    when {
                        consumed.y < 0f && canConsumeScrollUp() -> state.observeScrollDelta(consumed.y)
                        consumed.y > 0f && canConsumeScrollDown() -> state.observeScrollDelta(consumed.y)
                    }
                    when {
                        available.y < 0f && canConsumeScrollUp() -> state.observeScrollDelta(available.y)
                        available.y > 0f && canConsumeScrollDown() -> state.observeScrollDelta(available.y)
                    }
                    return Offset.Zero
                }
                // 向下滑动时，内部先消费，内部不消费后，外层再消费（后恢复高度）
                return if (available.y > 0f && canConsumeScrollDown()) {
                    Offset(0f, state.dispatchScrollDelta(available.y))
                } else {
                    Offset.Zero
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalHazeScaffoldState provides state,
        LocalHazeScaffoldContentPadding provides centerContentPadding,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (useLegacySurfaceModifiers) Modifier else Modifier.hazeSource(hazeState))
                    .nestedScroll(nestedScrollConnection)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layout { measurable, constraints ->
                            val topInset = when (topCenterAlignment) {
                                HazeScaffoldTopCenterAlignment.TOP -> 0
                                HazeScaffoldTopCenterAlignment.BOTTOM ->
                                    state.topVisibleHeightPx.roundToInt().coerceIn(0, constraints.maxHeight)
                            }
                            val adjustedConstraints = constraints.copy(
                                minHeight = (constraints.minHeight - topInset).coerceAtLeast(0),
                                maxHeight = (constraints.maxHeight - topInset).coerceAtLeast(0)
                            )
                            val placeable = measurable.measure(adjustedConstraints)
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeWithLayer(0, topInset) {
                                    clip = false
                                }
                            }
                        }
                ) {
                    state.center()
                }
            }

            HazeScaffoldSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(with(density) { state.topCurrentHeightPx.toDp() })
                    .then(
                        if (!useLegacySurfaceModifiers && topCenterAlignment == HazeScaffoldTopCenterAlignment.BOTTOM) {
                            legacyTopModifier(Modifier)
                        } else Modifier
                    )
                    .then(
                        if (minTopHeight.isSpecified) {
                            Modifier.draggable(
                                state = rememberDraggableState { delta ->
                                    state.dispatchScrollDelta(delta)
                                },
                                orientation = Orientation.Vertical
                            )
                        } else Modifier
                    )
                    .graphicsLayer { clip = true },
                hazeState = hazeState,
                surfaceModifier = resolvedTopSurfaceModifier,
                surfaceLayerModifier = Modifier.graphicsLayer { alpha = topSurfaceAlpha },
                content = {
                    val resolvedTopAlignment = if (topAlignment == Alignment.Top) Alignment.Top else Alignment.Bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true, align = resolvedTopAlignment)
                            .onSizeChanged { state.updateTopHeight(it.height.toFloat()) }
                    ) {
                        state.top()
                    }
                }
            )

            HazeScaffoldSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { state.updateBottomHeight(it.height.toFloat()) },
                hazeState = hazeState,
                surfaceModifier = resolvedBottomSurfaceModifier,
                content = {
                    state.bottom()
                }
            )
        }
    }
}

@Composable
private fun HazeScaffoldSurface(
    modifier: Modifier,
    hazeState: HazeState,
    surfaceModifier: (Modifier, HazeState) -> Modifier,
    surfaceLayerModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = surfaceModifier(
                Modifier
                    .matchParentSize()
                    .then(surfaceLayerModifier),
                hazeState
            )
        )
        content()
    }
}

fun defaultHazeScaffoldSurfaceModifier(
    modifier: Modifier,
    hazeState: HazeState,
    baseColor: Color = Color.White,
): Modifier {
    return modifier
        .background(baseColor.copy(alpha = 0.45f))
        .hazeEffect(hazeState) {
            blurEffect {
                blurRadius = 25.dp
                backgroundColor = baseColor.copy(alpha = 0.40f)
                colorEffects = listOf(
                    HazeColorEffect.tint(baseColor.copy(alpha = 0.10f))
                )
            }
        }
}

fun defaultLegacyTopModifier(
    modifier: Modifier,
    baseColor: Color = Color(0xFFF7F4EC),
): Modifier {
    return modifier.background(baseColor)
}

fun defaultLegacyBottomModifier(
    modifier: Modifier,
    baseColor: Color = Color(0xFFFBF8F1),
): Modifier {
    return modifier.background(baseColor)
}

@Composable
fun rememberHazeScaffoldState(): HazeScaffoldState = remember {
    HazeScaffoldState()
}

@Stable
interface HazeScaffoldScope {
    val expansionProgress: Float
}

@Stable
class HazeScaffoldState internal constructor() : HazeScaffoldScope {
    private var topHeightPx by mutableFloatStateOf(0f)
    private var bottomHeightPx by mutableFloatStateOf(0f)
    private var topOffsetPx by mutableFloatStateOf(0f)
    internal var minTopHeightPx by mutableFloatStateOf(Float.NaN)
        private set
    internal var maxTopOverlapPx by mutableFloatStateOf(Float.MAX_VALUE)
        private set

    val progress: Float
        get() = when (val travel = topTravelPx) {
            0f -> 0f
            else -> topOffsetPx / travel
        }

    internal val topMeasuredHeightPx: Float
        get() = topHeightPx

    internal val topScrollOffsetPx: Float
        get() = topOffsetPx

    override val expansionProgress: Float
        get() = 1f - progress

    val isExpanded: Boolean
        get() = topOffsetPx <= 0f

    val isCollapsed: Boolean
        get() = topOffsetPx > 0f

    val topCompressionPx: Float
        get() {
            if (minTopHeightPx.isNaN()) return 0f
            val compressionTravel = (topHeightPx - minTopHeightPx).coerceAtLeast(0f)
            return topOffsetPx.coerceIn(0f, compressionTravel)
        }

    val topOverlapPx: Float
        get() {
            val compressionTravel = if (minTopHeightPx.isNaN()) 0f else (topHeightPx - minTopHeightPx).coerceAtLeast(0f)
            return (topOffsetPx - compressionTravel).coerceAtLeast(0f)
        }

    val topCurrentHeightPx: Float
        get() = if (topHeightPx > 0f) (topHeightPx - topCompressionPx).coerceAtLeast(0f) else 0f

    val topVisibleHeightPx: Float
        get() = if (topHeightPx > 0f) (topHeightPx - topOffsetPx).coerceAtLeast(0f) else 0f

    val bottomOverlayHeightPx: Float
        get() = bottomHeightPx

    internal fun updateTopHeight(heightPx: Float) {
        if (topHeightPx != heightPx) {
            topHeightPx = heightPx
            topOffsetPx = topOffsetPx.coerceIn(0f, topTravelPx)
        }
    }

    internal fun updateBottomHeight(heightPx: Float) {
        bottomHeightPx = heightPx
    }

    internal fun updateMaxTopOverlap(overlapPx: Float) {
        maxTopOverlapPx = overlapPx
        topOffsetPx = topOffsetPx.coerceIn(0f, topTravelPx)
    }

    internal fun updateMinTopHeight(minHeightPx: Float) {
        minTopHeightPx = minHeightPx
        topOffsetPx = topOffsetPx.coerceIn(0f, topTravelPx)
    }

    internal fun dispatchScrollDelta(deltaY: Float): Float {
        val travel = topTravelPx
        if (travel <= 0f || deltaY == 0f) return 0f

        val previous = topOffsetPx
        val next = (previous - deltaY).coerceIn(0f, travel)
        topOffsetPx = next
        return previous - next
    }

    internal fun observeScrollDelta(deltaY: Float) {
        val travel = topTravelPx
        if (travel <= 0f || deltaY == 0f) return

        topOffsetPx = (topOffsetPx - deltaY).coerceIn(0f, travel)
    }

    internal val topTravelPx: Float
        get() {
            val compressionTravel = if (minTopHeightPx.isNaN()) 0f else (topHeightPx - minTopHeightPx).coerceAtLeast(0f)
            val overlapTravel = if (minTopHeightPx.isNaN()) {
                topHeightPx.coerceAtMost(maxTopOverlapPx)
            } else {
                minTopHeightPx.coerceAtMost(maxTopOverlapPx)
            }
            return compressionTravel + overlapTravel
        }

    suspend fun animateToExpanded() {
        val travel = topTravelPx
        if (travel <= 0f) {
            topOffsetPx = 0f
            return
        }
        animate(
            initialValue = topOffsetPx,
            targetValue = 0f,
            animationSpec = spring()
        ) { value, _ ->
            topOffsetPx = value
        }
    }
}

val LocalHazeScaffoldState = staticCompositionLocalOf<HazeScaffoldState?> {
    null
}

val LocalHazeScaffoldContentPadding = staticCompositionLocalOf {
    PaddingValues()
}
