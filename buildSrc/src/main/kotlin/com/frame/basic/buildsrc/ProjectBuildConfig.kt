package com.frame.basic.buildsrc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * 项目相关参数配置
 */
object ProjectBuildConfig {

    /**
     * apk打包参数
     */
    object Build {
        const val compileSdkVersion = 36
        const val buildToolsVersion = "36.0.0"
        const val applicationId = "com.basic.app"
        //vmp加密要求24以上，多个依赖库要求最低26起
        const val minSdkVersion = 26
        const val targetSdkVersion = 36
        const val versionCode = 1
        const val versionName = "1.0.0"
        const val appName = "KmpProject"
        //ndk官方版 https://github.com/android/ndk（存放android-sdk/ndk/）
        //ndk23.2.8568313 ollvm windows整合包 https://github.com/Ant-tree/ObfuscatorNDK
        const val ndkVersion = "23.2.8568313"
        const val cmakeVersion = "3.22.1"

        //min >= 28后，打包apk不会压缩dex，加上这个是开启dex压缩
        const val useDexLegacyPackaging = true
        //minSdk > 23后，打包apk不会压缩so，加上这个是开启so压缩(注意：vmp加密同样依赖这个配置，需共同约束)
        const val useJniLegacyPackaging = true

        //设计图尺寸
        const val designSize: Int = 375

        //生成打包APK名称的前缀
        @JvmStatic
        fun buildApkNamePrefix(environment: String): String = "${appName}_v${versionName}_"

        //生成打包APK的名称
        @JvmStatic
        @Suppress("SimpleDateFormat")
        fun buildApkName(environment: String): String {
            val sdf = SimpleDateFormat("MMddHHmm").apply {
                timeZone = TimeZone.getDefault()
            }
            val time = sdf.format(Date())
            return "${buildApkNamePrefix(environment)}${time}.apk"
        }
    }

    /**
     * 深度链接配置
     * Uri例如：${scheme}://${host}....
     *
     * 如需要配置深度链接，则在Manifest.xml中直接引用"${DEEP_LINK_SCHEME}","${DEEP_LINK_HOST}"即可
     */
    object Deeplink {
        const val scheme = "companyName"
        const val host = Build.applicationId
    }


    /**
     * 项目当前的版本状态(和xcode有依赖 ，不可随意更改)
     * 该状态直接反映当前App是测试版 还是正式版 或者预览版
     * 正式版:RELEASE、预发布:ALPHA、测试版BETA-开发:DEVELOP
     */
    object Version {

        const val RELEASE = "VERSION_STATUS_RELEASE"

        const val ALPHA = "VERSION_STATUS_ALPHA"

        const val BETA = "VERSION_STATUS_BETA"

        const val DEVELOP = "VERSION_STATUS_DEVELOP"
    }

    /**
     * 其他配置
     */
    object Other {
        //本地TestApk/apk目录的的访问端口，每个应用需要设置不同的端口
        const val serverPort: String = "8081"
    }
}