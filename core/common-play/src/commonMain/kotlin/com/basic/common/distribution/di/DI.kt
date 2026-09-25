package com.basic.common.distribution.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.common.distribution.di.impl.ApplicationServiceImpl
import com.basic.common.distribution.di.impl.commonDistributionServiceImpl
import com.basic.common.di.service.CommonDistributionService
import org.koin.dsl.module

val commonDistributionModule = module {
    registerSPI<ApplicationService> { ApplicationServiceImpl() }
    registerSPI<CommonDistributionService> { commonDistributionServiceImpl }
}
