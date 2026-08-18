package com.basic.base.vortex

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.basic.base.base.BaseScreen
import io.github.hristogochev.vortex.model.ScreenModelStore
import io.github.hristogochev.vortex.navigator.LocalNavigatorStateHolder
import io.github.hristogochev.vortex.navigator.Navigator
import io.github.hristogochev.vortex.screen.Screen
import io.github.hristogochev.vortex.screen.ScreenDisposableEffectStore
import io.github.hristogochev.vortex.screen.ScreenTransition
import io.github.hristogochev.vortex.screen.ScreenTransitionPredictiveBack
import io.github.hristogochev.vortex.screen.render
import io.github.hristogochev.vortex.stack.StackEvent
import io.github.hristogochev.vortex.util.currentOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
)

expect class BackEventCompat {

    /**
     * Absolute X location of the touch point of this event in the coordinate space of the view that
     * * received this back event.
     */
    val touchX: Float

    /**
     * Absolute Y location of the touch point of this event in the coordinate space of the view that
     * received this back event.
     */
    val touchY: Float

    /** Value between 0 and 1 on how far along the back gesture is. */
    @get:FloatRange(from = 0.0, to = 1.0)
    val progress: Float

    /** Indicates which edge the swipe starts from. */
    @get:IntRange(from = 0, to = 1)
    val swipeEdge: Int

    companion object {
        /** Indicates that the edge swipe starts from the left edge of the screen  */
        val EDGE_LEFT: Int

        /** Indicates that the edge swipe starts from the right edge of the screen  */
        val EDGE_RIGHT: Int
    }
}


