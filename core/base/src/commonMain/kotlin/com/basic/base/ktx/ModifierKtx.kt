package com.basic.base.ktx

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics

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
