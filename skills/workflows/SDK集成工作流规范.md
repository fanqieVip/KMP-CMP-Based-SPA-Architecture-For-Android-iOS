# SDK 集成工作流规范 (SDK Integration Workflow)

> 本文档定义 Android/iOS 原生 SDK 桥接至 KMP `libs/<name>` SDK 模块的分析、确认、编码与验收流程。目标是把平台差异收敛到模块内部，向 `commonMain` 提供稳定、可测试、无原生类型泄露的接口。

## 0. 适用范围与输入

### 0.1 适用任务
- 新增三方 SDK 适配模块。
- 在既有 `libs/<name>` 中补充 SDK 能力。
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
- 当前项目真实存在的 `libs/*` SDK 模块列表。
- 若目标 `libs/<name>` 已存在，读取其 `build.gradle.kts`、`AndroidManifest.xml`、`di/DI.kt`、`di/impl/ApplicationServiceImpl.*.kt`。
- 若为新增模块，先按 `Lib模版生成指南.md` 创建骨架，再继续 SDK 集成。

按需读取：
- `skills/knowledge/SDK集成案例手册.md`：需要参考既有 lib 模块经验时读取；读取后必须先按手册规则扫描当前项目，不能假设手册中的样例模块一定存在。
- `skills/knowledge/架构api文档.md`：仅当需要确认 SPI、路由、网络、弹窗等 API 细节时检索读取，禁止全量加载。

现状扫描建议：

```bash
find libs -maxdepth 2 -mindepth 2 -type d | sort
rg -n "include\\(\"libs:|projects\\.libs\\.|registerSPI|ApplicationService|cocoapods|pod\\(" settings.gradle.kts core/common build.gradle.kts libs -g '*.kt' -g '*.kts' -g '*.xml'
```

若当前项目没有任何既有 `libs/*` SDK 模块，AI 必须明确说明“当前项目暂无可复用 SDK 模块案例”，然后以模板与通用规范推进。

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

#### 1.2.1 SDK 文档资料归档

接入 SDK 时，所有用于判断实现方案的资料都必须归档到对应 SDK lib 目录，不能只留在对话、浏览器、命令输出或临时上下文中。

归档范围：
- 用户提供的文档、文档链接、截图、示例返回、账号配置说明、集成注意事项等关键信息。
- AI 自行检索、打开、下载或用于实现判断的官方文档、SDK 下载页、API 说明、示例代码、FAQ、Issue。
- 本地物理 SDK 资料，例如 `.h`、`.aar`、`.jar`、`.framework`、XCFramework、Pod synthetic headers 的路径与关键结论。

归档要求：
- 归档位置优先为当前 SDK 模块内的 `libs/<name>/docs/`；若模块尚未创建，应在创建模块时同步创建 `docs/`。
- 文档链接必须写入模块内 Markdown 索引文件，例如 `docs/README.md` 或 `docs/集成资料.md`。
- 若只能获得链接或文本摘要，必须记录链接、获取时间、来源、用途和关键字段说明。
- AI 自行采信的资料必须记录检索来源、访问时间、资料标题或文件名、URL 或本地路径、用于支撑的实现结论；只作为排除项时，也应简要记录排除原因。
- 涉及数据模型、初始化参数、回调字段、错误码、平台差异的内容，必须保留可检索关键词，便于后续通过 `rg` 定位。
- 敏感信息，例如密钥、账号、token、手机号或内部地址，归档前必须脱敏；确需保留原文时必须先向用户确认。
- 后续维护同一 SDK 时，必须先检查对应 lib 的 `docs/` 目录，再回溯对话或外部链接。
- 后续如果发现官方文档、下载页、API 说明、示例 payload、错误码、平台差异或物理 SDK 声明发生变化，必须主动更新对应 lib 的 `docs/` 归档，并记录变更时间、变更来源、影响范围和是否需要同步调整代码。

推荐目录结构：

```text
libs/<name>/
  docs/
    README.md
    sdk-document-links.md
    payload-samples.md
```

完成 SDK 接入时，交付说明中必须明确资料已归档到哪个 lib 目录。

