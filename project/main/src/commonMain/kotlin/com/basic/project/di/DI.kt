package com.basic.project.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.common.di.service.ProjectService
import com.basic.project.di.impl.ApplicationServiceImpl
import com.basic.project.di.impl.ProjectServiceImpl
import com.basic.router.generated.mainRouteModule
import org.koin.dsl.module

val projectModule = module {
    includes(mainRouteModule)
    registerSPI<ApplicationService>{ ApplicationServiceImpl() }
    registerSPI<ProjectService>{ ProjectServiceImpl() }
}
