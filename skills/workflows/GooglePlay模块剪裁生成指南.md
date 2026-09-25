# Google Play 模块剪裁生成指南

> 本指南定义了如何对指定的源模块（Lib 模块或 Project 模块）进行剪裁，生成 `<目标模块>-china` 和 `<目标模块>-play` 两个发行渠道模块，实现编译期包体与依赖裁剪。

---

## 1. 触发条件与概念定义 (Trigger & Concepts)

### 1.1 触发场景
- 当需要将某一源模块（如 `libs/xxx` 或 `project/xxx`）中存在国内（China）与海外（Google Play）依赖/能力差异的部分进行拆分时。
- 命令行或用户指令示例：
  - “对 `project/main` 进行 Google Play 模块剪裁”
  - “剪裁 `libs/pay` 模块为 china/play 渠道”
  - “为 `<源模块>` 生成 China / Play 渠道拆分模块”

### 1.2 剪裁母版 (Mother Templates)
剪裁生成的渠道模块以原模块的类型（**Lib 母版** 或 **Project 母版**）作为基础架构规范：
- **Lib 母版剪裁**：以 Lib 模版为母版。保留多平台规范（含 Android 与 iOS），将母版中的 `api(projects.core.base)` 替换为依赖源模块 `api(projects.libs.<源模块Accessor>)`。
- **Project 母版剪裁**：以 Project 模版为母版。保留多平台规范（含 Android 与 iOS），将母版中的 `api(projects.core.common)` 替换为依赖源模块 `api(projects.project.<源模块Accessor>)`。

---

## 2. 核心架构与设计规范 (Core Architecture)

### 2.1 物理路径与命名规则
假设源模块路径为 `<modulePath>`（如 `project/main` 或 `libs/pay`），其 Android namespace 为 `<parentNamespace>`（如 `com.basic.main` 或 `com.basic.pay`）：

| 概念 | 规范与模板 | 示例（以 `project/main` 为例） |
| :--- | :--- | :--- |
| **源模块路径 / Gradle path** | `<modulePath>` / `:<moduleGradlePath>` | `project/main` / `:project:main` |
| **China 模块路径 / Gradle path** | `<modulePath>-china` / `:<moduleGradlePath>-china` | `project/main-china` / `:project:main-china` |
| **Play 模块路径 / Gradle path** | `<modulePath>-play` / `:<moduleGradlePath>-play` | `project/main-play` / `:project:main-play` |
| **剪裁模块 Namespace** | `<parentNamespace>.distribution` | `com.basic.main.distribution` |
| **Common 源码包路径** | `src/commonMain/kotlin/<distributionPackagePath>/` | `src/commonMain/kotlin/com/basic/main/distribution/` |
| **iOS 源码包路径** | `src/iosMain/kotlin/<distributionPackagePath>/` | `src/iosMain/kotlin/com/basic/main/distribution/` |
| **Distribution Service 接口** | `<CapabilityPascal>DistributionService`（位于源模块 `di/service/`） | `MainDistributionService` |
| **Service 多平台实现变量** | `<capabilityCamel>DistributionServiceImpl` | `mainDistributionServiceImpl` |
| **Android & iOS 统一 DI 变量名** | `<capabilityCamel>DistributionModule` | `mainDistributionModule` |

### 2.2 多平台范围与路径一致性规范
- **对齐多平台配置**：China 模块 (`<源模块>-china`) 与 Play 模块 (`<源模块>-play`) **均保留完整的多平台能力**（包含 Android 与 iOS）。构建脚本均包含 `iosArm64()`、`iosSimulatorArm64()` 及对应的 `commonMain` / `androidMain` / `iosMain` 结构。
- **物理路径完全对齐**：`-china` 与 `-play` 两个剪裁模块的文件路径、Package 命名、DI 变量名必须完全一致，确保在 App 模块中可以无需分支代码直接挂载。

---

## 3. 分层代码架构与注入规范 (SPI & DI Architecture)

采用 **源模块定义接口 + 剪裁模块实现接口 + App 统一 commonMain 注入** 的分层设计：

### 3.1 源模块（Source Module）
位于 `<modulePath>`：
声明 Service 接口：位于 `src/commonMain/kotlin/<parentNamespacePath>/di/service/<CapabilityPascal>DistributionService.kt`
```kotlin
package <parentNamespace>.di.service

/**
 * <CapabilityPascal> 发行渠道公共能力接口。
 */
interface <CapabilityPascal>DistributionService
```

