package com.basic.base.utils

import android.app.Activity
import android.content.res.Configuration
import com.basic.base.local.WindowOrientation
import com.basic.base.local.appState

/**
 * @Description: 读取 Android 窗口方向并同步到 AppState。
 * @Author:         范俊
 * @CreateDate:     2026/08/24 09:28
 */
internal object AndroidScreenStateHelper {

    /**
     * 根据 Activity 当前配置同步窗口方向。
     *
     * @param activity 当前宿主 Activity。
     */
    fun syncWindowOrientation(activity: Activity) {
        syncWindowOrientation(activity.resources.configuration)
    }

    /**
     * 根据 Android Configuration 同步窗口方向。
     *
     * @param configuration 最新系统配置。
     */
    fun syncWindowOrientation(configuration: Configuration) {
        appState.updateWindowOrientation(
            when (configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> WindowOrientation.PORTRAIT
                Configuration.ORIENTATION_LANDSCAPE -> WindowOrientation.LANDSCAPE
                else -> WindowOrientation.UNKNOWN
            }
        )
    }
}
