package com.basic.base.ktx

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
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
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
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
            onClick()
        }
    }
}
