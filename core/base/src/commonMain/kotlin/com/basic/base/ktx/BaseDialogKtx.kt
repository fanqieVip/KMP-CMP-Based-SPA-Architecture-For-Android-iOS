package com.basic.base.ktx

import androidx.annotation.MainThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.basic.base.Os
import com.basic.base.base.MainScreenModel
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.getPlatform
import com.basic.base.local.DefaultTraceInfoScope
import com.benasher44.uuid.uuid4
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.ScreenModelStore
import io.github.hristogochev.vortex.model.rememberScreenModel
import io.github.hristogochev.vortex.navigator.LocalScreenStateKey
import io.github.hristogochev.vortex.util.multiplatformName

abstract class Dialog(
    private val alignment: Alignment = Alignment.Center,
    internal val cancelAble: Boolean = true,
    internal val shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    private val enter: EnterTransition = fadeIn(animationSpec = tween(200)),
    private val exit: ExitTransition = fadeOut(animationSpec = tween(200))
) {
    open val key: String = uuid4().toString()
    internal var priorityGroup: String? = null
    internal var isShow by mutableStateOf(true)
    internal var dialogStateHostKey: String? = null

    /**
     * 链路来源
     */
    internal var fromTraceId: String? = null

    @Composable
    internal fun Content(isTop: Boolean) {
        val hostScreenKey =
            LocalScreenStateKey.current ?: "rememberScreenModel called outside of a screen scope"
        if (dialogStateHostKey == null) {
            dialogStateHostKey = "${hostScreenKey}:${this::class.multiplatformName}:${key}"
        }
        CompositionLocalProvider(
            LocalScreenStateKey provides dialogStateHostKey,
            LocalHostScreenStateKey provides hostScreenKey
        ) {
            val dialogController = LocalDialogController.current
            val visible = remember { MutableTransitionState(false) }
            LaunchedEffect(visible.currentState, visible.targetState) {
                if (visible.currentState == visible.targetState) {
                    // 动画结束
                    if (visible.currentState) {
                        //进入动画结束 - 组件已完全显示
                        onShow()
                    } else {
                        //退出动画结束 - 组件已完全隐藏
                        if (!isShow) {
                            onDismiss()
                            dialogController.dismiss(this@Dialog)
                        }
                    }
                } else {
                    // 动画进行中
                }
            }
            LaunchedEffect(isShow) {
                visible.targetState = !visible.targetState
            }
            val backgroundColor by animateColorAsState(
                targetValue = if (isShow) shadowColor else Color.Transparent,
                animationSpec = tween(200)
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    if (isTop) backgroundColor else Color.Transparent
                )
            ) {
                val isIos = getPlatform().os == Os.IOS
                if (!isIos) {
                    Popup(
                        onDismissRequest = {
                            dismiss()
                        },
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = cancelAble,
                            dismissOnClickOutside = false,
                            clippingEnabled = false
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
                            Box(modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (cancelAble) {
                                    dismiss()
                                }
                            })
                            CreateUIContent(visible)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().clickable {
                        if (cancelAble) {
                            dismiss()
                        }
                    })
                    Box(modifier = Modifier.align(alignment)) {
                        CreateUIContent(visible)
                    }
                }

            }
        }
    }

    @Composable
    private fun CreateUIContent(visibleState: MutableTransitionState<Boolean>) {
        AnimatedVisibility(
            enter = enter,
            exit = exit,
            visibleState = visibleState
        ) {
            DefaultTraceInfoScope(fromTraceId, null) {
                CreateUI()
            }
        }
    }

    fun dismiss() {
        if (isShow) {
            isShow = false
        }
    }

    open fun onShow() {}

    open fun onDismiss() {}

    @Composable
    abstract fun CreateUI()
}

internal data class PriorityDialog(
    //优先级  值越小，优先级越高
    val priority: Int,
    val dialog: Dialog
)

class DialogController {
    companion object {
        private const val DEFAULT_PRIORITY_GROUP = "default"
    }

    internal val noPriorityStack by lazy { mutableStateListOf<Dialog>() }
    internal val priorityStack by lazy { mutableStateMapOf<String, SnapshotStateList<PriorityDialog>>() }
    internal val maxPriorityStack by lazy { mutableStateListOf<Dialog>() }
    var currentPriorityGroup by mutableStateOf(DEFAULT_PRIORITY_GROUP)

