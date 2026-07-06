# SDK 集成案例手册

> 本手册不是固定模块清单，而是 SDK 集成任务的“案例提取与迁移方法”。AI 必须先从当前项目扫描真实证据，再决定是否参考本手册中的样例。样例只用于抽象模式，不能替代当前项目事实。

## 0. 使用边界

### 0.1 什么时候读取

仅在以下任务中按需读取：
- 新增或改造 `libs/<name>` SDK 模块。
- 分析 SDK 对 Gradle、Pod、Manifest、Info.plist、宿主生命周期的侵入。
- 设计 Android/iOS 双端 `expect/actual` API。
- 排查 Pod/CInterop、外部唤起、回调丢失、平台类型泄露等问题。

### 0.2 读取后的第一动作

必须先扫描当前项目，而不是直接套用样例：

```bash
find libs -maxdepth 2 -mindepth 2 -type d | sort
rg -n "include\\(\\\":libs:|project\\(\\\":libs:|registerSPI|ApplicationService|ApplicationProxyManager|cocoapods|pod\\(" settings.gradle.kts build.gradle.kts shared_common app shared_base libs -g '*.kt' -g '*.kts' -g '*.xml'
```

结论必须分三类：

| 结论 | AI 行为 |
| :--- | :--- |
| 当前项目存在相同样例模块 | 可以引用该模块作为“本项目已有案例”，但仍要读物理文件核对 |
| 当前项目不存在相同模块，但存在相似 `libs/*` | 只能引用相似模式，不能引用不存在的路径 |
| 当前项目没有可复用 SDK 模块 | 明确说明暂无案例，按 `Lib模版生成指南.md` 和 `SDK集成工作流规范.md` 推进 |

### 0.3 禁止误用

- 禁止假设所有项目都有 `libs/topon`、`libs/pay`、`libs/openInstall`、`libs/umeng`。
- 禁止把“本仓库当前观察到的位置”写成跨项目绝对规则。
- 禁止只读本手册不读源码。
- 禁止从样例复制 AppKey、scheme、包名、Pod 版本、Activity 名称到新 SDK。

## 1. 案例提取流程

### 1.1 证据采集顺序

对任意 SDK lib，按以下顺序取证：

| 顺序 | 证据 | 目的 |
| :--- | :--- | :--- |
| 1 | `settings.gradle.kts` | 确认模块是否被 include |
| 2 | `shared_common/build.gradle.kts` 或业务聚合模块 | 确认业务侧如何看到 lib API |
| 3 | `libs/<name>/build.gradle.kts` | 确认 Android/iOS 依赖、Pod、CInterop、资源开关 |
| 4 | `libs/<name>/src/commonMain/**` | 确认 common API、模型、DI/SPI |
| 5 | `libs/<name>/src/androidMain/**` | 确认 Android actual、Manifest、回调 Activity、AAR/JAR |
| 6 | `libs/<name>/src/iosMain/**` | 确认 iOS actual、delegate、URL/UL、cocoapods API |
| 7 | `app/**/App.kt`、`ApplicationProxyManager` | 确认 Koin 挂载和生命周期分发 |
| 8 | `iosApp/Info.plist`、Android Manifest 合并点 | 确认宿主级配置是否必要 |

### 1.2 案例卡输出模板

每提取一个案例，AI 应形成以下“案例卡”，并只在物理证据存在时引用：

| 字段 | 内容要求 |
| :--- | :--- |
| 场景 | SDK 类型，例如支付、广告、统计、归因 |
| 证据文件 | 真实存在的文件路径，不得写猜测路径 |
| 侵入面 | Gradle、Pod、Manifest、Info.plist、生命周期、宿主入口 |
| common 协议 | `expect object`、sealed status、data class、Flow/suspend 等 |
| Android 落点 | 依赖、Manifest、actual、Activity/Intent、回调转换 |
| iOS 落点 | Pod、headers/moduleName、actual、delegate、URL/UL |
| 可迁移模式 | 从案例抽象出的通用设计 |
| 适用条件 | 目标 SDK 满足哪些条件时可套用 |
| 禁止套用 | 哪些条件不满足时必须重新设计 |
| 验收点 | 编译、回调、生命周期、隐私、日志、无宿主污染 |