#### 1.2.2 回调数据模型审计

接入或修改 SDK 回调解析逻辑前，必须先审计文档、头文件、AAR/JAR/Framework 暴露的真实数据模型，不得仅凭字段名、平台经验或另一端实现推断结构。

对每个回调结果必须建立字段路径矩阵，至少包含：

| 字段类别 | 字段路径 | 字段类型 | Android | iOS | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 成功码 | `code` | String/Int | 例如 `30000` | 例如 `30000` | 成功值必须明确 |
| 错误信息 | `msg`/`message` | String | 字段名 | 字段名 | 保留原始错误原因 |
| 业务关键字段 | `token`/`data.token` | String | 完整路径 | 完整路径 | 必须记录嵌套层级 |
| 流水字段 | `processID`/`process_id` | String | 完整路径 | 完整路径 | 必须记录命名差异 |
| 原始返回 | root/data/raw | Object/JSON String | 结构 | 结构 | 说明是否需要二次解析 |

强制规则：
- 禁止按字段名臆测映射。若文档未明确字段路径，必须优先从物理 SDK、示例 payload、运行日志或反编译信息确认。
- 若字段在不同平台存在命名或层级差异，解析实现必须显式兼容，并在代码附近保持最小必要说明。
- 对 token、手机号、授权码、订单号、流水号、错误码等关键字段，必须记录完整路径，不能只记录字段名。
- 若仍无法确认字段模型，必须在报告中标记风险并向用户确认，不得静默落地猜测实现。
- 字段映射完成后，如可行，应增加 focused test 或至少通过编译与人工对照完成验证。

### 1.3 阶段三：侵入分析与风险对冲

编码前必须输出 `[集成侵入性分析报告]`：

| 侵入点 | 目标文件/位置 | 是否允许 | 项目标准方案 | 风险 |
| :--- | :--- | :--- | :--- | :--- |
| Gradle 依赖 | `libs/<name>/build.gradle.kts` | 是 | 模块内管理依赖 | 版本冲突 |
| Pod 依赖 | `libs/<name>/build.gradle.kts` | 是 | 带版本号，必要时配置 `moduleName`/`headers` | CInterop 失败 |
| Android 配置 | `libs/<name>/src/androidMain/AndroidManifest.xml` | 是 | 权限、queries、activity/alias 收敛在 SDK 模块内 | 合并冲突、导出风险 |
| iOS 配置 | `iosApp/iosApp/Info.plist` 或 Xcode 能力项 | 谨慎 | 仅必须项，逐项注释用途 | Scheme/UL 不完整 |
| 生命周期 | `libs/<name>/.../ApplicationServiceImpl.*.kt` | 是 | 通过 SPI 接入 `ApplicationService` | 宿主污染 |
| Koin 挂载 | `app/src/commonMain/.../App.kt` | 是 | 在 `modules(...)` 追加模块 | 忘记挂载导致 SPI 不执行 |
| 宿主入口 | `MainActivity` / `AppDelegate` | 禁止新增业务逻辑 | 仅已有 `ApplicationProxyManager` 分发 | 污染宿主 |

风险匹配规则：
- SDK 需要 App 生命周期、前后台、Intent、URL、Universal Link 时，必须在对应 `libs/<name>` 内实现 `ApplicationService`。
- Android 回调 Activity、`activity-alias`、`queries`、权限必须放在 `libs/<name>/src/androidMain/AndroidManifest.xml`。
- iOS URL Scheme、Universal Link、`LSApplicationQueriesSchemes` 若必须修改 `Info.plist`，必须在报告中逐项说明业务原因；AppKey/AppId 等变量值必须通过 `com.basic.ios` 从 `SDKKeyConfig` 注入，不得直接硬编码到 `Info.plist`。
- `iosApp/Podfile` 由根 Gradle 自动汇总生成，禁止手动修改。
- 任何 AppKey/AppId 硬编码都必须列为风险。能运行时传入的参数优先运行时 `init(config)` 传入；必须配置的核心 key 通过 `SDKKeyConfig` 区分 Android/iOS，再由所在 SDK 模块注入和读取。

