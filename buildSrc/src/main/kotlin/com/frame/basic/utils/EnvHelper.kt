package com.frame.basic.utils

import com.frame.basic.buildsrc.ProjectBuildConfig
import org.gradle.api.Project
import java.io.File

private fun Project.envConfigProperties(): String? {
    val properties = File("${rootProject.projectDir.absolutePath}/iosApp/env/env_config.properties")
    if (!properties.exists()){
        return null
    }
    return properties.readText().trim()
}

fun Project.getBuildEnvName(): String {
    val taskNames = gradle.startParameter.taskNames
    return when {
        taskNames.any {it.contains("debug", ignoreCase = true) } -> {
            ProjectBuildConfig.Version.DEVELOP
        }
        taskNames.any {it.contains("beta", ignoreCase = true)} -> {
            ProjectBuildConfig.Version.BETA
        }
        taskNames.any {it.contains("alpha", ignoreCase = true)} -> {
            ProjectBuildConfig.Version.ALPHA
        }
        taskNames.any {it.contains("release", ignoreCase = true) || it.contains("main", ignoreCase = true)} -> {
            ProjectBuildConfig.Version.RELEASE
        }
        else -> {
            //xcode编译时传递的环境参数
            envConfigProperties()?:ProjectBuildConfig.Version.DEVELOP
        }
    }.also {
        println("----------App当前环境:${it}---------")
    }
}


fun Project.isReleaseEnv(): Boolean = getBuildEnvName() == ProjectBuildConfig.Version.RELEASE
fun Project.isAlphaEnv(): Boolean = getBuildEnvName() == ProjectBuildConfig.Version.ALPHA