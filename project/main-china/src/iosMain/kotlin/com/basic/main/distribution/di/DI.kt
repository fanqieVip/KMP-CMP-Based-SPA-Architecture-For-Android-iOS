/**
 * @Description: main 模块 iOS 发行渠道公共能力的 Koin 注册定义。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 17:02
 */
package com.basic.main.distribution.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.main.distribution.di.impl.ApplicationServiceImpl
import com.basic.main.distribution.di.impl.IosMainDistributionServiceImpl
import com.basic.main.di.service.MainDistributionService
import org.koin.dsl.module

/** main 模块 iOS 发行渠道公共能力的 Koin 模块。 */
val mainIosModule = module {
    registerSPI<ApplicationService> { ApplicationServiceImpl() }
    registerSPI<MainDistributionService> { IosMainDistributionServiceImpl() }
}
