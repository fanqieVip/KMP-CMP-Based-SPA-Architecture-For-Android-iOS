import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.ktx.toResourceClassName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.koinCompiler)
    id("com.basic.router")
    id("com.basic.lint")
    id("com.basic.protect-src")
}

val androidNameSpace = "com.basic.common.distribution"
kotlin {
    androidLibrary {
        namespace = androidNameSpace
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        androidResources.enable = true
        withJava()
        withSourcesJar(true)
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("proguard-rules.pro"))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.multiplatform.components)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                api(projects.core.common)
            }
        }

        androidMain {
            dependencies {
                compileOnly(
                    fileTree(
                        mapOf(
                            "dir" to "libs/android",
                            "include" to listOf("**/*.jar", "**/*.aar")
                        )
                    )
                )
                api(projects.core.common)
                implementation(libs.compose.multiplatform.components)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                implementation(libs.koin.android)
                val oaidLib = libs.github.cnOaid.get().let { "${it.group}:${it.name}:${it.version}" }
                implementation(oaidLib) {
                    exclude(group = "com.huawei.hms", module = "ads-identifier")
                    exclude(group = "com.hihonor.mcs", module = "ads-identifier")
                }
                runtimeOnly("com.huawei.hms:ads-identifier:3.4.62.300")
                runtimeOnly("com.hihonor.mcs:ads-identifier:1.0.3.300")
            }
        }

        iosMain {
            dependencies {
                api(projects.core.common)
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = auto
    nameOfResClass = androidNameSpace.toResourceClassName()
    packageOfResClass = androidNameSpace
}

buildkonfig {
    packageName = "buildkonfig"
    exposeObjectWithName = androidNameSpace.toBuildConfigClassName()
    defaultConfigs {}
}