### 1.3 模式抽象规则

从案例中抽象模式时，必须把“文件路径事实”和“架构模式”分开：

| 类型 | 示例 | 使用方式 |
| :--- | :--- | :--- |
| 文件路径事实 | `libs/pay/src/androidMain/AndroidManifest.xml` 有微信回调 alias | 仅当前项目存在该文件时引用 |
| 架构模式 | 固定回调路径可用 `activity-alias` 指向 lib 内 Activity | 可迁移到相似 SDK |
| 项目约定 | 当前仓库 Koin 挂载在 `App.kt` 的 `modules(...)` | 迁移前必须重新搜索 `startKoin` |
| SDK 特例 | TopOn Android 9+ WebView 多进程处理 | 仅同类 WebView/广告 SDK 参考 |

## 2. 可迁移模式目录

### 2.1 模块外壳模式 (Module Envelope)

适用：
- 新 SDK 需要 Android/iOS 双端依赖。
- 业务层只需要 common API，不应接触平台类型。

模式：
- 新建或复用 `libs/<name>`。
- `settings.gradle.kts` include 模块。
- 业务聚合模块按需通过 Gradle `api(project(":libs:<name>"))` 暴露类型。
- `libs/<name>` 内维护 Android Maven/AAR/JAR 与 iOS Pod。
- `commonMain` 只放接口、模型、DI，不放平台类型。

判断点：
- 如果 SDK 只有单端能力，也仍可放入 `libs/<name>`，但 common API 必须明确另一端策略：隐藏、no-op、抛 `UnsupportedOperationException`，或业务不暴露。

### 2.2 生命周期扇出模式 (Lifecycle Fan-out)

适用：
- SDK 要在应用启动、前后台、Intent、URL、Universal Link 时处理事件。

模式：
- 宿主入口只做统一分发。
- 每个 SDK lib 自己实现 `ApplicationService`。
- `DI.kt` 通过 `registerSPI<ApplicationService>` 注册。
- `ApplicationProxyManager` 或当前项目等价机制统一遍历所有实现。

迁移前必须确认：
- 当前项目是否存在 `ApplicationService`、`SPIRegisterCenter`、`ApplicationProxyManager`。
- 当前项目 `initKoin` 是否会实际加载该 lib 的 Koin module。

禁止：
- 把 SDK 逻辑写入 `MainActivity`、`AppDelegate`、`SceneDelegate`。
- 把多个 SDK 生命周期逻辑堆进 `shared_common` 的同一个 ApplicationService。

### 2.3 外部 App 调起回调模式 (External App Round-trip)

适用：
- 支付、分享、登录、三方授权、归因唤醒。
- 调起外部 App 后，结果从 Activity、Intent、URL Scheme、Universal Link 回来。

模式：
- Android 冷启动和热启动都要覆盖：`onCreate(intent)` 与 `onNewIntent(intent)`。
- iOS URL Scheme 和 Universal Link 分开接：`openURLContexts` 与 `continueUserActivity`。
- 冷启动参数单独审计：iOS `willConnectToOptions`，Android launcher intent。
- 回调先转成模块内部事件流，再映射到 common status。

验收点：
- 未安装目标 App。
- 用户取消。
- 回调丢失或超时。
- App 已启动、后台、被杀死三种入口。

### 2.4 回调状态机模式 (Callback State Machine)

适用：
- SDK 回调不一定可靠，业务必须知道进行中、成功、取消、失败、超时。

模式：
- common 层定义 sealed status。
- 发起前先进入进行中状态。
- 平台回调只负责把原始结果转为 common status。
- 对调起外部 App 的 SDK，引入页面生命周期或应用前后台状态兜底。
- 错误结果保留原始 code/message。

不适用：
- 一次性同步查询接口，不需要状态流。
- SDK 自身保证强一致结果，且业务不需要中间态。

### 2.5 平台对象归一化模式 (Platform Type Normalization)

适用：
- SDK 返回 `Bundle`、`Map`、`NSDictionary`、`NSError`、平台 model、图片/视图对象。

