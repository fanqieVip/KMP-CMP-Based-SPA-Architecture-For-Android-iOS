/**
 * @Description: 定义公共模块按发行渠道裁剪的基础能力协议。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 23:06
 */
package com.basic.common.di.service

import com.basic.common.beans.DeviceId

/**
 * 用于按发行渠道裁剪包体的公共基础能力协议。
 *
 * 当前协议承载设备标识获取；后续仅将确有发行渠道差异的基础能力追加到此处，
 * 避免公共模块直接依赖 China 或 Google Play 专属实现。
 */
interface CommonDistributionService {
    /** @return 当前发行渠道可用的设备标识信息。 */
    suspend fun getDeviceId(): DeviceId
}
