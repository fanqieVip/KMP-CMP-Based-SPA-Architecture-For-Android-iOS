package com.basic.common.di.impl

import com.basic.base.di.service.ApplicationService
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.webview.platform.preloadWebkit
import com.basic.common.Application

/**
 * ApplicationService的SPI实现类
 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate() {
        Application.onCreate()
        //预初始化webkit
        applicationScope.launchScope { preloadWebkit() }
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }

}