package com.basic.base.ktx

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 控制组件是否可见。
 * 当 [visible] 为 false 时，组件不会被放置（不显示，不响应交互），但仍会占用布局空间。
 * 解决了某些环境下 androidx.compose.foundation.layout.visible 无法在 Android 上编译的问题。
 */
@Stable
fun Modifier.visible(visible: Boolean): Modifier = this
    .semantics {
        if (!visible) hideFromAccessibility()
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (visible) {
                placeable.placeRelative(0, 0)
            }
        }
    }

/**
 * 带默认 500ms 防重点击间隔的点击扩展。
 */
@OptIn(ExperimentalTime::class)
fun Modifier.click(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    clickIntervalMillis: Long = 500L,
    hideKeyboard: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = indication
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastClickTime >= clickIntervalMillis) {
            lastClickTime = now
            if (hideKeyboard) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            onClick()
        }
    }
}

/**
 * 支持自定义触发时长的长按扩展。
 *
 * @param enabled 是否启用长按。
 * @param longClickMillis 长按触发时长，默认 500ms。
 * @param hideKeyboard 按下时是否清除焦点并收起键盘。
 * @param onLongClick 长按触发回调。
 */
fun Modifier.longClick(
    enabled: Boolean = true,
    longClickMillis: Long = 500L,
    hideKeyboard: Boolean = true,
    onLongClick: () -> Unit
): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    pointerInput(enabled, longClickMillis, hideKeyboard) {
        if (!enabled) {
            return@pointerInput
        }
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (hideKeyboard) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            val upOrCancellation = withTimeoutOrNull(longClickMillis.coerceAtLeast(0L)) {
                waitForUpOrCancellation()
            }
            if (upOrCancellation == null) {
                currentOnLongClick()
                waitForUpOrCancellation()
            }
        }
    }
}

/**
 * 等待手指抬起或手势取消。
 *
 * @return 手指抬起事件；返回 null 表示手势被取消。
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.waitForUpOrCancellation(): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull()
        if (change == null || change.positionChangeConsumed()) {
            return null
        }
        val upChange = event.changes.firstOrNull { it.changedToUpIgnoreConsumed() }
        if (upChange != null) {
            return upChange
        }
    }
}
