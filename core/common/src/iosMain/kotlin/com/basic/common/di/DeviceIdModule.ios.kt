/**
 * @Description: iOS 设备标识提供者的 SPI 注册模块。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 16:06
 */
package com.basic.common.di

import com.basic.base.spi.registerSPI
import com.basic.common.di.impl.IosCommonDistributionServiceImpl
import com.basic.common.di.service.CommonDistributionService
import org.koin.dsl.module

/** iOS 发行渠道公共能力的 Koin 模块。 */
val commonIosModule = module {
    registerSPI<CommonDistributionService> { IosCommonDistributionServiceImpl() }
}
