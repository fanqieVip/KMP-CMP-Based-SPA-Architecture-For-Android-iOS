/**
 * @Description: China 公共能力模块的 Koin 注册定义。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 16:06
 */
package com.basic.common.distribution.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.common.CommonDistributionProvider
import com.basic.common.distribution.ChinaCommonDistributionProvider
import com.basic.common.distribution.di.impl.ApplicationServiceImpl
import org.koin.dsl.module

/** China 公共能力模块的 Koin 注册定义。 */
val commonDistributionModule = module {
    registerSPI<ApplicationService> { ApplicationServiceImpl() }
    registerSPI<CommonDistributionProvider> { ChinaCommonDistributionProvider() }
}