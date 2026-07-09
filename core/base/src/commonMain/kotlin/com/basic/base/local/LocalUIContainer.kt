package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.basic.base.base.BaseScreen

expect class UIContainer
expect fun UIContainer.pop()
/**
 * 打开原生Screen
 * 启动一个全新的宿主页面来加载Screen,与NativeDialog类似，尽量用在最简单的页面上
 * 注意：由于运行在原生screen上，需要注意navigate导航是独立的，需要应用层特别处理
 */
expect fun UIContainer.push(screen: () -> BaseScreen)
val LocalUIContainer = staticCompositionLocalOf<UIContainer> {
    error("UIContainer not provided")
}