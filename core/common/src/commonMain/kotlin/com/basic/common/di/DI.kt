package com.basic.common.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.di.service.UIConfigService
import com.basic.base.spi.registerSPI
import com.basic.common.di.impl.ApplicationServiceImpl
import com.basic.common.di.impl.UIConfigServiceImpl
import com.basic.router.generated.commonRouteModule
import org.koin.dsl.module

val commonModule = module {
    includes(commonRouteModule)
    registerSPI<ApplicationService>{ ApplicationServiceImpl() }
    registerSPI<UIConfigService>{ UIConfigServiceImpl() }
}