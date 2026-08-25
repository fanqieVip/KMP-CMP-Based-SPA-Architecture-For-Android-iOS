# KMP-CMP Based SPA Architecture for Android & iOS

一个面向真实移动业务的 Kotlin Multiplatform + Compose Multiplatform 架构样板。它不是把 Android 代码“搬”到 iOS，也不是只展示几个跨端页面；它更像一套已经把分层、路由、生命周期、服务发现、构建环境、iOS 接入、工程红线都收拢好的移动端单页应用架构。

如果你在找一个能继续演进的 KMP 架构底座，这个项目关注的是长期可维护性：模块边界清楚，跨端能力集中，平台差异有出口，业务模块可插拔，构建和规范尽量自动化。

它展示的重点也不只是“会写 KMP 页面”，而是把 Android、iOS、Gradle、KSP、CocoaPods、生命周期、路由、AI Agent 协作和团队工程规则放在同一张工程图里。

## 这套架构解决什么问题

- 一套 Compose UI 同时服务 Android 与 iOS，平台壳只负责启动、宿主和系统回调。
- 业务以 `Screen` 为基本单元，页面路由、参数传递、生命周期和弹窗栈都走统一框架。
- 公共能力沉到 `core/base` 与 `core/common`，业务只在 `project/*` 中扩展。
- SDK、支付、统计、登录等三方能力预留在 `libs/*`，通过 SPI 和模块内配置接入，避免污染宿主。
- 基于设备物理 PPI 统一 Android 与 iOS 的 Compose 设计尺度，让同一套间距、控件和字号标注在不同设备上保持接近的物理尺寸，同时不污染原生页面与第三方 SDK 的 Density。
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
| 持久化 | MMKV KMP / StateFlow 响应式封装                              |
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
│   └── common       # 公共业务层：基础 UI、网络配置、Repository、通用服务
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
include("project:main")
```

## 依赖方向

```text
core/base
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
- 统一 `ScreenContext`注入：导航、弹窗、权限、App 状态、平台 UI 容器。
- 统一返回拦截。

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
- Screen 子类（含抽象类）的成员变量（包括构造器属性）如果字段本身或字段类型声明带 `kotlinx.serialization.Serializable`，字段类型必须同时实现 `io.github.hristogochev.vortex.util.Serializable`，避免页面恢复失败。
- 非抽象 Screen 必须有 `@Router`。
- 构造参数必须有 `@Params` 或默认值。
- 网络 API 必须收敛到 `*Repository`。
- 类、方法、成员变量必须补齐 KDoc 或注释。

### 7. Android APK 安全防护

项目内置 Android APK 安全防护链路，覆盖编译期、发布期和运行期：

- `com.basic.protect-src`：保护密钥、请求头 key、JSBridge 名称、Hook/Frida/Patch 特征等敏感字符串，避免直接以明文进入 Android 产物。
- `app/tasks/publish_online/mainVmp`：对生产 APK 执行 VMP 加固，并在加固后写入 APK 完整性签名。
- `VmpConfig.kt`：集中维护 VMP 加密范围、VMP so 名和初始化类名，安全核心类必须纳入加固范围。
- `EnvCheckerUtils.kt`：运行期环境校验核心类，负责证书、包体完整性、Hook、Frida、插件化和改包风险检测，必须被 VMP 保护。
- 反无障碍自动点击 / 反积分墙作弊：生产环境隐藏无障碍节点树，并基于 Android 8.0+ 标准 `dispatchGesture` 的虚拟设备及固定触摸特征拦截自动点击，正常手指操作不受无障碍服务开启状态影响。

具体接入规则、配置入口、双端一致性要求和发布验收清单见 [APK 安全防护知识库](./skills/knowledge/apk安全防护.md)。

### 8. PPI 适配统一跨端物理尺寸

Android 与 iOS 的系统 Density、物理 PPI 和逻辑坐标体系不同，同一个 `dp` / `sp` 标注直接跨端使用时，实际看到的控件、间距和字号可能大小不一。架构根据设计基准和设备物理 PPI 为 Compose 提供统一的设计 Density，使业务可以复用同一套设计标注，并让它们在不同设备上的物理尺寸尽量接近。