报告后必须询问开发者：
> 针对上述风险，是否有新的应对方案或特殊项目规范需要补充？

若用户补充了可复用规范，AI 只能按 `AI-Skill自动升级指南.md` 提议创建 `skills/patches/` 补丁；不得在同一轮擅自合并正式文档。

确认方式：
- 待确认事项必须拆成单一决策点逐项询问、逐项记录，严禁一次性堆叠多个互斥问题。
- 每个问题必须包含 AI 推荐选项与简短理由；当前问题未确认前，不得继续追问下一项，也不得进入编码。
- 若负责人主动补充多个结论，AI 可以汇总记录，但下一轮仍只处理尚未确认的一个阻塞决策。
- 决策排序优先级：模块结构/依赖来源 > 初始化参数来源 > Manifest/Info.plist > 授权页 UI/隐私策略 > 其他体验配置。

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
| 嵌套业务字段 | `token`/`data.token`/`process_id` | String/data class | `toCommon()`/专用 helper | 字段路径必须来自文档或物理签名 |

接口设计红线：
- `commonMain` 严禁暴露 `Intent`、`Bundle`、`Activity`、`UIViewController`、`NSDictionary`、`NSData` 等平台类型。
- 平台模型必须在 `androidMain`/`iosMain` 转换为 common data class、sealed class 或 JSON 字符串。
- SDK common 层新增或修改对业务方可见的参数模型、结果模型、状态模型、协议项模型时，必须补齐 `@Serializable`，除非能明确证明该模型只在平台实现内部使用。
- 一键登录类 SDK 的授权页 UI 配置不得抽象成跨项目通用 DSL 或大而全配置对象；`commonMain` 只暴露初始化、预取号、一键登录、关闭授权页、清缓存、版本查询、状态结果等稳定能力，授权页 UI 在对应 SDK 模块的平台实现内按项目直接修改。
- 一键登录类 SDK 的协议/隐私政策文案必须按平台能力实现富文本高亮与点击；多协议时必须保留标题与链接的映射关系。若平台暂不具备精确 range 点击能力，至少保证协议区域可点击，并在 `libs/<name>/docs/` 记录降级原因和后续可改进方向。
- 双端能力不一致时，必须明确：
  - 是否隐藏该能力。
  - 是否在缺失平台抛 `UnsupportedOperationException`。
  - 是否用 no-op，并说明业务后果。
- 回调型 SDK 必须先审计回调触发次数，再决定 common API 范式：
  - 若文档、Demo、物理签名或运行日志能够确认一次调用流程内只返回一次最终结果，可以转换为 `suspend` 协程范式。
  - 若回调可能多次触发，例如持续状态、多阶段结果、重复点击结果、生命周期事件或可重入回调，必须优先设计为 lambda 回调或 Flow，不得强行转换为 `suspend`。
  - 若无法确认回调是否只触发一次，必须先询问开发者：“该 SDK 回调是否保证一次调用流程内只返回一次最终结果？如果不保证，我将使用 lambda/Flow 形态暴露。”
  - 术语使用 `lambda`，不要误写为 `lambal`。
  - 需要页面生命周期兜底时，参考支付模块的 `ScreenLifecycle` 超时策略。
- 初始化 API 若存在异步回调结果，common 层初始化入口必须优先设计为 `suspend init(...): ResultModel`，不得只返回 `Unit`。
- 初始化结果模型必须至少表达成功/失败，并尽量保留 `code`、`message`、`raw` 等排障字段。
- Android/iOS 平台初始化回调必须在 `actual` 实现内转换为 common 结果模型，不得向 common 层泄露平台类型。
- 初始化回调可能不触达时，应设置合理超时并返回明确失败结果，例如 `INIT_TIMEOUT`。
- 只有 SDK 初始化确实没有异步结果，或业务/官方文档明确要求 fire-and-forget 初始化时，才允许返回 `Unit`，并需在集成报告中说明原因。

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
9. `libs/<name>/docs/` 资料归档与字段路径矩阵记录。
10. 业务调用示例或最小验证入口。
11. 编译/静态检查/人工验证步骤。

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
| SDK 资料已归档 | 通过/失败 | `libs/<name>/docs/` |
| 回调字段路径已审计 | 通过/失败 | 字段路径矩阵/示例 payload |
| 公共模型已序列化 | 通过/失败/不适用 | `@Serializable` |
| 协议文案可点击 | 通过/失败/不适用 | 高亮、链接映射、降级记录 |
| 异步初始化结果已协程化 | 通过/失败/不适用 | `suspend init(...): ResultModel` 或说明 |
| 编译或替代验证完成 | 通过/失败 | 命令与结果 |

