# KmpProject 关键 API 文档

> 面向 AI/开发者检索。本文按 API 所在层级分组，列出用途、签名、调用约定和代码位置。

## 核心规则速查清单

> ⚠️ **每次使用 API 前，必须快速扫描此清单！**

### 路由 API
- `@Router`：页面路由注解，注册路由路径
- `@Params`：路由参数注解
- `asRouter(url)`：生成路由 URL
- `ProjectRouter`：路由接口，定义导航方法

### 导航 API
- `ScreenRouter.push()`：推入新页面
- `ScreenRouter.pop()`：弹出当前页面
- `ScreenRouter.replace()`：替换当前页面

### SPI API
- `registerSPI()`：注册 SPI 服务实现
- `SPIRegisterCenter`：SPI 注册中心
- 服务发现通过 SPI + Koin 组合完成

### 网络 API
- `Http`：Ktorfit 网络客户端
- `TestApi`：示例 API 接口定义
- 网络配置在 `core/common/net/` 中

### 弹窗 API
- `DialogController.showNow()`：普通弹窗
- `DialogController.showPriority()`：优先级弹窗
- `DialogController.showMaxPriority()`：最高优先级弹窗

### WebView API
- `WebViewState`：WebView 状态管理
- `NativeWebView`：原生 WebView 组件

### 下载 API
- `DownloadManager`：下载管理器

### 数据存储 API
- `Settings`：DataStore 全局设置

### 平台能力 API
- `ApplicationService`：应用生命周期、DeepLink
- `PermissionController`：权限控制器
- `expect/actual`：跨平台平台能力桥接

---

## 1. App 入口 API

### `App`

| 项 | 内容 |
| --- | --- |
| 位置 | `app/src/commonMain/kotlin/com/basic/app/App.kt` |
| 签名 | `@Composable fun App(screen: () -> Screen = { SplashScreen() }, uiContainer: UIContainer, permissionController: PermissionController)` |
| 作用 | KMP 共享 Compose UI 入口。 |
| 输入 | 初始 `Screen`、平台 UI 容器、权限控制器。 |
| 依赖 | `BasicApp -> BaseApp`。 |

示例：

```kotlin
setContent {
    App(uiContainer = this, permissionController = this)
}
```

### `initKoin`

| 项 | 内容 |
| --- | --- |
| 位置 | `app/src/commonMain/kotlin/com/basic/app/App.kt` |
| 签名 | `fun initKoin()` |
| 作用 | 启动 Koin 并注册 `commonModule`、`projectModule`。 |
| 注意 | 实际服务发现通过 `SPIRegisterCenter` 完成，Koin module 只承担集中注册入口。 |

## 2. 生命周期与服务 API

### `ApplicationService`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/di/service/ApplicationService.kt` |
| 作用 | 跨平台应用生命周期、DeepLink/Universal Link、Android Intent 分发接口。 |

签名：

```kotlin
interface ApplicationService {
    fun onCreate()
    fun onBackground()
    fun onForeground()
    fun iosSceneContinueUserActivity(userActivity: IosNSUserActivity) {}
    fun iosSceneOpenURLContexts(urlContexts: Set<IosUIOpenURLContext>) {}
    fun iosSceneWillConnectToOptions(
        userActivities: Set<IosNSUserActivity>,
        urlContexts: Set<IosUIOpenURLContext>
    ) {}
    fun androidMainActivityOnCreate(intent: AndroidIntent?) {}
    fun androidMainActivityOnNewIntent(intent: AndroidIntent?) {}
}
```

当前实现：

| 实现 | 位置 | 行为 |
| --- | --- | --- |
| `common.di.impl.ApplicationServiceImpl` | `core/common` | 调用 `common.Application.onCreate()`，Android 会启动 APK 环境定时校验。 |
| `project.di.impl.ApplicationServiceImpl` | `shared_project` | App 创建时预加载 WebKit：`preloadWebkit("https://xxxx.com")`。 |

### `ApplicationProxyManager`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/ApplicationProxyManager.kt` |
| 作用 | 聚合所有 `ApplicationService` 实现并安全调用。 |
| 获取实现 | `SPIRegisterCenter.all<ApplicationService>()` |
| 内建行为 | `onCreate()` 中初始化日志、自动检测网络权限；前后台切换更新 `appState.appIsForeground`。 |

## 3. SPI / 服务发现 API

### `registerSPI`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/spi/KoinSPI.kt` |
| 签名 | `inline fun <reified T : Any> Module.registerSPI(crossinline impl: () -> T)` |
| 作用 | 在 Koin module DSL 中注册 SPI 实现实例。 |

示例：

```kotlin
val projectModule = module {
    registerSPI<ProjectRouter> { ProjectRouterImpl() }
}
```

### `withImpl`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/spi/SPIRegisterCenter.kt` |
| 签名 | `inline fun <reified T : Any> withImpl(): T?` |
| 作用 | 获取某接口的第一个实现。 |

示例：

```kotlin
val router = withImpl<ProjectRouter>()
navigator.push(router?.main())
```

### `SPIRegisterCenter`

| API | 作用 |
| --- | --- |
| `register(serviceClass, implementation)` | 注册实现。 |
| `all<T>()` | 获取所有实现。 |
| `first<T>()` | 获取第一个实现。 |
| `clear()` | 清空注册表。 |

## 4. Screen 与导航 API

### `BaseScreen`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/base/BaseScreen.kt` |
| 父类型 | `io.github.hristogochev.vortex.screen.Screen` |
| 子类 | 业务层通常继承 `BasicScreen`。 |

可覆写属性：

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `statusBarTextIsDark` | `Boolean` | `true` | 状态栏深色/浅色文字。 |
| `backgroundColor` | `Color` | `Transparent` | 页面背景。 |
| `orientation` | `ScreenOrientation` | `AUTO` | 页面方向策略。 |

必须实现：

```kotlin
@Composable
abstract fun CreateUI()
```

返回拦截：

```kotlin
BaseScreen.CanBackHandler(key = "form") {
    // true 允许返回；false 拦截返回
    true
}
```

### `BasicScreen`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/base/BasicScreen.kt` |
| 默认方向 | `ScreenOrientation.PORTRAIT` |
| 默认背景 | `Color.White` |
| 用途 | 业务页面推荐基类。 |

### `LocalTraceInfo`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/local/LocalTraceInfo.kt` |
| 类型 | `ProvidableCompositionLocal<TraceInfo?>` |
| 作用 | 在当前 Compose UI 子树中读取页面/弹窗链路上下文。 |

常用读取：

```kotlin
val traceInfo = LocalTraceInfo.current
val fromTraceId = traceInfo?.getFromTraceId()
val transitiveTraceId = traceInfo?.getTransitiveTraceId()
```

规则：

- `getFromTraceId()`：当前 UI 所属链路来源，主要用于埋点。
- `getTransitiveTraceId()`：传给下级 Screen 或 Dialog 的链路；优先使用当前作用域的新链路，没有新链路时使用所属链路来源。
- 根页面、普通页面、普通弹窗、原生弹窗都会由基础层包一层 `DefaultTraceInfoScope(fromTraceId, null)`。

### `TraceInfo`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/local/LocalTraceInfo.kt` |
| 签名 | `data class TraceInfo(private val fromId: String?, private val currentId: String?)` |
| 作用 | 保存当前 UI 的所属链路与当前作用域中新起的链路。 |

API：

| API | 返回 | 说明 |
| --- | --- | --- |
| `getFromTraceId()` | `String?` | 返回 `fromId`，用于当前页面/弹窗埋点。 |
| `getTransitiveTraceId()` | `String?` | 返回 `currentId ?: fromId`，用于打开下级 UI 时继续传递链路。 |

### `TraceInfoScope`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/local/LocalTraceInfo.kt` |
| 签名 | `@Composable fun TraceInfoScope(newTraceId: String?, content: @Composable () -> Unit)` |
| 作用 | 在当前 UI 内开启一段新链路作用域，并自动继承父级所属链路。 |

示例：

```kotlin
TraceInfoScope(newTraceId = "home_recommend_card") {
    val traceId = LocalTraceInfo.current?.getTransitiveTraceId()
    Button(onClick = {
        navigator.push(DetailScreen(), traceId)
    }) {
        Text("进入详情")
    }
}
```

说明：

- `TraceInfoScope` 会把父级 `getFromTraceId()` 保存为新作用域的 `fromId`。
- `newTraceId` 会保存为新作用域的 `currentId`。
- 当前作用域内做埋点读取 `getFromTraceId()`；打开下级 Screen/Dialog 时读取 `getTransitiveTraceId()`。
- `DefaultTraceInfoScope(fromTraceId, newTraceId, content)` 是基础层内部 API，业务侧不要直接使用。

