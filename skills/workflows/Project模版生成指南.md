# Project 模版生成指南

> 本文档定义了如何快速自动化创建一个业务 Project 模块。Project 模块用于承载业务页面、路由、业务 DI 与业务服务实现，统一收敛到 `project/<name>/` 目录下。

## 1. 命名与目录规范 (Naming Convention)

- **模糊指令问询**: 当用户只说“新建一个 xxx 模块”“创建 xxx 模块”“初始化 xxx 模块”等，没有明确 `project`、业务页面、业务功能等语义时，AI 必须先弹出对话框询问模块类型：`Project 模块` 还是 `Lib 模块`，不得直接按 Project 创建。
- **Project 触发条件**: 只有用户明确提到 `project_xxx`、`project:<name>`、`project/<name>`、业务模块、业务页面模块或明确要求放入 `project/` 目录时，才直接使用本指南。
- **模块路径**: 业务 Project 模块统一收敛到 `project/<name>/`，Gradle path 为 `:project:<name>`。
- **模块命名**: `<name>` 必须取原始输入中的业务名：
  - 输入 `project_xxx` 时，模块名取去掉 `project_` 后的 `xxx`，例如 `project_pay` -> `project/pay`。
  - 输入 `project:<name>`、`project/<name>` 或明确的 `<name>` 时，保留 `<name>` 原大小写。
  - 输入 `project` 且未说明具体名字时，必须先询问模块名；不得默认覆盖或复用 `project/main`。
- **Android Namespace**: 默认使用 `com.basic.<name>`；若 `<name>` 为 `main`，沿用 `com.basic.project`。
- **源码路径**: 默认匹配 namespace，例如 `src/commonMain/kotlin/com/basic/<name>/`；`main` 模块默认使用 `com/basic/project/`。
- **命名问询规则**:
  - 名字明确时不得二次改名。
  - 名字不明确时只问一个问题：确认最终 `<name>`。
  - 目标目录 `project/<name>` 已存在时必须停止并询问是否复用、取消或选择新名字；默认不覆盖。

## 2. 自动化生产线 (Automation Pipeline)

当收到“新建 project 模块”“新建 project_xxx 模块”“初始化 Project 模块”或类似指令时，AI 必须连续执行以下步骤。

### 2.1 物理结构生成 (Physical Tree)

1. 创建模块根目录 `project/<name>/`。
2. 创建标准源码树：
   - `src/commonMain/kotlin/com/basic/<suffix>/di/impl/`
   - `src/commonMain/composeResources/values/`
   - `src/androidMain/`
