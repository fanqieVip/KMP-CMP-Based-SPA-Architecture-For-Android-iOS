# SDK 集成工作流规范 (SDK Integration Workflow)

> 本文档定义了如何将 Android/iOS 原生 SDK 桥接至 KMP 层的逻辑设计与编码标准，旨在消融平台差异，提供统一的异步调用与数据模型。

## 1. 标准集成编排工作流 (Standard Orchestration Workflow)

AI 在执行三方 SDK 集成任务时，**必须**严格遵循以下五个阶段：

### 1.1 阶段一：文档感知与获取 (Doc Perception)
- **知识自检**：AI 检查内置知识库是否涵盖该 SDK。明确回复“已掌握”或“需要集成文档”。
- **获取优先级**：
    1. **会话隧道 (Session Tunneling)**：针对登录墙，提供 F12 指南并利用用户提供的凭证进行带权抓取。
    2. **上下文即时注入 (Injection)**：用户直接粘贴关键 API 说明或示例代码。
    3. **URL 直接读取 (URL Read)**：针对公开文档进行自主抓取。
    4. **API 签名逆向 (API Trace)**：若无文档，申请读取物理头文件执行逆推。

### 1.2 阶段二：侵入分析与风险对冲 (Impact & Mitigation)
本阶段的核心目标是**降低侵入性风险**并**累积工程经验**。在编写代码前，AI 必须输出 `[集成侵入性分析报告]`：

1.  **侵入定位 (Invasion Trace)**：
    *   明确列出本次集成将影响的物理文件（如 `AndroidManifest.xml`, `Podfile`, `AppDelegate` 等）。
2.  **风险与已有方案 (Risk & Existing Countermeasures)**：
    *   AI 必须针对识别出的风险，主动匹配本项目已有的标准应对方案。
    *   **示例**：
        - *风险*：SDK 污染 `MainActivity`。➜ *方案*：物理收紧至 `lib_xxx`，通过 `ApplicationService` 代理生命周期。
        - *风险*：iOS Pod 版本冲突。➜ *方案*：遵循 `Pod依赖使用指南` 锁定版本并配置 headers。
        - *风险*：配置不透明。➜ *方案*：在 Plist/XML 补丁中强制添加 `// SDK: [Name]` 注释。
3.  **方案补充与收集 (Knowledge Collection)**：
    *   AI 必须询问用户：“**针对上述风险，是否有新的应对方案或特殊项目规范需要补充？**”
    *   **知识沉淀 (Evolution)**：若用户补充了新的应对方案，AI **必须**立即触发「补丁机制」，将其记录在 `skills/patches/` 中，并在任务结束后申请合并至本规范，确保下次集成相同或类似 SDK 时不再重复询问。

### 1.3 阶段三：接口对齐与全量协议审计 (Full Probing & Protocol Alignment)
这是集成任务的“情报中心”，目标是消除 AI 的“主观猜测”。AI **严禁**直接进入接口设计，必须执行以下流程：

1.  **全量扫描与情报收集 (Information Exhaustion)**：
    *   AI 必须深入扒取文档中的每一个 API 说明。
    *   **底层物理复核**：必须读取 Android `java/kotlin` 源码和 iOS `Synthetic Headers`，核对方法签名与文档的细微差异。
2.  **输出 [双端功能差异对齐报告]**：
    AI 必须以表格形式展示分析结果，包含：
    - **共性汇总**：双端逻辑完全一致的接口，作为 `commonMain` 的基础。
    - **差异与歧义项 (Decision List)**：[核心要求] 明确罗列双端不一致的功能、AI 无法识别的参数或文档模糊处。
    - **信息闭环**：对每个歧义项，AI 必须附带 **在线文档链接** 或 **提取的原文片段**，确保开发者无需翻阅文档即可直接进行裁决。
3.  **输入输出值深度映射 (I/O Mapping)**：
    分析每个接口的入参和出参：
    - **映射矩阵**：列出 `Platform Type` ➜ `Common Type` 的转换逻辑。
    - **不一致处理清单**：针对双端枚举值、错误码、单位（如秒 vs 毫秒）的不一致，列出 AI 准备自动执行的转换逻辑，不确定的项必须强制询问。
4.  **接口公示**：在上述分析获得“无异议”认可后，输出 `expect object` 签名。

### 1.4 阶段四：制定集成计划 (Plan Formulation)
- 输出详细的 `[集成 Todo List]`。
- 计划必须包含：脚手架创建、依赖配置、原生桥接代码编写、DI 挂载、业务层调用示例。

