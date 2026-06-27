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
}
val androidNameSpace = "com.basic.project"
kotlin {
    androidLibrary {
        namespace = androidNameSpace
        compileSdk = ProjectBuildConfig.Build.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.minSdkVersion
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
                api(project(":shared_common"))
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
                api(project(":shared_common"))
            }
        }

        iosMain {
            dependencies {
                api(project(":shared_common"))
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