## 2. 项目核心桥接模式

### 2.1 模块边界
- SDK 模块统一收敛在 `libs/<name>/`，Gradle path 统一为 `:libs:<name>`。
- `<name>` 必须取原 `lib_xxx` 模块名去掉 `lib_` 后的 `xxx`，并保留原大小写，例如 `lib_geyan` -> `libs/geyan`、`lib_umeng` -> `libs/umeng`、`lib_openInstall` -> `libs/openInstall`、`lib_topon` -> `libs/topon`、`lib_pay` -> `libs/pay`；禁止按“认证/统计/广告/支付”等能力域重命名目录。
- 低风险迁移与现有模块可继续保留 namespace `com.basic.<suffix>`；新增模块默认与 `<name>` 保持语义一致。
- Android SDK 文件放在 `libs/<name>/libs/android/` 或 Maven 依赖中。
- iOS Pod 写在 `libs/<name>/build.gradle.kts` 的 `cocoapods` 内。
- `core/common` 只通过 Gradle `api(projects.libs.<nameAccessor>)` 获得需要暴露的 SDK 类型可见性，不承载 SDK 生命周期逻辑；未接入业务链路的 SDK 模块只 include，不强行暴露。
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
- 微信、支付宝等要求固定路径的回调 Activity，应放在 `libs/<name>/src/androidMain/kotlin/...`。
- SDK 模块如需新增 Android `Activity` 承载 SDK 页面、回调页、自定义授权页、透明中转页等，默认父类必须使用 `androidx.fragment.app.FragmentActivity`，方便后续使用 `supportFragmentManager`、`DialogFragment`、Fragment Result 和 Lifecycle 能力。
- 只有 SDK 官方明确要求继承特定 `Activity` 基类，或当前页面必须避免 AndroidX Fragment 依赖时，才允许改用其他父类；原因必须写入集成报告。
- 若平台要求 `${applicationId}.wxapi.*` 等宿主路径，优先使用 `activity-alias` 指向 SDK 模块内 Activity。
- openinstall、DeepLink 等外部唤起入口若希望复用当前壳 Activity，可使用 `activity-alias` 承载 `intent-filter`，并把 `targetActivity` 指向壳入口，以降低 lib 与壳工程耦合。
- 项目侧手写 Manifest 配置必须以官方集成文档为准；AAR 内部 Manifest 只用于物理审计、冲突排查和理解 SDK 内部声明，不得直接复制进项目 Manifest。
- 若 AAR Manifest 与官方文档不一致，集成报告必须标明差异，并优先采用官方文档配置；只有运行时或 manifest merge 明确证明缺失时，才做最小补充。
- 对已有 `activity-alias` 做文档对齐时，不得机械删除；必须先分析其是否承担宿主路径、外部入口、壳 Activity 或 SDK 固定回调路径的解耦职责。
- 修改 Android Manifest 后，必须通过 manifest 合并或 Android 编译任务验证。
- Activity 只做 SDK 回调分发，不写业务逻辑。

### 2.5 回调归一化
- 成功、取消、失败、超时必须映射为 common sealed status。
- 无法保证回调必达的 SDK，必须设计超时兜底或查单提示。
- 无法保证回调只触发一次的 SDK，common API 不得使用单次 `suspend` 返回值承载全部结果；应使用 lambda 回调或 Flow，并在报告中说明多次回调场景。
- 需要业务页面生命周期参与的能力，接口可要求传入 `CoroutineScope` 与 `StateFlow<ScreenLifecycle>`。
- 平台错误码必须保留 code/message，不能只返回 `Boolean`。
- 回调数据模型必须以文档、示例 payload 或物理 SDK 声明为准，字段路径必须显式记录并按平台差异转换。
- 若 SDK 初始化存在异步回调结果，必须转换为协程范式返回初始化结果，集中初始化或业务调用方应根据返回结果决定是否继续预取号、登录预热或状态上报。

