package com.basic.common.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.di.service.ToastService
import com.basic.base.spi.registerSPI
import com.basic.common.di.impl.ApplicationServiceImpl
import com.basic.common.di.impl.ToastServiceImpl
import org.koin.dsl.module

val commonModule = module {
    registerSPI<ApplicationService>{ ApplicationServiceImpl() }
    registerSPI<ToastService>{ ToastServiceImpl() }
}