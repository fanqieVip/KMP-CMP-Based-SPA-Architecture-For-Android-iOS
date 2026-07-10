# KMP-CMP Based SPA Architecture for Android & iOS

一个面向真实移动业务的 Kotlin Multiplatform + Compose Multiplatform 架构样板。它不是把 Android 代码“搬”到 iOS，也不是只展示几个跨端页面；它更像一套已经把分层、路由、生命周期、服务发现、构建环境、iOS 接入、工程红线都收拢好的移动端单页应用架构。

如果你在找一个能继续演进的 KMP 架构底座，这个项目关注的是长期可维护性：模块边界清楚，跨端能力集中，平台差异有出口，业务模块可插拔，构建和规范尽量自动化。

它展示的重点也不只是“会写 KMP 页面”，而是把 Android、iOS、Gradle、KSP、CocoaPods、生命周期、路由、AI Agent 协作和团队工程规则放在同一张工程图里。

## 这套架构解决什么问题

- 一套 Compose UI 同时服务 Android 与 iOS，平台壳只负责启动、宿主和系统回调。
- 业务以 `Screen` 为基本单元，页面路由、参数传递、生命周期和弹窗栈都走统一框架。
- 公共能力沉到 `core/base` 与 `core/common`，业务只在 `project/*` 中扩展。
- SDK、支付、统计、登录等三方能力预留在 `libs/*`，通过 SPI 和模块内配置接入，避免污染宿主。
- Android Studio 与 Xcode 的环境切换、iOS `Info.plist` 参数、Podfile 汇总尽量由 Gradle 管理。
- 架构约束不是靠口头约定，而是通过 `buildSrc` 内的 KSP processor 和自定义 lint 插件在编译期拦截。

## 技术栈

| 方向 | 选型                                                       |
| --- |----------------------------------------------------------|
| 跨端 | Kotlin Multiplatform、Compose Multiplatform               |
| UI 架构 | Compose、Material3、Haze、Vortex Screen                     |
| 导航 | 自研 `@Router` + KSP 生成路由表                                 |
| 生命周期 | `BaseScreen` + `BaseScreenModel` + 可见性分发                 |
| DI / SPI | Koin + `SPIRegisterCenter`                               |
| 网络 | Ktor + Ktorfit + KtorMonitor                             |
| 持久化 | Multiplatform Settings / DataStore 风格封装                  |
| 平台桥接 | `expect/actual`、Android 原生壳、iOS SwiftUI 壳                |
| 构建治理 | Gradle Kotlin DSL、buildSrc 插件、BuildKonfig、CocoaPods 自动汇总 |
| AI 协作 | `AGENTS.md` + `skills/` 分层规范                             |

## 当前模块

```text
.
├── app              # KMP 应用入口，生成 Android App 与 iOS ComposeApp framework
├── iosApp           # iOS SwiftUI 原生壳，承载 ComposeUIViewController
├── core
│   ├── base         # 架构内核：Screen、生命周期、SPI、路由、弹窗、WebView、下载、平台能力
│   ├── common       # 公共业务层：基础 UI、网络配置、Repository、通用服务
│   └── native       # 安全与加密能力：Android JNI/静态库、iOS/Kotlin 对应实现
├── project
│   └── main         # 示例业务模块：页面、菜单、路由实现、业务服务实现
├── libs             # 三方 SDK 模块预留目录，新增 SDK 收敛到 libs/<name>
├── buildSrc         # 内部 Gradle 插件、KSP 路由、架构 lint、构建参数
└── skills           # AI 协作规范、架构手册、集成流程和质量红线
```

当前 Gradle include：

```kotlin
include("app")
include("core:base")
include("core:common")
include("core:native")
include("project:main")
```

## 依赖方向

```text
core/base
   ↑
core/native
   ↑
core/common
   ↑
project/main
   ↑
app
   ↑
iosApp
```

核心原则很直接：

