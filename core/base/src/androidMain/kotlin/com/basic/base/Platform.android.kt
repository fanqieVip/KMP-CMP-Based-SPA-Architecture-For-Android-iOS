package com.basic.base

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.webkit.WebSettings
import android.widget.FrameLayout
import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.local.UIContainer
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.meituan.android.walle.WalleChannelReader

private val platform by lazy { AndroidPlatform() }
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val os: Os = Os.ANDROID
    override val systemVersion: String = "${Build.VERSION.SDK_INT}"
    override val appName: String
        get() = AppUtils.getAppName()
    override val appVersionName: String
        get() = AppUtils.getAppVersionName()
    override val appVersionCode: String
        get() = "${AppUtils.getAppVersionCode()}"
    override val appPackageName: String
        get() = AppUtils.getAppPackageName()
    override val userAgent: String
        get() = WebSettings.getDefaultUserAgent(Utils.getApp())
    override val brand: String
        get() = Build.BRAND
    override val model: String
        get() = Build.MODEL
}

actual fun getPlatform(): Platform = platform
actual typealias PlatformApp = Application
actual fun getPlatformApp(): PlatformApp = Utils.getApp()
actual typealias PlatformView = View
actual typealias PlatformViewGroup = FrameLayout

actual fun createPlatformViewGroup(uiContainer: UIContainer): PlatformViewGroup = FrameLayout(uiContainer)
actual fun PlatformViewGroup.addView(view: PlatformView) {
    addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
}
actual fun PlatformViewGroup.removeAllView() {
    removeAllViews()
}

actual typealias Bitmap = Bitmap

private val channel by lazy { WalleChannelReader.getChannel(Utils.getApp(), BuildConfig_com_basic_base.DEFAULT_CHANNEL)?: BuildConfig_com_basic_base.DEFAULT_CHANNEL }
actual fun getAppChannel(): String = channel