这套适配只作用于 Compose 范围，不修改 Android `Resources` 或 iOS UIKit 的全局配置，因此原生页面、原生控件和第三方 SDK 仍按平台规则显示；独立 Compose 弹窗、原生互操作控件和自研 WebView 也有明确的 Density 边界，避免重复缩放或比例错乱。横竖屏、平板和分屏下，控件物理尺寸保持稳定，页面排布仍交给响应式布局处理。

它解决的是跨设备的物理尺寸一致性，而不是让所有设备显示完全相同的内容量，也不是把整张设计稿按屏幕宽度等比缩放。实现原理、适配边界和验证方式见 [PPI 适配指南](./skills/knowledge/ppi适配指南.md)。

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
- 跨页面无内存泄漏回调。
- 主交互规范组件。
- 脚手架、分页、刷新、Coordinator 嵌套滚动等丰富组件。
- 普通弹窗、优先级弹窗。
- 原生弹窗、原生页面轻松堆叠，保持Compose风格且不惧遮挡
- 自研WebView 与 JSBridge，安全使用，无重组问题。
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
- `skills/knowledge/apk安全防护.md`：Android APK 安全防护入口，索引 `ProtectSrc`、VMP、完整性签名与运行期校验。
- `skills/knowledge/ppi适配指南.md`：Android、iOS、原生控件与 WebView 的跨端物理尺寸适配边界。
- `buildSrc/src/main/kotlin/com/frame/basic/plugin/IosConfigPlugin.kt`：iOS 配置聚合。
- `project/main/src/commonMain/kotlin/com/basic/main/ui/`：业务页面示例。

这些内置能力不是零散的功能演示，而是为了证明该架构在处理移动端“深水区”问题时的成熟度：

- **内存与生命周期的硬约束**：通过 `BaseScreenModel` 与 `autoClear` 机制，彻底解决了 Compose 与 Native 交叉引用时的内存泄漏顽疾，这是很多 KMP 项目在进入复杂业务期后的头号杀手。
- **混合渲染的工业级实现**：自研的 WebView 状态管理与原生弹窗容器，确保了在复杂的 Native/Compose 混合堆栈下，依然能保持 UI 的极致流畅与状态的物理对齐，不惧重组，不惧黑屏。
- **三方 SDK 的“沙盒化”接入**：基于 SPI 与生命周期代理（ApplicationService），让 SDK 模块实现真正的即插即用，宿主入口 0 污染，这是支撑多团队、多业务线并行开发的基础。
- **从“口头约定”到“编译期阻断”**：利用 `buildSrc` 内置的自定义 Lint 插件，将架构红线直接写进编译器。这不仅降低了代码评审的成本，更保证了即便团队人员流动，工程质量也能始终如一。
- **AI 协同的工程记忆**：通过 `skills/` 层将架构资产从开发者大脑外化为 AI 可感知的规约，使 AI Agent 成为具备“架构直觉”的协作者，实现了从“搬砖工”到“副驾驶”的跨越。

一句话：这套架构交付的不是一个 App，而是一套**可持续演进、天然抗腐蚀、且对 AI 友好的工程标准**。

## 文档地图

- [架构设计文档](./skills/knowledge/架构设计文档.md)
- [架构 API 文档](./skills/knowledge/架构api文档.md)
- [APK 安全防护知识库](./skills/knowledge/apk安全防护.md)
- [PPI 适配指南](./skills/knowledge/ppi适配指南.md)
- [AI Agent 总入口](./AGENTS.md)
- [UI 开发工作流](./skills/workflows/UI开发工作流规范.md)
- [SDK 集成工作流](./skills/workflows/SDK集成工作流规范.md)
- [通用代码规范](./skills/standards/通用代码规范.md)
- [UI 开发规范](./skills/standards/UI开发规范.md)
- [Pod 依赖使用指南](./skills/standards/Pod依赖使用指南与规范.md)
- [资源与切图规范](./skills/standards/资源与切图规范.md)

## License

见 [LICENSE](./LICENSE)。