- `core/base` 是架构内核，不依赖业务。
- `core/common` 只沉淀可复用公共能力和业务契约。
- `project/main` 实现具体业务页面、路由与服务。
- `app` 只做应用组合和入口初始化。
- `iosApp` 只做 SwiftUI 宿主、Scene 回调转发和调试入口。
- 三方 SDK 后续进入 `libs/<name>`，不把 Manifest、Pod、生命周期逻辑散落到宿主。

## 启动链路

### Android

```text
Application.onCreate()
  -> initKoin()
  -> ApplicationProxyManager.onCreate()
  -> registerActivityLifecycleCallbacks()

MainActivity.onCreate()
  -> installSplashScreen()
  -> setContent { App(...) }
  -> ApplicationProxyManager.androidMainActivityOnCreate(intent)
```

### iOS

```text
iOSApp @main
  -> SwiftAppDelegate.didFinishLaunching
  -> AppDelegate.shared.onAppCreate()
  -> initKoin()
  -> ApplicationProxyManager.onCreate()
  -> MainViewControllerKt.MainVC()
  -> ComposeUIViewController { App(...) }
```

Universal Link、URL Scheme、前后台切换等 iOS Scene 回调会继续转交到 KMP 的 `ApplicationProxyManager`，再分发给所有注册的 `ApplicationService`。

## 架构里的关键设计

### 1. Screen 是业务页面的唯一入口

业务页面继承 `BasicScreen`，实际能力来自 `BaseScreen`：

- 统一状态栏文字颜色、屏幕方向和默认背景。
- 统一返回拦截。
- 统一弹窗栈，包括普通弹窗、优先级弹窗、最高优先级弹窗。
- 统一 `ScreenContext` 注入：导航、弹窗、权限、App 状态、平台 UI 容器。
- 统一链路来源 `TraceInfo`，方便页面、弹窗、下级页面传递来源。

### 2. BaseScreenModel 管生命周期，不让页面散写状态

`rememberBaseScreenModel` 会把 `ScreenModel` 和页面可见性绑定起来，提供：

- `onInit()`：只调用一次。
- `onVisible()` / `onInvisible()` / `onDestroyed()`：适配页面显示生命周期。
- `uiLoading()` / `uiSuccess()` / `uiError()`：驱动 `BasicInteraction`。
- `PagingControl`：分页模型被自动初始化。

这让业务代码不会散落在 Composable 中，也避免直接使用 Vortex 原始 `rememberScreenModel()` 带来的生命周期断层。

### 3. 路由是编译期生成的，不靠手写注册表

页面通过 `@Router(path)` 声明路由，通过 `@Params` 声明 URL 参数映射。`buildSrc` 中的 `RouterSymbolProcessor` 会生成模块路由表并注册到 Koin。

它同时做参数检查：

- 路由 path 不能为空、不能重复。
- Screen 构造参数必须可从 URL 还原。
- 复杂对象必须显式声明 JSON 目标类型。
- 缺少默认值或可空声明的参数会在编译期报错。

### 4. SPI 让模块自己接入生命周期

模块通过 Koin DSL 注册服务：

```kotlin
registerSPI<ApplicationService> { ApplicationServiceImpl() }
```

`ApplicationProxyManager` 在应用启动、前后台、Android Intent、iOS URL/Universal Link 时统一分发。这是接入 SDK 的关键：SDK 模块自己处理生命周期和回调，宿主入口保持干净。

### 5. iOS 配置和 Podfile 不靠手改

`com.basic.ios` 插件会汇总各模块声明的 `iosConfig { field(...) }`，生成：

```text
iosApp/Configuration/iosConfig.xcconfig
```

根 Gradle 会扫描所有子模块的 `cocoapods { pod(...) }`，自动生成 `iosApp/Podfile`。Pod 版本、headers、moduleName 等配置留在所在模块，避免 Xcode 工程变成配置垃圾场。

### 6. 架构红线进编译期

`com.basic.lint` 不是普通文档，它会在 KSP 阶段拦截常见架构漂移：

