import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.OutputStream
import com.frame.basic.buildsrc.ProjectBuildConfig

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.kotlinCocoapods) apply false

}
fun commandSucceeds(vararg command: String): Boolean =
    runCatching {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        process.inputStream.use { it.copyTo(OutputStream.nullOutputStream()) }
        process.waitFor() == 0
    }.getOrDefault(false)

val isMac = DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX
val canBuildAppleTargets = isMac && commandSucceeds("xcrun", "--find", "xcodebuild")

fun isAppleRelatedTask(taskName: String): Boolean {
    val name = taskName.lowercase()
    return name.contains("ios") ||
        name.contains("cocoapods") ||
        name.contains("pod") ||
        name.contains("xcode") ||
        name.contains("cinterop") ||
        name.contains("apple")
}

subprojects {
    // 未安装完整 Xcode 时禁止执行 Apple 相关任务，避免 Android 同步/编译被 xcodeVersion 阻断。
    tasks.configureEach {
        if (!canBuildAppleTargets && isAppleRelatedTask(name)) {
            enabled = false
        }
    }

    // 修复 Kotlin CocoaPods 插件强制将 synthetic Podfile 的 deployment target 设为 12.0 的问题 (针对 Xcode 15+)
    // 使用 doFirst 确保即使 podGenIos 为 UP-TO-DATE 时也能执行修复逻辑
    tasks.matching { it.name.startsWith("podInstallSyntheticIos") }.configureEach {
        doFirst {
            val podfile = file("build/cocoapods/synthetic/ios/Podfile")
            if (podfile.exists()) {
                var text = podfile.readText()
                if (text.contains("IPHONEOS_DEPLOYMENT_TARGET")) {
                    val targetVersion = ProjectBuildConfig.Build.Ios.deploymentTarget
                    // 替换判定逻辑和版本号
                    text = text.replace(Regex("""deployment_target_major\s*<\s*12"""), "deployment_target_major < ${targetVersion.split(".")[0]}")
                    text = text.replace(Regex("""deployment_target_major\s*==\s*12"""), "deployment_target_major == ${targetVersion.split(".")[0]}")
                    text = text.replace(Regex("""version\s*=\s*"#\{12\}\.#\{0\}""""), "version = \"#{$targetVersion.split(\".\")[0]}.#{$targetVersion.split(\".\")[1]}\"")
                    podfile.writeText(text)
                    logger.lifecycle("Patched synthetic Podfile in ${project.path} to use iOS $targetVersion")
                }
            }
        }
    }
}

gradle.projectsEvaluated {
    subprojects {
        tasks.forEach { task ->
            if (!canBuildAppleTargets && isAppleRelatedTask(task.name)) {
                task.enabled = false
            }
        }
    }
}
//buildscript {
//    dependencies {
//        classpath(libs.agp)
//    }
//}

/************* 自动汇总所有子模块的 Pods 并同步到 iosApp/Podfile **************/
// 1.如果编译后遇到缺少系统库的情况可到Xcode中Build Phases中手动添加即可修复（目前大部分已添加好，一般不会遇到）
// 2.同步完成后，切换到iosApp下执行pod install命令进行安装
if (canBuildAppleTargets){
    gradle.projectsEvaluated {
        val podfile = file("iosApp/Podfile")
        if (!podfile.exists()) return@projectsEvaluated

        val allPods = mutableMapOf<String, String?>() // Pod名称 -> 版本号
        val orderedNames = mutableListOf<String>()

        subprojects.forEach { subproject ->
            // 尝试获取 KMP 扩展
            val extension = subproject.extensions.findByName("kotlin") as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
            // 尝试获取 cocoapods 扩展
            val cocoapods = extension?.extensions?.findByName("cocoapods") as? org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

            if (cocoapods != null) {
                val buildFileText = subproject.buildFile.readText()
                // 通过正则解析 build.gradle.kts 中的 pod() 调用顺序
                val podsInFile = Regex("""pod\s*\(\s*["']([^"']+)["']\s*\)""").findAll(buildFileText)
                    .map { it.groupValues[1] }
                    .filter { cocoapods.pods.findByName(it) != null }
                    .distinct()
                    .toList()

                podsInFile.forEach { name ->
                    if (!allPods.containsKey(name)) {
                        val pod = cocoapods.pods.getByName(name)
                        allPods[name] = pod.version
                        orderedNames.add(name)
                    }
                }
            }
        }

        if (orderedNames.isNotEmpty()) {
            val targetVersion = ProjectBuildConfig.Build.Ios.deploymentTarget
            val newContent = buildString {
                appendLine("# This file is automatically generated by Gradle. Do not edit manually.")
                appendLine("target 'iosApp' do")
                appendLine("  use_frameworks!")
                appendLine("  platform :ios, '$targetVersion'")
                orderedNames.forEach { name ->
                    val version = allPods[name]
                    val versionPart = if (version != null) ", '~> $version'" else ""
                    appendLine("  pod '$name'$versionPart")
                }
                appendLine("end")
                appendLine("")
                appendLine("post_install do |installer|")
                appendLine("  installer.pods_project.targets.each do |target|")
                appendLine("    target.build_configurations.each do |config|")
                appendLine("      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '$targetVersion'")
                appendLine("    end")
                appendLine("  end")
                appendLine("end")
            }

            if (podfile.readText().trim() != newContent.trim()) {
                podfile.writeText(newContent)
                logger.lifecycle("Podfile updated successfully with pods from all subprojects")
            }
        }
    }
}