模式：
- `commonMain` 只暴露 data class、sealed class、String JSON、基础类型。
- Android 平台 model 通过 `convert()`/`toCommon()` 转成 common model。
- iOS `NSError` 转 common error。
- iOS `NSDictionary`/`Map<Any?, *>` 转 data class 或 JSON 字符串。
- 平台 UI 对象只能留在平台 actual 或 Compose 平台容器内部。

风险：
- 双端字段名相同但单位不同。
- Android 为 Int，iOS 为 String/Double。
- iOS `Map<Any?, *>` 强转失败。

### 2.6 Pod/CInterop 诊断模式

适用：
- iOS Pod 无法生成 `.klib`、找不到头文件、module 名不一致。

诊断顺序：
1. Pod 是否在所在 `libs/<name>/build.gradle.kts` 中声明。
2. 是否显式写 `version`。
3. `build/cocoapods/synthetic/...` 或 Pods 目录里的 framework/module 名是否与 Pod 名一致。
4. 头文件是否在子目录或 framework headers 下。
5. 是否需要 umbrella header。

配置手段：
- `moduleName = "真实模块名"`。
- `headers = "相对头文件路径"`。
- `headers = project.file("src/nativeInterop/cinterop/xxx_umbrella.h").absolutePath`。

禁止：
- 手改 `iosApp/Podfile`。
- 为了过编译把 Pod 写到宿主工程里，除非侵入分析已确认。

### 2.7 隐私双阶段初始化模式 (Privacy Two-step Init)

适用：
- 统计、归因、广告、设备标识、崩溃/APM SDK。

模式：
- `preInit`：隐私授权前，只做 SDK 允许的无采集初始化或参数预置。
- `init`：用户授权后，执行正式初始化。
- Debug 日志、集成检测、诊断开关必须受环境控制，release 默认关闭。

必须审计：
- 是否读取剪贴板。
- 是否采集设备 ID、IMEI、OAID、IDFA。
- 是否自动页面采集。
- 是否在 preInit 阶段启动网络请求。

## 3. 当前仓库案例卡

以下案例只代表当前仓库的可观察证据。迁移到其他项目时，先按第 1 章重新提取。

### 3.1 `libs/openInstall`：归因唤醒与安装参数

场景：
- 安装参数、渠道归因、DeepLink 唤醒。

证据文件：
- `libs/openInstall/build.gradle.kts`
- `libs/openInstall/src/androidMain/AndroidManifest.xml`
- `libs/openInstall/src/commonMain/kotlin/com/basic/openinstall/OpenInstallHelper.kt`
- `libs/openInstall/src/androidMain/kotlin/com/basic/openinstall/OpenInstallHelper.android.kt`
- `libs/openInstall/src/iosMain/kotlin/com/basic/openinstall/OpenInstallHelper.ios.kt`
- `libs/openInstall/src/*Main/kotlin/com/basic/openinstall/di/impl/ApplicationServiceImpl.*.kt`

侵入面：
- Android 使用 JAR 与 Manifest `activity-alias`。
- iOS 使用 Pod，并通过 `headers = "OpenInstallSDK.h"` 处理头文件。
- 生命周期依赖 Android Intent、iOS Universal Link、iOS 冷启动 `userActivities`。

可迁移模式：
- 外部唤醒 SDK 必须覆盖冷启动和热启动。
- Android `getWakeUp(intent, callback)` 放在 `ApplicationService` 的 Intent hook。
- iOS `continueUserActivity` 和 `willConnectToOptions` 都要转交 SDK。
- 安装参数可封装为 `suspend fun`，失败统一返回 nullable 模型。

禁止套用：
- 目标 SDK 不走外部唤醒时，不需要复制 Intent/UL 全套 hook。
- 目标 SDK 不强制 Manifest alias 时，不要照搬 alias。
- AppKey/scheme 不能沿用样例占位值。

对工作流报告的影响：
- 侵入分析必须列出 Manifest alias、scheme、iOS UL 能力项。
- 双端协议审计必须区分 install 参数和 wakeup 参数。
- I/O 映射必须说明 Android 原始字符串与 iOS 对象转 JSON 的差异。