- Screen 必须继承项目基类，不能直接继承 Vortex `Screen`。
- Screen / ScreenModel 构造参数禁止函数类型，避免 URL 序列化和生命周期引用问题。
- 非抽象 Screen 必须有 `@Router`。
- 构造参数必须有 `@Params` 或默认值。
- 业务代码不能直接使用原始 Settings。
- 网络 API 必须收敛到 `*Repository`。
- 类、方法、成员变量必须补齐 KDoc 或注释。

## AI Agent 协作体系

这个项目把 AI Agent 当成工程协作者来设计，而不是临时问答工具。根目录的 `AGENTS.md` 是总入口，要求 Agent 先读工作流，再读补丁，最后按任务类型读取规约和知识手册，避免一上来全量扫文档、乱猜架构。

`skills/` 被拆成四层：

- `workflows/`：定义怎么做事。比如 UI 开发、SDK 集成、Lib 模块生成、Project 模块生成、IDE 环境同步。
- `standards/`：定义质量红线。比如通用代码规范、UI 原子约束、资源命名、Pod 依赖规则。
- `knowledge/`：定义可检索的架构和 API 手册。只在需要路由、SPI、网络、弹窗、组件参数等细节时读取。
- `patches/`：预留给协作中的增量共识。发现规范缺口时先沉淀补丁，再由负责人决定是否合并进正式手册。

这套设计的价值是：Agent 不只是“能改代码”，而是知道当前仓库的分层、禁区、提问顺序、降级规则和验收口径。对复杂任务，比如接一个双端 SDK，它会先做文档/物理签名审计、侵入分析、双端协议对齐，再进入编码；对 UI 页面，它会先确认页面拓扑、Scaffold、Interaction、Coordinator、Paging 等关键结构，再写实现。

换句话说，`skills/` 是这个仓库的工程记忆。人可以读它统一认知，Agent 可以读它减少误判，长期协作时也能把临时经验沉淀成可复用规则。

## 内置能力示例

`project/main` 不是空壳，里面放了一组用来证明架构能力的页面：

- Screen 栈管理与参数传递。
- 页面回调和链路透传。
- `BasicInteraction` 加载、空、错、成功状态。
- 分页、刷新、Coordinator 嵌套滚动。
- 普通弹窗、优先级弹窗、原生弹窗。
- WebView 与 JSBridge。
- 文件系统、图片/相机/目录选择。
- 下载器。
- 权限系统。
- Ktor + Ktorfit 网络示例。
- 全局数据共享和 Screen 级数据共享。

## 快速开始

### 环境

- macOS。
- Android Studio，建议安装并更新 Kotlin Multiplatform 插件。
- Xcode。
- JDK 21。
- Ruby / CocoaPods。

完整初始化命令见 [框架初始化说明.md](./框架初始化说明.md)。

### Android 构建

```bash
./gradlew :app:assembleDebug
```

### 生成通用元数据

```bash
./gradlew :app:compileCommonMainKotlinMetadata
```

### iOS 依赖安装

```bash
cd iosApp
pod install
```

### iOS 环境切换

项目内置四套环境：

| 环境 | Xcode Scheme | 状态 |
| --- | --- | --- |
| 开发 | `iosApp-debug` | `VERSION_STATUS_DEVELOP` |
| 测试 | `iosApp-beta` | `VERSION_STATUS_BETA` |
| 预发 | `iosApp-alpha` | `VERSION_STATUS_ALPHA` |
| 生产 | `iosApp-release` | `VERSION_STATUS_RELEASE` |

Android Studio 与 Xcode 的同步配置见 [RunConfiguration.md](./RunConfiguration.md)。

## 新增业务模块

业务模块统一放到 `project/<name>`：

```text
project/<name>/
  src/commonMain/kotlin/com/basic/<name>/
  src/androidMain/
  build.gradle.kts
```

新增模块需要做三件事：

1. `settings.gradle.kts` 添加 `include("project:<name>")`。
2. `app/build.gradle.kts` 在 `commonMain`、`androidMain`、`iosMain` 依赖中接入。
3. 在 `App.kt` 的 `modules(...)` 中挂载模块 Koin module。

