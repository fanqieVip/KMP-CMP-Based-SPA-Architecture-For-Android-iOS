import com.frame.basic.buildsrc.ProjectBuildConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koinCompiler)
}

val androidNameSpace = "com.basic.common.distribution"

kotlin {
    androidLibrary {
        namespace = androidNameSpace
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        withJava()
    }

    sourceSets {
        androidMain.dependencies {
            compileOnly(
                fileTree(
                    mapOf(
                        "dir" to "libs/android",
                        "include" to listOf("**/*.jar", "**/*.aar")
                    )
                )
            )
            api(projects.core.common)
            implementation(libs.koin.android)
        }
    }
}
