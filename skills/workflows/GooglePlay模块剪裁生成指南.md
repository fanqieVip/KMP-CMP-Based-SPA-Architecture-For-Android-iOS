# Google Play 模块剪裁生成指南

> 本指南用于把存在 China / Google Play 平台依赖差异的能力拆成两个 Android 实现模块，并由 App 的 product flavor 在构建期选择其一。默认只生成**空协议、空实现和 DI 骨架**；不得把设备标识、OAID、GMS 或任何业务能力擅自写入模板。

## 1. 触发范围与关键词

下列表述均应加载本指南：

- 为 `<模块>` 生成 Google Play 剪裁模块、Play 版模块或海外版模块。
- 为 `<模块>` 拆分 China / Play、国内 / 海外、国内 / Google Play 实现。
- 按发行渠道隔离 `<SDK/权限/依赖/功能>`，或为 Google Play 移除中国渠道依赖。
- 为 `<模块>` 增加发行渠道 flavor 实现、渠道包体裁剪、渠道专属依赖。

不适用：仅隐藏 UI、仅修改一个 Manifest 属性、没有 China / Play 二选一实现的普通功能。这些任务不应为了“预留”而新建空模块。

## 2. 目标与边界

目标是在**编译依赖图**中裁掉不属于当前发行渠道的代码和三方依赖：

```text
<modulePath>                      # 公共协议；如需 iOS，实现也留在这里
        ↑
<modulePath>-china                # 仅 Android，国内实现和国内专属依赖
<modulePath>-play                 # 仅 Android，Google Play 实现和 Play 专属依赖
        ↑（App flavor 二选一）
app: chinaImplementation / playImplementation
```

- `<modulePath>` 不能依赖 `-china` 或 `-play`，否则会形成循环依赖。
- 选择实现的依赖只能放在 `app` 的 `chinaImplementation` / `playImplementation`。
- 不能在 `<modulePath>`、`core/base` 或其他公共模块里通过任务名、`isPlayDimension()` 或 `if` 动态依赖两个子模块。
- 两个渠道模块可以拥有相同的 Kotlin 包名和 DI 入口；因为一个 App 变体的 classpath 中只能存在其中一个。
- 新模块默认只放渠道差异能力。公共 API、跨端模型和通用业务逻辑仍留在父模块。

## 3. 命名与目录模板

从用户指定的目标模块推导以下变量：`<modulePath>` 是不带前导冒号的文件系统路径，例如 `core/common` 或 `project/main`；`<moduleGradlePath>` 是对应 Gradle path，例如 `core:common` 或 `project:main`；`<capability>` 是末级模块名，标准化为 kebab-case；`<CapabilityPascal>` 是其 PascalCase 形式；`<parentNamespace>` 是目标模块现有的 Android namespace；`<distributionNamespace>` 固定为 `<parentNamespace>.distribution`；`<distributionPackagePath>` 是将 `<distributionNamespace>` 的点替换为斜杠后的源码路径。

| 项目 | 模板 |
| :--- | :--- |
| 公共模块 | `<modulePath>` |
| China 模块目录 / Gradle path | `<modulePath>-china` / `:<moduleGradlePath>-china` |
| Play 模块目录 / Gradle path | `<modulePath>-play` / `:<moduleGradlePath>-play` |
| Android namespace | `<parentNamespace>.distribution` |
| Kotlin 实现包 / 物理目录 | `<distributionNamespace>` / `src/androidMain/kotlin/<distributionPackagePath>/` |
| 公共 Service 包 / 物理目录 | `<parentNamespace>.di.service` / `<modulePath>/src/commonMain/kotlin/<parentNamespacePath>/di/service/` |
| 公共协议 | `<CapabilityPascal>DistributionService`（位于 `di/service`） |
| China 实现 | `China<CapabilityPascal>DistributionServiceImpl` |
| Play 实现 | `Play<CapabilityPascal>DistributionServiceImpl` |
| iOS 实现（需要时） | `Ios<CapabilityPascal>DistributionServiceImpl` |
| Android DI 变量 | `<capabilityCamel>DistributionModule` |
| iOS DI 变量 | `<capabilityCamel>IosModule` |

例如，目标模块 namespace 为 `com.basic.demo` 时，公共 Service 接口位于 `src/commonMain/kotlin/com/basic/demo/di/service/`，两个渠道模块的 namespace 均为 `com.basic.demo.distribution`，Service 实现源码必须位于 `src/androidMain/kotlin/com/basic/demo/distribution/di/impl/`。目标模块为 `core/common` 时，两个同级模块就是 `core/common-china` 与 `core/common-play`，命名分别为：`CommonDistributionService`、`ChinaCommonDistributionServiceImpl`、`PlayCommonDistributionServiceImpl`、`IosCommonDistributionServiceImpl`、`distributionModule`、`commonIosModule`。

每个新增渠道模块必须一次性创建以下完整骨架，不能遗漏空目录和发布配置：

