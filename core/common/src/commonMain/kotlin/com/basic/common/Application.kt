package com.basic.common

/**
 * 应用生命周期
 */
internal expect object Application {
    /**
     * 创建时回调
     */
    fun onCreate()
}