package com.basic.common.distribution.di.impl

import com.basic.base.di.service.ApplicationService

/**
 * Application生命周期服务实现类 (Play)。
 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate(isMainProcess: Boolean) = Unit
    override fun onBackground() = Unit
    override fun onForeground() = Unit
}
