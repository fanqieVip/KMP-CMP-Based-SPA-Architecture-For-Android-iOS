import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.buildsrc.SignConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.utils.ApkSha1Utils

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ksp)
}
val androidNameSpace = "com.basic.nativeLibs"
val androidLibsName = "shared_nativeLibs"
val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
android {
    namespace = androidNameSpace
    ndkVersion = ProjectBuildConfig.Build.Android.ndkVersion
    defaultConfig {
        buildToolsVersion = ProjectBuildConfig.Build.Android.buildToolsVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        consumerProguardFiles += project.file("proguard-rules.pro")
        ndk {
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                arguments( "-DANDROID_STL=c++_static")
                //注意：由于只编译了windows版的ollvm-ndk，所以仅在windows上使用ollvm混淆，安卓打包都在windows上打包
                if (isWindows){
                    cppFlags("-fexceptions -frtti -s -Wall -Wextra -std=c++17 -fvisibility=hidden -fvisibility-inlines-hidden -mllvm -fla -mllvm -bcf -mllvm -sub -mllvm -sobf")
                }else{
                    cppFlags("-fexceptions -frtti -s -Wall -Wextra -std=c++17 -fvisibility=hidden -fvisibility-inlines-hidden")
                }
                abiFilters("arm64-v8a")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path(file("src/androidMain/cpp/CMakeLists.txt"))
            version = ProjectBuildConfig.Build.Android.cmakeVersion
        }
    }
    buildTypes {
        debug {}
        create("beta"){}
        create("alpha"){}
        release {}
    }
}
kotlin {
    androidTarget {
        withSourcesJar(true)
    }
    androidNativeArm64 {
        binaries.staticLib {
            baseName = androidLibsName
            optimized = true
            debuggable = false
            outputDirectory = file("$projectDir/src/androidNativeArm64Main/staticLib/")
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                implementation(libs.kni.annotations)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.core.ktx)
                api(libs.github.blankJ.utilcodex)
                api(libs.androidx.multiDex)
            }
        }

        androidNativeArm64Main {
            dependencies {
                implementation(libs.kni)
            }
        }
        iosMain {
            dependencies {
            }
        }
    }
}
dependencies {
    add("kspAndroidNativeArm64", libs.kni.processor)
}
buildkonfig {
    packageName = "buildkonfig"
    exposeObjectWithName = androidNameSpace.toBuildConfigClassName()
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APK_VERIFY_CODE", ApkSha1Utils.getSha1("${rootDir.absolutePath}/buildSrc/${SignConfig.storeFile}", SignConfig.keyAlias, SignConfig.storePassword))
    }
}
