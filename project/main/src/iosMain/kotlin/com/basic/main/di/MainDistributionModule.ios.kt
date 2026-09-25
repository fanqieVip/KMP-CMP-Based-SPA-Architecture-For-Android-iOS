/**
 * @Description: main 模块 iOS 发行渠道公共能力的 Koin 注册定义。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 17:02
 */
package com.basic.main.di

import com.basic.base.spi.registerSPI
import com.basic.main.MainDistributionProvider
import com.basic.main.distribution.IosMainDistributionProvider
import org.koin.dsl.module

/** main 模块 iOS 发行渠道公共能力的 Koin 模块。 */
val mainIosModule = module {
    registerSPI<MainDistributionProvider> { IosMainDistributionProvider() }
}
