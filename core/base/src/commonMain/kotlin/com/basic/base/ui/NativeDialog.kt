package com.basic.base.ui

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.basic.base.Os
import com.basic.base.getPlatform
import com.basic.base.ktx.click
import com.basic.base.local.LocalAppState
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.UIContainer
import com.basic.base.local.appState
import com.benasher44.uuid.uuid4
import io.github.hristogochev.vortex.util.multiplatformName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 原生弹簧
 * 注意：原生弹窗不在单页宿主上，而是单独开的一张页面实现的，纯粹用于简单的对话框一类的业务，时间响应只能用callback回调
 */
abstract class NativeDialog(
    private val alignment: Alignment = Alignment.Center,
    private val cancelAble: Boolean = true,
    internal val shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    private val enter: EnterTransition = fadeIn(animationSpec = tween(200)),
    private val exit: ExitTransition = fadeOut(animationSpec = tween(200))
) {
    internal var isShow by mutableStateOf(true)
    open val key: String = uuid4().toString()
    internal var dialogStateHostKey: String? = null
    private val cleanupActions = mutableListOf<() -> Unit>()

    @Composable
    internal fun Content(
        uiContainer: UIContainer,
        onDismissCall: () -> Unit
    ) {
        if (dialogStateHostKey == null) {
            dialogStateHostKey = "${NativeDialog::class.multiplatformName}:${this::class.multiplatformName}:${key}"
        }
        DesignDensityProvider {
            CompositionLocalProvider(
                LocalUIContainer provides uiContainer,
                LocalAppState provides appState
            ) {
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
                                onDismissCall()
                                cleanUpAutoActions()
                                //回退状态，方便重用
                                isShow = true
                            }
                        }
                    } else {
                        // 动画进行中
                    }
                }
                LaunchedEffect(isShow) {
                    visible.targetState = !visible.targetState
                }
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                val backgroundColor by animateColorAsState(
                    targetValue = if (isShow) shadowColor else Color.Transparent,
                    animationSpec = tween(200)
                )
                Box(
                    modifier = Modifier.run {
                        when (getPlatform().os) {
                            Os.ANDROID -> navigationBarsPadding()
                            Os.IOS -> windowInsetsPadding(WindowInsets(0.dp))
                        }
                    }.fillMaxSize().background(backgroundColor).pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
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
                                Box(
                                    modifier = Modifier.fillMaxSize().clickable(
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
    }

    @Composable
    private fun CreateUIContent(visibleState: MutableTransitionState<Boolean>) {
        AnimatedVisibility(
            enter = enter,
            exit = exit,
            visibleState = visibleState
        ) {
            Box(modifier = Modifier.click {}) {
                CreateUI()
            }
        }
    }

    @Composable
    abstract fun CreateUI()
    fun dismiss() {
        if (isShow) {
            isShow = false
        }
    }

    open fun onShow() {}

    open fun onDismiss() {}

    /**
     * 弹窗关闭时自动清理包裹的对象，主要用于函数参数的自动回收
     */
    protected fun <T : Any> autoClear(initialValue: T? = null): ReadWriteProperty<Any?, T?> {
        return object : ReadWriteProperty<Any?, T?> {
            private var value: T? = initialValue

            init {
                cleanupActions.add {
                    value = null
                }
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): T? = value
            override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T?) {
                value = newValue
            }
        }
    }

    /**
     * 自动清理所有autoClear代理的对象
     */
    internal fun cleanUpAutoActions() {
        cleanupActions.forEach { it.invoke() }
        cleanupActions.clear()
    }

    /**
     * 弹出原生弹窗
     */
    fun show(uiContainer: UIContainer) {
        uiContainer.showNativeDialog(this)
    }
}


internal expect fun UIContainer.showNativeDialog(dialog: NativeDialog)
