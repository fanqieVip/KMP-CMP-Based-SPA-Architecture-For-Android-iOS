package com.basic.base

import com.basic.base.utils.ActivityStackManager
import com.yzq.immersion.setStatusBarDarkFont

actual object StatusBar {
    actual fun setStatusBarTextDark(isDark: Boolean) {
        ActivityStackManager.getCurrentActivity()?.setStatusBarDarkFont(isDark)
    }
}