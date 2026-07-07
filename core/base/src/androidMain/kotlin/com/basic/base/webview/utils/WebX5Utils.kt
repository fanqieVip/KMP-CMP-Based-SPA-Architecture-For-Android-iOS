package com.basic.base.webview.utils

import android.view.View
import com.blankj.utilcode.util.Utils
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.WebView

/**
 * x5 SDK初始化
 * Create by Leo at 2025/3/7 17:11
 */
object WebX5Utils {

    fun initX5(domain: String? = null) {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = mapOf(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true, TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE to true)
        QbSdk.initTbsSettings(map)
        QbSdk.initX5Environment(Utils.getApp(), object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * @param isX5 是否使用X5内核
             */
            override fun onViewInitFinished(isX5: Boolean) {
                domain?.let {
                    WebView(Utils.getApp()).apply {
                        settings.let {
                            it.domStorageEnabled = true
                            it.javaScriptEnabled = true
                        }
                        visibility = View.GONE
                    }.loadUrl(domain)
                }
            }
        })
    }
}