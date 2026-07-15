package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.hristogochev.vortex.screen.Screen

expect class UIContainer

/**
 * 关闭当前宿主页
 * @param rootToHome 当最后一张宿主页面调用时，是否模拟home点击（仅安卓有效）
 */
expect fun UIContainer.pop(rootToHome: Boolean = true)
/**
 * 打开原生Screen
 * 启动一个全新的宿主页面来加载Screen,与NativeDialog类似，尽量用在最简单的页面上
 * 注意：由于运行在原生screen上，需要注意navigate导航是独立的，需要应用层特别处理
 */
expect fun UIContainer.push(screen: Screen)
val LocalUIContainer = staticCompositionLocalOf<UIContainer> {
    error("UIContainer not provided")
}