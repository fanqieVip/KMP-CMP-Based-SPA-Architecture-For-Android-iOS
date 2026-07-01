# SDK 集成工作流规范 (SDK Integration Workflow)

> 本文档定义 Android/iOS 原生 SDK 桥接至 KMP `lib_xxx` 模块的分析、确认、编码与验收流程。目标是把平台差异收敛到模块内部，向 `commonMain` 提供稳定、可测试、无原生类型泄露的接口。

## 0. 适用范围与输入

### 0.1 适用任务
- 新增三方 SDK 适配模块。
- 在既有 `lib_xxx` 中补充 SDK 能力。
- 修复 SDK 双端签名、生命周期、回调、Pod/CInterop 或 Manifest/Info.plist 相关问题。

### 0.2 必要输入
AI 必须在任务开始时确认 SDK 信息来源：
1. 官方文档 URL 或用户粘贴的关键接口说明。
2. Android 依赖来源：Maven 坐标、AAR/JAR、官方 Demo 或源码。
3. iOS 依赖来源：Pod 名称、版本、XCFramework/Framework、头文件或官方 Demo。
4. 业务期望能力：初始化、登录、支付、分享、归因、广告、统计、外部唤起等。
5. AppKey/AppId 等敏感配置的来源：运行时传参、环境配置、Manifest/Info.plist 占位符或后台下发。

缺少上述输入时，AI 不得凭空设计 SDK 协议；必须进入「文档感知与获取」阶段补齐。

## 1. 标准集成编排工作流

AI 执行三方 SDK 集成任务时，必须按以下六个阶段推进。阶段二、三、四必须先输出报告并等待开发者确认；未经确认不得进入编码实施。

### 1.1 阶段一：项目现状加载

必须先读取以下项目内上下文：
- `AGENTS.md`。
- `skills/workflows/Lib模版生成指南.md`。
- `skills/standards/Pod依赖使用指南与规范.md`。
- 当前项目真实存在的 `lib_*` 模块列表。
- 若目标 `lib_xxx` 已存在，读取其 `build.gradle.kts`、`AndroidManifest.xml`、`di/DI.kt`、`di/impl/ApplicationServiceImpl.*.kt`。
- 若为新增模块，先按 `Lib模版生成指南.md` 创建骨架，再继续 SDK 集成。

按需读取：
- `skills/knowledge/SDK集成案例手册.md`：需要参考既有 lib 模块经验时读取；读取后必须先按手册规则扫描当前项目，不能假设手册中的样例模块一定存在。
- `skills/knowledge/架构api文档.md`：仅当需要确认 SPI、路由、网络、弹窗等 API 细节时检索读取，禁止全量加载。

现状扫描建议：

```bash
find . -maxdepth 2 -type d -name 'lib_*' | sort
rg -n "include\\(\\\":lib_|project\\(\\\":lib_|registerSPI|ApplicationService|cocoapods|pod\\(" settings.gradle.kts shared_common build.gradle.kts lib_* -g '*.kt' -g '*.kts' -g '*.xml'
```

若当前项目没有任何既有 `lib_*`，AI 必须明确说明“当前项目暂无可复用 SDK lib 案例”，然后以模板与通用规范推进。

若读取了 `SDK集成案例手册.md`，必须在后续报告中输出：

```text
[案例参考结论]
- 当前项目可复用案例：有/无，模块名：
- 物理证据已读取：
- 可迁移模式：
- 不可直接套用点：
- 需要用户确认的差异：
```

### 1.2 阶段二：文档感知与物理签名获取

AI 必须输出 `[SDK 文档/物理签名获取报告]`：

| 项 | Android | iOS | 结论 |
| :--- | :--- | :--- | :--- |
| 文档来源 | URL/粘贴/源码/无 | URL/粘贴/头文件/无 | 是否可信 |
| 依赖来源 | Maven/AAR/JAR | Pod/XCFramework/Framework | 是否可落地 |
| 版本 | 明确版本 | 明确版本 | 是否锁定 |
| 物理签名 | Java/Kotlin 声明 | Synthetic Headers/`.h` | 是否已复核 |
| Demo 交叉验证 | 有/无 | 有/无 | 差异说明 |