    /**
     * 弹出普通弹窗
     * @param traceId 链路来源
     */
    @MainThread
    fun showNow(dialog: Dialog, traceId: String? = null) {
        if (noPriorityStack.indexOf(dialog) < 0) {
            dialog.isShow = true
            dialog.fromTraceId = traceId
            noPriorityStack.add(dialog)
        }
    }

    /**
     * 弹出优先级弹窗
     * @param traceId 链路来源
     * @param priority 优先级 值越小，优先级越高
     * @param group 优先级弹窗所属分组
     */
    @MainThread
    fun showPriority(priority: Int, dialog: Dialog, traceId: String? = null, group: String = DEFAULT_PRIORITY_GROUP) {
        dialog.priorityGroup = group
        var groupStack = priorityStack[group]
        if (groupStack == null) {
            groupStack = mutableStateListOf<PriorityDialog>()
            priorityStack[group] = groupStack
        }
        dialog.isShow = true
        dialog.fromTraceId = traceId
        groupStack.add(PriorityDialog(priority, dialog))
    }

    /**
     * 弹出最高优先级弹窗
     * @param traceId 链路来源
     */
    @MainThread
    fun showMaxPriority(dialog: Dialog, traceId: String? = null) {
        if (maxPriorityStack.indexOf(dialog) < 0) {
            dialog.isShow = true
            dialog.fromTraceId = traceId
            maxPriorityStack.add(dialog)
        }
    }

    @MainThread
    internal fun dismiss(dialog: Dialog) {
        if (dialog.priorityGroup == null) {
            if (noPriorityStack.indexOf(dialog) >= 0) {
                noPriorityStack.remove(dialog)
            }
            if (maxPriorityStack.indexOf(dialog) >= 0) {
                maxPriorityStack.remove(dialog)
            }
        } else {
            priorityStack[dialog.priorityGroup]?.let { groupStack ->
                groupStack.find { it.dialog == dialog }?.let {
                    groupStack.remove(it)
                }
            }
        }
        dialog.dialogStateHostKey?.let {
            ScreenModelStore.dispose(it)
        }
    }

    /**
     * 清除所有弹窗
     */
    @MainThread
    fun clearAllStack() {
        clearNormalStack()
        clearAllPriorityStack()
        clearMaxPriorityStack()
    }

    /**
     * 清除所有普通弹窗
     */
    @MainThread
    fun clearNormalStack() {
        noPriorityStack.iterator().let {
            while (it.hasNext()) {
                it.next().run {
                    onDismiss()
                    dialogStateHostKey?.let {
                        ScreenModelStore.dispose(it)
                    }
                }
            }
        }
        noPriorityStack.clear()
    }

    /**
     * 清除所有最高级别弹窗
     */
    @MainThread
    fun clearMaxPriorityStack() {
        maxPriorityStack.iterator().let {
            while (it.hasNext()) {
                it.next().run {
                    onDismiss()
                    dialogStateHostKey?.let {
                        ScreenModelStore.dispose(it)
                    }
                }
            }
        }
        maxPriorityStack.clear()
    }

    /**
     * 清除全部优先级弹窗栈
     */
    @MainThread
    fun clearAllPriorityStack() {
        priorityStack.keys.forEach {
            clearPriorityStack(it)
        }
    }

    /**
     * 清除优先级弹窗栈
     * @param group 指定分组
     */
    @MainThread
    fun clearPriorityStack(group: String) {
        priorityStack[group]?.let {
            it.iterator().let { iterator ->
                while (iterator.hasNext()) {
                    iterator.next().dialog.run {
                        onDismiss()
                        dialogStateHostKey?.let {
                            ScreenModelStore.dispose(it)
                        }
                    }
                }
            }
            it.clear()
        }
        priorityStack.remove(group)
    }
}

val LocalDialogController = staticCompositionLocalOf<DialogController> {
    error("DialogController not provided")
}

val LocalHostScreenStateKey: ProvidableCompositionLocal<String> =
    staticCompositionLocalOf { "" }

/**
 * 用于弹窗和宿主Screen共享ScreenModel
 * 注意：原生弹窗无效
 */
@Composable
inline fun <reified T : ScreenModel> rememberHostScreenModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls () -> T,
): T {
    val hostScreenKey = LocalHostScreenStateKey.current
    return rememberScreenModel(hostScreenKey, tag, factory)
}

/**
 * 用于弹窗和宿主Screen共享MainScreenModel
 * 注意：原生弹窗无效
 */
@Composable
inline fun <reified T : MainScreenModel> rememberHostMainScreenModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    val hostScreenKey = LocalHostScreenStateKey.current
    return rememberMainScreenModel(hostScreenKey, tag, factory)
}