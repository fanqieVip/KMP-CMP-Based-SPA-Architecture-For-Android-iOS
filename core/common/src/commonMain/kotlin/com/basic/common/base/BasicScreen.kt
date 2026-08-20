package com.basic.common.base

import androidx.compose.ui.graphics.Color
import com.basic.base.ScreenOrientation
import com.basic.base.base.BaseScreen

/**
 * 业务 Screen 基类，预设了默认的状态栏和背景色
 */
abstract class BasicScreen: BaseScreen() {
    override val orientation: ScreenOrientation = ScreenOrientation.AUTO
    override val backgroundColor: Color = Color.White
}