### `Navigator` 链路扩展

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/local/LocalTraceInfo.kt` |
| 作用 | 为 Vortex `Navigator` 增加带 `traceId` 的跳转 API。 |

签名：

```kotlin
fun Navigator.push(screen: Screen, traceId: String?)
fun Navigator.push(screens: List<Screen>, traceId: String?)
fun Navigator.replace(screen: Screen, traceId: String?)
fun Navigator.replaceAll(screen: Screen, traceId: String?)
fun Navigator.replaceAll(screens: List<Screen>, traceId: String?)
fun Navigator.replaceUntil(screen: Screen, traceId: String?, predicate: (Screen) -> Boolean)
fun Navigator.replaceUntil(
    screens: List<Screen>,
    traceId: String?,
    predicate: (Screen) -> Boolean
)
```

行为：

- 目标 `screen` 是 `BaseScreen` 时，扩展会先写入 `screen.fromTraceId = traceId`。
- 目标 `screen` 不是 `BaseScreen` 时，`traceId` 不会被消费，但仍会继续执行原始导航。
- 写入来源后再调用原 Vortex `push/replace/replaceAll/replaceUntil`。

推荐调用：

```kotlin
val traceId = LocalTraceInfo.current?.getTransitiveTraceId()
asRouter("project/detail")?.let { screen ->
    navigator.push(screen, traceId)
}
```

### `@Router`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/router/Router.kt` |
| 签名 | `annotation class Router(val value: String)` |
| 作用 | 标记一个 `Screen` 的 URL 路由路径，由 KSP 生成路由表。 |
| 约束 | 被标记类必须实现 `io.github.hristogochev.vortex.screen.Screen`，否则编译报错。 |

示例：

```kotlin
@Router("project/main")
class MainScreen(
    @Params("id") val id: Int? = null,
    @Params("name", autoDecode = true) val name: String? = null
) : BasicScreen()
```

### `@Params`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/router/Router.kt` |
| 签名 | `annotation class Params(val value: String, val jsonTarget: KClass<*> = Nothing::class, val autoDecode: Boolean = false)` |
| 作用 | 标记 `@Router` Screen 构造参数对应的 URL query key。 |

参数说明：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | 无 | URL query key。 |
| `jsonTarget` | `Nothing::class` | JSON 参数目标类型；非基础类型必须设置，且必须与构造参数类型完全一致。 |
| `autoDecode` | `false` | 是否对 query value 执行 URL 解码；仅支持 `String` 参数和声明了 `jsonTarget` 的参数。 |

基础类型支持：

```text
String, Int, Long, Float, Double, Boolean
```

JSON 参数示例：

```kotlin
@Serializable
data class UserParam(val id: String)

@Router("user/detail")
class UserDetailScreen(
    @Params("user", jsonTarget = UserParam::class, autoDecode = true)
    val user: UserParam? = null
) : BasicScreen()
```

编译期校验：

- `@Router` 标记类不是 `Screen` 时，KSP 报错。
- 非基础类型没有 `jsonTarget` 时，KSP 报错。
- `jsonTarget` 与参数类型不完全一致时，KSP 报错。
- 构造参数没有默认值且没有 `@Params` 时，KSP 报错。
- `@Params` 非空参数没有默认值时，KSP 报错。
- `autoDecode = true` 用在非 `String` 且无 `jsonTarget` 的参数上时，KSP 报错。

### `asRouter`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/router/Router.kt` |
| 签名 | `fun asRouter(url: String): Screen?` |
| 作用 | 根据 URL 查找路由并创建 `Screen` 实例。 |
| 行为 | 只返回 `Screen?`，不直接执行导航。 |

示例：

```kotlin
val screen = asRouter("project/main?id=13&name=hello")
screen?.let { navigator.push(it) }
```

参数规则：

- 缺失 key 且构造参数有默认值：使用默认值。
- 缺失 key 且参数可空无默认值：传入 `null`。
- `key=` 空值：按 `null` 处理。
- 基础类型解析失败：按 `null` 处理；如果参数有默认值，则使用默认值。

### `RouteRegistry` / `RouteEntry`

| API | 作用 |
| --- | --- |
| `RouteRegistry.routes()` | 返回当前模块生成的路由表。 |
| `RouteEntry(path, factory)` | 描述 URL path 到 `Screen` 工厂的映射。 |
| `registerRouteRegistry(registry)` | 手动注册路由表；通常由 Koin module 自动暴露后被 `asRouter` 懒加载。 |

生成产物示例：

```kotlin
object SharedProjectRouteRegistry : RouteRegistry {
    override fun routes(): List<RouteEntry> = listOf(
        RouteEntry("project/main") { request ->
            MainScreen(id = request.int("id"))
        }
    )
}

val sharedProjectRouteModule: Module = module {
    single<RouteRegistry>(qualifier = named("sharedProjectRouteRegistry")) {
        SharedProjectRouteRegistry
    }
}
```

模块接入：

```kotlin
val projectModule = module {
    includes(sharedProjectRouteModule)
}
```

### `generateBuildRouter`

| 项 | 内容 |
| --- | --- |
| 位置 | `buildSrc/src/main/kotlin/com/frame/basic/plugin/RouterPlugin.kt` |
| 类型 | Gradle task |
| 作用 | 手动触发当前模块 commonMain 路由代码生成。 |

命令：

```bash
./gradlew :shared_project:generateBuildRouter
```

说明：

- 插件不在 Gradle sync 阶段生成占位文件。
- 路由文件由 `kspCommonMainKotlinMetadata` 生成到 KSP 标准目录。
- `compileCommonMainKotlinMetadata` 会依赖 `kspCommonMainKotlinMetadata`。

### URL 参数编码

传递包含 `?`、`&`、`=`、中文、JSON 等特殊字符的 query value 时，先编码 value，再配合 `autoDecode = true`：

```kotlin
val name = "https://xxxx.com"
    .encodeURLParameter()

asRouter("project/main?name=$name")
```

对应 Screen：

```kotlin
@Router("project/main")
class MainScreen(
    @Params("name", autoDecode = true) val name: String? = null
) : BasicScreen()
```

注意：

- 推荐使用 Ktor `encodeURLParameter()` 编码 query 参数值。
- 不要使用默认 `encodeURLQueryComponent()` 编码完整参数值；它默认会保留 `?`、`&`、`=`，可能导致 query 被提前拆断。
- 如必须使用 `encodeURLQueryComponent`，需要显式传 `encodeFull = true`。

### `ProjectRouter`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/navigation/ProjectRouter.kt` |
| 实现 | `shared_project/.../ProjectRouterImpl.kt` |
| 作用 | 旧的跨模块页面工厂接口。新增页面路由优先使用 `@Router` + `asRouter(url)`。 |

当前路由：

| 方法 | 页面 |
| --- | --- |
| `splash()` | `SplashScreen` |
| `main()` | `MainScreen` |
| `screen()` | `ScreenScreen` |
| `moduleCommunicationScreen()` | `ModuleCommunicationScreen` |
| `dataShare()` | `DataShareScreen` |
| `paramsTransitive()` | `ParamsTransitiveScreen` |
| `simpleInteraction()` | `SimpleInteractionScreen` |
| `diskData()` | `DiskDataScreen` |
| `net()` | `NetScreen` |
| `dialog()` | `DialogScreen` |
| `webview()` | `WebviewScreen` |
| `permission()` | `PermissionScreen` |
| `fileSystem()` | `FileSystemScreen` |
| `downloader()` | `DownloaderScreen` |
| `lifecycle()` | `LifecycleScreen` |
| `singleLifecycle(pageNo)` | `SinglePageScreen` |
| `embeddedLifecycle()` | `EmbeddedPageScreen` |
| `embeddedSlideLifecycle()` | `EmbeddedSlidePageScreen` |
| `stack()` | `StackScreen` |
| `stackDescribe()` | `StackDescribeScreen` |
| `globalDataShare()` | `GlobalDataShareScreen` |
| `screenDataShare()` | `ScreenDataShareScreen` |
| `singleParamsTransitive()` | `SingleParamsTransitiveScreen` |
| `basicTypeDiskData()` | `BasicTypeScreen` |
| `jsonTypeDiskData()` | `JsonTypeScreen` |
| `okioFileSystem()` | `OkioScreen` |
| `fileKitFileSystem()` | `FileKitScreen` |
| `fileWriteReader()` | `FileWriteReaderScreen` |
| `directoryMapping()` | `DictionaryMappingScreen` |
| `filePicker()` | `FilePickerScreen` |
| `galleryPicker()` | `GalleryPickerScreen` |
| `dictionaryPicker()` | `DictionaryPickerScreen` |
| `cameraPicker()` | `CameraPickerScreen` |
| `basicInteraction()` | `BasicInteractionScreen` |
| `pageInteraction()` | `PagingInteractionScreen` |
| `mixInteraction()` | `MixInteractionScreen` |
| `normalDialog()` | `NormalDialogScreen` |
| `priorityDialog()` | `PriorityDialogScreen` |
| `nativeDialog()` | `NativeDialogScreen` |

