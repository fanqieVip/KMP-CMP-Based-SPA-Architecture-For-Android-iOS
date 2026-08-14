@file:JvmName("IosFrameworkKtx")
package com.frame.basic.ktx

import com.frame.basic.buildsrc.ProjectBuildConfig
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

data class IosLocalFrameworkPodConfig(
    val name: String,
    // 用于 CocoaPods vendored_frameworks，目录下所有 .framework 都会参与宿主 App 链接。
    val frameworkDir: String = "libs/ios/framework",
    val systemFrameworks: List<String> = emptyList(),
    val weakFrameworks: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
    val otherLdFlags: List<String> = emptyList(),
    val cinterop: IosFrameworkCInteropConfig? = null,
)

data class IosFrameworkCInteropConfig(
    val packageName: String,
    // 只影响 umbrella.h 自动 import 的头文件，不影响 podspec 中 framework 的全量链接；默认依赖脚本自动过滤不可解析头。
    val excludeHeaderFrameworks: List<String> = emptyList(),
    // 用于补充非标准 umbrella header，例如 Foo.framework/Headers/Bar.h。
    val extraHeaders: List<String> = emptyList(),
    val headerFilter: String = "**",
)

/**
 * @Description: 生成本地 framework Podspec、可选 cinterop 文件，并注册到根工程 Podfile 自动聚合流程
 * @param config 本地 Pod 配置；资源文件不在这里声明，避免和宿主资源目录重复拷贝
 */
fun Project.registerIosLocalFrameworkPod(config: IosLocalFrameworkPodConfig): IosLocalFrameworkPodConfig {
    val normalizedFrameworkDir = config.frameworkDir.trim('/').ifBlank { "libs/ios/framework" }
    val generatedPodDir = localFrameworkPodGeneratedDir(config)
    val frameworkRoot = file(normalizedFrameworkDir)
    generatedPodDir.mkdirs()

    val podspec = projectDir.resolve("${config.name}.podspec")
    podspec.writeText(config.toPodspecText(normalizedFrameworkDir))
    config.cinterop?.let { cinterop ->
        generateFrameworkCInteropFiles(
            frameworkRoot = frameworkRoot,
            cinteropName = config.name,
            cinteropDir = localFrameworkPodCInteropDir(config),
            config = cinterop
        )
    }

    val iosAppDir = rootProject.file("iosApp").toPath()
    val podPath = iosAppDir
        .toAbsolutePath()
        .normalize()
        .relativize(projectDir.toPath().toAbsolutePath().normalize())
        .invariantSeparatorsPath()
    val currentPods = if (extensions.extraProperties.has("iosLocalPods")) {
        @Suppress("UNCHECKED_CAST")
        (extensions.extraProperties.get("iosLocalPods") as? Map<String, String>).orEmpty()
    } else {
        emptyMap()
    }
    extensions.extraProperties["iosLocalPods"] = currentPods + (config.name to podPath)
    return config
}

fun Project.localFrameworkPodGeneratedDir(config: IosLocalFrameworkPodConfig): File =
    layout.buildDirectory.dir("generated/iosLocalFrameworkPods/${config.name}").get().asFile

fun Project.localFrameworkPodCInteropDir(config: IosLocalFrameworkPodConfig): File =
    localFrameworkPodGeneratedDir(config).resolve("cinterop")

fun Project.localFrameworkPodDefFile(config: IosLocalFrameworkPodConfig): File =
    localFrameworkPodCInteropDir(config).resolve("${config.name}.def")

fun Project.localFrameworkPodCompilerOpts(config: IosLocalFrameworkPodConfig): List<String> =
    compilerOptsByDir(config.frameworkDir) + "-I${localFrameworkPodCInteropDir(config).absolutePath}"

fun Project.localFrameworkPodLinkerOpts(config: IosLocalFrameworkPodConfig): List<String> =
    linkerOptsByDir(config.frameworkDir)

private fun Project.generateFrameworkCInteropFiles(
    frameworkRoot: File,
    cinteropName: String,
    cinteropDir: File,
    config: IosFrameworkCInteropConfig,
) {
    cinteropDir.mkdirs()

    val umbrellaFileName = "${cinteropName}_umbrella.h"
    cinteropDir.resolve("$cinteropName.def").writeText(
        buildString {
            appendLine("package = ${config.packageName}")
            appendLine("language = Objective-C")
            appendLine("headers = $umbrellaFileName")
            appendLine("headerFilter = ${config.headerFilter}")
            appendLine("compilerOpts = -I${cinteropDir.absolutePath}")
        }
    )

    cinteropDir.resolve(umbrellaFileName).writeText(
        frameworkRoot.frameworkImportHeaders(
            excludeHeaderFrameworks = config.excludeHeaderFrameworks,
            extraHeaders = config.extraHeaders
        ).joinToString(separator = "\n", postfix = "\n") { "#import <$it>" }
    )
}

