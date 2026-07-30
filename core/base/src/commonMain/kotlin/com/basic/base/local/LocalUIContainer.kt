package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.hristogochev.vortex.screen.Screen

expect class UIContainer

/**
 * 关闭当前宿主页
 * @param rootToHome 当最后一张宿主页面调用时，是否模拟home点击（仅安卓有效）
 * @param useAnimation 是否使用出场动画
 */
expect fun UIContainer.pop(rootToHome: Boolean = true, useAnimation: Boolean = true)
/**
 * 打开原生Screen
 * 启动一个全新的宿主页面来加载Screen,与NativeDialog类似，尽量用在最简单的页面上
 * 注意：由于运行在原生screen上，需要注意navigate导航是独立的，需要应用层特别处理
 * @param screen 需要在原生宿主中打开的 Screen
 * @param useAnimation 是否使用进场动画
 * @param disablePhysicalBack 是否禁用物理返回
 */
expect fun UIContainer.push(
    screen: Screen,
    useAnimation: Boolean = true,
    disablePhysicalBack: Boolean = false
)
val LocalUIContainer = staticCompositionLocalOf<UIContainer> {
    error("UIContainer not provided")
}
