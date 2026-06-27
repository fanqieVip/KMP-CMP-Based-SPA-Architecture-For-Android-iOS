package com.basic.common.base

import androidx.compose.ui.graphics.Color
import com.basic.base.ScreenOrientation
import com.basic.base.base.BaseScreen

abstract class BasicScreen: BaseScreen() {
    override val orientation: ScreenOrientation = ScreenOrientation.PORTRAIT
    override val backgroundColor: Color = Color.White
}