private fun File.frameworkImportHeaders(
    excludeHeaderFrameworks: List<String>,
    extraHeaders: List<String>,
): List<String> {
    val excludeSet = excludeHeaderFrameworks.toSet()
    val frameworkSearchPaths = frameworkSearchPaths()
    // cinterop 只需要导入 Objective-C 可解析的 umbrella header；framework 链接由 podspec 全量处理。
    val autoHeaders = frameworkDirs()
        .filter { framework ->
            framework.nameWithoutExtension !in excludeSet &&
                    framework.isCInteropCandidateFramework() &&
                    framework.isCInteropImportable(frameworkSearchPaths)
        }
        .map { framework ->
            val frameworkName = framework.nameWithoutExtension
            "$frameworkName/$frameworkName.h"
        }

    return (autoHeaders + extraHeaders).distinct()
}

private fun File.isCInteropCandidateFramework(): Boolean {
    val frameworkName = nameWithoutExtension
    return resolve("Headers/$frameworkName.h").exists()
}

private fun File.isCInteropImportable(frameworkSearchPaths: List<String>): Boolean {
    val frameworkName = nameWithoutExtension
    val header = resolve("Headers/$frameworkName.h")
    val sdkPath = iosSimulatorSdkPath() ?: return true
    val command = buildList {
        add("xcrun")
        add("clang")
        add("-x")
        add("objective-c-header")
        add("-fsyntax-only")
        add("-isysroot")
        add(sdkPath)
        add("-mios-simulator-version-min=${ProjectBuildConfig.Build.Ios.deploymentTarget}")
        frameworkSearchPaths.forEach { searchPath ->
            add("-F$searchPath")
        }
        add(header.absolutePath)
    }

    return runCatching {
        ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    }.getOrDefault(true)
}

private fun iosSimulatorSdkPath(): String? =
    runCatching {
        ProcessBuilder("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
            .takeIf { it.isNotEmpty() }
    }.getOrNull()

private const val localFrameworkPodVersion = "1.0.0"
private const val localFrameworkPodSummary = "Local frameworks"
private const val localFrameworkPodHomepage = "https://www.baidu.com"

private fun IosLocalFrameworkPodConfig.toPodspecText(frameworkDir: String): String = buildString {
    appendLine("Pod::Spec.new do |s|")
    appendLine("  s.name = '${name.podspecEscaped()}'")
    appendLine("  s.version = '${localFrameworkPodVersion.podspecEscaped()}'")
    appendLine("  s.summary = '${localFrameworkPodSummary.podspecEscaped()}'")
    appendLine("  s.homepage = '${localFrameworkPodHomepage.podspecEscaped()}'")
    appendLine("  s.license = { :type => 'Commercial' }")
    appendLine("  s.author = { 'Local' => 'local' }")
    appendLine("  s.source = { :path => '.' }")
    appendLine("  s.platform = :ios, '${ProjectBuildConfig.Build.Ios.deploymentTarget.podspecEscaped()}'")
    appendLine()
    appendLine("  s.vendored_frameworks = '${frameworkDir.podspecEscaped()}/**/*.framework'")
    appendPodspecArray("frameworks", systemFrameworks)
    appendPodspecArray("weak_frameworks", weakFrameworks)
    appendPodspecArray("libraries", libraries)
    appendLine()
    appendLine("  s.requires_arc = true")
    if (otherLdFlags.isNotEmpty()) {
        appendLine("  s.user_target_xcconfig = {")
        appendLine("    'OTHER_LDFLAGS' => '$(inherited) ${otherLdFlags.joinToString(" ").podspecEscaped()}'")
        appendLine("  }")
    }
    appendLine("end")
}

private fun StringBuilder.appendPodspecArray(name: String, values: List<String>) {
    if (values.isEmpty()) return
    appendLine()
    appendLine("  s.$name = [")
    values.forEachIndexed { index, value ->
        val suffix = if (index == values.lastIndex) "" else ","
        appendLine("    '${value.podspecEscaped()}'$suffix")
    }
    appendLine("  ]")
}

private fun java.nio.file.Path.invariantSeparatorsPath(): String =
    toString().replace(File.separatorChar, '/')

private fun String.podspecEscaped(): String =
    replace("\\", "\\\\").replace("'", "\\'")