### 3.2 `libs/pay`：支付/登录/分享外部回调

场景：
- 微信/支付宝支付、登录、分享、小程序跳转。

证据文件：
- `libs/pay/build.gradle.kts`
- `libs/pay/src/androidMain/AndroidManifest.xml`
- `libs/pay/src/commonMain/kotlin/com/basic/pay/wechat/WechatUtils.kt`
- `libs/pay/src/androidMain/kotlin/com/basic/pay/wechat/*.kt`
- `libs/pay/src/iosMain/kotlin/com/basic/pay/wechat/WechatUtils.ios.kt`
- `libs/pay/src/commonMain/kotlin/com/basic/pay/alipay/AlipayUtils.kt`
- `libs/pay/src/iosMain/kotlin/com/basic/pay/di/impl/ApplicationServiceImpl.ios.kt`
- `libs/pay/src/commonMain/kotlin/com/basic/pay/bean/*.kt`

侵入面：
- Android Manifest 有 `queries`、回调 Activity、`activity-alias`。
- iOS 需要 URL Scheme、Universal Link、`LSApplicationQueriesSchemes`。
- common 层有多组 sealed status。
- 回调与业务调用之间通过状态流和页面生命周期兜底。

可迁移模式：
- 固定 Android 回调路径可用 `activity-alias` 指向 lib 内 Activity，减少宿主污染。
- iOS 支付/授权回调必须同时处理 URL Scheme 和 Universal Link。
- 发起外部 App 调用后，用页面生命周期判断“是否成功调起”和“是否回到前台仍无结果”。
- 支付类结果不能只返回 `Boolean`，必须表达成功、取消、失败、超时。

禁止套用：
- 目标 SDK 有服务端异步通知时，客户端成功只能作为参考，业务仍需查单。
- 目标 SDK 不要求固定 Activity 路径时，不要增加无意义 alias。
- 不要把样例中的微信/支付宝错误码套给其他支付 SDK。

对工作流报告的影响：
- 差异对齐报告必须列出 Android Activity 回调与 iOS URL/UL 回调的入口差异。
- I/O 映射必须保留平台错误码。
- 验收必须覆盖未安装、取消、超时、冷启动回调、热启动回调。

### 3.3 `libs/topon`：广告聚合与复杂回调模型

场景：
- 广告聚合、开屏、Banner、原生、插屏、激励视频。

证据文件：
- `libs/topon/build.gradle.kts`
- `libs/topon/src/androidMain/AndroidManifest.xml`
- `libs/topon/src/commonMain/kotlin/com/basic/topon/TpSdk.kt`
- `libs/topon/src/commonMain/kotlin/com/basic/topon/sdk/*.kt`
- `libs/topon/src/androidMain/kotlin/com/basic/topon/sdk/*.kt`
- `libs/topon/src/iosMain/kotlin/com/basic/topon/sdk/*.kt`
- `libs/topon/src/androidMain/kotlin/com/basic/topon/Convert.kt`
- `libs/topon/src/iosMain/kotlin/com/basic/topon/Convert.kt`
- `libs/topon/src/commonMain/kotlin/com/basic/topon/bean/*.kt`

侵入面：
- Android 使用 AAR、本地资源、权限、support/legacy 依赖。
- iOS 使用多个 Pod，并配置 `moduleName`。
- SDK 初始化和 integration checking 接入 `ApplicationService.onCreate()`。
- 广告对象和 delegate 留在平台层，common 只暴露模型和 Compose 包装后的能力。

可迁移模式：
- 多广告形态应拆成多个 `sdk/*.kt`，避免单个 helper 膨胀。
- 平台广告对象用 `internal expect class` 或平台 `typealias` 隔离。
- iOS delegate 继承 `NSObject` 并实现 SDK Protocol。
- 广告信息字段必须双端逐项映射，不得只按单端文档建模。

禁止套用：
- 目标 SDK 只有简单初始化时，不需要引入复杂广告状态架构。
- 目标 SDK 的广告 view 生命周期不同，不能照搬 destroy/load/show 顺序。
- TopOn 的 WebView 多进程处理是特定经验，只能给同类风险做提示。