获取优先级：
1. **上下文即时注入**：用户粘贴官方 API、Demo 或关键头文件。
2. **URL 直接读取**：公开文档可自主抓取。
3. **会话隧道**：登录墙文档需用户提供可访问内容或导出文件。
4. **物理文件复核**：读取 AAR/JAR 公开类、Pod synthetic headers、`.h` 文件。
5. **API 签名逆向**：无文档时，必须说明逆向依据与不确定项。

硬性要求：
- Android 不能只看文档，必须尽量复核 Maven/AAR/JAR 中的公开类签名。
- iOS 不能只看 Pod README，必须尽量复核 `build/cocoapods/synthetic/...` 或 SDK `.h`。
- 若物理签名暂不可得，必须把缺口列入报告，不得伪造签名。

### 1.3 阶段三：侵入分析与风险对冲

编码前必须输出 `[集成侵入性分析报告]`：

| 侵入点 | 目标文件/位置 | 是否允许 | 项目标准方案 | 风险 |
| :--- | :--- | :--- | :--- | :--- |
| Gradle 依赖 | `lib_xxx/build.gradle.kts` | 是 | 模块内管理依赖 | 版本冲突 |
| Pod 依赖 | `lib_xxx/build.gradle.kts` | 是 | 带版本号，必要时配置 `moduleName`/`headers` | CInterop 失败 |
| Android 配置 | `lib_xxx/src/androidMain/AndroidManifest.xml` | 是 | 权限、queries、activity/alias 收敛在 lib 内 | 合并冲突、导出风险 |
| iOS 配置 | `iosApp/iosApp/Info.plist` 或 Xcode 能力项 | 谨慎 | 仅必须项，逐项注释用途 | Scheme/UL 不完整 |
| 生命周期 | `lib_xxx/.../ApplicationServiceImpl.*.kt` | 是 | 通过 SPI 接入 `ApplicationService` | 宿主污染 |
| Koin 挂载 | `app/src/commonMain/.../App.kt` | 是 | 在 `modules(...)` 追加模块 | 忘记挂载导致 SPI 不执行 |
| 宿主入口 | `MainActivity` / `AppDelegate` | 禁止新增业务逻辑 | 仅已有 `ApplicationProxyManager` 分发 | 污染宿主 |

风险匹配规则：
- SDK 需要 App 生命周期、前后台、Intent、URL、Universal Link 时，必须在 `lib_xxx` 内实现 `ApplicationService`。
- Android 回调 Activity、`activity-alias`、`queries`、权限必须放在 `lib_xxx/src/androidMain/AndroidManifest.xml`。
- iOS URL Scheme、Universal Link、`LSApplicationQueriesSchemes` 若必须修改 `Info.plist`，必须在报告中逐项说明业务原因。
- `iosApp/Podfile` 由根 Gradle 自动汇总生成，禁止手动修改。
- 任何 AppKey/AppId 硬编码都必须列为风险。优先运行时 `init(config)` 传入；无法运行时传入时，必须说明平台约束。

报告后必须询问开发者：
> 针对上述风险，是否有新的应对方案或特殊项目规范需要补充？

若用户补充了可复用规范，AI 只能按 `AI-Skill自动升级指南.md` 提议创建 `skills/patches/` 补丁；不得在同一轮擅自合并正式文档。

### 1.4 阶段四：双端协议审计与接口对齐

AI 必须输出 `[双端功能差异对齐报告]`：

| 能力 | Android API/签名 | iOS API/签名 | common 设计 | 差异/歧义 | 决策 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 初始化 | 物理签名 | 物理签名 | `expect object XxxSdk` | 参数差异 | 待确认/已确认 |
| 外部唤起 | Intent/Activity | URL/UL | `ApplicationService` | 冷启动/热启动差异 | 待确认/已确认 |
| 回调结果 | code/message | code/message | sealed class/data class | 错误码差异 | 待确认/已确认 |

