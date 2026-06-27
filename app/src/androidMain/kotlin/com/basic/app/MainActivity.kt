package com.basic.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.basic.base.ApplicationProxyManager
import com.basic.base.base.BaseActivity

/**
 * 由于MainActivity在框架层有依赖，切记不要更改名字和路径
 */
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }
        setContent {
            App(uiContainer = this, permissionController = this)
        }
        ApplicationProxyManager.androidMainActivityOnCreate(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ApplicationProxyManager.androidMainActivityOnNewIntent(intent)
    }
}