## 5. ScreenModel 与交互 API

### `MainScreenModel`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/base/MainScreenModel.kt` |
| 父类型 | `ScreenModel` |
| 推荐业务别名 | `BasicScreenModel` |

生命周期：

```kotlin
abstract fun onInit(context: ScreenContext)
abstract fun onLoad(context: ScreenContext)
open fun onVisible(context: ScreenContext)
open fun onInvisible(context: ScreenContext)
open fun onDestroyed()
```

UI 状态：

```kotlin
suspend fun uiLoading(text: String?)
suspend fun uiSuccess(empty: Boolean = false)
suspend fun uiError(code: Int, error: String?)
suspend fun showPopLoading(info: String?)
suspend fun dismissPopLoading()
```

### `PagingControl`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/ktx/RefreshLazyListKtx.kt` |
| 类型 | `interface PagingControl` |
| 作用 | 为任意 `MainScreenModel`/`BasicScreenModel` 增加刷新与分页控制能力。 |

核心 API：

```kotlin
val refreshState: RefreshState
suspend fun pagingFirst()
suspend fun pagingMore()
suspend fun pagingOver(isOver: Boolean)
```

接入方式：

```kotlin
class XxxScreenModel : BasicScreenModel(), PagingControl {
    override val refreshState = RefreshState(
        enablePullUp = true,
        enablePullDown = true
    )

    override suspend fun pagingFirst() {
        // 首次加载或下拉刷新
    }

    override suspend fun pagingMore() {
        // 上拉加载更多
    }
}
```

运行机制：

- `BasicScreenModel` 继承自 `MainScreenModel`，页面首次可见时会走 `onInit/onLoad`。
- `MainScreenModel` 在首次可见初始化完成后，如果发现当前模型实现了 `PagingControl`，会自动调用内部 `initPagingControl(screenModelScope)`。
- `initPagingControl` 监听 `refreshState.state`，当状态变为 `PULL_DOWN_REFRESHING` 时调用 `pagingFirst()`，变为 `PULL_UP_REFRESHING` 时调用 `pagingMore()`。
- `pagingFirst()` 和 `pagingMore()` 执行结束后，框架会把 `refreshState.state` 恢复为 `IDLE`。
- 业务通过 `pagingOver(true)` 标记没有更多数据，通过 `pagingOver(false)` 标记仍可继续分页。

### `rememberMainScreenModel`

```kotlin
@Composable
inline fun <reified T : MainScreenModel> rememberMainScreenModel(
    tag: String? = null,
    crossinline factory: () -> T
): T
```

行为：

- 绑定 Vortex 当前 ScreenStateKey。
- 自动根据页面可见性调用 `onInit/onLoad/onVisible/onInvisible`。

### `ScreenContext`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `navigatorController` | `Navigator` | Vortex 导航控制器。 |
| `dialogController` | `DialogController` | 当前页面弹窗控制器。 |
| `appState` | `AppState` | 全局 App 状态。 |
| `permissionController` | `PermissionController` | 权限控制器。 |
| `uiContainer` | `UIContainer` | Android Activity 或 iOS UIViewController。 |

### `BasicInteraction`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/base/BasicInteraction.kt` |
| 签名 | `@Composable fun BasicInteraction(screenModel: MainScreenModel, ... content: BoxScope.(Modifier) -> Unit)` |
| 作用 | 统一处理加载态、错误态、空态、弹窗 loading。 |

## 6. 网络 API

### `ktorfit`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/net/Http.kt` |
| 类型 | `Ktorfit` lazy singleton |
| Base URL | `BuildConfig_com_basic_common.HTTP_URL` |

默认请求头：

| Header | 当前值/来源 |
| --- | --- |
| `Authorization` | `Bearer xx` |
| `versionName` | `1.4.5.2` |
| `factory` | `OPPO` |
| `deviceId` | `123243123434` |
| `model` | `oppo` |
| `platform` | `getPlatform().os.name` |
| `osVersion` | `getPlatform().systemVersion` |
| `channel` | `default` |
| `timestamp` | `Clock.System.now().epochSeconds` |
| `User-Agent` | `getPlatform().userAgent` |

超时：

| 类型 | 值 |
| --- | --- |
| request | 60s |
| connect | 20s |
| socket | 10s |

### `TestApi.queryUserInfo`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/api/TestApi.kt` |
| Method | `GET` |
| Path | `/user/getUserPublicInfo` |
| Query | `username: String` |
| 返回 | `Data<UserInfo?>` |

签名：

```kotlin
@GET("/user/getUserPublicInfo")
suspend fun queryUserInfo(@Query("username") username: String): Data<UserInfo?>
```

示例调用：

```kotlin
val result = TestRepository.queryUserInfo("fanjun004").throwFail()
```

### `Data<T>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `T?` | 业务数据。 |
| `code` | `Int` | 业务错误码，`200` 表示成功。 |
| `subCode` | `Int?` | 二级错误码。 |
| `msg` | `String?` | 错误说明。 |

关键方法：

```kotlin
fun throwFail(): T?
```

行为：

- `code == 200`：返回 `data`。
- 其他：抛出 `ApiException(code, msg)`。

### `UserInfo`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantName` | `String?` | 租户名称/所属公司。 |
| `userLogo` | `String?` | 用户头像。 |
| `realName` | `String?` | 姓名。 |
| `departmentName` | `String?` | 部门名称。 |

## 7. 协程与异常 API

### `launchScope`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/ktx/CoroutineScopeKtx.kt` |
| 签名 | `fun CoroutineScope.launchScope(context: CoroutineContext = Dispatchers.Default, execute: suspend CoroutineScope.() -> Unit): CoroutineJob` |
| 作用 | 包装协程启动、异常回调、最终回调。 |

示例：

```kotlin
scope.launchScope {
    val data = repository.load().throwFail()
}.catch { code, error, e ->
    // 统一错误处理
}.finally {
    // 收尾
}
```

### `ApiException`

```kotlin
open class ApiException(val code: Int = -1, error: String?) : Exception(error)
```

异常映射：

- `ResponseException` -> HTTP status code。
- `ApiException` -> 业务 code。
- 其他异常 -> `999999`。

## 8. 弹窗 API

### `Dialog`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/ktx/BaseDialogKtx.kt` |
| 推荐业务基类 | `BasicDialog` |

必须实现：

```kotlin
@Composable
abstract fun CreateUI()
```

可覆写：

```kotlin
open fun onShow() {}
open fun onDismiss() {}
```

控制：

```kotlin
fun dismiss()
```

### `DialogController`

| API | 作用 |
| --- | --- |
| `showNow(dialog, traceId = null)` | 展示普通弹窗，并可写入弹窗链路来源。 |
| `showPriority(priority, dialog, traceId = null, group = "default")` | 展示优先级弹窗，并可写入弹窗链路来源；数值越小越优先。 |
| `showMaxPriority(dialog, traceId = null)` | 展示最高优先级弹窗，并可写入弹窗链路来源。 |
| `clearAllStack()` | 清空所有弹窗。 |
| `clearNormalStack()` | 清空普通弹窗。 |
| `clearMaxPriorityStack()` | 清空最高优先级弹窗。 |
| `clearAllPriorityStack()` | 清空全部优先级弹窗。 |
| `clearPriorityStack(group)` | 清空指定优先级分组。 |

链路传递示例：

```kotlin
val traceId = LocalTraceInfo.current?.getTransitiveTraceId()
LocalDialogController.current.showNow(ConfirmDialog(), traceId)
```

弹窗渲染时，基础层会使用 `DefaultTraceInfoScope(fromTraceId, null)` 包裹 `Dialog.CreateUI()`，因此弹窗内部可以继续通过 `LocalTraceInfo.current` 读取和传递链路。

### `NativeDialog`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/ui/NativeDialog.kt` |
| 推荐业务基类 | `BasicNativeDialog` |
| 作用 | 跨平台原生弹窗基类。 |

核心 API：

```kotlin
@Composable
abstract fun CreateUI()

fun show(uiContainer: UIContainer, traceId: String? = null)

fun dismiss()
```

链路传递示例：

```kotlin
val traceId = LocalTraceInfo.current?.getTransitiveTraceId()
NativeConfirmDialog().show(LocalUIContainer.current, traceId)
```

