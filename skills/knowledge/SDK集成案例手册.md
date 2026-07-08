# SDK 集成案例手册

> 本手册只沉淀“具体 SDK 类型的接入案例”。通用流程、红线、资料归档、字段审计、Pod/CInterop、Manifest/Info.plist、生命周期等规则归 `skills/workflows/SDK集成工作流规范.md`，不要在本手册重复堆叠。

## 0. 使用边界

- 本手册用于在接入同类型 SDK 时，快速理解推荐的 common API、用户操作流程、数据流转和模型设计。
- 本手册不是任何项目的模块清单，不依赖某个项目一定存在某个 SDK 模块。
- 每个案例只描述“同类型 SDK 的通用接入形态”，真正落地时仍必须重新阅读目标 SDK 的官方文档、Demo、物理签名和示例 payload。

## 0.1 案例地图

| SDK 类型关键词 | 优先跳转 | 适用判断 |
| :--- | :--- | :--- |
| 一键登录、本机号码认证、运营商认证、号码认证、授权页登录 | [1. 一键登录类 SDK 接入案例](#1-一键登录类-sdk-接入案例) | SDK 托管授权页，并在用户点击登录后返回 token、授权码或临时凭证 |
| 广告、广告聚合、开屏、Banner、信息流、原生广告、插屏、激励视频 | [2. 广告类 SDK 接入案例](#2-广告类-sdk-接入案例) | SDK 提供广告加载、展示、点击、关闭、奖励、收益信息等多阶段回调 |
| 支付、收银台、App 支付、微信支付、支付宝支付、银联支付、签约支付、自动续费 | [3. 支付类 SDK 接入案例](#3-支付类-sdk-接入案例) | SDK 调起外部 App 或平台收银台，并通过 Activity、URL Scheme、Universal Link、同步 callback 等方式返回支付状态 |

## 1. 一键登录类 SDK 接入案例

### 1.1 适用场景

适用于以下 SDK 类型：
- 本机号码一键登录。
- 运营商认证。
- SDK 托管授权页。
- 用户在 SDK 页面点击登录按钮后，SDK 返回 token、授权码或临时凭证，再由业务服务端换取手机号。

不适用于：
- 纯短信验证码登录。
- WebView H5 登录。
- 三方 App OAuth 授权。
- SDK 只提供后台接口、不托管授权页的认证能力。

### 1.2 接入目标

common 层要表达的是稳定业务能力，而不是某个 SDK 的平台细节：
- 初始化 SDK。
- 预取号。
- 判断预取号是否有效。
- 获取 SDK 当前协议。
- 拉起授权页。
- 接收授权页用户操作。
- 接收登录结果。
- 关闭授权页。
- 清理预取号缓存。
- 查询 SDK 版本。

授权页 UI 由平台 SDK 托管，common 层不设计跨项目通用 UI DSL。

### 1.3 推荐 common API

以下接口是案例形态，实际命名按目标 SDK 模块调整。

```kotlin
expect object OneClickAuth {
    suspend fun init(
        channel: String? = null,
        enableDebug: Boolean = false,
        timeoutSeconds: Int = 8
    ): OneClickResult

    suspend fun preLogin(): OneClickResult

    fun isPreLoginValid(): Boolean

    fun getCurrentPrivacyTerms(): List<OneClickPrivacyTerm>

    fun oneKeyLogin(
        container: UIContainer,
        extraPrivacyTerms: List<OneClickPrivacyTerm> = emptyList(),
        onLoginClick: ((
            isPrivacyChecked: Boolean,
            retryLogin: () -> Unit,
            sdkTerms: List<OneClickPrivacyTerm>,
            extraTerms: List<OneClickPrivacyTerm>
        ) -> Unit)? = null,
        onPrivacyCheckboxClick: ((isChecked: Boolean) -> Unit)? = null,
        onPrivacyTermClick: ((term: OneClickPrivacyTerm) -> Unit)? = null,
        callback: (OneClickLoginStatus) -> Unit
    ): Boolean

    fun closeAuthPage()

    fun clearCache()

    fun getSdkVersion(): String?
}
```

设计要点：
- `init()` 和 `preLogin()` 是准备动作，若 SDK 有异步结果，应转换为 `suspend` 并返回明确结果模型。
- `oneKeyLogin()` 不设计为 `suspend`，只返回是否成功启动授权流程。
- 授权页里的成功、失败、取消、重复点击、协议未勾选等事件通过 lambda 回调表达。
- `closeAuthPage()` 必须显式暴露，授权页关闭权默认交给业务层。

### 1.4 数据模型设计

公共模型必须补齐 `@Serializable`，除非能明确证明只在平台内部使用。

#### 1.4.1 初始化/预取号结果

```kotlin
@Serializable
class OneClickResult(
    val success: Boolean,
    val code: String?,
    val message: String?,
    val operator: String?,
    val privacyName: String?,
    val privacyUrl: String?,
    val raw: String?,
    val privacyTerms: List<OneClickPrivacyTerm> = emptyList()
)
```

字段含义：
- `success`：当前动作是否成功。
- `code`：SDK 返回码。
- `message`：SDK 返回描述。
- `operator`：运营商或认证通道标识。
- `privacyName` / `privacyUrl`：SDK 当前返回的主协议名称和地址。
- `privacyTerms`：SDK 当前返回的协议列表。
- `raw`：平台 SDK 原始返回，便于联调和排障。

#### 1.4.2 协议项

```kotlin
@Serializable
class OneClickPrivacyTerm(
    val title: String,
    val url: String
)
```

设计要点：
- `title` 不包含书名号。
- `url` 必须保留原始协议链接。
- SDK 自带协议和业务额外协议不能混成一个不可区分的列表；回调里应分开传递。

#### 1.4.3 登录状态

```kotlin
@Serializable
sealed class OneClickLoginStatus {
    @Serializable
    object LoggingIn : OneClickLoginStatus()

    @Serializable
    class Success(
        val token: String?,
        val processId: String?,
        val operator: String?,
        val raw: String?,
        val sdkUserId: String? = null
    ) : OneClickLoginStatus()

    @Serializable
    class Cancel(
        val code: String?,
        val message: String?,
        val raw: String?
    ) : OneClickLoginStatus()

    @Serializable
    class Fail(
        val code: String?,
        val message: String?,
        val raw: String?
    ) : OneClickLoginStatus()
}
```

字段设计要点：
- `Success.token` 是服务端换取手机号的关键凭证，字段路径必须来自目标 SDK 文档、Demo、示例 payload、物理签名或运行日志。
- `processId`、`operator`、`sdkUserId` 只有目标 SDK 真实提供时才保留。
- `Cancel` 和 `Fail` 必须保留 `code`、`message`、`raw`。
- 不要只返回 `Boolean` 或只返回 `token`，否则排障信息不足。

### 1.5 用户操作流程

#### 1.5.1 标准成功流程

```text
业务页面
  -> 调用 init()
  -> init 返回成功
  -> 调用 preLogin()
  -> preLogin 返回成功，并缓存 SDK 当前协议
  -> 用户点击业务侧“一键登录”入口
  -> 业务侧做入口防重复点击
  -> 调用 oneKeyLogin(...)
  -> oneKeyLogin 返回 true，表示授权页启动成功
  -> SDK 授权页展示
  -> 用户勾选协议
  -> 用户点击授权页登录按钮
  -> callback(LoggingIn)
  -> SDK 返回 token / 授权码
  -> callback(Success)
  -> 业务侧请求服务端换取手机号
  -> 业务侧按结果决定是否 closeAuthPage()
```

#### 1.5.2 未勾选协议流程

```text
用户点击授权页登录按钮
  -> SDK 封装层检测到协议未勾选
  -> 调用 onLoginClick(
       isPrivacyChecked = false,
       retryLogin,
       sdkTerms,
       extraTerms
     )
  -> 业务层展示自己的协议确认弹窗
  -> 用户同意
  -> 业务层执行 retryLogin()
  -> 平台实现自动勾选协议并重试登录动作
  -> 后续结果继续通过 callback 返回
```

设计要点：
- SDK 封装层不弹业务弹窗。
- SDK 封装层不做 Toast 兜底。
- `retryLogin` 是执行器，只负责继续 SDK 登录动作，不承载业务弹窗逻辑。
- `sdkTerms` 和 `extraTerms` 分开回调，业务弹窗可以完整展示全部协议。

#### 1.5.3 协议点击流程

```text
用户点击 SDK 授权页中的协议文本
  -> 平台实现识别被点击的协议项
  -> 调用 onPrivacyTermClick(term)
  -> 业务层打开 WebView、浏览器或路由页面
```

设计要点：
- 协议跳转由业务层决定。
- SDK 封装层只提供 `title + url`。
- 平台暂时只能识别整块协议区域时，必须在实现说明或接入报告中标记降级。

#### 1.5.4 失败/取消流程

```text
用户取消或 SDK 返回失败
  -> callback(Cancel) 或 callback(Fail)
  -> 业务层展示错误、切换登录方式或重试
  -> 业务层决定是否 closeAuthPage()
```

设计要点：
- 成功、失败、取消后默认不自动关闭授权页。
- 如果目标 SDK 强制自动关闭且无法阻止，必须记录为平台限制。

### 1.6 数据流转

#### 1.6.1 初始化数据流

```text
业务参数
  -> common init(channel, enableDebug, timeoutSeconds)
  -> androidMain / iosMain actual
  -> 平台 SDK init callback
  -> 平台结果转换为 OneClickResult
  -> common 调用方根据 success 决定是否继续 preLogin
```

#### 1.6.2 预取号数据流

```text
common preLogin()
  -> 平台 SDK 预取号
  -> 平台返回运营商、协议名称、协议链接、缓存有效期
  -> actual 转换为 OneClickResult
  -> 模块内部缓存协议列表和有效期
  -> getCurrentPrivacyTerms() 可读取当前协议
```

#### 1.6.3 登录结果数据流

```text
SDK 原始登录回调
  -> 平台 actual 解析 code / message / token / processId / operator / raw
  -> 转换为 OneClickLoginStatus
  -> callback(status)
  -> 业务层拿 Success.token 请求服务端
  -> 服务端换取手机号或登录态
```

字段路径要求：
- `token`、`processId`、`operator`、成功码、失败码、取消码必须建立字段路径矩阵。
- Android 和 iOS 字段名相同也不能假设层级相同。
- `raw` 必须保留，便于服务端和 SDK 厂商联调。

### 1.7 平台实现职责

androidMain / iosMain 负责：
- 调用平台 SDK。
- 处理平台回调。
- 读取 SDK 当前协议。
- 配置授权页 UI。
- 实现协议高亮和点击能力。
- 将平台模型转换为 common 模型。
- 处理授权页显式关闭。

commonMain 不负责：
- 暴露 `Activity`、`Intent`、`UIViewController`、`NSDictionary` 等平台类型。
- 直接打开 WebView。
- 直接弹业务协议弹窗。
- 定义跨项目通用授权页 UI DSL。
- 猜测目标 SDK 的字段路径。

### 1.8 防重复点击与关闭策略

业务侧入口按钮：
- 业务层处理防重复点击。
- `oneKeyLogin()` 返回 `false` 时，业务层可以提示“授权页启动失败”或切换登录方式。

SDK 授权页内部按钮：
- 如果用户快速点击导致 SDK 报错，应优先确认 SDK 是否支持禁用按钮、回调前置拦截或官方防重复点击配置。
- 若 SDK 不支持，封装层不得用不确定状态强行吞掉回调；应记录平台限制并由业务层设计兜底体验。

关闭策略：
- 默认只在业务显式调用 `closeAuthPage()` 时关闭。
- 成功、失败、取消都交给业务层处理关闭。
- 若目标 SDK 成功后默认自动关闭且无法关闭该行为，必须在接入报告中说明。

### 1.9 验收清单

| 检查项 | 期望 |
| :--- | :--- |
| 初始化结果 | `success/code/message/raw` 可表达成功和失败 |
| 预取号结果 | 可读取运营商协议标题和链接 |
| 授权页启动 | `oneKeyLogin()` 返回 `Boolean` |
| 未勾选协议 | 只回调业务层，不在 SDK 层 Toast 或弹窗 |
| 协议点击 | 通过 `onPrivacyTermClick(term)` 交给业务层 |
| 登录成功 | `Success` 包含 token 和 raw，必要时包含 processId/operator |
| 失败/取消 | `Cancel/Fail` 保留 code/message/raw |
| 关闭授权页 | 业务层可显式调用 `closeAuthPage()` |
| 重复点击 | 业务入口防重；SDK 页内部重复点击风险已评估 |
| 数据模型 | common 暴露模型补齐 `@Serializable` |
| 字段路径 | token、成功码、错误码、取消码路径已审计 |

### 1.10 接入报告必须说明的问题

接入一键登录类 SDK 时，报告中必须回答：
- 初始化是否有异步结果，是否已协程化。
- 预取号是否有有效期，如何判断有效。
- 授权页是否由 SDK 托管。
- 成功、失败、取消后 SDK 是否会自动关闭页面。
- 未勾选协议时 SDK 是否允许拦截并自定义处理。
- 协议文本是否支持多协议、高亮和精确点击。
- SDK 自带协议有哪些字段，业务额外协议如何追加。
- token 的完整字段路径是什么。
- Android 和 iOS 的成功码、失败码、取消码是否一致。
- 授权页内部快速重复点击是否有 SDK 官方处理方案。

## 2. 广告类 SDK 接入案例

### 2.1 适用场景

适用于以下 SDK 类型：
- 广告聚合 SDK。
- 开屏广告 SDK。
- Banner 广告 SDK。
- 信息流、原生广告、模板广告 SDK。
- 插屏广告 SDK。
- 激励视频广告 SDK。
- 需要向业务返回展示、点击、关闭、奖励、收益、广告源等事件的商业化 SDK。

不适用于：
- 只提供服务端广告配置接口、不在客户端渲染广告的 SDK。
- 只做埋点归因、不负责广告加载和展示的 SDK。
- 纯 WebView 广告页，且无原生广告生命周期和素材对象管理需求。

### 2.2 类型划分

广告 SDK 不能抽象成一个统一的 `show()`。优先按“承载形态”和“交互语义”拆分。

| 广告类型 | 承载形态 | API 形态 | 关键事件 | 生命周期重点 |
| :--- | :--- | :--- | :--- | :--- |
| 开屏广告 | 页面首屏或启动页内的原生广告容器 | `State + Composable`，加载成功后展示到容器 | 加载、超时、无广告、展示、点击、关闭 | 启动链路超时兜底，关闭后释放容器 |
| Banner 广告 | 页面内固定尺寸区域 | `State + Composable`，根据布局宽度加载 | 加载、失败、展示、点击、关闭、自动刷新 | 尺寸变化去重，避免重组重复加载 |
| 信息流/原生广告 | 页面内可关闭广告卡片或模板广告 | `State + Composable`，成功后把广告 View 挂到容器 | 加载、失败、曝光、点击、关闭 | 原生 View 重挂载、素材对象释放、列表 key 稳定 |
| 插屏广告 | 全屏弹出，无奖励语义 | `State.show(container, lifecycle)` | 加载、失败、展示、点击、关闭 | 页面可见后再弹出，展示后预加载下一条 |
| 激励视频广告 | 全屏弹出，有奖励语义 | `State.show(container, lifecycle)` | 加载、失败、展示、点击、关闭、奖励 | 奖励事件必须独立返回，不能用关闭事件推断 |

设计结论：
- 嵌入式广告需要 Composable 承载原生 View。
- 全屏广告不需要 Composable，业务只调用 `show()`。
- 奖励类广告必须单独建模奖励事件，不能和点击、关闭混在一起。
- 开屏广告虽然也是全屏视觉，但常常需要嵌入启动页布局和底部品牌区，通常按“容器广告”处理。

### 2.3 接入目标

common 层要表达稳定广告能力，而不是某个 SDK 的平台对象：
- 初始化广告 SDK。
- 加载广告。
- 判断广告是否可展示。
- 展示广告。
- 重新加载。
- 接收加载状态。
- 接收展示、点击、关闭、奖励等交互事件。
- 接收广告源、收益、请求 ID 等广告信息。
- 销毁广告对象和原生 View。
- 查询 SDK 版本。

common 层不应该：
- 暴露 Android `View`、`Activity`、`Context`。
- 暴露 iOS `UIView`、`UIViewController`、`NSError`、`NSDictionary`。
- 把所有广告类型塞进一个大配置对象。
- 用一个 `Boolean` 表达加载、展示、关闭、奖励等多阶段结果。

### 2.4 推荐 common API

以下接口是案例形态，实际命名按目标 SDK 模块调整。

#### 2.4.1 SDK 级 API

```kotlin
expect object AdSdk {
    suspend fun init(
        enableDebug: Boolean = false,
        privacyAccepted: Boolean = true,
        deviceInfo: AdDeviceInfo? = null
    ): AdInitResult

    fun getSdkVersion(): String?
}
```

设计要点：
- 如果 SDK 初始化有异步结果，必须使用 `suspend` 返回 `AdInitResult`。
- 如果官方初始化确实是 fire-and-forget，才允许返回 `Unit`，并在接入报告中说明。
- 隐私、设备标识、调试日志等参数应显式建模，不依赖 SDK 默认值。

#### 2.4.2 嵌入式广告 API

```kotlin
class BannerAdState(
    scope: CoroutineScope,
    placementId: String,
    aspectRatio: Float
) {
    val loadStatus: StateFlow<AdLoadStatus>
    val actionEvents: SharedFlow<AdActionEvent>

    fun reload()

    fun destroy()
}

@Composable
fun SdkBannerAd(
    modifier: Modifier,
    state: BannerAdState
)
```

```kotlin
class NativeAdState(
    scope: CoroutineScope,
    placementId: String,
    aspectRatio: Float
) {
    val loadStatus: StateFlow<AdLoadStatus>
    val actionEvents: SharedFlow<AdActionEvent>

    fun reload()

    fun destroy()
}

@Composable
fun SdkNativeAd(
    modifier: Modifier,
    state: NativeAdState
)
```

设计要点：
- `State` 持有广告对象、原生容器、回调桥接和释放逻辑。
- `Composable` 只负责把平台广告容器放进 Compose 树，并把布局尺寸同步给 `State`。
- `reload()` 是业务显式重新加载入口，不应依赖重组自动重试。
- `destroy()` 必须由页面生命周期或持有者显式调用。

#### 2.4.3 全屏广告 API

```kotlin
class InterstitialAdState(
    placementId: String
) {
    val loadStatus: StateFlow<AdLoadStatus>
    val actionEvents: SharedFlow<AdActionEvent>

    suspend fun show(
        container: UIContainer,
        screenLifecycle: StateFlow<ScreenLifecycle>
    )

    fun reload()

    fun destroy()
}
```

```kotlin
class RewardVideoAdState(
    placementId: String
) {
    val loadStatus: StateFlow<AdLoadStatus>
    val actionEvents: SharedFlow<AdActionEvent>

    suspend fun show(
        container: UIContainer,
        screenLifecycle: StateFlow<ScreenLifecycle>
    )

    fun reload()

    fun destroy()
}
```

设计要点：
- `show()` 必须等广告加载成功且页面处于可见状态后再调用平台 SDK。
- 展示后应按业务策略预加载下一条，避免下一次点击时才加载。
- `show()` 不返回单次最终结果，展示、点击、关闭、奖励都通过 `actionEvents` 返回。
- 奖励视频不能用“关闭时 rewarded=true/false”替代奖励事件；如果 SDK 只有关闭回调携带奖励状态，必须在转换层明确映射。

#### 2.4.4 开屏广告 API

```kotlin
class SplashAdState(
    scope: CoroutineScope,
    placementId: String,
    bottomHeight: Dp,
    timeoutMillis: Long
) {
    val loadStatus: StateFlow<SplashAdLoadStatus>
    val actionEvents: SharedFlow<SplashAdActionEvent>

    fun reload()

    fun destroy()
}

@Composable
fun SdkSplashAd(
    modifier: Modifier,
    state: SplashAdState,
    bottom: @Composable ColumnScope.() -> Unit
)
```

设计要点：
- 开屏广告必须有超时结果，不能无限等待 SDK 回调。
- 底部品牌区、备案区、版权区通常由业务 Compose 提供，不放进 SDK 封装层。
- 加载失败、无广告、超时都要能被业务区分，便于进入首页或兜底页面。

### 2.5 数据模型设计

公共模型必须补齐 `@Serializable`，除非能明确证明只在平台内部使用。

#### 2.5.1 初始化结果

```kotlin
@Serializable
class AdInitResult(
    val success: Boolean,
    val code: String?,
    val message: String?,
    val raw: String?
)
```

#### 2.5.2 广告尺寸

```kotlin
@Serializable
class AdSize(
    val width: Int,
    val height: Int
) {
    companion object {
        fun byAspectRatio(width: Int, aspectRatio: Float): AdSize {
            return AdSize(width, (width / aspectRatio).toInt())
        }
    }
}
```

设计要点：
- Android 通常使用 px。
- iOS 通常使用 point。
- common 层必须明确单位转换位置，不能让业务猜测传 px 还是 dp。

#### 2.5.3 错误模型

```kotlin
@Serializable
class AdError(
    val code: String?,
    val message: String?,
    val raw: String?
)
```

设计要点：
- 错误码、错误描述、原始返回都要保留。
- Android/iOS 错误类型不同，必须在平台层转换为 common 模型。

#### 2.5.4 广告信息

```kotlin
@Serializable
class AdInfo(
    val placementId: String?,
    val adSourceId: String?,
    val adSourceName: String?,
    val networkName: String?,
    val requestId: String?,
    val showId: String?,
    val ecpm: Double?,
    val currency: String?,
    val revenue: Double?,
    val precision: String?,
    val scenarioId: String?,
    val rewardName: String?,
    val rewardAmount: Int?,
    val raw: String?
)
```

字段设计要点：
- `placementId`：广告位 ID。
- `requestId` / `showId`：排查曝光、收益、服务端对账时常用。
- `ecpm` / `revenue` / `currency`：广告收益相关字段，必须记录单位和平台字段路径。
- `rewardName` / `rewardAmount`：只在激励类广告真实提供时使用。
- `raw`：平台 SDK 原始广告信息，便于广告平台联调。

#### 2.5.5 加载状态

```kotlin
@Serializable
sealed class AdLoadStatus {
    @Serializable
    object Idle : AdLoadStatus()

    @Serializable
    object Loading : AdLoadStatus()

    @Serializable
    class Success(
        val adInfo: AdInfo? = null
    ) : AdLoadStatus()

    @Serializable
    class Fail(
        val error: AdError?
    ) : AdLoadStatus()
}
```

设计要点：
- `StateFlow` 建议使用 `Idle` 作为初始值。
- `Success` 可以携带广告信息，也可以只表示“可展示”，具体以 SDK 能力为准。
- 不能用 nullable 表示初始状态，业务侧难以区分未开始和异常空值。

#### 2.5.6 开屏加载状态

```kotlin
@Serializable
sealed class SplashAdLoadStatus {
    @Serializable
    object Idle : SplashAdLoadStatus()

    @Serializable
    object Loading : SplashAdLoadStatus()

    @Serializable
    class Success(
        val adInfo: AdInfo? = null
    ) : SplashAdLoadStatus()

    @Serializable
    class Fail(
        val error: AdError?,
        val type: FailType
    ) : SplashAdLoadStatus() {
        @Serializable
        enum class FailType {
            TIMEOUT,
            NO_AD,
            LOAD_ERROR
        }
    }
}
```

设计要点：
- 开屏广告必须区分超时、无广告和加载错误。
- 超时不是普通错误，业务通常要直接进入首页。

#### 2.5.7 交互事件

```kotlin
@Serializable
sealed class AdActionEvent {
    @Serializable
    class Show(
        val adInfo: AdInfo?
    ) : AdActionEvent()

    @Serializable
    class Click(
        val adInfo: AdInfo?
    ) : AdActionEvent()

    @Serializable
    class Close(
        val adInfo: AdInfo?
    ) : AdActionEvent()

    @Serializable
    class Reward(
        val adInfo: AdInfo?,
        val rewardName: String?,
        val rewardAmount: Int?
    ) : AdActionEvent()
}
```

```kotlin
@Serializable
class SplashAdActionEvent(
    val type: Type,
    val adInfo: AdInfo?,
    val closeType: CloseType? = null
) {
    @Serializable
    enum class Type {
        SHOW,
        CLICK,
        CLOSE
    }

    @Serializable
    enum class CloseType {
        UNKNOWN,
        NORMAL,
        CLICK_SKIP,
        COUNT_DOWN,
        CLICK_AD,
        SHOW_ERROR
    }
}
```

设计要点：
- 事件是一次性的，推荐用 `SharedFlow`。
- 状态是可观察的当前值，推荐用 `StateFlow`。
- 不要把奖励事件压缩成 `Close(rewarded = true)`，否则业务容易重复发奖或漏发奖。

### 2.6 用户操作流程

#### 2.6.1 嵌入式广告标准流程

```text
业务页面创建并持有 BannerAdState / NativeAdState
  -> Composable 渲染 SdkBannerAd / SdkNativeAd
  -> Compose 完成测量，拿到容器宽度
  -> State 接收 width
  -> State 根据 width + aspectRatio 计算平台广告尺寸
  -> androidMain / iosMain 创建或复用原生广告对象
  -> 平台 SDK 加载广告
  -> loadStatus = Loading
  -> SDK 回调加载成功
  -> loadStatus = Success
  -> 平台 actual 把原生广告 View 挂到 Compose 承载容器
  -> SDK 回调展示/点击/关闭
  -> actionEvents 发出 Show / Click / Close
  -> 页面销毁时调用 destroy()
```

关键点：
- 原生广告对象由 `State` 保活。
- Compose 重组只能导致尺寸同步，不能导致重复创建广告对象。
- 广告 View 重新挂载前必须先从旧父容器移除。
- 页面关闭或列表 item 移除时必须销毁广告对象和原生 View。

#### 2.6.2 插屏广告标准流程

```text
业务页面创建并持有 InterstitialAdState
  -> State 初始化后预加载广告
  -> loadStatus = Loading
  -> SDK 回调加载成功
  -> loadStatus = Success
  -> 用户触发展示时调用 show(container, screenLifecycle)
  -> show 等待 loadStatus = Success 且 ScreenLifecycle = Visible
  -> 平台 actual 调用 SDK 展示广告
  -> actionEvents 发出 Show / Click / Close
  -> 展示后按策略预加载下一条
  -> 页面销毁时调用 destroy()
```

关键点：
- 全屏广告必须等页面可见后再展示。
- 展示入口要防重复点击，避免多次调用平台 `show`。
- 广告关闭不等于广告加载失败，不能混用状态。

#### 2.6.3 激励视频标准流程

```text
业务页面创建并持有 RewardVideoAdState
  -> State 初始化后预加载广告
  -> 用户点击“看视频领奖励”
  -> 业务层做入口防重复点击
  -> 调用 show(container, screenLifecycle)
  -> SDK 展示视频
  -> actionEvents 发出 Show
  -> 用户看完达到 SDK 奖励条件
  -> actionEvents 发出 Reward
  -> 用户关闭广告
  -> actionEvents 发出 Close
  -> 业务层按 Reward 事件发起领奖或服务端校验
```

关键点：
- 奖励发放只能基于 `Reward` 事件或服务端校验结果，不能基于 `Close` 推断。
- 同一次广告展示中，Reward 和 Close 都可能触发，业务必须去重发奖。
- 如果 SDK 支持服务端回调校验，应优先以服务端结果为最终发奖依据。

#### 2.6.4 开屏广告标准流程

```text
启动页创建并持有 SplashAdState
  -> Composable 渲染 SdkSplashAd
  -> State 创建原生广告容器
  -> State 开始 load
  -> loadStatus = Loading
  -> SDK 加载成功
  -> loadStatus = Success
  -> 平台 actual 把广告展示到容器
  -> 用户点击、跳过、倒计时结束或展示失败
  -> actionEvents 发出 Show / Click / Close(closeType)
  -> 业务进入首页或下一步
  -> 调用 destroy()
```

关键点：
- 开屏广告必须有 `timeoutMillis`。
- `TIMEOUT`、`NO_AD`、`LOAD_ERROR` 要分开建模。
- 底部品牌、版权或备案区域由业务提供，不强耦合到 SDK 封装层。

### 2.7 数据流转

#### 2.7.1 初始化数据流

```text
业务隐私状态 / debug 开关 / 设备信息
  -> common init(...)
  -> androidMain / iosMain actual
  -> 平台 SDK 初始化
  -> 平台回调或同步结果转换为 AdInitResult
  -> 业务根据 success 决定是否加载广告
```

审计要求：
- SDK 是否支持隐私授权前预初始化。
- SDK 是否在初始化时采集设备标识。
- SDK 是否要求 Android Manifest 权限或 iOS Info.plist 字段。
- SDK 是否有异步初始化结果。

#### 2.7.2 加载数据流

```text
广告位 ID + 尺寸/场景参数
  -> State.reload() 或尺寸变化触发 load
  -> actual 调用平台 SDK load
  -> 平台回调 loaded / failed / timeout / no ad
  -> 转换为 AdLoadStatus 或 SplashAdLoadStatus
  -> 业务页面展示占位、广告或兜底内容
```

审计要求：
- 成功回调是否携带广告信息。
- 失败回调是否区分无填充、网络错误、配置错误、超时。
- 同一次 load 是否可能多次回调。
- load 成功后是否必须单独调用 render 或 show。

#### 2.7.3 展示与交互数据流

```text
平台 SDK 展示广告
  -> show / impression 回调
  -> click 回调
  -> close 回调
  -> reward 回调
  -> 平台 adInfo/error 转 common 模型
  -> actionEvents 发出一次性事件
  -> 业务埋点、发奖、关闭页面、预加载下一条
```

审计要求：
- 曝光回调和展示成功回调是否是同一个概念。
- 点击后是否会关闭广告。
- 关闭原因是否有枚举。
- 激励视频是否存在播放完成、奖励成功、播放失败三个独立回调。
- 广告收益字段是否在展示回调、关闭回调或专门收益回调中返回。

### 2.8 平台实现职责

androidMain / iosMain 负责：
- 创建平台广告对象。
- 创建平台广告容器。
- 调用平台 load/show/render/destroy。
- 保活 listener、delegate、广告对象、广告 View。
- 将平台错误、广告信息、关闭原因、奖励信息转换为 common 模型。
- 在销毁时清理 listener、delegate、View、Controller、广告素材对象。

commonMain 负责：
- 定义 State、结果模型、状态模型、事件模型。
- 暴露 Composable 承载入口或全屏 `show()`。
- 管理加载状态和交互事件。
- 避免平台类型泄露。
- 提供显式 `reload()` 和 `destroy()`。

业务层负责：
- 持有广告 State。
- 控制广告入口防重复点击。
- 决定广告加载失败时的兜底 UI。
- 决定激励视频奖励发放策略。
- 在页面销毁时调用 `destroy()`。

### 2.9 Compose 显示与重组规避

嵌入式广告：
- 用 `BoxWithConstraints` 或等价方式等布局测量完成后再加载。
- 宽度为 0 时不得加载广告。
- 尺寸变化必须去重，例如 `distinctUntilChanged()`。
- iOS 需要把 Compose px 转成 point，Android 通常使用 px；转换必须封装在 SDK 模块内部。
- `State.getOrCreate()` 必须保证原生广告对象只创建一次。
- 原生 View 被重新挂载前，必须先从旧父容器移除。
- 列表中使用广告时，广告 item 必须有稳定 key。

全屏广告：
- 不放在 Composable 中自动弹出。
- 由业务事件触发 `show(container, screenLifecycle)`。
- `show()` 内部等待页面处于可见状态。
- `show()` 调用后必须防止同一广告对象被并发展示。

State 持有规则：
- State 应由 ScreenModel 或页面等稳定生命周期对象持有。
- 禁止在 Composable 函数体中直接 `AdState(...)`。
- 如果使用 `remember` 创建 State，必须配合 `DisposableEffect` 调用 `destroy()`，并确认不会跨页面泄露。
- 广告 State 内部启动的协程必须跟随可取消 scope；长期 `applicationScope` 只适合全局广告缓存，不适合页面广告 View。

### 2.10 内存释放与泄露规避

必须释放的对象：
- Android `View` / `ViewGroup`。
- iOS `UIView`。
- 平台广告对象。
- 广告素材对象。
- listener / callback / delegate。
- 持有 `Activity`、`UIViewController`、`UIContainer` 的引用。
- 持续 collect 的 Flow 任务。

释放策略：
- 每个广告 State 必须有 `destroy()`。
- `destroy()` 必须幂等，多次调用不能崩溃。
- Android 侧需要清空 listener，并从父容器移除广告 View。
- iOS 侧需要清空 delegate/callback，并 `removeFromSuperview()`。
- 原生广告素材对象如果有专用 destroy/destory 方法，必须调用。
- 嵌入式广告容器销毁时必须 `removeAllViews()` 或等价清理。
- iOS delegate 若 SDK 不强持有，必须由 State 或 actual class 属性保活。
- 不确定 SDK 是否强持有 delegate 时，按“不强持有”处理。

高风险场景：
- 在 `reload()` 中重复启动 `collect`。
- 页面销毁后仍用长生命周期 scope 持有广告 View。
- iOS delegate 只存在于局部变量。
- Android listener 匿名对象持有外部页面引用。
- 原生广告 View 被多个父容器重复挂载。
- 激励视频重复触发奖励事件导致重复发奖。

### 2.11 状态与事件维护策略

推荐规则：
- 当前加载状态用 `StateFlow<AdLoadStatus>`。
- 一次性交互用 `SharedFlow<AdActionEvent>`。
- 初始状态用 `Idle`，不要用 `null`。
- 加载失败后由业务决定是否 `reload()`。
- 展示成功、点击、关闭、奖励都是事件，不应写进加载状态。
- 加载状态与交互事件都应携带 `AdInfo?`，但不能要求所有 SDK 都一定有值。

并发控制：
- 同一个广告位同一时间只允许一个 load 任务。
- 同一个全屏广告对象同一时间只允许一个 show 任务。
- 如果业务连续点击展示按钮，封装层可以返回“当前不可展示/正在展示”事件，但入口防重复点击仍由业务层负责。
- 如果 SDK 文档明确回调可能多次触发，API 必须保持 Flow/lambda，不得转换成单次 suspend 返回。

### 2.12 字段路径审计

广告 SDK 字段多且双端差异大，必须建立字段路径矩阵。

| 字段类别 | common 字段 | Android 字段路径 | iOS 字段路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 广告位 | `placementId` | 待填 | 待填 | 广告平台配置 ID |
| 请求 ID | `requestId` | 待填 | 待填 | 排障、对账常用 |
| 展示 ID | `showId` | 待填 | 待填 | 曝光链路标识 |
| 广告源 | `adSourceId/adSourceName` | 待填 | 待填 | 聚合广告源 |
| 网络名称 | `networkName` | 待填 | 待填 | 第三方广告网络 |
| 收益 | `ecpm/revenue/currency` | 待填 | 待填 | 必须确认单位 |
| 关闭原因 | `closeType` | 待填 | 待填 | 开屏/插屏常见 |
| 奖励 | `rewardName/rewardAmount` | 待填 | 待填 | 激励视频专用 |
| 错误码 | `error.code` | 待填 | 待填 | 不同平台可能类型不同 |
| 原始返回 | `raw` | 待填 | 待填 | 用于联调 |

强制要求：
- 不能只按字段名映射。
- 收益字段必须确认币种、单位、精度。
- 关闭原因必须确认枚举含义。
- 奖励事件必须确认触发时机。
- Android 和 iOS 字段名相同也不能假设含义完全一致。

### 2.13 验收清单

| 检查项 | 期望 |
| :--- | :--- |
| 初始化 | 有明确初始化结果或说明 fire-and-forget 原因 |
| 广告类型 | 按开屏、Banner、原生/信息流、插屏、激励视频拆分 |
| 加载状态 | `Idle/Loading/Success/Fail` 可表达完整状态 |
| 交互事件 | 展示、点击、关闭、奖励分开建模 |
| 开屏失败 | 超时、无广告、加载错误可区分 |
| 嵌入式显示 | Composable 内承载原生 View，不暴露平台类型 |
| 全屏显示 | `show(container, lifecycle)` 等页面可见后展示 |
| 重组安全 | 尺寸变化去重，不因重组重复创建广告对象 |
| 内存释放 | `destroy()` 清理 listener/delegate/View/广告对象 |
| iOS delegate | delegate 被长生命周期对象保活 |
| Android View | View 重新挂载前从旧父容器移除 |
| 激励视频 | Reward 事件独立，业务可避免重复发奖 |
| 字段路径 | 广告信息、收益、错误码、关闭原因、奖励字段路径已审计 |
| 公共模型 | 暴露给业务的模型补齐 `@Serializable` |

### 2.14 接入报告必须说明的问题

接入广告类 SDK 时，报告中必须回答：
- SDK 支持哪些广告类型。
- 每种广告是嵌入式展示还是全屏展示。
- 初始化是否有异步结果。
- 是否支持隐私授权前预初始化。
- Android/iOS 需要哪些权限、Manifest、Info.plist 或 SKAdNetwork 配置。
- 每种广告的加载成功、加载失败、展示、点击、关闭、奖励回调分别是什么。
- 回调是否可能多次触发。
- 全屏广告是否允许重复 show，SDK 如何处理并发展示。
- 嵌入式广告 View 是否可以复用，是否必须每次重新创建。
- iOS delegate 是否由 SDK 强持有。
- Android listener 是否需要手动清空。
- 广告素材对象是否有专用销毁 API。
- 广告收益字段有哪些，单位和币种是什么。
- 激励视频奖励事件触发时机是什么，是否需要服务端校验。
- 开屏广告超时、无广告、展示失败如何区分。
- 业务层何时调用 `destroy()`。

## 3. 支付类 SDK 接入案例

### 3.1 适用场景

适用于以下 SDK 类型：
- App 支付。
- 第三方钱包支付。
- 银行卡、银联或聚合收银台支付。
- 跳转外部 App 完成支付。
- 支付签约、免密签约、自动续费签约。
- 通过 URL Scheme、Universal Link、Activity、Intent 或 SDK callback 返回支付结果的能力。

不适用于：
- 纯服务端支付，不需要客户端调起 SDK 或外部 App。
- 纯 WebView 收银台，且没有原生 SDK 回调、URL 回流或生命周期兜底。
- 只生成支付二维码，由用户在其他设备扫码完成支付的场景。

### 3.2 类型划分

支付 SDK 不能只按渠道名设计接口，应先按“调起方式”和“回调方式”拆分。

| 类型 | 发起方式 | 回调方式 | 关键风险 |
| :--- | :--- | :--- | :--- |
| 外部 App 支付 | 组装支付请求后调起第三方 App | Android 回调 Activity、iOS URL Scheme / Universal Link | 外部 App 未安装、调起失败、回调路径配置错误、回调丢失 |
| SDK 内置收银台 | 传入订单字符串或支付参数，SDK 展示收银台 | 同步 callback、异步 callback、URL 回流补偿 | 同步返回不等于订单最终态、重复提交、页面生命周期异常 |
| 聚合支付 | 业务传入渠道和订单参数，SDK 内部选择渠道 | 多渠道 callback 或统一 callback | 渠道错误码差异、状态语义不一致 |
| 签约/自动续费 | 传入签约参数或签约 URL | URL Scheme、Universal Link、后台配置回调页 | 后台路径强绑定，签约成功不等于首笔扣款成功 |

设计结论：
- 普通支付、签约支付、自动续费不能混用一个状态模型。
- 客户端成功只表示渠道侧返回成功，不等于订单最终成功。
- 所有支付链路都必须保留“需查单”或“超时待确认”状态。
- 支付回调必须与宿主解耦，宿主只做统一分发。

### 3.3 接入目标

common 层要表达稳定支付能力，而不是某个支付渠道的原生对象：
- 初始化支付 SDK。
- 判断目标支付 App 或支付能力是否可用。
- 发起支付。
- 接收支付状态变化。
- 接收失败码、失败原因和原始返回。
- 表达取消、失败、成功、超时和需查单。
- 支持页面生命周期兜底。
- 支持支付回调与宿主入口解耦。
- 查询 SDK 版本。

common 层不应该：
- 暴露 Android `Activity`、`Intent`、`Bundle`、支付 SDK `Resp` 对象。
- 暴露 iOS `NSURL`、`NSUserActivity`、`UIViewController`、`NSDictionary`、`NSError`。
- 只返回 `Boolean` 或只返回 `Success/Fail`。
- 把客户端支付成功当成业务订单最终成功。
- 把支付、分享、授权登录混成一个不可区分的结果流。

### 3.4 推荐 common API

以下接口是案例形态，实际命名按目标 SDK 模块调整。

#### 3.4.1 SDK 级 API

```kotlin
expect object PaymentSdk {
    suspend fun init(
        enableDebug: Boolean = false
    ): PaymentInitResult

    fun isChannelAvailable(
        channel: PaymentChannel
    ): Boolean

    fun getSdkVersion(
        channel: PaymentChannel
    ): String?
}
```

设计要点：
- 初始化若有异步结果，必须用 `suspend` 返回明确结果。
- 如果 SDK 只要求注册 AppId 且无回调，可返回 `Unit`，但接入报告必须说明。
- AppId、Universal Link、URL Scheme 等核心配置不从 common API 传入，平台 actual 从配置系统读取。

#### 3.4.2 支付 API

```kotlin
expect object PaymentClient {
    fun pay(
        request: PaymentRequest,
        scope: CoroutineScope,
        lifecycle: StateFlow<ScreenLifecycle>,
        onStatusChange: suspend (PaymentStatus) -> Unit
    ): Boolean
}
```

设计要点：
- `pay()` 返回 `Boolean`，只表示是否成功提交 SDK 调起请求。
- 最终状态通过 `onStatusChange` 返回。
- 不推荐设计为单次 `suspend fun pay(): PaymentStatus`，因为支付回调可能多次、丢失、延迟或需要生命周期兜底。
- 业务层负责入口防重复点击；SDK 封装层负责避免并发支付串扰。

#### 3.4.3 签约/自动续费 API

```kotlin
expect object PaymentContractClient {
    fun sign(
        request: PaymentContractRequest,
        scope: CoroutineScope,
        lifecycle: StateFlow<ScreenLifecycle>,
        onStatusChange: suspend (PaymentContractStatus) -> Unit
    ): Boolean
}
```

设计要点：
- 签约成功只表示协议签约成功，不等于扣款成功。
- 签约状态必须独立于普通支付状态。
- 如果签约回调路径由支付平台后台强绑定，必须在接入报告和 SDK 资料归档中记录。

### 3.5 数据模型设计

公共模型必须补齐 `@Serializable`，除非能明确证明只在平台内部使用。

#### 3.5.1 支付渠道

```kotlin
@Serializable
enum class PaymentChannel {
    WECHAT,
    ALIPAY,
    UNION_PAY,
    BANK_CARD,
    AGGREGATE,
    OTHER
}
```

#### 3.5.2 初始化结果

```kotlin
@Serializable
class PaymentInitResult(
    val success: Boolean,
    val channel: PaymentChannel?,
    val code: String?,
    val message: String?,
    val raw: String?
)
```

#### 3.5.3 支付请求

```kotlin
@Serializable
sealed class PaymentRequest {
    abstract val channel: PaymentChannel
    abstract val paymentId: String?
    abstract val orderId: String?

    @Serializable
    class OrderString(
        override val channel: PaymentChannel,
        override val paymentId: String?,
        override val orderId: String?,
        val orderInfo: String
    ) : PaymentRequest()

    @Serializable
    class AppPayParams(
        override val channel: PaymentChannel,
        override val paymentId: String?,
        override val orderId: String?,
        val partnerId: String?,
        val prepayId: String?,
        val packageValue: String?,
        val nonceStr: String?,
        val timestamp: String?,
        val sign: String?
    ) : PaymentRequest()
}
```

设计要点：
- `paymentId` 是客户端本次支付流程 ID，用于避免并发串扰。
- `orderId` 是业务订单 ID，用于查单。
- 第三方支付参数必须来自服务端，客户端不拼签名。
- 不同渠道参数差异较大，优先用 sealed class，避免一个超大 nullable 参数对象。

#### 3.5.4 支付状态

```kotlin
@Serializable
sealed class PaymentStatus {
    @Serializable
    class Paying(
        val paymentId: String?,
        val orderId: String?
    ) : PaymentStatus()

    @Serializable
    class Launched(
        val paymentId: String?,
        val orderId: String?
    ) : PaymentStatus()

    @Serializable
    class Success(
        val paymentId: String?,
        val orderId: String?,
        val channel: PaymentChannel,
        val raw: String?
    ) : PaymentStatus()

    @Serializable
    class Cancel(
        val paymentId: String?,
        val orderId: String?,
        val channel: PaymentChannel,
        val code: String?,
        val message: String?,
        val raw: String?
    ) : PaymentStatus()

    @Serializable
    class Fail(
        val paymentId: String?,
        val orderId: String?,
        val channel: PaymentChannel,
        val code: String?,
        val message: String?,
        val raw: String?
    ) : PaymentStatus()

    @Serializable
    class Timeout(
        val paymentId: String?,
        val orderId: String?,
        val channel: PaymentChannel,
        val message: String?
    ) : PaymentStatus()

    @Serializable
    class NeedQueryOrder(
        val paymentId: String?,
        val orderId: String?,
        val channel: PaymentChannel,
        val reason: String?
    ) : PaymentStatus()
}
```

字段设计要点：
- `Success` 不是业务订单最终成功，业务仍应查单或等待服务端通知。
- `Timeout` 表示客户端支付回调未按预期到达。
- `NeedQueryOrder` 表示客户端无法给出可信最终态，业务必须查服务端。
- `Fail.code/message/raw` 必须结构化，不能只给一个拼接后的字符串。
- 支付回调必须带回 `paymentId/orderId`，防止并发支付串扰。

#### 3.5.5 签约状态

```kotlin
@Serializable
sealed class PaymentContractStatus {
    @Serializable
    object Signing : PaymentContractStatus()

    @Serializable
    class Success(
        val contractId: String?,
        val raw: String?
    ) : PaymentContractStatus()

    @Serializable
    class Cancel(
        val code: String?,
        val message: String?,
        val raw: String?
    ) : PaymentContractStatus()

    @Serializable
    class Fail(
        val code: String?,
        val message: String?,
        val raw: String?
    ) : PaymentContractStatus()

    @Serializable
    class Timeout(
        val message: String?
    ) : PaymentContractStatus()

    @Serializable
    class NeedQueryContract(
        val reason: String?
    ) : PaymentContractStatus()
}
```

设计要点：
- 签约状态不要复用支付状态。
- 签约成功后的扣款结果必须由服务端或后续业务接口确认。

### 3.6 用户操作流程

#### 3.6.1 普通支付标准流程

```text
用户点击支付按钮
  -> 业务层做防重复点击
  -> 业务服务端创建订单
  -> 服务端返回支付参数或 orderInfo
  -> 业务调用 PaymentClient.pay(...)
  -> pay() 返回 true，表示 SDK 调起请求已提交
  -> onStatusChange(Paying)
  -> 平台 actual 调用支付 SDK 或外部 App
  -> 页面进入 Invisible，说明外部 App 或收银台大概率已拉起
  -> 第三方支付完成、取消或失败
  -> Android Activity / iOS URL Scheme / Universal Link / SDK callback 返回
  -> SDK 封装层转换为 PaymentStatus
  -> 业务收到 Success / Cancel / Fail
  -> 业务向服务端查单或等待服务端通知
  -> 业务根据服务端最终订单状态更新 UI
```

关键点：
- 支付按钮防重复点击由业务层处理。
- SDK 封装层必须防止并发支付结果串扰。
- 客户端 `Success` 后仍应查单。
- 客户端 `Cancel` 一般可直接终止当前支付流程，但仍可按业务策略查单。
- 客户端 `Fail` 要展示错误或允许重新支付。

#### 3.6.2 调起失败流程

```text
业务调用 PaymentClient.pay(...)
  -> 平台 actual 检查 SDK 初始化、目标 App 安装、参数完整性
  -> 检查失败
  -> pay() 返回 false 或 onStatusChange(Fail)
  -> 业务提示用户安装 App、切换支付方式或重试
```

关键点：
- 未安装目标 App、未初始化 SDK、参数缺失，应尽早失败。
- `pay()` 返回 false 只表示没有成功提交调起请求，不代表订单失败。
- 参数缺失属于接入或服务端问题，应保留 code/message/raw。

#### 3.6.3 回调丢失/超时流程

```text
支付请求已提交
  -> 等待页面变为 Invisible
  -> 超过调起等待时间仍 Visible
  -> onStatusChange(Timeout / NeedQueryOrder)
  -> 或页面从 Invisible 恢复 Visible 后仍未收到支付回调
  -> onStatusChange(Timeout / NeedQueryOrder)
  -> 业务向服务端查单
```

关键点：
- 支付 SDK 回调不能保证必达。
- 生命周期兜底是支付 SDK 封装的核心能力。
- 超时不应直接当失败，应优先引导查单。

#### 3.6.4 签约/自动续费流程

```text
用户点击开通自动续费
  -> 业务服务端创建签约请求
  -> 服务端返回签约参数或签约 URL
  -> 业务调用 PaymentContractClient.sign(...)
  -> 平台 actual 调起支付平台签约页
  -> URL Scheme / Universal Link / callback 返回签约结果
  -> SDK 封装层转换为 PaymentContractStatus
  -> 业务查询服务端签约状态
```

关键点：
- 签约成功不等于支付成功。
- 签约回调路径经常与平台后台配置强绑定，必须归档。
- 签约状态和普通支付状态不要共用同一个结果流。

### 3.7 数据流转

#### 3.7.1 支付参数数据流

```text
业务订单信息
  -> 服务端创建支付单
  -> 服务端生成渠道支付参数和签名
  -> common PaymentRequest
  -> androidMain / iosMain actual
  -> 平台 SDK PayReq / orderInfo / payment params
  -> 第三方支付 App 或收银台
```

审计要求：
- 客户端不生成支付签名。
- 服务端返回参数必须逐字段对照 SDK 文档。
- 参数字段名相同也要确认平台类型，例如时间戳可能是 String、Long、UInt。
- `orderId/paymentId` 必须贯穿请求和回调。

#### 3.7.2 回调数据流

```text
平台支付回调
  -> Android Activity / Intent
  -> iOS URL Scheme / Universal Link
  -> SDK callback
  -> 平台 actual 解析 code / message / raw
  -> 转换为 PaymentStatus
  -> common 回调业务
  -> 业务查单
```

审计要求：
- 同一渠道可能同时存在 callback 和 URL 回流。
- Android 冷启动、热启动、singleTask/newIntent 都要覆盖。
- iOS URL Scheme、Universal Link、冷启动参数都要覆盖。
- 回调解析要区分支付、签约、分享、授权登录等不同业务类型。

#### 3.7.3 服务端查单数据流

```text
客户端收到 Success / Timeout / NeedQueryOrder
  -> 业务调用服务端查单
  -> 服务端读取支付平台通知或主动查渠道订单
  -> 服务端返回最终订单状态
  -> 业务更新订单 UI
```

审计要求：
- 客户端成功只作为查单触发信号。
- 超时、回调丢失、重复提交、处理中都应进入查单。
- 服务端最终态应覆盖客户端临时态。

### 3.8 支付回调与宿主解耦

支付回调解耦是支付 SDK 接入红线。宿主入口只允许做统一生命周期、Intent、URL Scheme、Universal Link 分发，不允许写具体支付渠道逻辑。

#### 3.8.1 Android 解耦方式

推荐结构：

```text
宿主 MainActivity
  -> 只调用 ApplicationProxyManager.androidMainActivityOnCreate/onNewIntent

SDK lib AndroidManifest
  -> 声明支付 SDK 要求的回调 Activity 或 activity-alias

SDK lib 回调 Activity
  -> 接收 SDK Intent
  -> 调用 SDK handleIntent
  -> 将平台 resp 转换为 PaymentStatus
  -> emit 到模块内部结果流
  -> finish()
```

设计要点：
- 如果支付 SDK 要求固定包名路径，优先用 `activity-alias` 指向 SDK lib 内真实 Activity。
- 真实回调 Activity 放在 SDK lib 内，不放在 app 模块。
- Activity 只做回调分发和状态转换，不写业务订单逻辑。
- 新增 Activity 默认继承 `FragmentActivity`，除非官方要求其他父类。
- Android Manifest 权限、queries、回调 Activity、alias 收敛在 SDK lib。

#### 3.8.2 iOS 解耦方式

推荐结构：

```text
Swift SceneDelegate / AppDelegate
  -> 只把 URL Scheme / Universal Link / cold start options 转给 KMP AppDelegate

KMP AppDelegate
  -> 只调用 ApplicationProxyManager

ApplicationProxyManager
  -> 遍历所有 ApplicationService

支付 SDK lib ApplicationService
  -> 识别自己的 URL Scheme / Universal Link
  -> 调用平台 SDK process / handle
  -> 将平台结果转换为 PaymentStatus
  -> emit 到模块内部结果流
```

设计要点：
- Swift 层不写具体支付渠道逻辑。
- KMP app 模块不写具体支付渠道逻辑。
- 每个支付 SDK lib 通过自己的 `ApplicationService` 消费回调。
- 多个支付 SDK 并存时，只新增各自 lib 的服务实现，不反复修改宿主入口。
- iOS delegate/callback 若 SDK 不强持有，必须由 lib 内长生命周期对象保活。

### 3.9 生命周期兜底策略

支付调起后，客户端不能只等 SDK 回调。推荐至少做两段兜底：
- 调起兜底：发起支付后，如果一段时间内页面仍然 `Visible`，说明外部 App 或收银台可能没有成功调起。
- 回流兜底：页面从 `Invisible` 回到 `Visible` 后，如果仍未收到支付结果，说明回调可能丢失。

推荐流程：

```text
emit Paying
  -> 调用 SDK send/pay
  -> 等待 ScreenLifecycle.Invisible，超时则 emit Timeout("调起超时")
  -> 等待 ScreenLifecycle.Visible
  -> 延迟一个短时间窗口等待回调落地
  -> 如果仍无最终态，emit NeedQueryOrder("未收到支付结果")
```

设计要点：
- 调起超时时间要可配置，默认可以从 8 到 15 秒之间取值。
- 回到前台后应短暂延迟，给 SDK callback 或 URL 分发一点落地时间。
- 超时和未收到结果不要直接当失败，应交给业务查单。
- 生命周期兜底需要业务传入页面级 `CoroutineScope` 和 `ScreenLifecycle`。

### 3.10 并发与串扰规避

支付 SDK 通常是全局回调，必须额外防止串扰。

强制规则：
- 同一时间默认只允许一个支付流程。
- 每次支付必须生成或传入 `paymentId`。
- 回调状态必须携带 `paymentId/orderId`。
- 如果 SDK 回调无法携带业务 ID，封装层必须维护当前活跃支付上下文。
- 新支付开始前，如果已有支付流程未结束，必须返回 false 或发出明确失败状态。
- 业务层必须做支付按钮防重复点击。

高风险场景：
- 全局 `SharedFlow` 无支付 ID，多个支付并发时结果串扰。
- 用户连续点击多个支付渠道。
- 支付调起后页面被销毁，scope 取消导致结果无人接收。
- 回调晚到，覆盖了下一笔支付状态。

### 3.11 平台实现职责

androidMain / iosMain 负责：
- 读取平台配置，例如 AppId、Universal Link、URL Scheme。
- 判断目标支付 App 是否安装或支付能力是否可用。
- 调用平台 SDK 发起支付。
- 处理 Activity、Intent、URL Scheme、Universal Link、callback。
- 将平台错误码、取消码、成功码转换为 common 状态。
- 维护当前活跃支付上下文。
- 在必要时保活 delegate/callback。

commonMain 负责：
- 定义请求模型、状态模型、渠道枚举。
- 暴露发起支付 API。
- 避免平台类型泄露。
- 定义生命周期兜底语义。
- 规定客户端结果和服务端最终态的边界。

业务层负责：
- 创建订单和获取支付参数。
- 支付入口防重复点击。
- 展示支付中、取消、失败、待确认 UI。
- 在 Success、Timeout、NeedQueryOrder 时查单。
- 根据服务端最终态更新订单状态。

### 3.12 字段路径审计

支付 SDK 字段必须建立字段路径矩阵。

| 字段类别 | common 字段 | Android 字段路径 | iOS 字段路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 支付流程 ID | `paymentId` | 待填 | 待填 | 客户端本次支付流程 ID |
| 业务订单 ID | `orderId` | 待填 | 待填 | 服务端查单依据 |
| 成功码 | `Success.code` | 待填 | 待填 | 渠道成功码 |
| 取消码 | `Cancel.code` | 待填 | 待填 | 用户取消码 |
| 失败码 | `Fail.code` | 待填 | 待填 | 渠道失败码 |
| 错误描述 | `message` | 待填 | 待填 | 原始错误描述 |
| 重复提交 | `NeedQueryOrder/Processing` | 待填 | 待填 | 不应静默吞掉 |
| 原始返回 | `raw` | 待填 | 待填 | 用于联调 |

强制要求：
- 支付成功码、取消码、处理中码、重复提交码必须逐渠道确认。
- 支付参数字段必须逐字段对照文档和服务端返回。
- iOS URL host/path 与 Android Activity path 必须记录。
- 如果客户端回调无法携带订单 ID，必须记录封装层如何关联当前支付上下文。

### 3.13 安全与服务端边界

强制规则：
- 客户端不生成支付签名。
- 客户端不保存支付密钥。
- 客户端不把支付成功作为发货、开通会员、充值到账的最终依据。
- 订单最终态以服务端查单或支付平台异步通知为准。
- 支付参数、签约参数、回调 URL、Universal Link、URL Scheme 必须归档。
- 敏感字段归档前必须脱敏。

业务提示：
- `Success` 后显示“支付确认中”比直接显示“支付成功”更安全。
- `Timeout` 和 `NeedQueryOrder` 应提供查单或刷新订单状态入口。
- `Cancel` 可以允许重新选择支付方式。
- `Fail` 应保留错误码，便于客服和服务端排查。

### 3.14 验收清单

| 检查项 | 期望 |
| :--- | :--- |
| 支付状态 | Paying、Success、Cancel、Fail、Timeout、NeedQueryOrder 可表达 |
| 客户端/服务端边界 | 客户端成功不作为订单最终态 |
| 支付请求 | 有 paymentId/orderId，参数来自服务端 |
| 并发控制 | 同时只允许一个活跃支付流程或有明确隔离 |
| Android 回调 | Activity/alias 收敛在 SDK lib，宿主无支付逻辑 |
| iOS 回调 | URL/UL 通过 ApplicationService 在 SDK lib 内消费 |
| 生命周期兜底 | 调起超时和回流无结果可触发 Timeout/NeedQueryOrder |
| 错误模型 | code/message/raw 结构化保留 |
| 签约支付 | 与普通支付状态隔离 |
| 公共模型 | 暴露给业务的模型补齐 `@Serializable` |
| 资料归档 | 支付参数、回调路径、URL Scheme、Universal Link、错误码已归档 |

### 3.15 接入报告必须说明的问题

接入支付类 SDK 时，报告中必须回答：
- SDK 支持哪些支付渠道。
- 每个渠道的发起参数来自哪里，是否全部由服务端生成。
- 是否需要初始化，初始化是否有异步结果。
- Android 是否需要固定回调 Activity 路径。
- Android 是否可以用 `activity-alias` 解耦宿主路径和 lib 内真实 Activity。
- iOS 需要哪些 URL Scheme、Universal Link、Associated Domains 或 Info.plist 配置。
- iOS URL Scheme、Universal Link、冷启动参数分别如何处理。
- 宿主入口是否只做统一分发，支付逻辑是否完全在 SDK lib 内。
- 成功码、取消码、失败码、重复提交码、处理中码分别是什么。
- 回调是否可能丢失或多次触发。
- 是否有同步 callback 和 URL 回流同时存在。
- 如何判断外部 App 或收银台是否成功调起。
- 页面回到前台但未收到回调时如何查单。
- 是否允许并发支付，如何通过 paymentId/orderId 防串扰。
- 客户端 Success 后业务如何查单确认最终态。
- 支付、签约、授权登录、分享是否使用了相互隔离的状态流。