```text
<modulePath>-china/
├── .gitignore
├── build.gradle.kts
├── proguard-rules.pro
├── libs/android/.gitkeep
└── src/androidMain/
    ├── AndroidManifest.xml
    └── kotlin/<distributionPackagePath>/
        └── di/
            ├── DI.kt
            └── impl/
                ├── ApplicationServiceImpl.kt
                └── China<CapabilityPascal>DistributionServiceImpl.kt

<modulePath>-play/
├── .gitignore
├── build.gradle.kts
├── proguard-rules.pro
├── libs/android/.gitkeep
└── src/androidMain/
    ├── AndroidManifest.xml
    └── kotlin/<distributionPackagePath>/
        └── di/
            ├── DI.kt
            └── impl/
                ├── ApplicationServiceImpl.kt
                └── Play<CapabilityPascal>DistributionServiceImpl.kt
```

`.gitignore` 固定为：

```gitignore
/build
/.gradle
```

`proguard-rules.pro` 默认只保留说明注释；未有真实 SDK 规则时不得编造 keep 规则。`AndroidManifest.xml` 默认只保留基础 `manifest` 节点。`libs/android/.gitkeep` 用于让空目录可被 Git 保留；本地 AAR/JAR 到位后放在该目录，并由 Gradle 的 `compileOnly(fileTree(...))` 接入。

Kotlin 的 `package` 与物理文件夹必须逐段一致。例如父模块 namespace 为 `com.basic.demo` 时，公共 Service 接口的 package 是 `com.basic.demo.di.service`，位于 `src/commonMain/kotlin/com/basic/demo/di/service/`；渠道 Service 实现的 package 是 `com.basic.demo.distribution.di.impl`，DI 的 package 是 `com.basic.demo.distribution.di`，分别位于渠道模块的 `src/androidMain/kotlin/com/basic/demo/distribution/di/impl/` 与其 `di/` 子目录；不得仍使用 `com.basic.<capability>` 之类的固定路径。

## 4. 协议、实现与 DI 模板

### 4.1 公共协议：默认为空

在 `<modulePath>/src/commonMain/kotlin/<parentNamespacePath>/di/service/` 定义协议，package 为 `<parentNamespace>.di.service`。新建时接口没有成员；只有用户明确要求某项渠道差异能力时，才把那项能力加到协议中。

```kotlin
package <parentNamespace>.di.service

interface <CapabilityPascal>DistributionService
```

不得把 DeviceId、OAID、Android ID、广告 ID、权限、SDK 初始化或业务方法作为模板默认内容。若后续新增方法，必须同步补齐 China、Play 和 iOS（如适用）实现，并重新做隐私与包体依赖审计。

### 4.2 空实现：默认不引入任何专属依赖

```kotlin
class China<CapabilityPascal>DistributionServiceImpl : <CapabilityPascal>DistributionService

class Play<CapabilityPascal>DistributionServiceImpl : <CapabilityPascal>DistributionService
```

上述 Service 实现均放在 `<distributionNamespace>.di.impl` 包；若公共模块支持 iOS，同样提供空的 `Ios<CapabilityPascal>DistributionServiceImpl`，并注册到 iOS Koin 模块。iOS 不依赖 China / Play Android 子模块。

所有新 Kotlin 文件必须遵循项目通用代码规范，包含文件头、KDoc 和真实作者/时间；不能直接复制示例中的占位元数据。

### 4.3 应用生命周期服务与 DI 模板

每个 China / Play 模块都必须提供一个同包名的空 `ApplicationServiceImpl`，作为该模块接入应用生命周期的标准入口。两个实现可拥有相同的全限定名，因为 App 每次只会选择一个渠道模块；未有真实需求时，不能在生命周期方法中加入任何业务、SDK 初始化或权限行为。

```kotlin
package <distributionNamespace>.di.impl

import com.basic.base.di.service.ApplicationService

class ApplicationServiceImpl : ApplicationService {
    override fun onCreate(isMainProcess: Boolean) = Unit

    override fun onBackground() = Unit

    override fun onForeground() = Unit
}
```

若渠道能力需要处理 Activity Intent 或 iOS Scene 回调，只能在用户明确要求后覆盖 `ApplicationService` 的对应方法；Android 渠道模块不应替 iOS 添加实现。

China 和 Play 的 `DI.kt` 使用相同 package、相同变量名；除绑定各自 Distribution Service 外，必须注册模块自身的 `ApplicationServiceImpl`：

```kotlin
package <distributionNamespace>.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import <parentNamespace>.di.service.<CapabilityPascal>DistributionService
import <distributionNamespace>.di.impl.China<CapabilityPascal>DistributionServiceImpl
import <distributionNamespace>.di.impl.ApplicationServiceImpl
import org.koin.dsl.module

val <capabilityCamel>DistributionModule = module {
    registerSPI<ApplicationService> { ApplicationServiceImpl() }
    registerSPI<<CapabilityPascal>DistributionService> {
        China<CapabilityPascal>DistributionServiceImpl()
    }
}
```