`NativeDialog` 与普通 `Dialog` 一样，会在创建 UI 内容时注入 `DefaultTraceInfoScope(fromTraceId, null)`。

### `LoadingDialog`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/base/LoadingDialog.kt` |
| 签名 | `fun show(dialogController: DialogController, text: String)` |
| 作用 | 最高优先级 loading 弹窗。 |

## 9. 响应式存储 API

### `settings`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/datastore/PubSetting.kt` |
| Android | SharedPreferences，文件名 `pub_Settings`。 |
| iOS | NSUserDefaults，文件名 `pub_Settings`。 |

扩展：

```kotlin
fun Settings.asFlowInt(key: String, initialValue: Int? = null)
fun Settings.asFlowDouble(key: String, initialValue: Double? = null)
fun Settings.asFlowString(key: String, initialValue: String? = null)
fun Settings.asFlowBoolean(key: String, initialValue: Boolean? = null)
fun Settings.asFlowFloat(key: String, initialValue: Float? = null)
fun Settings.asFlowLong(key: String, initialValue: Long? = null)
inline fun <reified T : Any> Settings.asFlowJson(key: String, initialValue: T? = null)
```

返回对象通用能力：

| API | 作用 |
| --- | --- |
| `state: StateFlow<T?>` | 响应式状态。 |
| `value: T?` | 当前值。 |
| `setValue(newValue: T?)` | 更新内存与磁盘；`null` 表示删除。 |

### `ShareData`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `currentNo` | `MutableStateFlow<Int>` | 仅内存全局状态。 |
| `diskString` | `DataStoreFlow<String>` | 持久化字符串。 |
| `diskBean` | `DataStoreFlowJson<DiskBean>` | 持久化 JSON 对象。 |

## 10. WebView / JSBridge API

### `WebViewState`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/webview/state/WebViewState.kt` |
| 构造 | `WebViewState(scope: CoroutineScope)` |
| 持有位置 | 必须放在 `ScreenModel` 中，推荐使用 `screenModelScope` 作为构造参数。 |

生命周期约束：

- `WebViewState` 不要直接创建在 Composable 内，也不要只依赖 `remember { ... }` 保存。Compose 重组、条件分支切换、slot 重新进入、父布局重建等场景都可能让原生 WebView 重新创建，造成 URL 重新加载、JSBridge 重复注册、历史栈丢失或资源泄漏。
- 推荐在 `BasicScreenModel` / `MainScreenModel` 中声明 `val webviewState = WebViewState(scope = screenModelScope)`。
- 在 `onInit(context)` 中执行首次 `loadUrl(url)`，避免每次 Compose 重组重复加载。
- 在 `onDestroyed()` 中调用 `webviewState.destroyed()`，释放原生 WebView 并清空 JSBridge。
- 这个模式适用于所有 KMP 中承载原生 UI 的控件：原生控件状态、控制器、回调注册和资源释放都应归属 `ScreenModel`，Composable 只负责展示和事件转发。

正确结构：

```kotlin
class WebviewScreenModel : BasicScreenModel() {
    val webviewState = WebViewState(scope = screenModelScope)

    override fun onInit(context: ScreenContext) {
        webviewState.loadUrl(url)
    }

    override fun onLoad(context: ScreenContext) {
    }

    override fun onDestroyed() {
        super.onDestroyed()
        webviewState.destroyed()
    }
}
```

不推荐：

```kotlin
@Composable
fun WebContent() {
    val state = WebViewState(rememberCoroutineScope())
    NativeWebView(Modifier.fillMaxSize(), state)
}
```

这种写法容易在重组或局部重建时重复创建原生实例。

状态字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `loadingState` | `LoadingState` | 加载状态。 |
| `title` | `String?` | 页面标题。 |
| `currentUrl` | `String?` | 当前 URL。 |
| `canGoForward` | `Boolean` | 是否可前进。 |
| `canGoBack` | `Boolean` | 是否可后退。 |

页面控制：

```kotlin
fun loadUrl(url: String)
fun goBack()
fun goForward()
fun reload()
fun stopLoading()
fun destroyed()
```

KMP 调 JS：

```kotlin
inline fun <reified T, reified R> evaluateJavaScripts(
    methodName: String?,
    jsonParams: T?,
    noinline callback: (suspend (R?) -> Unit)?
)
```

JS 调 KMP：

```kotlin
inline fun <reified T, reified R> registerJsBridge(
    methodName: String,
    crossinline work: suspend (jsonParams: T?) -> R?
)
```

清理：

```kotlin
fun clearJsBridges()
fun destroyed()
```

### `NativeWebView`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/webview/platform/NativeWebView.kt` |
| 签名 | `@Composable expect fun NativeWebView(modifier: Modifier, state: WebViewState)` |
| 作用 | 跨平台 Native WebView Composable。 |
| 职责边界 | 只负责把 `WebViewState` 中持有的原生 WebView 挂到 Compose 树上，不负责持久化状态。 |

使用方式：

```kotlin
val model = rememberMainScreenModel { WebviewScreenModel() }

NativeWebView(
    modifier = Modifier.fillMaxSize().background(Color.White),
    state = model.webviewState
)
```

首次进入时，`WebViewState.getOrCreate(uiContainer)` 会创建原生 WebView；后续重组应复用 `ScreenModel` 中的同一个 `WebViewState` 和原生实例。

### `preloadWebkit`

```kotlin
expect fun preloadWebkit(domain: String? = null)
```

当前业务在 `project.ApplicationServiceImpl.onCreate()` 中调用。

## 11. 下载 API

### `DownloadManager.downloadAndGet`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/base/src/commonMain/kotlin/com/basic/base/downloader/DownloadManager.kt` |
| 签名 | `suspend fun downloadAndGet(url: String, dir: PlatformFile): DownloadTask` |
| 作用 | 获取或创建下载任务，并加入全局下载队列。 |
| 并发 | 全局最多 4 个活动任务。 |

### `DownloadTask`

| 字段/API | 类型 | 说明 |
| --- | --- | --- |
| `url` | `String` | 下载地址。 |
| `targetDir` | `PlatformFile` | 目标目录。 |
| `state` | `StateFlow<DownloadState>` | 下载状态。 |
| `progress` | `StateFlow<DownloadProgressState>` | 下载进度。 |
| `cancel()` | `suspend fun` | 取消下载。 |

### `DownloadState`

| 状态 | 说明 |
| --- | --- |
| `Idle` | 等待中。 |
| `Downloading` | 下载中。 |
| `Cancel` | 已取消。 |
| `Completed(filePath)` | 下载完成。 |
| `Failed(e)` | 下载失败。 |

### `DownloadProgressState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `downloadedBytes` | `Long` | 已下载字节。 |
| `totalBytes` | `Long` | 总字节；为 0 时表示未知。 |
| `percent` | `Float` | `downloadedBytes * 100 / totalBytes`；总字节为 0 时返回 0。 |

## 12. 平台能力 API

### `Platform`

| 字段 | 说明 |
| --- | --- |
| `name` | 平台名称。 |
| `os` | `IOS` 或 `ANDROID`。 |
| `systemVersion` | 系统版本。 |
| `appName` | App 名称。 |
| `appVersionName` | 版本名。 |
| `appVersionCode` | 版本号。 |
| `appPackageName` | 包名。 |
| `userAgent` | UA。 |
| `appChannel` | 渠道。 |
| `brand` | 品牌。 |
| `model` | 机型。 |

获取：

```kotlin
expect fun getPlatform(): Platform
```

### `DeviceId`

```kotlin
data class DeviceId(
    val oaid: String,
    val androidId: String,
    val idfv: String,
    val idfa: String,
    val keychainId: String,
    val uniqueId: String,
    val agreeIdfa: Boolean
)
```

获取：

```kotlin
expect suspend fun getDeviceId(): DeviceId
```

### `ScreenOrientation`

| 值 | 说明 |
| --- | --- |
| `LANDSCAPE` | 强制横屏。 |
| `PORTRAIT` | 强制竖屏。 |
| `FOLLOW_SENSOR` | 跟随传感器。 |
| `AUTO` | 自动。 |

## 13. Native 安全 API

### `Encrypt`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/native/src/commonMain/kotlin/com/basic/native/Encrypt.kt` |
| 类型 | `expect object` |
| 实现源头 | 具体加解密实现集中在 `commonMain` 的 `EncryptNativeImpl`；平台 `actual` 负责选择直接调用或通过 JNI 调用。 |

签名：

```kotlin
fun encodeData(data: String): String
fun decodeData(data: String): String
fun createSign(data: String): String
```

平台行为：

