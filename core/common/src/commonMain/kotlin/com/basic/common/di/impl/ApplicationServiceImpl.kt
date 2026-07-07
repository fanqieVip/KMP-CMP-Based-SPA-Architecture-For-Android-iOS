package com.basic.common.di.impl

import com.basic.base.di.service.ApplicationService
import com.basic.common.Application

class ApplicationServiceImpl : ApplicationService {
    override fun onCreate() {
        Application.onCreate()
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }

}