/**
 *  Displays the current screen of a [Navigator] with an predictive-back transition.
 *
 *  The predictive-back transition must be of type [ScreenTransitionPredictiveBack].
 *
 *  Takes in a default [ScreenTransition] for when a screen enters and leaves the visible area.
 *
 *  Each [Screen] can have it's own transition for when it enters and leaves the visible area.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CurrentScreenPredictiveBack(
    navigator: Navigator,
    defaultPredictiveBackTransition: ScreenTransitionPredictiveBack,
    swipeSides: List<Int> = listOf(0, 1),
    defaultOnScreenAppearTransition: ScreenTransition? = null,
    defaultOnScreenDisappearTransition: ScreenTransition? = null,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    contentKey: (Screen) -> Any = { it.key },
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit = { it.Content() },
) {
    // This updates instantly when the stack changes
    var unexpectedScreenStateKeysQueue by rememberSaveable(saver = unexpectedScreenStateKeysQueueSaver()) {
        mutableStateOf(emptySet())
    }

    val oldScreens = navigator.items

    DisposableEffect(oldScreens) {
        onDispose {
            val oldScreenStateKeys = oldScreens.map { "${it.key}:${navigator.key}" }
            val currentScreenStateKeys = navigator.items.map { "${it.key}:${navigator.key}" }
            val unexpectedScreenStateKeys = oldScreenStateKeys.filter {
                it !in currentScreenStateKeys
            }
            unexpectedScreenStateKeysQueue += unexpectedScreenStateKeys
        }
    }

    // Make sure the transition state's target state is always the state of the latest screen
    val transitionState = remember {
        SeekableTransitionState(navigator.current)
    }

    val transition = rememberTransition(transitionState, label = "entry")

    LaunchedEffect(navigator.current) {
        transitionState.animateTo(navigator.current)
    }

    val prevScreen by remember(navigator.current) {
        derivedStateOf {
            if (navigator.items.size < 2) {
                return@derivedStateOf null
            }
            navigator.items[navigator.items.lastIndex - 1]
        }
    }

    var isInPredictiveBack by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    PredictiveBackHandler(
        enabled = prevScreen != null,
        onBack = onBack@{ progress ->
            val prevScreen = prevScreen ?: return@onBack
            if (navigator.current is BaseScreen) {
                val currentScreen = navigator.current as BaseScreen
                if (!currentScreen.canBack()) {
                    return@onBack
                }
                //如果最高级别的弹窗正在展示，则关闭该弹窗
                val lastMaxPriorityDialog =
                    currentScreen.dialogController?.maxPriorityStack?.lastOrNull()
                if (lastMaxPriorityDialog?.isShow == true) {
                    if (lastMaxPriorityDialog.cancelAble) {
                        lastMaxPriorityDialog.dismiss()
                    }
                    return@onBack
                }
                //如果优先级弹窗正在展示，则关闭该弹窗
                val showingPriorityDialog = currentScreen.currentShowingPriorityDialog
                if (showingPriorityDialog?.isShow == true) {
                    if (showingPriorityDialog.cancelAble) {
                        showingPriorityDialog.dismiss()
                    }
                    return@onBack
                }
                //如果普通弹窗正在展示，则关闭该弹窗
                val normalDialog = currentScreen.dialogController?.noPriorityStack?.lastOrNull()
                if (normalDialog?.isShow == true) {
                    if (normalDialog.cancelAble) {
                        normalDialog.dismiss()
                    }
                    return@onBack
                }
            }
            progress
                .filter { backEvent ->
                    swipeSides.contains(backEvent.swipeEdge)
                }.onEach { backEvent ->
                    if (!isInPredictiveBack && transitionState.fraction > 0) return@onEach
                    isInPredictiveBack = true
                    transitionState.seekTo(backEvent.progress, prevScreen)
                }.onCompletion { cause ->
                    if (!isInPredictiveBack) return@onCompletion
                    when (cause) {
                        null -> {
                            navigator.pop()
                            isInPredictiveBack = false
                        }

                        is CancellationException -> {
                            coroutineScope.launch {
                                val mutex = Mutex()
                                animate(
                                    transitionState.fraction,
                                    0f,
                                    animationSpec = defaultPredictiveBackTransition.cancelAnimationSpec
                                ) { value, _ ->
                                    launch {
                                        mutex.withLock {
                                            transitionState.seekTo(value)
                                            if (value == 0f) {
                                                isInPredictiveBack = false
                                                transitionState.snapTo(navigator.current)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            isInPredictiveBack = false
                        }
                    }
                }.collect()
        })

    var currentContentTransform by remember { mutableStateOf<ContentTransform?>(null) }
    transition.AnimatedContent(
        transitionSpec = {
            val transition = when {
                isInPredictiveBack -> initialState.predictiveBackTransition
                    ?: defaultPredictiveBackTransition

                navigator.lastEvent == StackEvent.Pop -> initialState.onDisappearTransition
                    ?: defaultOnScreenDisappearTransition

                else -> targetState.onAppearTransition ?: defaultOnScreenAppearTransition
            }

            ContentTransform(
                targetContentEnter = transition?.enter() ?: EnterTransition.None,
                initialContentExit = transition?.exit() ?: ExitTransition.None,
                targetContentZIndex = transition?.zIndex ?: 0f,
                sizeTransform = transition?.sizeTransform() ?: SizeTransform()
            ).also {
                currentContentTransform = it
            }
        },
        contentAlignment = contentAlignment,
        contentKey = contentKey,
        modifier = modifier
    ) { screen ->
        if (this.transition.targetState == this.transition.currentState) {
            val stateHolder = LocalNavigatorStateHolder.currentOrThrow

            // This updates when the transition is done
            LaunchedEffect(Unit) {
                currentContentTransform?.targetContentZIndex = 0f

                // We perform a check again, we remove all from the unexpected queue that are actually expected
                val currentScreenStateKeys = navigator.items.map { "${it.key}:${navigator.key}" }

                val unexpectedScreenStateKeys = unexpectedScreenStateKeysQueue
                    .filter { it !in currentScreenStateKeys }

                if (unexpectedScreenStateKeys.isNotEmpty()) {

                    for (unexpectedScreenStateKey in unexpectedScreenStateKeys) {
                        ScreenModelStore.dispose(unexpectedScreenStateKey)

                        ScreenDisposableEffectStore.dispose(unexpectedScreenStateKey)

                        stateHolder.removeState(unexpectedScreenStateKey)

                        navigator.disassociateScreenStateKey(unexpectedScreenStateKey)
                    }

                    navigator.clearEvent()
                }

                unexpectedScreenStateKeysQueue = emptySet()
            }
        }

        screen.render {
            var offsetInDp by remember { mutableStateOf(0.dp) }
            var swipeMaxWidth by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                swipeMaxWidth = maxWidth
                Box(modifier = Modifier.onGloballyPositioned {
                    val windowPosition = it.positionInWindow()
                    val xInPx = windowPosition.x
                    offsetInDp = with(density) { xInPx.toDp() }
                }) {
                    // 关键判断：
                    // 1. 如果不在预测性返回中，当前 screen 就是 active
                    // 2. 如果在预测性返回中，只有顶层（正在滑走的）是 active，底层（预览的）不是
                    val isTarget = transition.targetState == screen
                    val isFinished = transition.currentState == transition.targetState
                    val isActive = if (isInPredictiveBack) {
                        // 在滑动时，只有初始页面（还在滑动的那个）保持 active
                        // 或者等滑动彻底结束（transition 已经 snap 到新状态）
                        screen == transition.currentState || (isTarget && isFinished)
                    } else {
                        isTarget
                    }
                    CompositionLocalProvider(LocalScreenActive provides isActive) {
                        content(it)
                    }
                }
            }
            val progress = if (swipeMaxWidth > 0.dp) {
                (offsetInDp / swipeMaxWidth).coerceIn(0f, 1f)
            } else 0f

            val alpha = 0.2f * (1f - progress)
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = -(offsetInDp.toPx())
                }.fillMaxHeight()
                    .width(offsetInDp).background(Color.DarkGray.copy(alpha = alpha))
            )
        }
    }
}

/**
 * Just an utility saver for the screens that should be disposed during a transition.
 */
private fun unexpectedScreenStateKeysQueueSaver(): Saver<MutableState<Set<String>>, List<String>> {
    return Saver(
        save = { it.value.toList() },
        restore = { mutableStateOf(it.toSet()) }
    )
}

internal val LocalScreenActive = staticCompositionLocalOf { true }