项目的 AI 协作规范已经把这些动作整理成模板，见 [skills/workflows/Project模版生成指南.md](./skills/workflows/Project模版生成指南.md)。

## 新增 SDK 模块

三方 SDK 统一放到 `libs/<name>`，不要平铺到根目录，也不要把 SDK 生命周期写进 `MainActivity` 或 `AppDelegate`。

推荐边界：

- Android AAR/JAR 放到 `libs/<name>/libs/android/`。
- Android Manifest、权限、Activity、queries 收敛在 SDK 模块内。
- iOS Pod 在 SDK 模块 `build.gradle.kts` 中声明，必须锁版本。
- iOS AppKey、URL Scheme 等配置优先通过 `iosConfig` 或运行时参数处理。
- 生命周期和外部唤起通过 `ApplicationService` 接入。
- `commonMain` 不暴露 `Intent`、`Bundle`、`UIViewController`、`NSDictionary` 等平台类型。

详细流程见 [skills/workflows/Lib模版生成指南.md](./skills/workflows/Lib模版生成指南.md) 和 [skills/workflows/SDK集成工作流规范.md](./skills/workflows/SDK集成工作流规范.md)。

## 阅读路线

想快速读懂这套架构，可以按这些文件往下看：

- `app/src/commonMain/kotlin/com/basic/app/App.kt`：共享入口与模块组装。
- `core/base/src/commonMain/kotlin/com/basic/base/base/BaseScreen.kt`：页面基类、弹窗栈、上下文注入。
- `core/base/src/commonMain/kotlin/com/basic/base/base/BaseScreenModel.kt`：页面生命周期和交互状态。
- `core/base/src/commonMain/kotlin/com/basic/base/ApplicationProxyManager.kt`：应用生命周期代理。
- `core/base/src/commonMain/kotlin/com/basic/base/router/Router.kt`：路由注解和协议。
- `buildSrc/src/main/kotlin/com/frame/basic/router/RouterSymbolProcessor.kt`：路由生成器。
- `buildSrc/src/main/kotlin/com/frame/basic/lint/LintSymbolProcessor.kt`：架构红线。
- `buildSrc/src/main/kotlin/com/frame/basic/plugin/IosConfigPlugin.kt`：iOS 配置聚合。
- `project/main/src/commonMain/kotlin/com/basic/main/ui/`：业务页面示例。

读完这些点，基本就能看见这套工程的主干：它不是几个页面拼起来的 Demo，而是从入口、分层、路由、生命周期、构建到协作规范都成体系的移动端底座。

## 工程视角

这个项目真正想表达的不是“会 KMP”，而是：

- 能把跨端 UI、原生宿主、构建系统和平台回调打通。
- 能把业务扩展点前置设计出来，而不是每接一个 SDK 就改宿主。
- 能用编译期工具约束团队写法，减少靠代码评审兜底。
- 能同时理解 Android 和 iOS 的工程现实：Manifest、Pod、Xcode Scheme、Info.plist、URL 回调、CInterop。
- 能把 AI Agent 协作也纳入工程流程，用 `skills/` 维护开发规范、集成流程和知识补丁。

一句话：这是一个偏工程系统视角的 KMP/CMP 工程，不是 Demo 视角的跨端页面集合。

## 文档地图

- [架构设计文档](./skills/knowledge/架构设计文档.md)
- [架构 API 文档](./skills/knowledge/架构api文档.md)
- [AI Agent 总入口](./AGENTS.md)
- [UI 开发工作流](./skills/workflows/UI开发工作流规范.md)
- [SDK 集成工作流](./skills/workflows/SDK集成工作流规范.md)
- [通用代码规范](./skills/standards/通用代码规范.md)
- [UI 开发规范](./skills/standards/UI开发规范.md)
- [Pod 依赖使用指南](./skills/standards/Pod依赖使用指南与规范.md)
- [资源与切图规范](./skills/standards/资源与切图规范.md)

## License

见 [LICENSE](./LICENSE)。
