import com.frame.basic.buildsrc.CompressConfig
import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.buildsrc.SDKKeyConfig
import com.frame.basic.buildsrc.SignConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.ktx.toResourceClassName
import com.frame.basic.utils.isAlphaEnv
import com.frame.basic.utils.isReleaseEnv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.koinCompiler)
    id("com.waynell.tinypng")
    id("com.basic.ios")
}
apply(from = "../batchTask.gradle")
val androidNameSpace = "com.basic.app"

//ios模块中可以$(fieldName)这样进行引用, 如：$(APPLICATION_ID)
iosConfig {
    field("APP_NAME", ProjectBuildConfig.Build.appName)
    field("VERSION_NAME", ProjectBuildConfig.Build.Ios.versionName)
    field("VERSION_CODE", "${ProjectBuildConfig.Build.Ios.versionCode}")
    field("APPLICATION_ID", ProjectBuildConfig.Build.applicationId)
}

kotlin {
    androidTarget {
        withSourcesJar(true)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries {
            framework {
                baseName = "ComposeApp"
                isStatic = true
                optimized = isReleaseEnv() || isAlphaEnv()
                debuggable = !(isReleaseEnv() || isAlphaEnv())
                freeCompilerArgs += listOf(
                    "-Xdebug-prefix-map=${rootDir.absolutePath}=."//将编译时产生的调试信息（如符号表）中的绝对路径替换为相对路径
                )
                linkerOpts("-dead_strip") //移除所有未引用的代码，减小体积
                if (isReleaseEnv() || isAlphaEnv()){
                    freeCompilerArgs += listOf(
                        "-Xadd-light-debug=disable",//禁用轻量级调试信息,减小二进制文件的体积，但代价是生产环境的崩溃堆栈可能更难阅读
                        "-Xg-generate-debug-trampoline=disable",//禁用轻量级调试信息，减少无用代码生成，优化运行效率并微量减小体积
                    )
                }
                //仅导出无安全隐患的core/base模块
                export(projects.core.base)
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(fileTree(mapOf(
                "dir" to "shared_libs/android",
                "include" to listOf("**/*.jar", "**/*.aar")
            )))
            implementation(fileTree(mapOf(
                "dir" to "libs/android",
                "include" to listOf("**/*.jar", "**/*.aar")
            )))
            implementation(libs.coil.ktor.android)
            api(projects.sharedProject)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.compose.multiplatform.components)
            api(projects.sharedProject)
        }
        iosMain.dependencies {
            api(projects.sharedProject)
        }
    }
}

android {
    namespace = androidNameSpace
    ndkVersion = ProjectBuildConfig.Build.Android.ndkVersion
    defaultConfig {
        applicationId = ProjectBuildConfig.Build.applicationId
        buildToolsVersion = ProjectBuildConfig.Build.Android.buildToolsVersion
        targetSdk = ProjectBuildConfig.Build.Android.targetSdkVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        versionCode = ProjectBuildConfig.Build.Android.versionCode
        versionName = ProjectBuildConfig.Build.Android.versionName
        ndk {
            abiFilters += "arm64-v8a"
        }
        manifestPlaceholders.apply {
            put("APP_NAME", ProjectBuildConfig.Build.appName)
            put("DEEP_LINK_SCHEME", ProjectBuildConfig.Deeplink.scheme)
            put("DEEP_LINK_HOST", ProjectBuildConfig.Deeplink.host)
        }
    }
    packaging {
        dex.useLegacyPackaging = ProjectBuildConfig.Build.Android.useDexLegacyPackaging
        jniLibs.useLegacyPackaging = ProjectBuildConfig.Build.Android.useJniLegacyPackaging
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            enableV1Signing = SignConfig.v1SigningEnabled
            enableV2Signing = SignConfig.v2SigningEnabled
            keyAlias = SignConfig.keyAlias
            keyPassword = SignConfig.keyPassword
            storePassword = SignConfig.storePassword
            storeFile = file("${rootDir.absolutePath}/buildSrc/${SignConfig.storeFile}")

        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = false
            isDebuggable = true
            isJniDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("beta") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = false
            isDebuggable = true
            isJniDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("alpha") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = ProjectBuildConfig.Build.Android.buildApkName(variant.buildType.name)
            }
    }
}

dependencies {
    debugImplementation(libs.compose.multiplatform.ui.tooling )
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
tinyInfo {
    resourceDir = CompressConfig.getResourceDir(rootProject.getRootDir())
    resourcePattern = CompressConfig.RESOURCE_PATTERN
    whiteList = CompressConfig.WHITE_LIST
    apiKey = SDKKeyConfig.tinyPngApiKey
}