| 平台 | 行为 |
| --- | --- |
| `commonMain` | 定义 `expect object Encrypt`，并提供 `EncryptNativeImpl` 作为真正的加解密实现入口。 |
| `iosMain` | `actual object Encrypt` 直接调用 `EncryptNativeImpl`。 |
| `androidNativeArm64Main` | `actual object Encrypt` 直接调用 `EncryptNativeImpl`；同时提供 KNI/JNI 映射函数，先校验 JVM 桥接签名，再调用 `EncryptNativeImpl`。手动执行 `:core:native:androidNativeArm64Binaries` 会生成 `src/androidNativeArm64Main/staticLib/libshared_nativeLibs.a`。 |
| `androidMain` | `actual object Encrypt` 调用 `EncryptJni.external` 方法；`EncryptJni` 通过 `System.loadLibrary("shared_nativeLibs")` 加载 Android 打包阶段生成的 `.so`。 |

Android 构建产物链路：

```text
commonMain EncryptNativeImpl
  -> androidNativeArm64Main KNI/JNI 映射函数
  -> :core:native:androidNativeArm64Binaries
  -> src/androidNativeArm64Main/staticLib/libshared_nativeLibs.a
  -> Android 打包时 CMake 链接 staticLib
  -> shared_nativeLibs.so
```

运行时实际调用顺序：

```text
业务代码
  -> Encrypt.encodeData/decodeData/createSign
  -> androidMain actual Encrypt
  -> EncryptJni.encodeData/decodeData/createSign
  -> shared_nativeLibs.so JNI 方法
  -> androidNativeArm64Main 映射函数
  -> checkEnv(source, bridge)
  -> commonMain EncryptNativeImpl
```

### `checkEnv`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/native/src/commonMain/kotlin/com/basic/native/EnvChecker.kt` |
| 签名 | `expect inline fun checkEnv()` |
| 作用 | 运行环境检测；建议敏感操作前调用。 |

平台行为：

| 平台 | 行为 |
| --- | --- |
| Android | 校验签名、Application、线程、篡改/插件化/Hook 痕迹等。 |
| iOS | 空实现。 |
| androidNativeArm64 | 空实现。 |

## 14. UI 组件 API

### `BasicTitleBar`

```kotlin
@Composable
fun BasicTitleBar(
    title: String,
    left: @Composable ((Modifier) -> Unit)? = { TitleBarLeft(it) },
    right: @Composable ((Modifier) -> Unit)? = { TitleBarRight(it) }
)
```

行为：

- 左侧默认返回；栈内页面调用 `navigator.pop()`，根页面调用 `uiContainer.pop()`。
- 右侧默认“首页”，调用 `navigator.popUntilRoot()`。

### `BasicHazeScaffold`

| 项 | 内容 |
| --- | --- |
| 位置 | `core/common/src/commonMain/kotlin/com/basic/common/base/BasicScaffold.kt` |
| 作用 | 对 `HazeScaffold` 的业务默认封装。 |
| Slots | `top`、`center`、`bottom`。 |

## 15. 依赖库管理规则

### 本地依赖

| 类型 | 放置位置 | 说明 |
| --- | --- | --- |
| Android 本地依赖 | 当前模块 `libs/android/` | 放模块专属 `.jar`、`.aar` 等。 |
| iOS 本地依赖 | 当前模块 `libs/ios/` | 放模块专属 iOS 本地依赖。 |

约束：

- 本地依赖按模块归属放置，谁使用谁维护。
- 框架脚本会自动处理本地依赖的构建接入问题。
- 不要把模块专属依赖集中放到 `app` 或 `iosApp`。

### 远程依赖

| 类型 | 配置位置 | 说明 |
| --- | --- | --- |
| Maven/Gradle 依赖 | 实际使用模块的 `build.gradle.kts` | 按 `commonMain`、`androidMain`、`iosMain` 等 source set 配置。 |
| iOS Pod 依赖 | 实际使用模块的 `cocoapods { pod(...) }` | 必须指定版本。根脚本会自动同步到 `iosApp/Podfile`。 |

禁止项：

- 禁止为了 Pod 依赖在业务模块额外添加 framework 配置。
- 禁止在多个共享模块各自对外生成 iOS framework。

### iOS Framework 输出

全局只允许生成 `app` 模块中配置的 `ComposeApp` framework：

```kotlin
framework {
    baseName = "ComposeApp"
    isStatic = true
}
```

约束：

- `ComposeApp` 必须是静态库 framework。
- 其他共享模块通过 `app` 的依赖链进入最终 framework，不单独生成 framework。
- 这个约束用于降低 iOS 编译链接阶段的重复符号、缺失库、重复 framework 等异常风险。

## 16. 构建配置 API

### `ProjectBuildConfig`

| 字段 | 值 |
| --- | --- |
| `applicationId` | `com.basic.app` |
| `appName` | `KmpProject` |
| `versionName` | `1.0.0` |
| `versionCode` | `1` |
| `compileSdkVersion` | `36` |
| `minSdkVersion` | `26` |
| `targetSdkVersion` | `36` |
| `ndkVersion` | `23.2.8568313` |
| `cmakeVersion` | `3.22.1` |
| `designSize` | `375` |

### `HttpUrlConfig`

| 环境 | API | H5 |
| --- | --- | --- |
| DEVELOP | `https://xxxx.com` | `https://xxxx.com` |
| BETA | `https://xxxx.com` | `https://xxxx.com` |
| ALPHA | `https://xxxx.com` | `https://xxxx.com` |
| RELEASE | `https://xxxx.com` | `https://xxxx.com` |

### `SDKKeyConfig`

| 项 | 内容 |
| --- | --- |
| 位置 | `buildSrc/src/main/kotlin/com/frame/basic/buildsrc/SDKKeyConfig.kt` |
| 作用 | 统一保存本地 SDK key、AppId、AppKey、secret、iOS universalLink 等敏感配置入口。 |
| 平台拆分 | 三方 SDK 配置必须按 `Android` / `IOS` 子对象区分，禁止默认复用双端 key。 |

推荐结构：

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

使用约束：

- 必须核心 key 由对应 `libs/<name>` SDK 模块通过 `BuildKonfig` 注入，并在 Android/iOS actual 实现中分别读取。
- common 初始化 API 不暴露 `appId`、`appKey`、`secret` 等必须核心 key。
- 可运行时决定的非必须参数，例如 channel、授权状态、设备隐私参数，可保留在 common API 中作为初始化参数。
- Android Manifest placeholder 与 iOS `com.basic.ios` 字段值都必须来自 `SDKKeyConfig`。

### `com.basic.ios`

| 项 | 内容 |
| --- | --- |
| 位置 | `buildSrc/src/main/kotlin/com/frame/basic/plugin/IosConfigPlugin.kt` |
| 插件 ID | `com.basic.ios` |
| 作用 | 聚合各 SDK lib 模块声明的 iOS plist 参数，生成 `iosApp/Configuration/iosConfig.xcconfig`。 |
| 适用场景 | 三方 SDK 要求在 iOS `Info.plist` 配置 AppKey、AppId、URL Scheme、Universal Link 等参数。 |

模块接入：

```kotlin
import com.frame.basic.buildsrc.SDKKeyConfig

plugins {
    id("com.basic.ios")
}

iosConfig {
    field("SOME_SDK_IOS_APP_ID", SDKKeyConfig.SomeSdk.IOS.appId)
}
```

`Info.plist` 引用：

```xml
<key>SomeSdkAppId</key>
<string>$(SOME_SDK_IOS_APP_ID)</string>
```

生成文件：

```text
iosApp/Configuration/iosConfig.xcconfig
```

`Config.xcconfig` 固定 include：

```xcconfig
#include "iosConfig.xcconfig"
```

任务：

| 类型 | Gradle task |
| --- | --- |
| 根任务 | `./gradlew generateIosConfig` |
| 模块代理任务 | `./gradlew :libs:<name>:generateIosConfig` |

约束：

- 字段值必须来自 `SDKKeyConfig`，禁止把真实 key 直接写在 `build.gradle.kts` 或 `Info.plist`。
- 删除 `field(...)` 后，macOS Gradle Sync 会自动重写生成文件并清理旧字段。
- 非 macOS 上任务默认禁用，不写 `iosConfig.xcconfig`。
- 多模块声明同名 key 且值不一致时构建失败。

## 17. AI 快速任务指南

### 新增页面

```text
BasicScreen subclass
  -> optional BasicScreenModel
  -> ProjectRouter method
  -> ProjectRouterImpl method
```

### 新增接口

```text
core/common/api/XxxApi.kt
  -> @GET/@POST...
core/common/beans/XxxDto.kt
  -> @Serializable
shared_project/repository/XxxRepository.kt
  -> ktorfit.createXxxApi()
ScreenModel
  -> repository.call().throwFail()
```

