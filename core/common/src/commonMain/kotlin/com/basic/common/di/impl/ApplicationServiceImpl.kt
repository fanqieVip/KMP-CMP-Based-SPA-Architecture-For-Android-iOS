package com.basic.common.di.impl

import com.basic.base.di.service.ApplicationService
import com.basic.common.Application

/**
 * ApplicationService的SPI实现类
 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate() {
        Application.onCreate()
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }

}