### 1.5 阶段五：编码实施 (Execution)
- 按照计划分步执行，每一步完成后进行自检，确保符合下方的核心模式与规范。

## 2. 核心桥接模式 (Bridging Patterns)

### 2.1 iOS 代理实现 (iOS Delegate Protocol)
- **规则**：代理类必须继承 `NSObject` 并实现对应的 `Protocol`。
- **内存安全**：在 `iosMain` 顶部标记 `@file:OptIn(ExperimentalForeignApi::class)`。
```kotlin
actual object SDKHelper {
    private val delegate = object : NSObject(), SDKDelegateProtocol {
        override fun onResult(data: NativeData?) {
            // 处理回调
        }
    }
}
```

## 3. 数据归一化 (Data Normalization)

- **禁令**：禁止将平台原生类型（如 `NSDictionary`, `NSData`, `Bundle`）泄露给 `commonMain`。
- **转换策略**：在平台侧 `actual` 方法中通过 `toCommon()` 扩展函数完成转换。
    - **iOS**：利用 `com.basic.base.webview.platform.anyToJsonString` 处理复杂的 `Any?` 对象。
    - **Android**：直接映射到 Kotlin Data Class。

## 4. 生命周期挂载 (Lifecycle Injection)

所有具备初始化或回调需求的 SDK **必须**接入 `ApplicationService` SPI 体系。**严禁**直接在 `MainActivity` (Android) 或 `AppDelegate/SceneDelegate` (iOS) 中编写 SDK 逻辑。

### 4.1 核心生命周期映射表

| 场景 | 推荐 Hook (ApplicationService) | 典型用途 |
| :--- | :--- | :--- |
| **应用同步初始化** | `onCreate()` | SDK 的基础 `init` 或 `preInit`。 |
| **前后台切换** | `onForeground()` / `onBackground()` | 统计 SDK 活跃上报、长连接重连。 |
| **Android 外部唤起** | `androidMainActivityOnCreate(intent)` | 处理支付、分享回调及 DeepLink 首次进入。 |
| **Android 热启动唤起** | `androidMainActivityOnNewIntent(intent)` | 处理 App 在后台时被外部唤起。 |
| **iOS 外部唤起 (URL)** | `iosSceneOpenURLContexts(urlContexts)` | 处理 Scheme / 支付跳转回调。 |
| **iOS 通用链接 (UL)** | `iosSceneContinueUserActivity(userActivity)` | 处理 Universal Link 唤起。 |
| **iOS 场景初始化** | `iosSceneWillConnectToOptions(...)` | 处理冷启动时的启动参数（如 UserActivity 集合）。 |

### 4.2 挂载准则
1. **去中心化原则**：每个 `lib_xxx` 模块应有独立的 `ApplicationService` 实现，由 `DI.kt` 自动注册。
2. **静默执行**：业务层不应感知 SDK 的初始化逻辑，只需在 `shared_common` 的 `DI.kt` 中通过 `includes` 引入该模块即可完成自动挂载。

## 5. AI 行为规范 (AI Implementation Checklist)

为防止 SDK 集成代码“四处流窜 (Scattered Code)”，AI 必须严格执行以下清单：

1. **探测优先**：在编写代码前，AI 必须先读取该 SDK 的 Android `java/kotlin` 声明和 iOS 的 `header` 文件（位于 `build/cocoapods/synthetic/...`）。
2. **拒绝硬编码**：所有 AppKey 或 ID 必须通过 `commonMain` 的 `init(config)` 动态传入，或从 `shared_base` 的环境配置中读取。
3. **降级声明**：若发现某平台 SDK 缺失特定功能，必须在 `expect` 接口中标注并在另一平台抛出 `UnsupportedOperationException`。
4. **禁止污染原生宿主 (Anti-Pooping Rules)**：
    - ❌ **严禁** 在 `app/src/androidMain/.../MainActivity.kt` 中添加任何 SDK 业务逻辑。
    - ❌ **严禁** 在 `iosApp/iosApp/.../AppDelegate.swift` 中添加任何 SDK 业务逻辑。
    - ❌ **严禁** 在 `shared_common` 的 `ApplicationServiceImpl.kt` 中堆填多模块 SDK 逻辑。
    - ✅ **必须** 在 `lib_xxx` 内部实现 `ApplicationService`，通过 SPI 机制自动挂载。
