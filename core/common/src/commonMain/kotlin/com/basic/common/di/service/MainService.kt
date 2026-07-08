package com.basic.common.di.service

/**
 * project模块spi协议
 */
interface MainService {
    /**
     * 测试协议示例
     * @param text 文本内容
     */
    fun sayHello(text: String)
}