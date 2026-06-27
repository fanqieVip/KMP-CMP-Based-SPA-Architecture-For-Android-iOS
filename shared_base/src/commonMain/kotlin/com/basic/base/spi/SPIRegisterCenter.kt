package com.basic.base.spi

import kotlin.reflect.KClass

/**
 * 泛型SPI注册中心
 */
object SPIRegisterCenter {
    val registry = mutableMapOf<KClass<*>, MutableList<Any>>()

    /**
     * 注册服务实现
     */
    fun <T : Any> register(serviceClass: KClass<T>, implementation: T) {
        val implementations = registry.getOrPut(serviceClass) { mutableListOf() }
        implementations.add(implementation)
    }

    /**
     * 获取所有实现
     */
    inline fun <reified T : Any> all(): List<T> {
        return (registry[T::class]?.filterIsInstance<T>() ?: emptyList())
    }

    /**
     * 获取第一个实现
     */
    inline fun <reified T : Any> first(): T? {
        return all<T>().firstOrNull()
    }

    /**
     * 清空注册表
     */
    fun clear() {
        registry.clear()
    }
}

/**
 * 从spi注册中心获取第一个实现
 */
inline fun <reified T : Any> withImpl(): T? {
    return SPIRegisterCenter.first<T>()
}