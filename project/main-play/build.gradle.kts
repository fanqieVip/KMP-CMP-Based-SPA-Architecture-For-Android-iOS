import com.frame.basic.buildsrc.ProjectBuildConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koinCompiler)
}

val androidNameSpace = "com.basic.main.distribution"

kotlin {
    androidLibrary {
        namespace = androidNameSpace
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        androidResources.enable = true
        withJava()
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("proguard-rules.pro"))
        }
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
            api(projects.project.main)
            implementation(libs.koin.android)
        }
    }
}