### 2.6 Debug 与隐私
- Debug 日志、集成检测只能在非 release 环境开启。
- 统计/归因/广告 SDK 若支持 `preInit`，必须区分隐私授权前 `preInit` 与授权后 `init`。
- 不读取剪贴板、不采集设备标识等隐私选项必须显式记录在实现或报告中。

### 2.7 SDK 参数来源与平台 key
- 必须先根据官方文档和物理签名判断参数来源能力，区分“运行时可传参数”和“必须配置参数”。
- SDK 文档中同时支持运行时传入和 Manifest/Info.plist 配置的参数，优先作为初始化参数运行时传入，不优先走平台配置文件。
- 非必须参数可作为 `init(...)` 参数传入，例如渠道 `channel`、隐私授权状态、授权页行为开关、设备隐私参数等。
- 必须传入的核心参数一般是 `appId`、`appKey`、`secret`、iOS `universalLink` 等，统一写入 `SDKKeyConfig`，并必须区分 Android 与 iOS。
- common 层 `expect` API 不应暴露必须核心 key；平台 `actual` 实现应在各自平台模块中读取 `BuildKonfig` 注入字段。
- 只有 SDK 官方文档或物理 SDK 强制要求 Manifest meta-data 的参数，才允许进入 Android Manifest placeholder；真实值仍必须来自 `SDKKeyConfig`。
- iOS SDK 强制要求 `Info.plist` 配置时，必须通过 `com.basic.ios` 声明字段并从 `SDKKeyConfig` 生成到 `iosConfig.xcconfig`，再由 `Info.plist` 使用 `$(KEY)` 引用。
- 无平台 API 消费路径的配置禁止定义。例如 iOS SDK 没有 channel API 时，不得定义 `IOS.channel`。
- 遇到上游 AAR 自带多余 Manifest 占位符时，必须先向开发者确认处理策略，不得擅自使用 `tools:node="remove"` 或 app placeholder 兜底。

推荐配置结构：

```kotlin
object SomeSdk {
    object Android {
        const val appId = ""
        const val appKey = ""
    }

    object IOS {
        const val appId = ""
        const val appKey = ""
        const val universalLink = ""
    }
}
```

平台读取示例：

```kotlin
actual fun init(runtimeParam: String?) {
    NativeSdk.init(SomeSdkBuildConfig.ANDROID_APP_ID, runtimeParam)
}
```

## 3. 禁令

- 禁止在 `app/src/androidMain/.../MainActivity.kt` 添加 SDK 业务逻辑。
- 禁止在 `app/src/iosMain/.../AppDelegate.kt` 或 `iosApp` Swift 入口添加 SDK 业务逻辑，除非是框架级生命周期分发本身。
- 禁止手动修改 `iosApp/Podfile`。
- 禁止在 `core/common` 的 `ApplicationServiceImpl.kt` 堆叠多模块 SDK 逻辑。
- 禁止 `commonMain` 暴露平台原生类型。
- 禁止 `commonMain` 初始化 API 暴露必须核心 key，例如 `appId`、`appKey`、`secret`。
- 禁止把 Android key 复用为 iOS key，或反向复用。
- 禁止直接手工把 `SDKKeyConfig` 的值复制进 `Manifest` 或 `Info.plist`。
- 禁止省略 Pod 版本号。
- 禁止未经确认采用 no-op、模拟数据、占位 AppKey、跳过物理签名复核等降级方案。
- 禁止 SDK 回调字段按字段名猜测映射，尤其是 token、授权码、流水号、错误码等关键字段。
- 禁止将已使用的 SDK 文档、链接、示例 payload 或物理 SDK 结论只保存在对话上下文中。
- 禁止 SDK 初始化存在异步结果时仍只暴露 `Unit`，除非文档或业务明确要求 fire-and-forget 并已在报告中说明。
