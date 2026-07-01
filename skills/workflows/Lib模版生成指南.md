# Lib 模版生成指南

> 本文档定义了如何快速自动化创建一个三方 SDK 适配模块（如 WeChat, QQ, SensorsData 等）。AI 必须严格遵守此指南，并使用内置的模板代码，严禁依赖外部不确定的文件夹。

## 1. 命名与目录规范 (Naming Convention)

- **模块名**: 统一以 `lib_` 开头（小写）。
- **Android Namespace**: 格式为 `com.basic.<suffix>`（如 `com.basic.alipay`）。
- **源码路径**: 物理文件夹路径必须匹配 namespace，例如 `src/commonMain/kotlin/com/basic/xxx/`。

## 2. 自动化生产线 (Automation Pipeline)

当收到“初始化 Lib 模块”指令时，AI 必须连续执行以下步骤：

### 2.1 物理结构生成 (Physical Tree)
1. 创建根目录 `lib_xxx/`。
2. 创建 `libs/android/` 目录。
3. 创建标准源码树：
    - `src/commonMain/kotlin/com/basic/xxx/di/impl/`
    - `src/androidMain/kotlin/com/basic/xxx/di/impl/`
    - `src/iosMain/kotlin/com/basic/xxx/di/impl/`
4. 创建基础清单：`src/androidMain/AndroidManifest.xml` (仅含基础 `manifest` 节点)。
5. 创建基础混淆：`proguard-rules.pro` (仅含默认注释)。

### 2.2 构建配置注入 (Gradle & Settings)
1. **build.gradle.kts**: 必须使用 [内置 Gradle 模板](#51-buildgradlekts-模板)。
2. **settings.gradle.kts**: 自动添加 `include(":lib_xxx")`。
3. **shared_common/build.gradle.kts**: 在 `commonMain`、`androidMain`、`iosMain` 依赖块中自动添加 `api(project(":lib_xxx"))`。
4. **Android 本地 SDK**: AAR/JAR 必须放在 `lib_xxx/libs/android/`，并使用模板中的 `compileOnly(fileTree(...))`。这是本项目框架规范，框架会处理最终依赖打包，AI 不得擅自改成 `implementation(files(...))`、`api(files(...))` 或复制到 app 模块。

### 2.3 代码模板生成 (DI & SPI)
1. **ApplicationService 模板**: 必须使用 [内置 SPI 模板](#52-spi-生命周期模板)。
2. **DI 模块模板**: 必须使用 [内置 DI 模板](#53-di-模块模板)。

### 2.4 全局挂载 (App Hook)
1. **App.kt**: 自动探测 `initKoin` 方法，在 `modules(...)` 列表中追加 `xxxModule` 并补充 `import`。

## 3. 质量红线 (Quality Redline)

- ❌ **禁止包含业务代码**：仅产生 DI 和生命周期骨架。
- ❌ **禁止配置文件残留**：`proguard-rules.pro` 必须保持“出厂设置”级别的洁净。
- ❌ **禁止空头引用**：所有修改 Gradle 配置的操作必须基于物理文件夹已存在的保障。

## 4. 常用指令 (AI Instructions)

- **指令：`初始化 Lib 模块 <Name>`**：启动全自动生产线。

## 5. 标准模板库 (Standard Templates)

### 5.1 build.gradle.kts 模板
```kotlin
import com.frame.basic.buildsrc.ProjectBuildConfig
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
    alias(libs.plugins.koinCompiler)
    alias(libs.plugins.kotlinCocoapods)
}
val androidNameSpace = "com.basic.<suffix>"
kotlin {
    androidLibrary {
        namespace = androidNameSpace
        compileSdk = ProjectBuildConfig.Build.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.minSdkVersion
        androidResources.enable = true
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("proguard-rules.pro"))
        }
    }
    listOf(iosArm64(), iosSimulatorArm64())
    cocoapods {
        summary = androidNameSpace
        homepage = "https://www.example.com"
        version = "1.0"
        ios.deploymentTarget = "15.0"
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.multiplatform.components)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                api(project(":shared_base"))
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
                api(project(":shared_base"))
            }
        }

        iosMain {
            dependencies {
                api(project(":shared_base"))
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
```

### 5.2 SPI 生命周期模板
- **commonMain (`di/impl/ApplicationServiceImpl.kt`)**:
```kotlin
package com.basic.<suffix>.di.impl
import com.basic.base.di.service.ApplicationService
expect val applicationServiceImpl : ApplicationService
```
- **androidMain (`di/impl/ApplicationServiceImpl.android.kt`)**:
```kotlin
package com.basic.<suffix>.di.impl
import com.basic.base.di.service.ApplicationService
actual val applicationServiceImpl: ApplicationService = object : ApplicationService {
    override fun onCreate() {}
    override fun onBackground() {}
    override fun onForeground() {}
}
```
- **iosMain (`di/impl/ApplicationServiceImpl.ios.kt`)**:
```kotlin
package com.basic.<suffix>.di.impl
import com.basic.base.di.service.ApplicationService
actual val applicationServiceImpl: ApplicationService = object : ApplicationService {
    override fun onCreate() {}
    override fun onBackground() {}
    override fun onForeground() {}
}
```

### 5.3 DI 模块模板
- **commonMain (`di/DI.kt`)**:
```kotlin
package com.basic.<suffix>.di
import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.<suffix>.di.impl.applicationServiceImpl
import org.koin.dsl.module
val <suffix>Module = module {
    registerSPI<ApplicationService>{ applicationServiceImpl }
}
```