### 3.2 剪裁模块（Clipped Modules: China / Play）
分别位于 `<modulePath>-china` 和 `<modulePath>-play`（两者物理文件路径完全一致）：
1. **ApplicationService 基础实现**：位于 `src/commonMain/kotlin/<distributionPackagePath>/di/impl/ApplicationServiceImpl.kt`
   ```kotlin
   package <distributionNamespace>.di.impl

   import com.basic.base.di.service.ApplicationService

   class ApplicationServiceImpl : ApplicationService {
       override fun onCreate(isMainProcess: Boolean) = Unit
       override fun onBackground() = Unit
       override fun onForeground() = Unit
   }
   ```
2. **DistributionService 接口与多平台实现**：
   - **`commonMain` 声明**：位于 `src/commonMain/kotlin/<distributionPackagePath>/di/impl/<capabilityCamel>DistributionServiceImpl.kt`
     ```kotlin
     package <distributionNamespace>.di.impl

     import <parentNamespace>.di.service.<CapabilityPascal>DistributionService

     expect val <capabilityCamel>DistributionServiceImpl: <CapabilityPascal>DistributionService
     ```
   - **`androidMain` 实现**：位于 `src/androidMain/kotlin/<distributionPackagePath>/di/impl/<capabilityCamel>DistributionServiceImpl.android.kt`
     ```kotlin
     package <distributionNamespace>.di.impl

     import <parentNamespace>.di.service.<CapabilityPascal>DistributionService

     actual val <capabilityCamel>DistributionServiceImpl: <CapabilityPascal>DistributionService = object : <CapabilityPascal>DistributionService {
         // 默认空实现或 Android 特定发行渠道能力
     }
     ```
   - **`iosMain` 实现**：位于 `src/iosMain/kotlin/<distributionPackagePath>/di/impl/<capabilityCamel>DistributionServiceImpl.ios.kt`
     ```kotlin
     package <distributionNamespace>.di.impl

     import <parentNamespace>.di.service.<CapabilityPascal>DistributionService

     actual val <capabilityCamel>DistributionServiceImpl: <CapabilityPascal>DistributionService = object : <CapabilityPascal>DistributionService {
         // 默认空实现或 iOS 特定发行渠道能力
     }
     ```
3. **DI 模块注入**：位于 `src/commonMain/kotlin/<distributionPackagePath>/di/DI.kt`
   ```kotlin
   package <distributionNamespace>.di

   import com.basic.base.di.service.ApplicationService
   import com.basic.base.spi.registerSPI
   import <parentNamespace>.di.service.<CapabilityPascal>DistributionService
   import <distributionNamespace>.di.impl.<capabilityCamel>DistributionServiceImpl
   import <distributionNamespace>.di.impl.ApplicationServiceImpl
   import org.koin.dsl.module

   val <capabilityCamel>DistributionModule = module {
       registerSPI<ApplicationService> { ApplicationServiceImpl() }
       registerSPI<<CapabilityPascal>DistributionService> {
           <capabilityCamel>DistributionServiceImpl
       }
   }
   ```
   *注意：China 与 Play 两个剪裁模块的 DI.kt 及变量完全对齐，均统一引用 `<capabilityCamel>DistributionServiceImpl`。*

4. **iOS 源码目录**：位于 `src/iosMain/kotlin/<distributionPackagePath>/`（含 `.gitkeep` 或 iOS 平台特定实现）。

### 3.3 App 模块统一注册 (App Common Hooks)

1. **`settings.gradle.kts` 依赖包含**：
   在根目录 `settings.gradle.kts` 中包含剪裁出来的两个渠道模块：
   ```kotlin
   include("<moduleGradlePath>-china")
   include("<moduleGradlePath>-play")

   // 示例 1（core/common 模块）：
   // include("core:common-china")
   // include("core:common-play")

   // 示例 2（project/main 模块）：
   // include("project:main-china")
   // include("project:main-play")
   ```

2. **`app/build.gradle.kts` 渠道依赖配置**：
   在 `app/build.gradle.kts` 的 `sourceSets` 中根据 `isPlayDimension()` 动态引入对应的渠道模块：
   ```kotlin
   if (isPlayDimension()){
       api(projects.<modulePlayAccessor>)
   } else {
       api(projects.<moduleChinaAccessor>)
   }

   // 示例：
   // if (isPlayDimension()){
   //     api(projects.project.mainPlay)
   //     api(projects.core.commonPlay)
   // } else {
   //     api(projects.project.mainChina)
   //     api(projects.core.commonChina)
   // }
   ```

3. **`commonMain` 统一 Koin 注册 (`app/src/commonMain/kotlin/com/basic/app/App.kt`)**：
   直接在 `commonMain` 的 `initKoin()` 中注册渠道模块（不需要在 Android/iOS 宿主中单独注入平台模块）：
   ```kotlin
   import <distributionNamespace>.di.<capabilityCamel>DistributionModule

   fun initKoin() {
       startKoin {
           modules(
               commonModule,
               mainModule,
               <capabilityCamel>DistributionModule
           )
       }
   }
   ```

