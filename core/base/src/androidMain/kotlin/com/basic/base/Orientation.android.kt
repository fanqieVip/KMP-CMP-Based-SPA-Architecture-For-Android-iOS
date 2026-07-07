package com.basic.base

import android.content.pm.ActivityInfo
import com.basic.base.local.UIContainer

internal actual fun setScreenOrientation(
    uiContainer: UIContainer,
    orientation: ScreenOrientation
) {
    uiContainer.requestedOrientation = when (orientation) {
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientation.FOLLOW_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
