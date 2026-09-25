/**
 * @Description: China 公共能力渠道模块的应用生命周期 SPI 空实现。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 20:17
 */
package com.basic.common.distribution.di.impl

import com.basic.base.di.service.ApplicationService

/** China 公共能力渠道模块的应用生命周期服务。 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate(isMainProcess: Boolean) = Unit

    override fun onBackground() = Unit

    override fun onForeground() = Unit
}
