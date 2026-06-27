package com.basic.base.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.basic.base.ScreenOrientation
import com.basic.base.ktx.CallbackFunctionModel
import com.basic.base.ktx.Dialog
import com.basic.base.ktx.DialogController
import com.basic.base.ktx.LocalDialogController
import com.basic.base.local.DefaultTraceInfoScope
import com.basic.base.local.LocalAppState
import com.basic.base.local.LocalContext
import com.basic.base.local.LocalPermissionController
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.ScreenContext
import com.basic.base.local.pop
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.rememberScreenModel
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.screen.Screen
import io.github.hristogochev.vortex.screen.ScreenDisposableEffect
import io.github.hristogochev.vortex.screen.uniqueScreenKey
import io.github.hristogochev.vortex.util.BackHandler
import io.github.hristogochev.vortex.util.currentOrThrow
import kotlin.jvm.Transient

/**
 * @Description:    Screen基类，空实现
 * @Author:         fanj
 * @CreateDate:     2026/1/12 23:42
 * @Version:
 */
abstract class BaseScreen : Screen {
    companion object {
        /**
         * 添加返回拦截
         * 1.不作用于Dialog
         * 2.仅在生命周期可见时生效
         * @param key 处理器key
         * @param handler 拦截方法 true：通过  false: 拦截
         */
        @Composable
        fun CanBackHandler(key: String, handler: () -> Boolean) {
            (LocalNavigator.currentOrThrow.current as? BaseScreen)?.let { currentScreen ->
                AutoUpdateVisibleState(
                    onVisible = {
                        currentScreen.canBackHandlers[key] = handler
                    },
                    onInvisible = {
                        currentScreen.canBackHandlers.remove(key)
                    }
                )
            }
        }
    }

    //voyager混淆配置有问题，必须给key赋值，否则混淆后会丢失，导致数据存储异常
    override val key: String = uniqueScreenKey()

    /**
     * 状态栏的图标颜色
     * true=深色文字（适合浅色背景），false=浅色文字（适合深色背景）
     */
    open val statusBarTextIsDark: Boolean = true

    /**
     * 默认背景颜色
     */
    open val backgroundColor: Color = Color.Transparent

    /**
     * 屏幕方向策略
     */
    open val orientation: ScreenOrientation = ScreenOrientation.AUTO

    /**
     * 去掉该api，很不灵活，改由自定义控制
     */
    final override val canPop: Boolean
        get() = true

    /**
     * 不参与状态保存，避免序列化崩溃
     * 是否允许返回
     */
    @Transient
    private val canBackHandlers = hashMapOf<String, () -> Boolean>()

    /**
     * 弹窗控制器
     */
    @Transient
    internal var dialogController: DialogController? = null

    /**
     * 正在展示中的优先级弹窗
     */
    @Transient
    internal var currentShowingPriorityDialog: Dialog? = null

    /**
     * 链路来源
     */
    internal var fromTraceId: String? = null

    /**
     * 触发返回动作
     * 执行所有backHandler拦截，只要有一个拦截都返回false
     * @return true：通过  false：拦截
     */
    open fun canBack(): Boolean {
        if (canBackHandlers.isEmpty()) {
            return true
        }
        var isCanBack = true
        canBackHandlers.values.forEach {
            if (!it.invoke()) {
                isCanBack = false
            }
        }
        return isCanBack
    }

    @Composable
    final override fun Content() {
        ScreenDisposableEffect {
            onDispose {
                canBackHandlers.clear()
                dialogController = null
                currentShowingPriorityDialog = null
            }
        }
        val uiController = LocalUIContainer.current
        val navigator = LocalNavigator.currentOrThrow
        BackHandler(navigator.size <= 1) {
            if (canBack()) {
                uiController.pop()
            }
        }
        val dialogProviderModel = rememberScreenModel { DialogStackModel() }
        LaunchedEffect(dialogProviderModel) {
            dialogController = dialogProviderModel.dialogController
        }
        CompositionLocalProvider(
            LocalDialogController provides dialogProviderModel.dialogController,
            LocalContext provides ScreenContext(
                dialogController = dialogProviderModel.dialogController,
                navigatorController = LocalNavigator.currentOrThrow,
                appState = LocalAppState.current,
                permissionController = LocalPermissionController.current,
                uiContainer = LocalUIContainer.current
            )
        ) {
            rememberMainScreenModel {
                ScreenProviderModel(statusBarTextIsDark = statusBarTextIsDark, orientation = orientation)
            }
            rememberScreenModel {
                CallbackFunctionModel(key)
            }
            Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
                DefaultTraceInfoScope(fromTraceId, null){
                    CreateUI()
                }
                DealDialog()
            }
        }
    }

    @Composable
    abstract fun CreateUI()

    @Composable
    private fun DealDialog() {
        val dialogController = LocalDialogController.current
        val normalDialogSize = dialogController.noPriorityStack.size
        if (normalDialogSize > 0) {
            dialogController.noPriorityStack.forEachIndexed { index, dialog ->
                dialog.Content(index == normalDialogSize - 1)
            }
        }
        val currentPriorityDialog = remember {
            derivedStateOf {
                dialogController.priorityStack[dialogController.currentPriorityGroup]?.let { priorityDialogGroup ->
                    val priorityDialogSize = priorityDialogGroup.size
                    if (priorityDialogSize > 0) {
                        priorityDialogGroup.minBy { it.priority }
                    } else {
                        null
                    }
                }
            }
        }
        if (currentPriorityDialog.value != null) {
            currentShowingPriorityDialog = currentPriorityDialog.value?.dialog
            currentPriorityDialog.value?.dialog?.Content(true)
        }
        val maxPriorityDialogSize = dialogController.maxPriorityStack.size
        if (maxPriorityDialogSize > 0) {
            dialogController.maxPriorityStack.forEach { dialog ->
                dialog.Content(true)
            }
        }
    }
}

internal class DialogStackModel : ScreenModel {
    /**
     * 弹窗栈
     */
    val dialogController = DialogController()
}

internal class ScreenProviderModel(private val statusBarTextIsDark: Boolean, private val orientation: ScreenOrientation) : MainScreenModel() {
    override fun onVisible(context: ScreenContext) {
        super.onVisible(context)
        context.appState.statusBarTextIsDark(statusBarTextIsDark)
        context.appState.setScreenOrientation(orientation)
    }

    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}