对工作流报告的影响：
- 双端功能差异报告必须按广告形态拆分。
- I/O 映射必须包含广告信息、错误码、关闭原因、奖励事件。
- 侵入分析必须列出 Android 权限、资源、AAR、iOS moduleName。

### 3.4 `libs/umeng`：统计/APM 与隐私预初始化

场景：
- 统计、崩溃、APM、设备相关能力。

证据文件：
- `libs/umeng/build.gradle.kts`
- `libs/umeng/src/commonMain/kotlin/com/basic/umeng/UMHelper.kt`
- `libs/umeng/src/androidMain/kotlin/com/basic/umeng/UMHelper.android.kt`
- `libs/umeng/src/iosMain/kotlin/com/basic/umeng/UMHelper.ios.kt`
- `libs/umeng/src/nativeInterop/cinterop/UMAPM_umbrella.h`

侵入面：
- Android 使用 Maven 依赖和权限。
- iOS 使用多个 Pod，其中 `UMAPM` 通过 umbrella header 聚合多头文件。
- common API 区分 `preInit` 与 `init`。
- Debug 日志受环境控制。

可迁移模式：
- 隐私敏感 SDK 必须区分授权前预初始化和授权后正式初始化。
- 多头文件 Pod 优先用 umbrella header，而不是在 Gradle 中堆多个不稳定路径。
- 自动页面采集、崩溃、内存、网络等开关应显式配置，不依赖 SDK 默认值。

禁止套用：
- 不确认 SDK 隐私行为前，不得把 `preInit` 当作安全操作。
- 目标 Pod 能正常 CInterop 时，不要无意义增加 umbrella header。
- 不要把统计 SDK 的自动页面采集策略套到业务埋点 SDK。

对工作流报告的影响：
- 侵入分析必须列出隐私授权前后行为。
- Pod 报告必须列出每个 Pod 的版本、headers、是否需要 umbrella header。
- 验收必须包含 release 关闭 debug 日志。

## 4. 场景到模式的选择表

| 目标 SDK 场景 | 优先参考模式 | 可参考案例 |
| :--- | :--- | :--- |
| 支付、授权、分享 | 外部 App 调起回调 + 回调状态机 | `libs/pay` |
| DeepLink、归因、唤醒 | 外部 App 调起回调 + 平台对象归一化 | `libs/openInstall` |
| 广告聚合、多广告位 | 平台对象归一化 + 生命周期扇出 | `libs/topon` |
| 统计、APM、崩溃 | 隐私双阶段初始化 + Pod/CInterop 诊断 | `libs/umeng` |
| 单纯工具 SDK | 模块外壳 + 平台对象归一化 | 视当前项目扫描结果而定 |
| 只有 iOS Pod 问题 | Pod/CInterop 诊断 | `libs/umeng`、`libs/openInstall`、`libs/pay`、`libs/topon` 中的对应 Pod 配置 |

## 5. 输出到 SDK 工作流的结论模板

读取本手册后，AI 在 `SDK集成工作流规范.md` 的报告中应补充以下结论：

```text
[案例参考结论]
- 当前项目可复用案例：有/无，模块名：
- 物理证据已读取：
- 可迁移模式：
- 不可直接套用点：
- 需要用户确认的差异：
```

示例：

```text
[案例参考结论]
- 当前项目可复用案例：有，libs/pay
- 物理证据已读取：libs/pay/build.gradle.kts、AndroidManifest.xml、WechatUtils.*、ApplicationServiceImpl.ios.kt
- 可迁移模式：外部 App 调起回调、状态流、超时兜底
- 不可直接套用点：目标 SDK 的错误码、Android 回调 Activity 路径、iOS URL host
- 需要用户确认的差异：支付最终结果是否必须服务端查单
```

## 6. 质量红线

- 案例必须有物理文件证据；没有证据只能写“推测”或“待确认”。
- 模式必须可解释为什么适用，不能只说“参考某模块”。
- 每次引用案例都要说明不可直接套用点。
- 当前项目没有样例模块时，不得降低 SDK 工作流质量。
- 本手册不能替代官方 SDK 文档和物理签名复核。