同时输出 `[I/O 与错误码映射矩阵]`：

| 平台值 | 平台类型 | common 类型 | 转换函数 | 不一致处理 |
| :--- | :--- | :--- | :--- | :--- |
| `Bundle`/`Map`/`NSDictionary` | 原生容器 | data class/String JSON | `toCommon()`/`anyToJsonString` | 不泄露原生类型 |
| 错误码 | Int/String/NSError | sealed status/error model | `convert()` | 双端含义对齐 |
| 时间/尺寸/金额 | 秒/毫秒/px/dp/分/元 | 明确单位 | 显式换算 | 不确定则询问 |

接口设计红线：
- `commonMain` 严禁暴露 `Intent`、`Bundle`、`Activity`、`UIViewController`、`NSDictionary`、`NSData` 等平台类型。
- 平台模型必须在 `androidMain`/`iosMain` 转换为 common data class、sealed class 或 JSON 字符串。
- 双端能力不一致时，必须明确：
  - 是否隐藏该能力。
  - 是否在缺失平台抛 `UnsupportedOperationException`。
  - 是否用 no-op，并说明业务后果。
- 回调型 SDK 优先转换为 suspend、Flow 或状态回调；需要页面生命周期兜底时，参考支付模块的 `ScreenLifecycle` 超时策略。

接口公示要求：
- 在阶段四末尾输出拟定的 `expect object` / `expect class` 签名。
- 获得“无异议”认可后，才能进入集成计划。

### 1.5 阶段五：制定集成 Todo List

必须输出 `[集成 Todo List]`，至少包含：
1. 脚手架/模块结构调整。
2. Android 依赖、Manifest、混淆、资源。
3. iOS Pod、`moduleName`、`headers`、umbrella header、Info.plist 必要项。
4. `commonMain` API、data class、sealed status。
5. `androidMain` actual 实现与回调转换。
6. `iosMain` actual 实现、`@file:OptIn(ExperimentalForeignApi::class)`、delegate 保活策略。
7. `ApplicationService` 生命周期和外部唤起接入。
8. `DI.kt` 注册与 `app/src/commonMain/.../App.kt` 模块挂载。
9. 业务调用示例或最小验证入口。
10. 编译/静态检查/人工验证步骤。

### 1.6 阶段六：编码实施与验收

执行顺序：
1. 依赖与构建配置。
2. 公共 API 与模型。
3. 平台 actual 实现。
4. 生命周期/SPI/外部唤起。
5. 宿主最小挂载。
6. 编译验证与报告。

验收必须输出 `[SDK 集成验收清单]`：

| 检查项 | 结果 | 说明 |
| :--- | :--- | :--- |
| `commonMain` 无原生类型泄露 | 通过/失败 | 文件 |
| Android Manifest 收敛在 lib | 通过/失败 | 权限/Activity |
| iOS Pod 带版本且可 CInterop | 通过/失败 | moduleName/headers |
| `ApplicationService` 已注册 | 通过/失败 | `DI.kt` |
| Koin 模块已挂载 | 通过/失败 | `App.kt` |
| 宿主无 SDK 业务污染 | 通过/失败 | MainActivity/AppDelegate |
| Debug 日志/集成检测不进 release | 通过/失败 | 版本判断 |
| 编译或替代验证完成 | 通过/失败 | 命令与结果 |

## 2. 项目核心桥接模式

### 2.1 模块边界
- 模块名统一 `lib_xxx`。
- namespace 统一 `com.basic.<suffix>`。
- Android SDK 文件放在 `lib_xxx/libs/android/` 或 Maven 依赖中。
- iOS Pod 写在 `lib_xxx/build.gradle.kts` 的 `cocoapods` 内。
- `shared_common` 只通过 Gradle `api(project(":lib_xxx"))` 获得类型可见性，不承载 SDK 生命周期逻辑。
- Koin 模块挂载位置必须以当前项目实际 `initKoin`/`startKoin` 代码为准。本仓库当前观察到的位置是 `app/src/commonMain/kotlin/com/basic/app/App.kt` 的 `modules(...)`。