4. **宿主入口无参调用**：
   Android 端的 `Application.kt` 与 iOS 端的 `AppDelegate.kt` 均只需无参调用 `initKoin()`。

---

## 4. 自动化构建配置模板 (Gradle Templates)

`China` 模块与 `Play` 模块基于对应母版完整保留 iOS/Android 平台能力，`api` 替换为源模块：

```kotlin
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
    id("com.basic.protect-src")
}

val androidNameSpace = "<parentNamespace>.distribution"

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
                api(projects.<源模块Accessor>) // Lib母版为 api(projects.libs.<Accessor>)，Project母版为 api(projects.project.<Accessor>)
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
                api(projects.<源模块Accessor>)
            }
        }

        iosMain {
            dependencies {
                api(projects.<源模块Accessor>)
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

buildkonfig {
    packageName = "buildkonfig"
    exposeObjectWithName = androidNameSpace.toBuildConfigClassName()
    defaultConfigs {}
}
```

---

## 5. 自动化生成步骤 (Pipeline Execution Steps)

收到剪裁模块创建指令时（如 `剪裁 project/main 模块`），AI 必须连续执行以下步骤：

1. **识别源模块与母版类型**：
   - 确定源模块路径 `<modulePath>`，判断是 Lib 模块 (`libs/xxx`) 还是 Project 模块 (`project/xxx`)。
   - 读取源模块 namespace `<parentNamespace>`。
2. **创建剪裁模块物理树**：
   - 创建 `<modulePath>-china/` 和 `<modulePath>-play/`。
   - 分别创建 `libs/android/` 与 `libs/ios/framework/` 目录及 `.gitignore`。
   - 分别创建 `proguard-rules.pro` 与基础 `src/androidMain/AndroidManifest.xml`。
   - 物理源码路径必须一致：
     - Common 源码路径：`src/commonMain/kotlin/<distributionPackagePath>/`（建立 `di/` 与 `di/impl/`）。
     - iOS 源码路径：`src/iosMain/kotlin/<distributionPackagePath>/`（含 `.gitkeep`）。
3. **注入 build.gradle.kts 与 settings.gradle.kts**：
   - 两个渠道模块均注入包含 Android/iOS 的多平台 `build.gradle.kts`（模板 4）。
   - 在 `settings.gradle.kts` 中添加 `include("<moduleGradlePath>-china")` 和 `include("<moduleGradlePath>-play")`。
4. **生成代码骨架**：
   - 在源模块 `di/service/` 下创建 `<CapabilityPascal>DistributionService.kt` 接口。
   - 在 China 和 Play 模块中生成 `ApplicationServiceImpl.kt` 及 `DI.kt`。
   - 对于 `DistributionService` 的实现：
     - 在 `commonMain` 中生成 `expect val <capabilityCamel>DistributionServiceImpl: <CapabilityPascal>DistributionService`。
     - 在 `androidMain` 与 `iosMain` 中生成对应平台的 `actual val` 实现（默认提供空实现对象或平台特化实现）。
5. **App 模块统一挂载**：
   - 在 `settings.gradle.kts` 中配置 `include(...)`；在 `app/build.gradle.kts` 的 `sourceSets` 中使用 `if (isPlayDimension())` 判定引入对应渠道依赖（如 `api(projects.xxxPlay)` / `api(projects.xxxChina)`）。
   - 在 `app/src/commonMain/kotlin/com/basic/app/App.kt` 的 `initKoin()` 中直接注册 `<capabilityCamel>DistributionModule`。
   - Android `Application.kt` 与 iOS `AppDelegate.kt` 中均无参调用 `initKoin()`。
6. **校验与构建**：
   - 执行 `:app:assemblePlayDebug` 与 `:app:assembleChinaDebug` 验证工程无误。

---

## 6. 质量红线 (Quality Redline)

- ❌ **剪裁模块平台与路径必须完全一致**：`-china` 与 `-play` 必须具备完全相同的多平台 Target（Android + iOS）、包名路径及变量定义。
- ❌ **Kotlin 源码统一置于 commonMain**：通用代码、DI 及 Services 实现必须放在 `src/commonMain/kotlin/` 下，严禁误写入 `src/androidMain/kotlin/`。
- ❌ **禁止在平台宿主重复注入**：App 模块的 Android/iOS 入口统一调用 `initKoin()`，不得在各自平台中二次注册渠道模块。
- ❌ **禁止循环依赖与跨渠道引用**：源模块不能反向依赖剪裁模块，渠道专属依赖只置于对应剪裁模块中。
- ✅ **遵守通用代码规范**：新建 Kotlin 文件需写明文件头注释、KDoc 与注释。
