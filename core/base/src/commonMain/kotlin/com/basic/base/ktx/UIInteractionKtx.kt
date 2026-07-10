package com.basic.base.ktx

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.rememberScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext


@Composable
fun UIInteraction(
    state: InteractionState,
    onLoading: @Composable BoxScope.(modifier: Modifier, text: String?) -> Unit,
    onError: @Composable BoxScope.(modifier: Modifier, code: Int, error: String?) -> Unit,
    onEmpty: @Composable BoxScope.(modifier: Modifier) -> Unit,
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    CompositionLocalProvider(
        LocalInteractionState provides state
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(Modifier.fillMaxSize())
            when (val uiStatus = state.uiStatus.collectAsState(null).value) {
                is ScreenUIStatus.Empty -> onEmpty(
                    Modifier.fillMaxSize().clickable(true, interactionSource = null) {})

                is ScreenUIStatus.Error -> onError(
                    Modifier.fillMaxSize().clickable(true, interactionSource = null) {},
                    uiStatus.code ?: -1,
                    uiStatus.info
                )

                is ScreenUIStatus.Loading -> onLoading(
                    Modifier.fillMaxSize().clickable(true, interactionSource = null) {},
                    uiStatus.info
                )

                else -> {}
            }
        }
    }
}

sealed class ScreenUIStatus(
    val code: Int?,
    val info: String?
) {
    object Success : ScreenUIStatus(null, null)
    object Empty : ScreenUIStatus(null, null)
    class Error(code: Int?, error: String?) : ScreenUIStatus(code, error)
    class Loading(info: String?) : ScreenUIStatus(null, info)
}

class InteractionState {
    private val _uiStatus: MutableStateFlow<ScreenUIStatus?> =
        MutableStateFlow<ScreenUIStatus?>(null)

    /**
     * 当前的UI交互状态
     */
    val uiStatus: StateFlow<ScreenUIStatus?> get() = _uiStatus

    /**
     * 加载中
     */
    suspend fun uiLoading(text: String?) {
        withContext(Dispatchers.Main) {
            _uiStatus.value = ScreenUIStatus.Loading(text)
        }
    }

    /**
     * 加载成功后
     * @param empty 数据是否为空
     */
    suspend fun uiSuccess(empty: Boolean = false) {
        withContext(Dispatchers.Main) {
            if (empty) {
                _uiStatus.value = ScreenUIStatus.Empty
            } else {
                _uiStatus.value = ScreenUIStatus.Success
            }

        }
    }

    /**
     * 加载失败
     * @param code 错误码
     * @param error 错误信息
     */
    suspend fun uiError(code: Int?, error: String?) {
        withContext(Dispatchers.Main) {
            _uiStatus.value = ScreenUIStatus.Error(code, error)
        }
    }
}

/**
 * 主交互状态
 */
val LocalInteractionState = staticCompositionLocalOf<InteractionState> {
    error("InteractionState not provided")
}

/**
 * 主交互状态
 */
@Composable
fun rememberInteractionState(tag: String? = null): InteractionState {
    return rememberScreenModel(tag) { InteractionStateScreenModel() }.interactionState
}

private class InteractionStateScreenModel : ScreenModel {
    val interactionState: InteractionState = InteractionState()
}