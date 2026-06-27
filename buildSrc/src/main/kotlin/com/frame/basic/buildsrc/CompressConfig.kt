package com.frame.basic.buildsrc

import java.io.File

/**
 * 图片压缩配置
 */
object CompressConfig {
    /**
     * 图片源路径（所有module都回按这个相对路径读图片资源）
     */
    private const val RES_PREX = "/src/commonMain/composeResources"

    /**
     * 具体的图片资源目录
     */
    val RESOURCE_PATTERN = arrayListOf("drawable[a-z-]*", "mipmap[a-z-]*")

    /**
     * 图片白名单（支持正则表达式）
     */
    val WHITE_LIST = arrayListOf<String>()

    /**
     * 获取图片源路径
     */
    @JvmStatic
    fun getResourceDir(rootDir: File) = ArrayList<String>().apply {
        rootDir.listFiles()?.forEach {
            val targetResDir = File(it.absolutePath + RES_PREX)
            if (targetResDir.exists() && targetResDir.isDirectory) {
                add(it.name + RES_PREX)
            }
        }
    }
}