3. 创建模块 `.gitignore`，内容必须使用 [模块 .gitignore 模板](#58-模块-gitignore-模板)。
4. 创建基础清单：`src/androidMain/AndroidManifest.xml`。
5. 创建基础混淆：`proguard-rules.pro`。
6. `composeResources/values/strings.xml` 仅在模块确实需要字符串资源时创建；模板生成阶段不默认创建占位字符串。

### 2.2 构建配置注入 (Gradle & Settings)

1. **build.gradle.kts**: 必须使用 [内置 Gradle 模板](#51-buildgradlekts-模板)。
2. **settings.gradle.kts**: 自动添加 `include("project:<name>")`。
3. **禁止旧写法**: 不得使用 `include(":project:<name>")`，不得使用 `project(":xxx").projectDir = file(...)`。
4. **app 自动依赖**: 无需询问，必须自动在 `app/build.gradle.kts` 的 `commonMain`、`androidMain`、`iosMain` 依赖块中追加：
   - `api(projects.project.<nameAccessor>)`
5. **本地 Android 包**: 若模块需要本地 AAR/JAR，必须放在 `project/<name>/libs/android/`，并使用模板中的 `compileOnly(fileTree(...))`；不得复制到 app 模块。

### 2.3 代码模板生成 (DI & Service)

1. **DI 模块**: 必须生成 `src/commonMain/kotlin/com/basic/<suffix>/di/DI.kt`。
2. **Service 接口**: 必须在 `core/common/src/commonMain/kotlin/com/basic/common/di/service/` 中生成 `<NamePascal>Service.kt`：
   - 包名固定为 `com.basic.common.di.service`。
   - 接口名为 `<NamePascal>Service`。
   - 若同名 Service 已存在，必须复用，不得重复创建。
3. **Service 实现**: 必须在当前模块 `src/commonMain/kotlin/com/basic/<suffix>/di/impl/` 中生成 `<NamePascal>ServiceImpl.kt`：
   - 实现 `com.basic.common.di.service.<NamePascal>Service`。
   - 实现类名为 `<NamePascal>ServiceImpl`。
4. **路由模块接入**: DI 中必须导入并 include KSP 生成的路由模块：
   - `import com.basic.router.generated.<xxxRouteModule>`
   - `includes(<xxxRouteModule>)`
5. **模块变量命名**: DI module 变量名必须为 `<nameCamel>Module`，例如 `mainModule`、`payModule`、`openInstallModule`。
6. **ApplicationService**: 默认生成 `ApplicationServiceImpl` 并通过 `registerSPI<ApplicationService>{ ApplicationServiceImpl() }` 注册。
7. **业务 Service SPI**: DI 中必须导入 `<NamePascal>Service` 与 `<NamePascal>ServiceImpl`，并注册：
   - `registerSPI<<NamePascal>Service>{ <NamePascal>ServiceImpl() }`

### 2.4 App Koin 自动挂载 (App Hook)

1. 自动探测 `app/src/commonMain/.../App.kt` 中的 `initKoin` 方法。
2. 自动追加 import：
   - `import com.basic.<suffix>.di.<nameCamel>Module`
3. 自动在 `modules(...)` 中追加 `<nameCamel>Module`。
4. app Koin 注入为强制步骤，无需询问。
5. 禁止重复追加 import 或重复追加 module。

## 3. 质量红线 (Quality Redline)

- 禁止创建根目录平铺的 `project_xxx` 模块。
- 禁止把 Project 模块建到 `libs/`、`core/` 或 `shared_*` 目录。
- 禁止空头引用：修改 Gradle 或 app Koin 挂载前必须确保物理模块已创建。
- 生成后必须至少执行 `:project:<name>:compileCommonMainKotlinMetadata`；若已自动接入 app，还必须执行 `:app:compileCommonMainKotlinMetadata`。

## 4. 常用指令 (AI Instructions)

- **指令：`新建 project 模块 <Name>`**：创建 `project/<Name>` 并自动接入 app。
- **指令：`新建 project_xxx 模块`**：创建 `project/xxx` 并自动接入 app。
- **指令：`初始化 Project 模块 <Name>`**：创建 `project/<Name>` 并自动接入 app。
- **指令：`新建 <Name> 模块`**：若未明确模块类型，必须先询问 `Project 模块` 或 `Lib 模块`，不得自行推断。

## 5. 标准模板库 (Standard Templates)

### 5.1 build.gradle.kts 模板

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
}
val androidNameSpace = "com.basic.<suffix>"
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
                api(projects.core.common)
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
                api(projects.core.common)
            }
        }

        iosMain {
            dependencies {
                api(projects.core.common)
            }
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
    defaultConfigs {}
}
```

### 5.2 AndroidManifest.xml 模板

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

### 5.3 ApplicationService 模板

```kotlin
package com.basic.<suffix>.di.impl

import com.basic.base.di.service.ApplicationService

/**
 * <Name> 模块应用生命周期服务实现。
 */
class ApplicationServiceImpl : ApplicationService {
    override fun onCreate() {
    }

    override fun onBackground() {
    }

    override fun onForeground() {
    }
}
```

### 5.4 业务 Service 接口模板

- **core/common (`di/service/<NamePascal>Service.kt`)**:

```kotlin
package com.basic.common.di.service

/**
 * <Name> 业务服务接口。
 */
interface <NamePascal>Service
```

### 5.5 业务 Service 实现模板

- **project/<name> (`di/impl/<NamePascal>ServiceImpl.kt`)**:

```kotlin
package com.basic.<suffix>.di.impl

import com.basic.common.di.service.<NamePascal>Service

/**
 * <Name> 业务服务默认实现。
 */
class <NamePascal>ServiceImpl : <NamePascal>Service
```

### 5.6 DI 模块模板

```kotlin
package com.basic.<suffix>.di

import com.basic.base.di.service.ApplicationService
import com.basic.base.spi.registerSPI
import com.basic.common.di.service.<NamePascal>Service
import com.basic.<suffix>.di.impl.ApplicationServiceImpl
import com.basic.<suffix>.di.impl.<NamePascal>ServiceImpl
import com.basic.router.generated.<nameCamel>RouteModule
import org.koin.dsl.module

val <nameCamel>Module = module {
    includes(<nameCamel>RouteModule)
    registerSPI<ApplicationService>{ ApplicationServiceImpl() }
    registerSPI<<NamePascal>Service>{ <NamePascal>ServiceImpl() }
}
```

### 5.7 App.kt 自动挂载模板

```kotlin
import com.basic.<suffix>.di.<nameCamel>Module

fun initKoin(){
    startKoin {
        modules(commonModule, <nameCamel>Module)
    }
}
```

### 5.8 模块 .gitignore 模板

```gitignore
/build
/.gradle
```