### 新增跨模块服务

```text
core/common/di/service/XxxService.kt
shared_project/di/impl/XxxServiceImpl.kt
shared_project/di/DI.kt registerSPI<XxxService> { XxxServiceImpl() }
caller -> withImpl<XxxService>()
```

### 新增平台能力

```text
core/base/src/commonMain/... expect API
core/base/src/androidMain/... actual API
core/base/src/iosMain/... actual API
```

## 18. Demo 示例 API 使用说明

本章按 `shared_project` 中的 demo 页面反向整理 API。阅读 demo 时优先看这里，可以更快判断某个框架能力应该怎么接入。

### Demo 到 API 映射

| Demo 页面 | 关键 API | 位置 |
| --- | --- | --- |
| 跨模块通信 | `withImpl<ProjectService>()`、`toastShort()`、`DateUtils` | `shared_project/ui/ModuleCommunicationScreen.kt` |
| 单页生命周期 | `LocalNavigator`、`navigator.push/popUntilRoot`、`rememberMainScreenModel`、`onVisible/onInvisible` | `shared_project/ui/screen/lifecycle/SinglePageScreen.kt` |
| 嵌套生命周期 | `HorizontalPagerLifecycle`、`LocalPageLifecycleVisible`、`rememberMainScreenModel(tag)` | `shared_project/ui/screen/lifecycle/EmbeddedPageScreen.kt` |
| 滑动嵌套生命周期 | `HorizontalPagerLifecycle`、`PagerState`、`animateScrollToPage` | `shared_project/ui/screen/lifecycle/EmbeddedSlidePageScreen.kt` |
| 全局数据共享 | `ShareData.currentNo`、`MutableStateFlow.collectAsState()` | `shared_project/ui/screen/datashare/GlobalDataShareScreen.kt`、`SinglePageScreen.kt` |
| Screen 内数据共享 | `rememberScreenModel`、`rememberMainScreenModel(tag)` | `shared_project/ui/screen/datashare/ScreenDataShareScreen.kt` |
| 参数传递和页面回调 | `buildCallbackId`、`asCallback`、`rememberScreenModel` | `shared_project/ui/screen/paramstransitive/SingleParamsTransitiveScreen.kt` |
| 基础交互 | `BasicInteraction`、`uiLoading/uiSuccess/uiError`、`showPopLoading` | `shared_project/ui/screen/interaction/BasicInteractionScreen.kt` |
| 分页交互 | `PagingControl`、`RefreshState`、`BasicRefreshLazyListInteraction` | `shared_project/ui/screen/interaction/PagingInteractionScreen.kt` |
| 混合交互 | `BasicHazeScaffold`、`CoordinatorLayout`、`rememberCoordinatorLayoutState` | `shared_project/ui/screen/interaction/MixInteractionScreen.kt` |
| 网络请求 | `TestRepository`、`Data.throwFail()`、`launchScope.catch` | `shared_project/ui/screen/NetScreen.kt` |
| 响应式磁盘数据 | `settings.asFlowString`、`settings.asFlowJson`、`setValue` | `shared_project/ui/screen/diskdata` |
| 普通弹窗 | `LocalDialogController.current.showNow`、`BasicDialog.dismiss`、`onDismiss` | `shared_project/ui/screen/dialog/NormalDialogScreen.kt` |
| 优先级弹窗 | `showPriority(priority, dialog, group)` | `shared_project/ui/screen/dialog/PriorityDialogScreen.kt` |
| 原生弹窗 | `BasicNativeDialog`、`show(uiContainer)`、`LocalUIContainer` | `shared_project/ui/screen/dialog/NativeDialogScreen.kt` |
| 权限系统 | `LocalPermissionController`、`providePermission`、`permissionState` | `shared_project/ui/screen/PermissionScreen.kt` |
| FileKit 文件选择 | `FileKit.openFilePicker`、`absolutePath` | `shared_project/ui/screen/filesystem/filekit/FilePickerScreen.kt` |
| FileKit 目录/相机 | `openDirectoryPicker`、`openCameraPicker` | `shared_project/ui/screen/filesystem/filekit` |
| FileKit 读写 | `PlatformFile`、`createDirectories`、`writeString/readString` | `shared_project/ui/screen/filesystem/filekit/FileWriteReaderScreen.kt` |
| 图库裁剪 | `FileKitType.Image`、`rememberImageCropper`、`crop`、`ImageCropperDialog` | `shared_project/ui/screen/filesystem/filekit/GalleryPickerScreen.kt` |
| 下载器 | `DownloadManager.downloadAndGet`、`DownloadTask.state/progress`、`cancel` | `shared_project/ui/screen/DownloaderScreen.kt` |
| WebView | `WebViewState`、`NativeWebView`、`registerJsBridge`、`evaluateJavaScripts`、`CanBackHandler` | `shared_project/ui/screen/WebviewScreen.kt` |

### 跨模块服务调用

Demo 使用 `ProjectService` 展示跨模块通信：

```kotlin
withImpl<ProjectService>()?.sayHello("跨模块通信成功")
```

服务定义在 `core/common/di/service/ProjectService.kt`，实现与注册在 `shared_project`：

```kotlin
class ProjectServiceImpl : ProjectService {
    override fun sayHello(text: String) {
        toastShort(text)
    }
}

val projectModule = module {
    registerSPI<ProjectService> { ProjectServiceImpl() }
}
```

约定：

- 服务接口放在上层公共模块，避免调用方依赖具体实现模块。
- 实现放在实际业务模块，通过 `registerSPI` 暴露。
- 调用方使用 `withImpl<T>()` 获取第一个实现；生命周期类使用 `SPIRegisterCenter.all<T>()` 聚合所有实现。

### 导航与栈操作

Demo 页面直接使用 Vortex 的 `LocalNavigator.currentOrThrow`：

```kotlin
val navigator = LocalNavigator.currentOrThrow
navigator.push(SinglePageScreen(pageNo + 1))
navigator.pop()
navigator.popUntilRoot()
navigator.replace(MainScreen())
```

返回按钮默认由 `BasicTitleBar` 处理：

```kotlin
TitleBarLeft {
    if (navigator.size <= 1) uiContainer.pop() else navigator.pop()
}
```

WebView demo 自定义返回逻辑：

```kotlin
CanBackHandler("webviewScreen") {
    if (model.webviewState.canGoBack) {
        scope.launch { model.webviewState.goBack() }
        false
    } else {
        true
    }
}
```

规则：

- 普通页面跳转优先使用 `navigator.push(Screen())`。
- 跨模块页面工厂使用 `ProjectRouter`；具体页面入口由业务自行组织。
- 页面需要拦截系统返回时，使用 `BaseScreen.CanBackHandler(key) { ... }`。

### ScreenModel 生命周期 demo

单页生命周期 demo：

```kotlin
val screenModel = rememberMainScreenModel { SinglePageScreenModel() }

class SinglePageScreenModel : BasicScreenModel() {
    override fun onVisible(context: ScreenContext) {}
    override fun onInvisible(context: ScreenContext) {}
}
```

`rememberMainScreenModel` 会自动把页面可见性映射到模型生命周期。生命周期顺序：

```text
首次可见: onInit -> onLoad -> onVisible
离开页面: onInvisible
再次可见: onVisible
销毁页面: onDestroyed
```

嵌套页 demo 需要给不同 tab 使用不同 tag：

```kotlin
fun EmbeddedInnerPage(
    title: String,
    screenModel: EmbeddedInnerPageScreenModel =
        rememberMainScreenModel(title) { EmbeddedInnerPageScreenModel() }
)
```

要点：

- `tag` 相同会复用同一个 ScreenModel。
- `tag` 不同会为同一个宿主 Screen 内的不同区域创建独立模型。
- 嵌套 pager 使用 `HorizontalPagerLifecycle` 后，只有当前页的 `LocalPageLifecycleVisible` 为 `true`，因此内页 `MainScreenModel` 可正确收到 `onVisible/onInvisible`。

### Pager 生命周期 API

`HorizontalPagerLifecycle` 是对 Compose `HorizontalPager` 的封装：

```kotlin
HorizontalPagerLifecycle(
    state = pagerState,
    userScrollEnabled = false
) { page ->
    EmbeddedInnerPage(screenModel.tabs[page])
}
```

关键参数：

| 参数 | 说明 |
| --- | --- |
| `state` | `PagerState`，通常由 `rememberPagerState { count }` 创建。 |
| `userScrollEnabled` | 是否允许用户手势滑动。 |
| `autoResetHazeScaffoldOnPageSettled` | 与 `HazeScaffold` 配合时，切页后是否自动展开 scaffold。 |
| `pageContent` | 页面内容；内部会注入 `LocalPageLifecycleVisible`。 |

