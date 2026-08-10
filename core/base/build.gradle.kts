import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.frame.basic.buildsrc.ChannelConfig
import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.buildsrc.SignConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.ktx.toResourceClassName
import com.frame.basic.utils.ApkSha1Utils
import com.frame.basic.utils.getBuildEnvName

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
    alias(libs.plugins.kotlinCocoapods)
}
val androidNameSpace = "com.basic.base"
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
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main") {
            val observer by cinterops.creating {
                defFile(project.file("src/iosMain/cinterop/observer.def"))
            }
        }
    }

    cocoapods {
        summary = androidNameSpace
        homepage = "http://www.baidu.com"
        version = "1.0"
        ios.deploymentTarget = ProjectBuildConfig.Build.Ios.deploymentTarget

        //处理ios的uuid https://github.com/guojunliu/XYUUID
        pod("XYUUID"){
            version = "1.0.0"
        }
        //处理ios网络连接授权库 https://github.com/lanlinxl/LLNetworkAccessibility-OC https://juejin.cn/post/7176081860361977893
        pod("LLNetworkAccessibility-OC"){
            version = "1.0.2"
        }
        //toast https://github.com/scalessec/Toast
        pod("Toast"){
            version = "4.1.1"
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
            api(libs.androidx.core.ktx)
            api(libs.github.blankJ.utilcodex)
            api(libs.androidx.multiDex)
            api(libs.androidx.activity.compose)
            api(libs.koin.android)
            api(libs.androidx.lifecycle.process)
            api(libs.androidx.splashscreen)
            api(libs.androidx.constraintLayout)
            api(libs.android.material)
            api(libs.github.walle)
            api(libs.xxPermission)
            api(libs.autoSize)
            api(libs.coil.ktor.android)
            api(libs.android.immersionbar)
            val oaidLib = libs.github.cnOaid.get().let { "${it.group}:${it.name}:${it.version}" }
            implementation(oaidLib) {
                exclude(group = "com.huawei.hms", module = "ads-identifier")
                exclude(group = "com.hihonor.mcs", module = "ads-identifier")
            }
            runtimeOnly("com.huawei.hms:ads-identifier:3.4.62.300")
            runtimeOnly("com.hihonor.mcs:ads-identifier:1.0.3.300")
        }
        commonMain.dependencies {
            api(libs.kotlin.stdlib)
            api(libs.compose.multiplatform.components)
            api(libs.compose.multiplatform.runtime)
            api(libs.compose.multiplatform.foundation)
            api(libs.compose.multiplatform.material3)
            api(libs.compose.multiplatform.ui)
            api(libs.compose.multiplatform.ui.tooling.preview)
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.androidx.lifecycle.runtimeCompose)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization)
            api(libs.multiplatform.settings)

            api(libs.vortex)

            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)

            implementation(libs.napier)

            api(libs.filekit.core)
            api(libs.filekit.dialogs)
            api(libs.filekit.dialogs.compose)
            api(libs.filekit.coil)

            api(libs.imagepicker)

            api(libs.permissions)
            api(libs.permissions.compose)

            api(libs.ktorfit.lib)
            api(libs.ktor.core)
            api(libs.ktor.serialization)
            api(libs.ktor.json)
            api(libs.ktor.negotiation)
            api(libs.ktor.logging)

            api(libs.pullrefresh)

            api(libs.uuid)

            api(libs.konnectivity)

            api(libs.coil.ktor.network)

            api(libs.compottie)
            api(libs.compottie.lite)
            api(libs.compottie.dot)
            api(libs.compottie.network)
            api(libs.compottie.resources)

            api(libs.haze)
            api(libs.haze.blur)
        }

        iosMain.dependencies {
            api(libs.coil.ktor.ios)
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
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APPLICATION_ID", ProjectBuildConfig.Build.applicationId)
        buildConfigField(FieldSpec.Type.INT, "DESIGN_SIZE", "${ProjectBuildConfig.Build.designSize}")
        buildConfigField(FieldSpec.Type.STRING, "APP_NAME", ProjectBuildConfig.Build.appName)
        buildConfigField(FieldSpec.Type.STRING, "DEFAULT_CHANNEL", ChannelConfig.default_channel)
        buildConfigField(FieldSpec.Type.STRING, "VERSION_TYPE", project.getBuildEnvName())
        buildConfigField(FieldSpec.Type.STRING, "DEEP_LINK_SCHEME", ProjectBuildConfig.Deeplink.scheme)
        buildConfigField(FieldSpec.Type.STRING, "DEEP_LINK_HOST", ProjectBuildConfig.Deeplink.host)
        buildConfigField(FieldSpec.Type.STRING, "APK_VERIFY_CODE", ApkSha1Utils.getSha1("${rootDir.absolutePath}/buildSrc/${SignConfig.storeFile}", SignConfig.keyAlias, SignConfig.storePassword))
    }
}