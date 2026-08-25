package com.basic.main.di.impl

import com.basic.base.di.service.ApplicationService

/**
 * application模块spi实现
 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate(isMainProcess: Boolean) {
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }

}
