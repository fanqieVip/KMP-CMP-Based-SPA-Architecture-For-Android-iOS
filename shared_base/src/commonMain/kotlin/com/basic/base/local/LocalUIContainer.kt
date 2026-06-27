package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf

expect class UIContainer
expect fun UIContainer.pop()

val LocalUIContainer = staticCompositionLocalOf<UIContainer> {
    error("UIContainer not provided")
}