# Lib 模版生成指南

> 本文档定义了如何快速自动化创建一个三方 SDK 适配模块（如 WeChat, QQ, SensorsData 等）。AI 必须严格遵守此指南，并使用内置的模板代码，严禁依赖外部不确定的文件夹。

## 1. 命名与目录规范 (Naming Convention)

- **模糊指令问询**: 当用户只说“新建一个 xxx 模块”“创建 xxx 模块”“初始化 xxx 模块”等，没有明确 `lib`、`libs`、SDK、三方平台接入等语义时，AI 必须先弹出对话框询问模块类型：`Lib 模块` 还是 `Project 模块`，不得直接按 Lib 创建。
- **Lib 触发条件**: 只有用户明确提到 `lib_xxx`、`libs:<name>`、`libs/<name>`、三方 SDK、平台 SDK、统计/登录/支付/广告/归因等 SDK 适配语义时，才直接使用本指南。
- **模块路径**: SDK 模块统一收敛到 `libs/<name>/`，Gradle path 为 `:libs:<name>`，不得继续在根目录平铺新增 `lib_*` 模块。
- **模块命名**: `<name>` 必须取原 `lib_xxx` 模块名去掉 `lib_` 后的 `xxx`，并保留原大小写，例如 `lib_geyan` -> `libs/geyan`、`lib_openInstall` -> `libs/openInstall`。
- **Android Namespace**: 低风险迁移与现有模块可继续使用 `com.basic.<suffix>`（如 `com.basic.geyan`）；新增模块默认与 `<name>` 保持语义一致，不得按“认证/统计/广告”等能力域另起目录名。
- **源码路径**: 物理文件夹路径必须匹配 namespace，例如 `src/commonMain/kotlin/com/basic/xxx/`。

## 2. 自动化生产线 (Automation Pipeline)

当收到“初始化 Lib 模块”指令时，AI 必须连续执行以下步骤：

### 2.1 物理结构生成 (Physical Tree)
1. 创建模块根目录 `libs/<name>/`。
2. 创建 `libs/android/` 目录。
3. 创建标准源码树：
    - `src/commonMain/kotlin/com/basic/xxx/di/impl/`
    - `src/androidMain/kotlin/com/basic/xxx/di/impl/`
    - `src/iosMain/kotlin/com/basic/xxx/di/impl/`
4. 创建模块 `.gitignore`，内容必须使用 [模块 .gitignore 模板](#54-模块-gitignore-模板)。
5. 创建基础清单：`src/androidMain/AndroidManifest.xml` (仅含基础 `manifest` 节点)。
6. 创建基础混淆：`proguard-rules.pro` (仅含默认注释)。

### 2.2 构建配置注入 (Gradle & Settings)
1. **build.gradle.kts**: 必须使用 [内置 Gradle 模板](#51-buildgradlekts-模板)。
2. **settings.gradle.kts**: 自动添加 `include("libs:<name>")`。
3. **core/common/build.gradle.kts**: 只有该能力需要对公共业务层可见时，才在 `commonMain`、`androidMain`、`iosMain` 依赖块中添加 `api(projects.libs.<nameAccessor>)`；未接入业务链路的 SDK 模块只 include，不强行暴露。
4. **Android 本地 SDK**: AAR/JAR 必须放在 `libs/<name>/libs/android/`，并使用模板中的 `compileOnly(fileTree(...))`。这是本项目框架规范，框架会处理最终依赖打包，AI 不得擅自改成 `implementation(files(...))`、`api(files(...))` 或复制到 app 模块。
5. **iOS Info.plist 参数**: 模板默认引入 `com.basic.ios`。若 SDK 需要在 iOS `Info.plist` 添加 AppKey、AppId、URL Scheme 等参数，必须在当前 `libs/<name>/build.gradle.kts` 通过 `iosConfig { field(...) }` 声明，字段值统一从 `SDKKeyConfig` 读取，禁止直接硬编码到 `Info.plist`。

### 2.3 代码模板生成 (DI & SPI)
1. **ApplicationService 模板**: 必须使用 [内置 SPI 模板](#52-spi-生命周期模板)。
2. **DI 模块模板**: 必须使用 [内置 DI 模板](#53-di-模块模板)。

### 2.4 全局挂载 (App Hook)
1. **App.kt**: 自动探测 `initKoin` 方法，在 `modules(...)` 列表中追加 `xxxModule` 并补充 `import`。

## 3. 质量红线 (Quality Redline)

- ❌ **禁止包含业务代码**：仅产生 DI 和生命周期骨架。
- ❌ **禁止配置文件残留**：`proguard-rules.pro` 必须保持“出厂设置”级别的洁净。
- ❌ **禁止空头引用**：所有修改 Gradle 配置的操作必须基于物理文件夹已存在的保障。
- ✅ **新增 Activity 父类默认值**：SDK 模块后续若新增 Android `Activity` 承载 SDK 页面、回调页、自定义授权页或透明中转页，默认继承 `androidx.fragment.app.FragmentActivity`，除非官方 SDK 明确要求其他父类或必须避免 AndroidX Fragment 依赖。

## 4. 常用指令 (AI Instructions)

- **指令：`初始化 Lib 模块 <Name>`**：启动全自动生产线；执行前必须先按 SDK 名确定 `<name>`，迁移既有 `lib_xxx` 时必须使用原 `xxx`。
- **指令：`新建 <Name> 模块`**：若未明确模块类型，必须先询问 `Lib 模块` 或 `Project 模块`，不得自行推断。

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
    id("com.basic.ios")
}
val androidNameSpace = "com.basic.<suffix>"

// SDK 需要写入 iOS Info.plist 参数时启用，字段值必须来自 SDKKeyConfig。
// import com.frame.basic.buildsrc.SDKKeyConfig
// iosConfig {
//     field("<SDK_IOS_KEY>", SDKKeyConfig.<Sdk>.IOS.<key>)
// }

kotlin {
    androidLibrary {
        namespace = androidNameSpace
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
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
        ios.deploymentTarget = ProjectBuildConfig.Build.Ios.deploymentTarget
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.multiplatform.components)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                api(projects.core.base)
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
                api(projects.core.base)
            }
        }

        iosMain {
            dependencies {
                api(projects.core.base)
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

### 5.4 模块 .gitignore 模板

```gitignore
/build
/.gradle
```
