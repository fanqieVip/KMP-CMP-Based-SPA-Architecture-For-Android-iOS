package com.basic.native.jvm

import dev.datlag.nkommons.CallableFromNative

/**
 * @Description:    native调用jvm的桥梁(注意：需要处理混淆)
 * @Author:         fanj
 * @CreateDate:     2026/3/24 14:28
 * @Version:
 */
@CallableFromNative
interface JvmBridge: AutoCloseable {
    /**
     * 环境检测
     */
    fun process(source: String): String

    /**
     * java-hashCode
     */
    fun identify(): String
}