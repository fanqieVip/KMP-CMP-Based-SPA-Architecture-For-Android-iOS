package com.basic.base.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.basic.base.ktx.DialogController
import com.basic.base.ktx.LocalDialogController
import com.basic.base.ktx.LocalPageLifecycleVisible
import com.basic.base.ktx.PagingControl
import com.basic.base.ktx.initPagingControl
import com.basic.base.local.LocalContext
import com.basic.base.local.ScreenContext
import com.basic.base.vortex.LocalScreenActive
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.rememberScreenModel
import io.github.hristogochev.vortex.model.screenModelScope
import io.github.hristogochev.vortex.navigator.LocalScreenStateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext


enum class ScreenLifecycle {
    Visible, Invisible, Destroyed
}

/**
 * 主Model，一个Screen原则上只能有唯一一个MainScreenModel，除非是内嵌页需要独立维护生命周期的Compose组件也可见创建一个唯一的MainScreenModel
 */
abstract class MainScreenModel : ScreenModel {
    internal val _hasInit = MutableStateFlow(false)
    private val _screenLifecycle = MutableStateFlow<ScreenLifecycle?>(null)
    val screenLifecycle: StateFlow<ScreenLifecycle?> get() = _screenLifecycle
    private val _screenUIStatus = MutableStateFlow<ScreenUIStatus?>(null)
    val screenUIStatus: StateFlow<ScreenUIStatus?> get() = _screenUIStatus
    private val _screenUIPopLoading = MutableSharedFlow<Boolean>()
    internal var popLoadingText: String? = null
    val screenUIPopLoading: Flow<Boolean> get() = _screenUIPopLoading

    /**
     * 生命周期只会调用一次，用于替换init方法
     */
    abstract fun onInit(context: ScreenContext)

    /**
     * 生命周期可能多次调用，初始化后立即调用一次，主交互空页面，错误页面也会调用
     * 调用顺序 onInit -> onLoad
     */
    abstract fun onLoad(context: ScreenContext)

    final override fun onDispose() {
        super.onDispose()
        onDestroyed()
    }

    open fun onVisible(context: ScreenContext) {
    }

    open fun onInvisible(context: ScreenContext) {
    }

    open fun onDestroyed() {
        _screenLifecycle.value = ScreenLifecycle.Destroyed
    }

    @PublishedApi
    internal fun uiVisible(context: ScreenContext) {
        if (_screenLifecycle.value != ScreenLifecycle.Visible) {
            if (!_hasInit.value) {
                onInit(context)
                onLoad(context)
                _hasInit.value = true
                if (this is PagingControl) {
                    initPagingControl(screenModelScope)
                }
            }
            _screenLifecycle.value = ScreenLifecycle.Visible
            onVisible(context)
        }
    }

    @PublishedApi
    internal fun uiInvisible(context: ScreenContext) {
        if (_screenLifecycle.value != ScreenLifecycle.Invisible) {
            _screenLifecycle.value = ScreenLifecycle.Invisible
            onInvisible(context)
        }
    }

    suspend fun uiLoading(text: String?) {
        withContext(Dispatchers.Main) {
            _screenUIStatus.value = ScreenUIStatus.LoadingUI(text)
        }
    }

    /**
     * 加载成功后，数据是否为空
     */
    suspend fun uiSuccess(empty: Boolean = false) {
        withContext(Dispatchers.Main) {
            if (empty) {
                _screenUIStatus.value = ScreenUIStatus.EmptyUI()
            } else {
                _screenUIStatus.value = ScreenUIStatus.SuccessUI()
            }

        }
    }

    suspend fun uiError(code: Int, error: String?) {
        withContext(Dispatchers.Main) {
            _screenUIStatus.value = ScreenUIStatus.ErrorUI(code, error)
        }
    }

    suspend fun showPopLoading(info: String?) {
        withContext(Dispatchers.Main) {
            popLoadingText = info
            _screenUIPopLoading.emit(true)
        }
    }

    suspend fun dismissPopLoading() {
        withContext(Dispatchers.Main) {
            popLoadingText = null
            _screenUIPopLoading.emit(false)
        }
    }
}

/**
 * 主Model，一个Screen原则上只能有唯一一个MainScreenModel，除非是内嵌页需要独立维护生命周期的Compose组件也可见创建一个唯一的MainScreenModel
 */
@Composable
inline fun <reified T : MainScreenModel> rememberMainScreenModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    val screenStateKey =
        LocalScreenStateKey.current ?: "rememberScreenModel called outside of a screen scope"
    return rememberMainScreenModel(screenStateKey, tag, factory)
}

@PublishedApi
@Composable
internal inline fun <reified T : MainScreenModel> rememberMainScreenModel(
    holderKey: String,
    tag: String?,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    return rememberScreenModel(holderKey, tag, factory).also {
        val context = LocalContext.current
        AutoUpdateVisibleState(onVisible = {
            it.uiVisible(context)
        }, onInvisible = {
            it.uiInvisible(context)
        })
    }
}

@Composable
fun AutoUpdateVisibleState(onVisible: () -> Unit, onInvisible: () -> Unit) {
    val isActive = LocalScreenActive.current && LocalPageLifecycleVisible.current
    LifecycleResumeEffect(LocalScreenStateKey.current, isActive) {
        if (isActive) {
            onVisible()
        }
        onPauseOrDispose {
            if (isActive) {
                onInvisible()
            }
        }
    }
}

@Composable
fun UIInteraction(
    screenModel: MainScreenModel,
    onLoading: @Composable BoxScope.(modifier: Modifier, text: String?) -> Unit,
    onPopLoading: (dialogController: DialogController, isShow: Boolean, text: String?) -> Unit,
    onError: @Composable BoxScope.(modifier: Modifier, code: Int, error: String?) -> Unit,
    onEmpty: @Composable BoxScope.(modifier: Modifier) -> Unit,
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content(Modifier.fillMaxSize())
        val uiStatus = screenModel.screenUIStatus.collectAsState(null).value
        when (uiStatus?.uiStatus) {
            ScreenUIStatus.UIStatus.EMPTY -> onEmpty(
                Modifier.fillMaxSize().clickable(true, interactionSource = null) {})

            ScreenUIStatus.UIStatus.ERROR -> onError(
                Modifier.fillMaxSize().clickable(true, interactionSource = null) {},
                uiStatus.code ?: -1,
                uiStatus.info
            )

            ScreenUIStatus.UIStatus.LOADING -> onLoading(
                Modifier.fillMaxSize().clickable(true, interactionSource = null) {},
                uiStatus.info
            )

            else -> {}
        }
        val dialogController = LocalDialogController.current
        LaunchedEffect(Unit) {
            screenModel.screenUIPopLoading.collectLatest {
                onPopLoading(dialogController, it, screenModel.popLoadingText)
            }
        }
    }
}

sealed class ScreenUIStatus(
    val uiStatus: UIStatus,
    val code: Int?,
    val info: String?
) {
    enum class UIStatus {
        SUCCESS, ERROR, LOADING, EMPTY
    }

    class SuccessUI : ScreenUIStatus(UIStatus.SUCCESS, null, null)
    class EmptyUI : ScreenUIStatus(UIStatus.EMPTY, null, null)
    class ErrorUI(code: Int, error: String?) : ScreenUIStatus(UIStatus.ERROR, code, error)
    class LoadingUI(info: String?) : ScreenUIStatus(UIStatus.LOADING, null, info)
}


