package com.basic.project.di.impl

import com.basic.base.utils.toastShort
import com.basic.common.di.service.MainService

/**
 * project模块spi实现
 */
class MainServiceImpl : MainService {
    override fun sayHello(text: String) {
        toastShort(text)
    }
}