package com.basic.project.di.impl

import com.basic.base.di.service.ApplicationService
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.webview.platform.preloadWebkit

class ApplicationServiceImpl : ApplicationService {
    override fun onCreate() {
        applicationScope.launchScope {
            preloadWebkit("https://baidu.com")
        }
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }

}