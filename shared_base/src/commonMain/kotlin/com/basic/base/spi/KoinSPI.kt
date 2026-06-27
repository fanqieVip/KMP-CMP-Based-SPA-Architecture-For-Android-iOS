package com.basic.base.spi

import org.koin.core.module.Module

/**
 * Koin SPI模块扩展
 */
inline fun <reified T : Any> Module.registerSPI(crossinline impl: () -> T) {
    SPIRegisterCenter.register(T::class, impl())
}