滑动 tab demo 使用双向同步：

```kotlin
LaunchedEffect(pagerState.currentPage) {
    screenModel.current = screenModel.tabs[pagerState.currentPage]
}

LaunchedEffect(screenModel.current) {
    pagerState.animateScrollToPage(screenModel.tabs.indexOf(screenModel.current))
}
```

### 数据共享 demo

全局数据共享使用普通 Flow/StateFlow：

```kotlin
object ShareData {
    val currentNo = MutableStateFlow(0)
}

Text("全局数据序号：${ShareData.currentNo.collectAsState().value}")
ShareData.currentNo.value += 1
```

Screen 内共享使用 `rememberScreenModel` 的 holder key：

```kotlin
val model = rememberMainScreenModel(tag) { ScreenDataShareInnerPageScreenModel() }
val shareModel = rememberScreenModel { ScreenDataShareSharedScreenModel() }
```

差异：

- `rememberMainScreenModel(tag)`：相同 tag 共享，不同 tag 隔离，并参与 `MainScreenModel` 生命周期。
- `rememberScreenModel { ... }`：不传 tag 时，在当前 ScreenModelStore 内共享实例，适合 Screen 内共享状态。
- 全局共享状态不绑定 Screen 生命周期，需要自行控制内存和事件消费。

### 参数传递与回调 demo

父页面创建 callbackId：

```kotlin
navigator.push(
    SingleParamsTransitiveNextScreen(
        params,
        buildCallbackId { text: String ->
            resultParams = text
        }
    )
)
```

子页面转换并调用：

```kotlin
class SingleParamsTransitiveNextScreenModel(callbackId: String?) : ScreenModel {
    val callback = asCallback<(String) -> Unit>(callbackId)
}

model.callback?.invoke(inputText)
navigator.pop()
```

约束：

- `asCallback<T>(callbackId)` 只能取一次，取出后会从全局 map 移除，避免内存泄漏。
- callbackId 会跟创建它的 Screen key 绑定；源页面销毁时，`CallbackFunctionModel` 会清理相关回调。
- 适合轻量页面结果回传，不适合长期持有或跨进程持久化。

### 基础交互 demo

页面包一层 `BasicInteraction(model)`：

```kotlin
val model = rememberMainScreenModel { BasicInteractionScreenModel() }
BasicInteraction(model) { modifier ->
    // 成功状态下显示的业务内容
}
```

模型控制 UI 状态：

```kotlin
screenModelScope.launchScope {
    uiLoading("加载中...")
    delay(2000)
    uiSuccess()
}.catch { code, error, _ ->
    uiError(code, error)
}
```

空页面：

```kotlin
uiSuccess(empty = true)
```

弹窗 loading：

```kotlin
showPopLoading("提交中...")
dismissPopLoading()
```

默认行为：

- `uiLoading`：显示 `BasicLoading`。
- `uiError`：显示 `BasicError`，点击重试默认调用 `screenModel.onLoad(context)`。
- `uiSuccess(true)`：显示空态 UI。
- `showPopLoading`：通过 `LoadingDialog.showMaxPriority` 展示最高优先级 loading。

### 分页与刷新 demo

模型使用普通 `BasicScreenModel`，再实现 `PagingControl` 协议：

```kotlin
class PagingInteractionScreenModel : BasicScreenModel(), PagingControl {
    val data = mutableStateListOf<Int>()
    override val refreshState = RefreshState(
        enablePullUp = true,
        enablePullDown = true
    )

    override suspend fun pagingFirst() {}
    override suspend fun pagingMore() {}
}
```

首次加载仍然通常在 `onLoad` 中主动执行 `pagingFirst()`：

```kotlin
override fun onLoad(context: ScreenContext) {
    screenModelScope.launchScope {
        uiLoading("加载中...")
        pagingFirst()
        uiSuccess()
    }.catch { code, error, _ ->
        uiError(code, error)
    }
}
```

页面使用刷新列表：

```kotlin
BasicRefreshLazyListInteraction(
    modifier = modifier,
    state = model.refreshState,
    dataSize = { model.data.size },
    childScrollState = rememberLazyListState()
) {
    items(model.data) { item -> ... }
}
```

分页结束：

```kotlin
pagingOver(loadTime > 2)
```

关键状态：

| API | 说明 |
| --- | --- |
| `refreshState.state` | `IDLE`、`PULL_DOWN_REFRESHING`、`PULL_UP_REFRESHING`。 |
| `refreshState.progress` | 下拉进度，demo 用它控制 `HazeScaffold.canConsumeScrollUp`。 |
| `refreshState.noMore` | 是否没有更多数据。 |
| `pagingFirst()` | 下拉刷新或首次加载。 |
| `pagingMore()` | 上拉到底时加载更多。 |
| `pagingOver(isOver)` | 设置分页是否结束，`true` 表示没有更多数据。 |

### 混合滚动 demo

`MixInteractionScreen` 组合了 `BasicHazeScaffold`、`CoordinatorLayout`、`BasicRefreshLazyListInteraction`。

Scaffold 顶部高度随折叠区域变化：

```kotlin
BasicHazeScaffold(
    minTopHeight = titlebarHeightDp.value,
    maxTopOverlap = 0.dp,
    canConsumeScrollUp = { pullDownProgress <= 0f },
    top = { ... },
    center = { ... }
)
```

协调头部：

```kotlin
val coordinatorState = rememberCoordinatorLayoutState()

CoordinatorLayout(
    state = coordinatorState,
    canConsumeScrollUp = { pullDownProgress <= 0f },
    header = { expandedProgress -> ... },
    content = { ... }
)
```

手动展开/收起：

```kotlin
scope.launch { coordinatorState.animateToExpanded() }
scope.launch { coordinatorState.animateToCollapsed() }
```

关键 API：

| API | 说明 |
| --- | --- |
| `CoordinatorLayout` | 管理 header 折叠/展开，不直接持有子列表状态。 |
| `CoordinatorLayoutState.expandedFraction` | header 展开比例。 |
| `CoordinatorLayoutState.collapsedFraction` | header 折叠比例。 |
| `animateToExpanded()` | 动画展开 header。 |
| `animateToCollapsed()` | 动画折叠 header。 |
| `Modifier.coordinatorMainScroll(...)` | 标记主要滚动源，刷新列表封装内部已使用。 |
| `Modifier.coordinatorDragProxy(...)` | 将 header 或代理区域滑动转发给协调容器。 |

### 网络 demo

页面内使用页面级协程：

```kotlin
val scope = rememberSupervisorCoroutineScope()
scope.launchScope {
    val result = TestRepository.queryUserInfo("fanjun004").throwFail()
    text = result.toString()
}.catch { code, error, _ ->
    text = "code: $code error: $error"
}
```

要点：

- Repository 返回 `Data<T>`，页面或模型调用 `.throwFail()` 转成业务异常。
- `launchScope.catch` 会统一接收 `ApiException` 和 Ktor `ResponseException`。
- 如果是页面主数据加载，推荐放到 `MainScreenModel.onLoad` 并配合 `BasicInteraction`。

### 响应式磁盘存储 demo

基本类型：

```kotlin
val diskString = ShareData.diskString.state.collectAsState().value
ShareData.diskString.setValue(input)
```

JSON 类型：

```kotlin
val diskBean = ShareData.diskBean.state.collectAsState().value
ShareData.diskBean.setValue(DiskBean(input))
```

监听变化：

```kotlin
LaunchedEffect(Unit) {
    ShareData.diskString.state.collect {
        toastShort("您已输入: $it")
    }
}
```

规则：

- `setValue(null)` 会删除磁盘值。
- JSON 类型必须是 `@Serializable`。
- `state` 是 `StateFlow<T?>`，Compose 内使用 `collectAsState()`。

### 弹窗 demo

普通弹窗：

```kotlin
val dialogController = LocalDialogController.current
dialogController.showNow(Dialog1("弹窗1") { tag ->
    toastShort("关闭了$tag")
})
```

弹窗定义：

```kotlin
class Dialog1(...) : BasicDialog(cancelAble = false) {
    @Composable
    override fun CreateUI() {}

    override fun onDismiss() {
        dismiss(tag)
    }
}
```

优先级弹窗：

```kotlin
dialogController.showPriority(priority = 2, Dialog2("弹窗1") {})
dialogController.showPriority(priority = 0, Dialog2("弹窗2") {})
dialogController.showPriority(priority = 1, Dialog2("弹窗3") {})
```

优先级规则：

- 数值越小优先级越高。
- `group` 不传时使用默认组。
- 当前组内只展示最高优先级的弹窗。

原生弹窗：

