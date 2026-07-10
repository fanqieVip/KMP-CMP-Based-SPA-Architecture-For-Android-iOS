package com.basic.base.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import com.basic.base.base.BaseScreen.Companion.LifecycleVisibleEffect
import com.basic.base.ktx.PagingControl
import com.basic.base.ktx.initPagingControl
import com.basic.base.local.LocalContext
import com.basic.base.local.ScreenContext
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.rememberScreenModel
import io.github.hristogochev.vortex.model.screenModelScope
import io.github.hristogochev.vortex.navigator.LocalScreenStateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


enum class ScreenLifecycle {
    Visible, Invisible, Destroyed
}

/**
 * ScreenModel基类
 */
abstract class BaseScreenModel : ScreenModel {
    internal val _hasInit = MutableStateFlow(false)
    private val _screenLifecycle = MutableStateFlow<ScreenLifecycle?>(null)
    val screenLifecycle: StateFlow<ScreenLifecycle?> get() = _screenLifecycle

    /**
     * 生命周期只会调用一次，用于替换init方法
     */
    abstract fun onInit(context: ScreenContext)

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
}

/**
 * 加载ScreenModel
 */
@Composable
inline fun <reified T : BaseScreenModel> rememberBaseScreenModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    val screenStateKey = LocalScreenStateKey.current ?: "rememberScreenModel called outside of a screen scope"
    return rememberBaseScreenModel(screenStateKey, tag, factory)
}

@PublishedApi
@Composable
internal inline fun <reified T : BaseScreenModel> rememberBaseScreenModel(
    holderKey: String,
    tag: String?,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    return rememberScreenModel(holderKey, tag, factory).apply {
        val context = LocalContext.current
        LifecycleVisibleEffect {
            if (it) {
                uiVisible(context)
            } else {
                uiInvisible(context)
            }
        }
    }
}

