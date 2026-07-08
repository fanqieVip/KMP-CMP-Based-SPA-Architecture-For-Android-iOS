package com.basic.main.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.common.di.service.MainService
import com.basic.main.di.impl.ApplicationServiceImpl
import com.basic.main.di.impl.MainServiceImpl
import com.basic.router.generated.mainRouteModule
import org.koin.dsl.module

val mainModule = module {
    includes(mainRouteModule)
    registerSPI<ApplicationService>{ ApplicationServiceImpl() }
    registerSPI<MainService>{ MainServiceImpl() }
}