Play 版本只将 Service 实现替换为 `Play<CapabilityPascal>DistributionServiceImpl`，其 `ApplicationServiceImpl` 路径和注册写法保持相同。iOS 的 `<capabilityCamel>IosModule` 在公共模块的 `iosMain` 注册 `Ios<CapabilityPascal>DistributionServiceImpl`；只有 iOS 也存在对应生命周期行为时，才在公共模块的 iOS 实现中注册 iOS 生命周期服务。

## 5. Gradle 与 App 接入模板

### 5.1 Settings

```kotlin
include("<moduleGradlePath>-china")
include("<moduleGradlePath>-play")
```

### 5.2 渠道模块基础依赖

两个渠道模块都使用完整的 Android-only KMP 库基线。以下配置是创建渠道模块时的必备项：Android 资源、Java API、源码 Jar、消费者混淆规则、Compose 资源公开 `R` 类，以及 Compose / KSP / 序列化 / Ktorfit / 源码保护插件必须在 China 与 Play 模块保持对称。

```kotlin
import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.ktx.toBuildConfigClassName
import com.frame.basic.ktx.toResourceClassName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
val kspAndroidMainGeneratedSources = layout.buildDirectory.dir(
    "generated/ksp/android/androidMain/kotlin"
)

kotlin {
    androidLibrary {
        namespace = androidNameSpace
        minSdk = ProjectBuildConfig.Build.Android.minSdkVersion
        compileSdk = ProjectBuildConfig.Build.Android.compileSdkVersion
        androidResources.enable = true
        withJava()
        withSourcesJar(true)
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("proguard-rules.pro"))
        }
    }

@OptIn(ExperimentalKotlinGradlePluginApi::class)
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
        api(projects.<moduleAccessor>)
        implementation(libs.compose.multiplatform.components)
        implementation(libs.koin.core)
        implementation(libs.koin.annotations)
        implementation(libs.koin.compose)
        implementation(libs.koin.android)
    }

    getByName("androidMain").generatedKotlin.srcDir(kspAndroidMainGeneratedSources)
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

China-only Maven/AAR/Manifest/ProGuard 内容只进入 `-china`；Play-only Maven/AAR/Manifest/ProGuard 内容只进入 `-play`。公共模块、App 的非 flavor 依赖和另一渠道模块都不得带入这些内容。

上述是新渠道模块的固定编译与发布基线。具体能力额外需要的 Maven/AAR、权限、Manifest、ProGuard 或业务依赖，再分别添加到对应渠道模块；不得用删减这组基线的方式处理渠道差异。

使用 Ktorfit 或其他 KSP 生成代码时，必须保留 `generatedKotlin.srcDir(kspAndroidMainGeneratedSources)`。它会把 Android KSP 输出登记为 IDE 的生成源码根目录，确保生成的 API 工厂可解析、可跳转，而不仅是在 Gradle 编译时可见。

### 5.3 App flavor 与选择依赖

若 App 尚未定义发行维度，创建：

```kotlin
flavorDimensions += "distribution"
productFlavors {
    create("china") { dimension = "distribution" }
    create("play") { dimension = "distribution" }
}
```

App 根 `dependencies` 中选择实现：

```kotlin
add("chinaImplementation", projects.<moduleChinaAccessor>)
add("playImplementation", projects.<modulePlayAccessor>)
```

App 的 Android `Application` 传入 `<capabilityCamel>DistributionModule`；共享 `initKoin` 应接收 `vararg Module`。iOS `AppDelegate` 传入 `<capabilityCamel>IosModule`。Android Gradle Plugin 会自动识别 `src/china`、`src/play`，不要额外添加冗余 `sourceSets { getByName("china").java.srcDir(...) }`。

## 6. 执行顺序与验收

1. 先确认目标能力确实存在 China / Play 的编译依赖差异，并识别要迁移的源码、Maven/AAR、Manifest、ProGuard 和资源。
2. 创建两个模块的完整目录骨架（含 `libs/android/.gitkeep`、Manifest、ProGuard、`.gitignore`）。
3. 创建空协议、空实现和 DI；没有用户明确要求时不写任何实际能力。
4. 接入 `settings.gradle.kts`、App flavors 和 `chinaImplementation` / `playImplementation`。
5. 将原先位于公共模块的渠道专属依赖、实现、Manifest 或 ProGuard 规则迁移到对应子模块；确认公共模块不再引用它们。
6. 分别执行 `:app:assemblePlayDebug` 与 `:app:assembleChinaDebug`。若项目的构建保护禁止同一 Gradle 命令混用两个渠道任务，必须分两次执行。
7. 检查 `git diff --check`、渠道模块目录完整性、旧渠道专属依赖是否仍残留在公共模块，以及 Play 产物是否没有 China 专属依赖。

交付时应列出：新增模块、裁掉/迁移的依赖、两个变体的构建结果，以及仍需用户决定的实际能力和隐私配置。
