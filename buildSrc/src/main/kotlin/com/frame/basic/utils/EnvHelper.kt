package com.frame.basic.utils

import com.frame.basic.buildsrc.ProjectBuildConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

/**
 * Android distribution targets that can produce different packaged capabilities.
 */
enum class Distribution {
    /** Domestic distribution with domestic-only capabilities. */
    CHINA,

    /** Google Play distribution. */
    PLAY
}

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

/**
 * Resolves the Android distribution from the Gradle entry task.
 *
 * Android library modules do not inherit the app flavor selected by AGP. Resolving the
 * distribution from the requested task keeps their generated BuildKonfig values aligned
 * with the app variant. Tasks without a distribution are IDE/common compilation tasks and
 * intentionally default to the domestic configuration.
 *
 * @return the distribution selected for this Gradle invocation.
 */
fun Project.getDistribution(): Distribution {
    val taskNames = gradle.startParameter.taskNames
    val isChina = taskNames.any { it.contains("china", ignoreCase = true) }
    val isPlay = taskNames.any { it.contains("play", ignoreCase = true) }
    require(!(isChina && isPlay)) {
        "A single Gradle invocation cannot configure both China and Play distributions. " +
            "Run their tasks separately."
    }
    val genericPackageTask = Regex("^(assemble|bundle)(Debug|Beta|Alpha|Release)$", RegexOption.IGNORE_CASE)
    if (taskNames.any { genericPackageTask.matches(it.substringAfterLast(':')) }) {
        throw GradleException(
            "Select a distribution explicitly, for example assembleChinaRelease or bundlePlayRelease."
        )
    }
    return when {
        isChina -> Distribution.CHINA
        isPlay -> Distribution.PLAY
        else -> Distribution.CHINA
    }.also {
        println("----------App当前渠道:${it.name.lowercase()}---------")
    }
}

fun Project.isReleaseEnv(): Boolean = getBuildEnvName() == ProjectBuildConfig.Version.RELEASE
fun Project.isAlphaEnv(): Boolean = getBuildEnvName() == ProjectBuildConfig.Version.ALPHA

/**
 * Indicates whether the current Gradle invocation targets Google Play.
 *
 * @return `true` when the Play distribution is selected.
 */
fun Project.isPlayDimension(): Boolean = getDistribution() == Distribution.PLAY
