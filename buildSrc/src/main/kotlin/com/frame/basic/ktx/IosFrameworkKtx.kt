@file:JvmName("IosFrameworkKtx")
package com.frame.basic.ktx

import org.gradle.api.Project
import java.io.File

/**
 * @Description: 为 Gradle iOS 本地 framework 集成提供目录扫描与编译参数生成能力
 * @Author:         范俊
 * @CreateDate:     2026/08/13 19:56
 */
fun File.frameworkDirs(): List<File> =
    walkTopDown()
        .filter { it.isDirectory && it.extension == "framework" }
        .sortedBy { it.relativeTo(this).path }
        .toList()

/**
 * @Description: 扫描目录内所有 framework 的父目录并生成去重后的 framework search path
 * @return framework 父目录绝对路径列表
 */
fun File.frameworkSearchPaths(): List<String> =
    frameworkDirs()
        .mapNotNull { it.parentFile?.absolutePath }
        .distinct()

/**
 * @Description: 扫描目录内所有 framework 并生成 Kotlin/Native linkerOpts 参数
 * @return 包含 `-F` 与 `-framework` 的 linkerOpts 参数列表
 */
fun File.frameworkLinkerOpts(): List<String> = buildList {
    frameworkSearchPaths().forEach { root ->
        add("-F$root")
    }
    frameworkDirs().forEach { framework ->
        add("-framework")
        add(framework.nameWithoutExtension)
    }
}

/**
 * @Description: 根据本地 framework 根目录生成 Kotlin/Native linkerOpts 参数
 * @param path framework 根目录路径，基于当前 Project 解析
 * @return 包含 `-F` 与 `-framework` 的 linkerOpts 参数列表
 */
fun Project.linkerOptsByDir(path: String): List<String> =
    file(path).frameworkLinkerOpts()

/**
 * @Description: 根据本地 framework 根目录生成 Kotlin/Native cinterop compilerOpts 参数
 * @param path framework 根目录路径，基于当前 Project 解析
 * @return 包含 `-F` 的 compilerOpts 参数列表
 */
fun Project.compilerOptsByDir(path: String): List<String> =
    file(path).frameworkSearchPaths().map { "-F$it" }
