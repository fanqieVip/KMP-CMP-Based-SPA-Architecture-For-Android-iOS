package com.basic.base.webview.utils

import com.blankj.utilcode.util.Utils
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk

/**
 * x5 SDK初始化
 * Create by Leo at 2025/3/7 17:11
 */
object WebX5Utils {

    fun initX5(domain: String? = null) {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = mapOf(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true, TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE to true)
        QbSdk.initTbsSettings(map)
        QbSdk.initX5Environment(Utils.getApp(), null)
    }
}