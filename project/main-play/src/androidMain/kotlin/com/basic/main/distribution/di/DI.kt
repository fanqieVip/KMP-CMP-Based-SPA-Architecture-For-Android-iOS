/**
 * @Description: main 模块 Google Play 发行渠道公共能力的 Koin 注册定义。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 17:02
 */
package com.basic.main.distribution.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.main.MainDistributionProvider
import com.basic.main.distribution.PlayMainDistributionProvider
import com.basic.main.distribution.di.impl.ApplicationServiceImpl
import org.koin.dsl.module

/** main 模块 Google Play 发行渠道公共能力的 Koin 模块。 */
val mainDistributionModule = module {
    registerSPI<ApplicationService> { ApplicationServiceImpl() }
    registerSPI<MainDistributionProvider> { PlayMainDistributionProvider() }
}
