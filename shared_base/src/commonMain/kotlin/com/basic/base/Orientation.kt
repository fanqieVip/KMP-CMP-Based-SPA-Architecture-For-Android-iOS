package com.basic.base

import com.basic.base.local.UIContainer

enum class ScreenOrientation {
    //强制横屏
    LANDSCAPE,
    //强制竖屏
    PORTRAIT,
    //跟随传感器
    FOLLOW_SENSOR,
    //自动
    AUTO
}

/**
 * 设置横竖屏模式
 */
internal expect fun setScreenOrientation(uiContainer: UIContainer, orientation: ScreenOrientation)