```kotlin
val uiContainer = LocalUIContainer.current
DemoNativeDialog {
    nativeToast(uiContainer, "您关闭了原生弹窗")
}.show(uiContainer)
```

`BasicDialog` 与 `BasicNativeDialog` 差异：

| 类型 | 宿主 | 适用 |
| --- | --- | --- |
| `BasicDialog` | 当前 `BaseScreen` 的 `DialogController` | Compose 内普通业务弹窗。 |
| `BasicNativeDialog` | Android `DialogFragment` / iOS `UIViewController` | 需要脱离 Screen 弹窗栈、使用平台原生弹窗容器时。 |

### 权限 demo

页面获取权限控制器：

```kotlin
val controller = LocalPermissionController.current
val model = rememberMainScreenModel { PermissionScreenModel() }
```

查询权限：

```kotlin
permissionState.value =
    context.permissionController.permissionState(Permission.RECORD_AUDIO)
```

申请权限：

```kotlin
permissionState.value =
    permissionController.providePermission(Permission.RECORD_AUDIO)
```

状态：

| 状态 | 说明 |
| --- | --- |
| `NOT_DETERMINED` | 未授权或未查询到授权。 |
| `SUCCESS` | 已授权。 |
| `DENIED` | 本次拒绝。 |
| `DENIED_ALWAYS` | 永久拒绝，需要引导到设置页。 |

平台配置：

- Android 需要在 `AndroidManifest.xml` 添加对应 `<uses-permission ...>`。
- iOS 需要在 `Info.plist` 添加对应 usage description，例如 `NSMicrophoneUsageDescription`。
- Android `BaseActivity` 负责 `permissionClient.bind(this)`。

### FileKit demo

文件选择：

```kotlin
val file = FileKit.openFilePicker()
toastShort("你选择了：${file?.absolutePath()}")
```

目录选择：

```kotlin
val dir = FileKit.openDirectoryPicker()
```

相机选择：

```kotlin
val photo = FileKit.openCameraPicker()
```

应用目录：

```kotlin
FileKit.filesDir
FileKit.cacheDir
FileKit.databasesDir
```

文件读写：

```kotlin
val dir = PlatformFile("${FileKit.filesDir}/to".toPath())
if (!dir.exists()) dir.createDirectories()

val file = PlatformFile("${FileKit.filesDir}/to/file.txt".toPath())
file.writeString(inputStr)
val content = file.readString()
```

常用 API：

| API | 说明 |
| --- | --- |
| `PlatformFile(path)` | 平台文件对象。 |
| `exists()` | 文件/目录是否存在。 |
| `isDirectory()` | 是否目录。 |
| `createDirectories()` | 创建目录。 |
| `writeString(text)` | 写文本。 |
| `readString()` | 读文本。 |
| `write(bytes)` | 写二进制。 |
| `delete(false)` | 删除文件。 |
| `absolutePath()` | 获取绝对路径字符串。 |
| `FileKit.filesDir / "download"` | 使用 `/` 运算符拼接子路径。 |

图库裁剪 demo：

```kotlin
val imageCropper = rememberImageCropper()
val cropState = imageCropper.cropState
if (cropState != null) ImageCropperDialog(state = cropState)

FileKit.openFilePicker(type = FileKitType.Image)?.let { image ->
    val result = imageCropper.crop(image.toImageSrc())
    if (result is CropResult.Success) {
        newFile.write(result.bitmap.encodeToByteArray())
    }
}
```

Android 初始化：

```kotlin
FileKit.init(this)
```

当前在 `BaseActivity.onCreate()` 中已完成。

### 下载器 demo

启动下载：

```kotlin
task = DownloadManager.downloadAndGet(
    url = "...apk",
    dir = FileKit.filesDir / "download"
)
```

监听状态：

```kotlin
downloadTask.state.collectLatest { state = it }
downloadTask.progress.collectLatest { progress = it }
```

取消：

```kotlin
task?.cancel()
```

状态与进度：

```kotlin
DownloadState.Idle
DownloadState.Downloading
DownloadState.Cancel
DownloadState.Completed(filePath)
DownloadState.Failed(e)

DownloadProgressState(downloadedBytes, totalBytes).percent
```

实现特性：

- 同 URL + 同目录会复用等待队列或活动队列中的任务。
- 临时文件后缀为 `.downloading`。
- 正式文件名由 URL 的 MD5 和扩展名生成。
- 支持 HTTP `Range` 断点续传。

### WebView demo

模型创建并持有 `WebViewState`：

```kotlin
class WebviewScreenModel : BasicScreenModel() {
    val webviewState = WebViewState(scope = screenModelScope)

    override fun onInit(context: ScreenContext) {
        webviewState.loadUrl(url)
    }

    override fun onDestroyed() {
        webviewState.destroyed()
    }
}
```

这是 WebView demo 最重要的约束：`WebViewState` 必须放在 `ScreenModel`，并在 `onDestroyed()` 中销毁。不要把它作为 Composable 局部状态创建，否则 Compose 重组可能触发 WebView 重建和重复加载。

页面渲染：

```kotlin
NativeWebView(
    modifier = Modifier.fillMaxSize().background(Color.White),
    state = model.webviewState
)
```

Composable 只读取 `model.webviewState` 并渲染，不负责创建和释放 WebView。

注册 JS 调 KMP：

```kotlin
LaunchedEffect(Unit) {
    model.webviewState.registerJsBridge<TestUserInfo, TestUserInfo>("getUserInfo") {
        TestUserInfo("2", "name123123", 123213423)
    }
}
```

注册建议：

- 在 Composable 中注册 JSBridge 时，用 `LaunchedEffect(Unit)` 包住，避免重组重复注册。
- 更复杂的业务可以把注册封装到 `ScreenModel` 方法中，由 `onInit` 或页面首次进入时调用。
- `WebViewState.destroyed()` 会调用 `clearJsBridges()`，页面销毁后不应继续持有 JSBridge 回调。

KMP 调 JS：

```kotlin
model.webviewState.evaluateJavaScripts<TestUserInfo, TestUserInfo>(
    "getUserInfoByH5",
    TestUserInfo("2", "name123123", 123213423)
) { result ->
    logDebug("webview", "$result")
}
```

导航控制：

```kotlin
webviewState.goBack()
webviewState.goForward()
webviewState.reload()
webviewState.stopLoading()
```

加载状态：

```kotlin
when (val loadingState = webviewState.loadingState) {
    is LoadingState.Loading -> loadingState.progress
    is LoadingState.Error -> model.uiError(loadingState.code, loadingState.error)
    else -> model.uiSuccess()
}
```

返回处理：

- 顶栏返回：`canGoBack` 时调用 `webviewState.goBack()`，否则 `navigator.pop()`。
- 系统返回：通过 `CanBackHandler` 拦截，优先 WebView 后退。

JSBridge 约定：

- KMP 注册的方法名存入 `WebViewState.jsProcessors`。
- JS 侧调用时传入 `callbackId`、`methodName`、`jsonParams`。
- KMP 返回时执行 `window:onKmpCallback(callbackId, result)`。
- KMP 调 JS 当前拼接脚本形态为 `window:{methodName}('{jsonParams}')`，新增复杂参数时要注意字符串转义。

原生 UI 通用实践：

- WebView、地图、播放器、相机预览、广告位等原生 UI 控件都应采用同类模式。
- `ScreenModel` 持有状态对象和原生资源引用。
- `onInit` 做首次加载或注册。
- Composable 只渲染 `NativeXxx(state = model.xxxState)`。
- `onDestroyed` 释放原生资源、注销回调、清空引用。

### UI 脚手架 demo

多数页面使用：

```kotlin
BasicHazeScaffold(
    modifier = Modifier.fillMaxSize(),
    top = { BasicTitleBar("标题") },
    center = { ... },
    bottom = { ... }
)
```

常用参数：

| 参数 | 说明 |
| --- | --- |
| `top` | 顶部区域，通常放 `BasicTitleBar`。 |
| `center` | 主内容区域。 |
| `bottom` | 底部区域，tab 或操作栏。 |
| `canConsumeScrollUp/down` | 控制 scaffold 与子滚动组件的滑动消费关系。 |
| `minTopHeight` | 顶部最小高度。 |
| `maxTopOverlap` | 顶部最大重叠距离。 |

内嵌页可读取内容安全边距：

```kotlin
val scaffoldContentPadding = LocalHazeScaffoldContentPadding.current
contentPadding = PaddingValues(
    bottom = scaffoldContentPadding.calculateBottomPadding()
)
```

### Toast与日志 demo

Toast：

```kotlin
toastShort("提示")
toastLong("长提示")
nativeToast(uiContainer, "原生 Toast")
```


日志：

```kotlin
logDebug("webview", "$data")
logError("webview", error)
```
