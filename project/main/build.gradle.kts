import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.ktx.toResourceClassName

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.koinCompiler)
    id("com.basic.router")
    id("com.basic.lint")
}
val androidNameSpace = "com.basic.main"
kotlin {
    androidLibrary {
        namespace = androidNameSpace
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
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

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.multiplatform.components)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                api(projects.core.common)
                api(libs.permissions.microphone)
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
                implementation(libs.koin.android)
                api(projects.core.common)
            }
        }

        iosMain {
            dependencies {
                api(projects.core.common)
            }
        }
    }
}
dependencies {
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