### 2.2 SPI 生命周期接入

所有具备初始化、前后台、外部唤起或回调分发需求的 SDK 必须接入 `ApplicationService`。

| 场景 | Hook | 典型用途 |
| :--- | :--- | :--- |
| 应用初始化 | `onCreate()` | SDK debug 开关、integration checking、预初始化 |
| 前后台切换 | `onForeground()` / `onBackground()` | 统计、长连接、状态同步 |
| Android 冷启动 Intent | `androidMainActivityOnCreate(intent)` | DeepLink、归因、支付/分享回调 |
| Android 热启动 Intent | `androidMainActivityOnNewIntent(intent)` | 后台唤起 |
| iOS Universal Link | `iosSceneContinueUserActivity(userActivity)` | 微信/支付宝/OpenInstall UL |
| iOS URL Scheme | `iosSceneOpenURLContexts(urlContexts)` | 支付、分享、登录回调 |
| iOS 冷启动参数 | `iosSceneWillConnectToOptions(...)` | 冷启动 UL/URL 补偿 |

实现规则：
- `commonMain/di/impl/ApplicationServiceImpl.kt` 声明 `expect val applicationServiceImpl: ApplicationService`。
- `androidMain`/`iosMain` 分别提供 `actual val`。
- `commonMain/di/DI.kt` 使用 `registerSPI<ApplicationService> { applicationServiceImpl }`。
- iOS 文件只要引用 cocoapods 或 Objective-C/Swift 互操作 API，文件顶部必须有 `@file:OptIn(ExperimentalForeignApi::class)`。

### 2.3 iOS Delegate 与内存安全
- 代理类必须继承 `NSObject` 并实现对应 `Protocol`。
- 长生命周期回调的 delegate 不得只存在于临时局部变量中；若 SDK 不强持有 delegate，必须在 object/actual class 中用属性保活。
- 回调中不得直接泄露 `NSError`、`NSDictionary`、`NSData`，必须转换为 common 模型。

### 2.4 Android 回调 Activity 与别名
- 微信、支付宝等要求固定路径的回调 Activity，应放在 `lib_xxx/src/androidMain/kotlin/...`。
- 若平台要求 `${applicationId}.wxapi.*` 等宿主路径，优先使用 `activity-alias` 指向 lib 内 Activity。
- Activity 只做 SDK 回调分发，不写业务逻辑。

### 2.5 回调归一化
- 成功、取消、失败、超时必须映射为 common sealed status。
- 无法保证回调必达的 SDK，必须设计超时兜底或查单提示。
- 需要业务页面生命周期参与的能力，接口可要求传入 `CoroutineScope` 与 `StateFlow<ScreenLifecycle>`。
- 平台错误码必须保留 code/message，不能只返回 `Boolean`。

### 2.6 Debug 与隐私
- Debug 日志、集成检测只能在非 release 环境开启。
- 统计/归因/广告 SDK 若支持 `preInit`，必须区分隐私授权前 `preInit` 与授权后 `init`。
- 不读取剪贴板、不采集设备标识等隐私选项必须显式记录在实现或报告中。

## 3. 禁令

- 禁止在 `app/src/androidMain/.../MainActivity.kt` 添加 SDK 业务逻辑。
- 禁止在 `app/src/iosMain/.../AppDelegate.kt` 或 `iosApp` Swift 入口添加 SDK 业务逻辑，除非是框架级生命周期分发本身。
- 禁止手动修改 `iosApp/Podfile`。
- 禁止在 `shared_common` 的 `ApplicationServiceImpl.kt` 堆叠多模块 SDK 逻辑。
- 禁止 `commonMain` 暴露平台原生类型。
- 禁止省略 Pod 版本号。
- 禁止未经确认采用 no-op、模拟数据、占位 AppKey、跳过物理签名复核等降级方案。
