package com.basic.base.local

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.basic.base.ScreenOrientation
import com.basic.base.StatusBar
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val appState = AppState()

val LocalAppState = staticCompositionLocalOf<AppState> {
    error("AppState not provided")
}

data class AppState(
    private val _appIsForeground: MutableStateFlow<Boolean> = MutableStateFlow(true),
    private var _toastCountdown: MutableState<Long> = mutableStateOf(0L),
    private var _toastText: MutableState<String> = mutableStateOf("0L"),
    private var _toastUpdateTime: MutableState<Long> = mutableStateOf(0L),
    private val _statusBarTextIsDark: MutableSharedFlow<Boolean> = MutableSharedFlow(),
    private val _screenOrientation: MutableState<ScreenOrientation> = mutableStateOf(ScreenOrientation.PORTRAIT),
) {
    val appIsForeground: StateFlow<Boolean> get() = _appIsForeground
    internal val toastCountdown: State<Long> get() = _toastCountdown
    internal val toastText: State<String> get() = _toastText
    internal val toastUpdateTime: State<Long> get() = _toastUpdateTime
    internal val screenOrientation: State<ScreenOrientation> get() = _screenOrientation
    internal fun updateAppIsForeground(isForeground: Boolean) {
        applicationScope.launch {
            _appIsForeground.emit(isForeground)
        }
    }

    @OptIn(ExperimentalTime::class)
    internal fun updateToast(text: String?, duration: Long) {
        _toastCountdown.value = duration
        _toastText.value = text ?: ""
        _toastUpdateTime.value = Clock.System.now().epochSeconds
    }

    /**
     * 设置状态栏的图标颜色
     * @param isDark  true=深色文字（适合浅色背景），false=浅色文字（适合深色背景）
     */
    fun statusBarTextIsDark(isDark: Boolean) {
        applicationScope.launch {
            _statusBarTextIsDark.emit(isDark)
        }
    }

    /**
     * 设置屏幕方向策略
     */
    fun setScreenOrientation(screenOrientation: ScreenOrientation) {
        _screenOrientation.value = screenOrientation
    }

    init {
        applicationScope.launchScope {
            _statusBarTextIsDark.conflate().debounce(50).collectLatest {
                withContext(Dispatchers.Main) {
                    StatusBar.setStatusBarTextDark(it)
                }
            }
        }
    }
}