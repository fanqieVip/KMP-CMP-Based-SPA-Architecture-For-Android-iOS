# AI 开发规范索引

> ⚠️ **强制要求（优先级最高）：**
>
> **整个项目首次阅读时**，必须立即按照以下顺序逐一执行，完成所有阅读和分析后，再执行用户的具体工作指令：
>
> 1. [架构设计文档.md](./skills/架构设计文档.md) — 理解模块依赖、启动流程、UI 与导航架构
> 2. [架构api文档.md](./skills/架构api文档.md) — 熟悉路由、SPI、网络、WebView 等核心 API
> 3. [cocoapods使用说明.md](./skills/cocoapods使用说明.md) — 了解 iOS Pod 依赖配置
> 4. **深度分析整个项目代码** — 理解实际代码结构和模式
> 5. **阅读本文件后续章节** — 掌握界面开发规范、切图规范、提问指南
>
> **⚠️ 禁止跳过任何步骤！禁止在未完成全部阅读前开始编写代码！**

> 本文件作为 AI 协作的总入口。当 AI 需要查找项目规范、组件用法、最佳实践时，先读取本索引，再按需加载具体文档。

> ⚠️ **工作流程强制要求：每次接收新页面编写指令时，必须严格遵循以下流程：**
>
> 1. **分析设计稿**：仔细阅读用户提供的设计稿或需求描述
> 2. **按指南提问**：根据本文件「界面开发提问指南」章节，向用户逐一提问确认组件选择、样式细节等不确定项，必须以对话方式一个一个确认，且能不让我输入就不要让我输入
> 3. **制定实施计划**：在得到用户确认后，制定详细的实施计划（Todo 列表）
> 4. **开始编码**：按照计划逐步实现

---

## 一、核心规则速查清单（必读）

> ⚠️ **每次开始工作前，必须快速扫描此清单！** 这是所有规范的精华提炼。

### 页面开发
- `@Router` 注解必须加
- ScreenModel 必须继承 `BasicScreenModel`
- 分页必须实现 `PagingControl` 接口
- `onLoad` 中失败调 `uiError`、空数据调 `uiSuccess(true)`
- `pagingMore` 成功必须调 `pagingOver()`

### UI 组件规范
- 文本必须设 `lineHeight`（=fontSize）和 `fontWeight`
- 图片必须设 `contentScale`（默认 `FillWidth`）
- Color 用十六进制 `Color(0xffffffff)`，透明度用 `.copy(alpha)`
- Lazy 列表 item 必须指定 `key`

### 组件禁用
- ❌ 禁止直接用 `HorizontalPager`/`VerticalPager`，用 `HorizontalPagerLifecycle`/`VerticalPagerLifecycle`
- ❌ 禁止硬编码 HazeScaffold 内容间距，用 `LocalHazeScaffoldContentPadding`
- ❌ 禁止在 item 中加 Spacer/padding 做间距，用 `verticalArrangement`/`contentPadding`

### 切图规范
- ✅ 优先用 webp 格式
- ✅ 只切 drawable-xxhdpi
- ✅ 命名前缀：`project_`/`common_`/`base_`
- ❌ 图片资源放好后，如果引用图片ide爆红，则先手动 import导入, 再执行 `generateComposeResClass`，最后再`build`，如果还是ide爆红不管了

### 工作流程
- 新页面开发：分析设计稿 → 按指南提问 → 制定计划 → 编码
- 所有代码仅限本项目目录，禁止引用其他项目

---

## 二、项目结构

```
kmp/
├── app/                      # KMP application：Android/iOS 共享 App 入口、Koin 启动、平台壳桥接
├── iosApp/                   # iOS native shell：SwiftUI App、SceneDelegate、KMP ViewController 容器
├── shared_base/              # KMP library：Screen、生命周期、CompositionLocal、SPI、WebView、下载、存储
│   └── src/commonMain/kotlin/com/basic/base/
│       ├── base/             # BaseScreen、MainScreenModel、PagingControl、RefreshState
│       ├── di/service/       # ApplicationService
│       ├── ktx/              # PagingControl、RefreshState、CoordinatorLayout
│       ├── local/            # LocalTraceInfo、TraceInfo
│       ├── router/           # @Router、@Params、asRouter
│       ├── ui/               # NativeDialog
│       ├── webview/          # WebViewState、NativeWebView
│       ├── downloader/       # DownloadManager
│       ├── datastore/        # Settings
│       └── spi/              # registerSPI、SPIRegisterCenter
├── shared_native/            # KMP/Android native library：加解密、JNI/staticLib、APK 环境校验
├── shared_common/            # KMP library：公共业务组件、BasicXxx、网络配置、API DTO、通用服务实现
│   └── src/commonMain/kotlin/com/basic/common/
│       ├── base/             # BasicScreen、BasicHazeScaffold、BasicTitleBar、BasicDialog、BasicInteraction、BasicNativeDialog、LoadingDialog
│       ├── navigation/       # ProjectRouter
│       ├── net/              # Http (Ktorfit)
│       ├── api/              # TestApi
│       └── di/service/       # ProjectService
├── shared_project/           # KMP feature library：示例业务页面、Repository、路由实现、业务服务实现
│   └── src/commonMain/kotlin/com/basic/project/
│       ├── ui/               # 页面实现（Screen/Dialog/Model）
│       ├── repository/       # 数据仓库
│       └── di/impl/          # ServiceImpl、ProjectRouterImpl
├── router_processor/         # @Router 注解 KSP 处理器
├── buildSrc/                 # Gradle build logic：构建参数、环境、签名、压缩、TinyPNG 插件
├── skills/                   # 开发规范文档（AI 参考）
│   ├── 架构api文档.md
│   ├── 架构设计文档.md
│   └── cocoapods使用说明.md
└── tools/                    # 工具脚本
```

模块依赖方向（自下而上）：

```text
shared_base
    ^
    |
shared_native ----\
    ^             |
    |             |
shared_common ----/
    ^
    |
shared_project
    ^
    |
app
    ^
    |
iosApp
```

约束：
- `shared_base` 不依赖业务模块，是公共架构内核
- `shared_native` 被 `shared_common` 依赖，用于安全能力
- `shared_common` 定义公共契约（`ProjectRouter`、`ProjectService`、`TestApi`）
- `shared_project` 实现 `shared_common` 中的业务契约
- `app` 组合 `commonModule` 与 `projectModule` 并启动 UI
- `iosApp` 只作为 SwiftUI 宿主，引入 `ComposeApp` framework

---

## 三、工作范围（重要）

**所有代码、文件、资源的操作，仅限当前 AGENTS.md 所在的项目目录（即 `d:\workplace\kmp\`）内。**

- 禁止读取、引用、复制其他项目的文件或代码
- 禁止修改项目目录以外的任何文件
- 禁止将其他项目的实现直接搬运过来
- 所有实现必须基于本项目的规范、组件和架构

---

## 四、界面开发规范

### 4.1 Screen 开发

#### 基础结构

创建 Screen 需继承 `BasicScreen`，使用 `@Router` 注解注册路由：

```kotlin
@Router(RouterConstant.YOUR_PATH)
class YourScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        // 界面内容
    }
}
```

#### 标准布局（推荐）

`BasicHazeScaffold` 和 `BasicTitleBar` 是非必须的，是否使用取决于 UI 设计图是否符合该标准骨架风格。

`BasicHazeScaffold` 是 `HazeScaffold` 的默认实现，主要用于模拟 iOS 的毛玻璃效果（针对 top 和 bottom 区域），并内置了常见的滑动优先级处理逻辑。

**⚠️ 关键约束：**
- **禁止设置背景色**：在 `BasicHazeScaffold` 的 `top` 和 `bottom` 插槽（Slot）内，**禁止为最外层容器设置背景色**。背景色会导致毛玻璃效果被覆盖，从而失效。
- **视觉冲突处理**：如果 UI 设计稿中 `top/bottom` 的颜色与 `BasicHazeScaffold` 默认的毛玻璃基础色完全不符（不仅是透明度差异），请在实现后明确告知开发者进行人工介入处理。

**使用示例：**

```kotlin
@Composable
override fun CreateUI() {
    BasicHazeScaffold(
        modifier = Modifier.fillMaxSize(),
        top = { BasicTitleBar("页面标题") },
        center = {
            LazyColumn { items(100) { Text("Item $it") } }
        },
        bottom = { TabBar() },
        canConsumeScrollUp = { pullDownProgress <= 0f }
    )
}
```

#### 内容安全边距

当页面存在 `top` 或 `bottom` 区域时，`center` 区域需要使用 `LocalHazeScaffoldContentPadding` 获取准确边距，**不要硬编码间距值**，也**不要在安全边距上追加额外间距**。

```kotlin
val scaffoldPadding = LocalHazeScaffoldContentPadding.current
contentPadding = PaddingValues(bottom = scaffoldPadding.calculateBottomPadding())
```

#### ScreenModel 与生命周期

每个 Screen 都有自己的 `ScreenModel`，由 `rememberMainScreenModel` 绑定：

```kotlin
@Composable
inline fun <reified T : MainScreenModel> rememberMainScreenModel(
    tag: String? = null,
    crossinline factory: () -> T
): T
```

生命周期顺序：`onInit -> onLoad -> onVisible -> onInvisible -> onDestroyed`

**ScreenModel 职责：** 业务数据、接口调用、加载状态控制、原生 UI 资源持有、资源释放

**Composable 不应做：** 直接执行业务逻辑、直接调用接口、创建原生 UI 控件的状态对象

#### 导航跳转

```kotlin
val navigator = LocalNavigator.currentOrThrow
navigator.push(YourScreen())
navigator.pop()

// URL 路由（推荐）
val screen = asRouter("project/main?id=13&name=hello")
screen?.let { navigator.push(it) }
```

---

### 4.2 Dialog 开发

#### 创建弹窗

```kotlin
class YourDialog : BasicDialog() {
    @Composable
    override fun CreateUI() {
        Box(modifier = Modifier.size(200.dp, 150.dp).background(Color.White)) {
            // 弹窗内容
        }
    }
}
```

#### 展示弹窗

```kotlin
val dialogController = LocalDialogController.current
dialogController.showNow(YourDialog())                    // 普通弹窗
dialogController.showPriority(priority = 1, YourDialog()) // 优先级弹窗
dialogController.showMaxPriority(YourDialog())            // 最高优先级弹窗
```

---

### 4.3 NativeDialog 开发

#### 创建原生弹窗

```kotlin
class YourNativeDialog : BasicNativeDialog() {
    @Composable
    override fun CreateUI() { /* ... */ }
}
```

#### 展示原生弹窗

```kotlin
val uiContainer = LocalUIContainer.current
YourNativeDialog().show(uiContainer)
```

#### Dialog 与 NativeDialog 区别

| 特性 | Dialog | NativeDialog |
|------|--------|--------------|
| 管理方式 | DialogController 栈管理 | UIContainer 原生管理 |
| 生命周期 | 跟随宿主 Screen | 独立生命周期 |
| 跨平台 | 纯 Compose 实现 | 原生平台实现 |

---

### 4.4 交互状态管理组件（BasicInteraction）

`BasicInteraction` 定义了用户从进入页面到加载完成的标准流程：`loading → success/error/empty`。

#### 状态控制方法

| 方法 | 说明 |
|------|------|
| `uiLoading(text)` | 显示加载中状态 |
| `uiSuccess(empty)` | 显示成功状态 |
| `uiError(code, error)` | 显示错误状态 |
| `showPopLoading(text)` | 显示弹窗加载 |

#### 与分页组件混用

| 组件 | 职责范围 |
|------|---------|
| `BasicInteraction` | 首次加载的整体状态 |
| `RefreshLazyList` | 下拉刷新和上拉更多 |

**交互规则：**
- 首次加载失败 → `uiError`，空数据 → `uiSuccess(true)`
- 下拉刷新/上拉加载失败 → `initPagingControl` 自动处理（toast + 重置状态）

---

### 4.5 分页组件开发

#### 核心组件

| 组件 | 适用场景 |
|------|---------|
| `RefreshLazyList` | 列表分页 |
| `RefreshLazyGrid` | 网格分页 |

#### 与 Lazy 列表组件的关系

分页组件包含所有主要的 Lazy 列表属性，可直接作为 Lazy 列表使用：
- item 间距：`verticalArrangement = Arrangement.spacedBy(12.dp)`
- 内容间距：`contentPadding = PaddingValues(horizontal = 16.dp)`
- item key：**必须指定**，避免不必要的重组

#### 分页控制接口

```kotlin
interface PagingControl {
    val refreshState: RefreshState
    suspend fun pagingFirst()
    suspend fun pagingMore()
    suspend fun pagingOver(isOver: Boolean)
}
```

#### 完整示例

```kotlin
class YourScreenModel : BasicScreenModel(), PagingControl {
    val data = mutableStateListOf<Item>()
    override val refreshState = RefreshState(true, true)
    
    override fun onLoad(context: ScreenContext) {
        screenModelScope.launchScope {
            uiLoading("加载中...")
            pagingFirst()
            uiSuccess(data.isEmpty())
        }.catch { code, error, e -> uiError(code, error) }
    }
    
    override suspend fun pagingFirst() { /* 实现 */ }
    override suspend fun pagingMore() { /* 实现 */ }
}
```

---

### 4.6 分栏组件开发

#### 核心组件

| 组件 | 适用场景 |
|------|---------|
| `HorizontalPagerLifecycle` | 左右滑动分栏（带生命周期） |
| `VerticalPagerLifecycle` | 上下滑动分栏（带生命周期） |

**禁止直接使用 `HorizontalPager`/`VerticalPager`**，必须使用封装的组件。

#### Tab 联动规范

| 联动方式 | `userScrollEnabled` |
|---------|--------------------|
| 双向联动 | `true` |
| 单向联动（仅点击 Tab） | `false` |

#### 内容区复用与拆分

| 场景 | 处理方式 |
|------|---------|
| 各 Tab 内容相同 | 复用同一个 Composable，通过参数区分 |
| 各 Tab 内容完全不同 | 在当前 Screen/Dialog 所在目录创建单独文件处理 |

#### 4.6.4 分栏生命周期与 ScreenModel 规范

为确保分栏组件（Pager）内各页面的生命周期（onVisible/onInvisible）正确触发，必须遵循以下开发规范：

1.  **内容提取**：每个 Tab 的内容建议拆分为独立的 Composable 函数。
2.  **ScreenModel 就近创建**：分栏专属的 `ScreenModel` **必须**在上述拆分出的 Composable 函数内部创建。
3.  **严禁作用域外创建**：禁止在分栏组件（`HorizontalPagerLifecycle`等）的 Compose 作用域外提前创建其内部专属的 `ScreenModel`。
    *   **风险说明**：提前创建会导致 `ScreenModel` 绑定了宿主 Screen 的生命周期，而非 Pager 的页面可见性（`LocalPageLifecycleVisible`）。这会造成 Pager 切换时生命周期回调（`onVisible`/`onInvisible`）异常，从而引发业务逻辑错误或崩溃。

---

### 4.7 组件混用最佳实践

#### BasicHazeScaffold + RefreshLazyList（最常用）

```kotlin
val pullDownProgress = model.refreshState.progress.collectAsState().value
BasicHazeScaffold(
    modifier = Modifier.fillMaxSize(),
    canConsumeScrollUp = { pullDownProgress <= 0f },
    top = { BasicTitleBar("页面标题") },
    center = {
        BasicInteraction(model) { modifier ->
            BasicRefreshLazyListInteraction(
                modifier = modifier.fillMaxSize(),
                state = model.refreshState,
                dataSize = { data.size }
            ) {
                items(data) { /* ... */ }
            }
        }
    }
)
```

#### CoordinatorLayout + RefreshLazyList（复杂折叠）

用于页面有可折叠 Header 区域的场景，配置方式类似，额外添加 `CoordinatorLayout`。

#### 4.7.3 嵌套 Pager 时的滑动协调 (Scaffold + Pager + List)

当 `BasicHazeScaffold` 嵌套 `HorizontalPagerLifecycle` 再嵌套 `RefreshLazyList` 等滚动组件时，由于嵌套层次深，Scaffold 无法直接获取准确的滑动消费状态，导致滑动冲突。

**解决方案：状态提升与手动回调**

1.  **父页面定义状态**：在宿主 Screen 中定义 `canConsumeScrollUp` 或 `canConsumeScrollDown` 变量并传给 Scaffold。
2.  **向下传递回调**：将更新该状态的回调函数（如 `(Boolean) -> Unit`）层层传递给 Pager 内部的具体页面。
3.  **内页监听并同步**：内页使用 `LaunchedEffect` 监听自身的 `refreshState.progress` 或滚动组件的滚动状态，并实时调用回调同步给父页面。
4.  **规则**：通常当 `progress <= 0f` 时，通知父页面 `canConsumeScrollUp = true`。

**示例：**
```kotlin
// 父页面
var canConsumeScrollUp by remember { mutableStateOf(true) }
var canConsumeScrollDown by remember { mutableStateOf(true) }
BasicHazeScaffold(
    canConsumeScrollUp = { canConsumeScrollUp },
    canConsumeScrollDown = { canConsumeScrollDown },
    center = {
        HorizontalPagerLifecycle(...) { page ->
            InnerPage(
                onUpScrollChange = { canConsumeScrollUp = it },
                onDownScrollChange = { canConsumeScrollDown = it }
            )
        }
    }
)

// 内页
val progress by model.refreshState.progress.collectAsState()
LaunchedEffect(progress) {
    onUpScrollChange(progress <= 0f)
}
```

#### 4.7.4 处理 Bottom 区域遮挡 Center 内容

当 `BasicHazeScaffold` 设置了 `bottom` 区域时，`center` 区域的内容（特别是滚动组件）可能会被底栏遮挡。

**解决方案：使用 LocalHazeScaffoldContentPadding**

1.  **获取边距**：在 `center` 闭包内通过 `LocalHazeScaffoldContentPadding.current` 获取脚手架自动计算的安全边距。
2.  **应用边距**：
    *   **滚动组件**：必须通过 `contentPadding` 属性应用底部边距，确保列表最后一项能滚动到遮挡区上方。
    *   **普通组件**：通过 `Modifier.padding()` 应用。

**示例：**
```kotlin
center = {
    val scaffoldPadding = LocalHazeScaffoldContentPadding.current
    BasicRefreshLazyListInteraction(
        // 关键：将脚手架计算的底部边距应用到列表内容边距上
        contentPadding = PaddingValues(bottom = scaffoldPadding.calculateBottomPadding()),
        ...
    ) {
        // items...
    }
}
```

---

### 4.8 UI 组件通用规范

#### 文本类组件

- **必须设置 `lineHeight`**（=fontSize），避免平台渲染差异
- **必须设置 `fontWeight`**（除非设计稿读不到值），确保粗细一致

#### 图片类组件

- **必须设置 `contentScale`**（默认 `FillWidth`）

#### Color 定义

- 统一使用十六进制范式 `Color(0xffffffff)`
- 透明度用 `.copy(alpha)`，禁止使用 RGB 写法

#### Lazy 列表组件

- item 必须指定 `key`，避免不必要的重组
- 不要在 item 中加 Spacer/padding 做间距，用 `verticalArrangement`/`contentPadding`

---

### 4.9 CoordinatorLayout 悬停者组件规范

`CoordinatorLayout` 是一个专门负责“头部折叠/展开”与“内容悬停”的协调者组件。

#### 4.9.1 核心特性
- **解耦设计**：不关心子内容具体实现，只要子组件支持 Nested Scroll（如 `LazyColumn`、`verticalScroll`）即可自然协作。
- **灵活悬停**：提供两种主流悬停实现方式（固定高度悬停、随内容吸顶）。
- **状态感知**：提供 `expandedProgress`（0f~1f）回调，方便实现头部的实时 Alpha 或位移动画。

#### 4.9.2 悬停实现方案
- **方案 A（固定高度悬停）**：
  - 设置 `minHeaderHeight` 为 Header 区域需要悬停的那部分高度。
  - 效果：滑动时 Header 会收缩到指定高度后固定。
- **方案 B（随内容悬停）**：
  - 设置 `minHeaderHeight = 0dp`（允许 Header 完全折叠）。
  - 在 `content` 插槽中使用 `Column` 包裹：`悬停部分` + `滚动列表组件`。
  - 效果：悬停部分会随着列表滚动到顶部后自然吸顶。

#### 4.9.3 关键参数与修饰符
- `minHeaderHeight`: Header 最小高度。`Dp.Unspecified` 则不允许折叠。
- `scrollUpPriority / scrollDownPriority`: 滑动消费优先级（默认上划 PARENT 优先，下划 CHILD 优先）。
- `Modifier.coordinatorMainScroll`: **必须**标记在子内容中主要的滚动源组件上（如 `LazyColumn`）。
- `Modifier.coordinatorDragProxy`: 将该组件的滑动事件转发给协调者处理（通常用于 Header 内部）。

#### 4.9.4 组件选择决策（HazeScaffold vs CoordinatorLayout）
两者场景相似，但侧重点不同：
- **优先用 `BasicHazeScaffold`**：如果页面是标准的“标题栏 + 背景毛玻璃”结构，且只需要处理简单的标题栏折叠或沉浸式状态栏。
- **优先用 `CoordinatorLayout`**：如果页面有复杂的 Header 交互（如个人中心背景大图折叠）、多阶段悬停、或者需要精准控制头部与列表的滑动优先级。
- **⚠️ 决策障碍**：如果无法确定该选择哪一个组件，**必须向开发者询问确认**。

---

## 五、界面开发提问指南

### 5.1 提问流程

1. **初始评估**：分析设计稿，判断明确需要的组件和需要确认的组件
2. **按优先级提问**（按组件聚合）：
   - BasicHazeScaffold（页面骨架）
   - BasicTitleBar（标题栏）
   - RefreshLazyList / RefreshLazyGrid（列表/网格）
   - CoordinatorLayout（协调器）
   - 分栏组件（HorizontalPagerLifecycle / VerticalPagerLifecycle）
   - BasicInteraction（状态管理）
   - Dialog / NativeDialog（弹窗）
3. **追问机制**：每个组件问完后，追问"你是否有补充说明？"
4. **记忆机制**：记录回答、补充内容、已确定的配置，避免重复提问

### 5.2 提问原则

- ✅ 遇到不确定的就问
- ✅ 如果无法区分内容区，让开发者指出
- ✅ 如果无法判断是否需要 Tab 联动，向开发者确认
- ✅ 设计稿未提供 Tab 选中效果时，暂用相同效果

### 5.3 跳过机制

- ✅ 开发者补充的内容如果是 AI 将要问到的，跳过该问题
- ✅ 如果开发者说"跳过，由 AI 自行判断"，AI 可自行决定

### 5.4 聚合提问模板

#### BasicHazeScaffold 聚合提问

```
针对页面骨架配置，我需要确认以下几点：

1. 页面布局是否符合标准骨架结构？
   - 完全符合 / 部分符合 / 完全不符合

2. 顶部和底部区域是否需要毛玻璃效果？
   - 都需要 / 只顶部需要 / 只底部需要 / 都不需要

3. 页面背景色是什么？
   - 白色 / 浅色（请提供色值） / 深色（请提供色值） / 渐变/图片背景

4. 状态栏文字颜色是深色还是浅色？
   - 深色 / 浅色

5. 页面是否支持横屏？
   - 只支持竖屏 / 只支持横屏 / 自适应

6. 页面是否有下拉刷新功能？（用于配置滑动优先级）
   - 有 / 没有
```

#### BasicTitleBar 聚合提问

```
针对标题栏配置，我需要确认以下几点：

1. 页面是否有标题栏？
   - 有标题栏，符合标准样式 / 有标题栏，不符合标准样式 / 没有标题栏

2. 如果有标题栏，请确认：
   - 左侧是否需要返回按钮？（需要 / 不需要）
   - 右侧是否有操作按钮？有几个？（没有 / 1 个 / 2 个 / 3 个及以上）
   - 中间标题是普通文字还是特殊样式？（普通文字 / 加粗 / 特殊颜色 / 图标+文字）
```

#### RefreshLazyList / RefreshLazyGrid 聚合提问

```
针对列表/网格配置，我需要确认以下几点：

1. 页面是否有列表或网格数据展示？
   - 有列表数据 / 有网格数据 / 没有

2. 如果有数据展示，请确认：
   - 是否需要下拉刷新功能？（需要 / 不需要）
   - 是否需要上拉加载更多（分页）功能？（需要 / 不需要）
   - 如果需要分页，如何判断是否还有更多数据？（返回数据为空 / 根据 hasMore 字段 / 根据总条数 / 根据返回条数）
   - 数据展示是列表形式还是网格形式？（列表 / 网格）
   - 如果是网格形式，每行列数是多少？（2 列 / 3 列 / 4 列 / 自适应）
```

#### CoordinatorLayout 聚合提问

```
针对协调者组件配置，我需要确认以下几点：

1. 页面是否有复杂的头部折叠或悬停需求？
   - 有（如个人中心大图折叠、多段悬停） / 没有（仅简单标题栏）

2. 如果有协调需求，请确认：
   - 悬停方式是哪种？（方案 A：Header 固定高度悬停 / 方案 B：内容区吸顶悬停）
   - Header 的最小高度（minHeaderHeight）是多少？（0dp / 具体高度 / 不允许折叠）
   - 上划/下划的优先级是否有特殊要求？（默认 PARENT 优先 / CHILD 优先）
```

#### 分栏组件聚合提问

```
针对分栏组件配置，我需要确认以下几点：

1. 页面是否需要左右或上下滑动切换内容区域？
   - 需要左右滑动 / 需要上下滑动 / 不需要滑动切换

2. 设计稿中哪个区域是分栏内容区？（如果我无法区分，需要你指出）

3. 是否需要左右或上下滑动时自动切换 Tab？
   - 需要双向联动 / 只需要点击 Tab 切换 / 不需要 Tab 联动

4. 设计稿是否提供了 Tab 选中和未选中的效果？
   - 提供了完整效果 / 暂未提供 / 样式完全无法联动

5. 各 Tab 下的内容是否相同？
   - 内容相同，可复用 / 内容完全不同，需要拆分
```

#### BasicInteraction 聚合提问

```
针对状态管理配置，我需要确认以下几点：

1. 页面是否需要展示数据加载状态（loading/error/empty）？
   - 需要展示所有状态 / 只需要部分状态 / 不需要任何状态

2. 如果需要状态管理，请确认：
   - loading 状态是否需要显示文字提示？（需要 / 不需要）
   - error 状态是否需要显示错误信息和重试按钮？（显示 / 只显示信息 / 不显示）
   - empty 状态是否需要显示提示文字和重试按钮？（显示 / 只显示信息 / 不显示）
   - 是否需要全屏遮罩式的弹窗加载？（需要 / 不需要）
```

#### Dialog / NativeDialog 聚合提问

```
针对弹窗配置，我需要确认以下几点：

1. 页面是否有弹窗交互需求？
   - 有 / 没有

2. 如果有弹窗需求，请确认：
   - 弹窗是否需要穿透到原生层（如权限申请）？（需要 / 不需要）
   - 弹窗的展示方式是怎样的？（普通弹窗 / 需要优先级管理 / 需要置顶显示）
   - 弹窗是否可以点击遮罩层取消？（可以 / 不可以）
```

---

## 六、切图规范

> ⛔ **MANDATORY（强制规则）：图片资源必须放在当前页面所属模块的 `composeResources/drawable/` 目录下！**
>
> **决策流程：**
> 1. 先确认当前正在开发的 Screen/Dialog 文件位于哪个模块（`shared_project` / `shared_common` / `shared_base`）
> 2. 将图片资源放置到该模块对应的 `composeResources/drawable/` 目录
> 3. 命名使用对应模块的前缀（`project_` / `common_` / `base_`）

### 6.1 切图尺寸

图片只切 **drawable-xxhdpi** 尺寸，放到所属模块的：

```
composeResources/drawable/
```

**切图和尺寸测量统一按照 Android 平台、375ppi 来处理。**

### 6.2 图片格式

> ⚠️ **必须优先使用 webp 格式！** 除非设计稿明确要求 png，否则一律用 webp。

**优先级：webp > png**

| 优先级 | 格式 | 说明 |
|--------|------|------|
| 1（强制优先） | webp | 推荐，体积小、质量好，默认必选 |
| 2（备选） | png | 仅 webp 不可行时使用 |

**常见误区：**
- ❌ 不要因为"习惯"或"其他项目用 png"就用 png
- ❌ 不要假设 png 兼容性更好就默认用 png（本项目 webp 全平台支持）

### 6.3 命名规范

#### 前缀规则

| 模块 | 前缀 | 示例 |
|------|------|------|
| `shared_project` | `project_` | `project_icon_user.webp` |
| `shared_common` | `common_` | `common_btn_submit.webp` |
| `shared_base` | `base_` | `base_ic_back.webp` |

#### 命名规则

- 仅支持小写字母和下划线 `_`
- 不能以下划线开头或结尾
- 使用英文语义化命名

### 6.4 压缩规则

图片不得进行压缩，使用原始资源。

### 6.5 蓝湖设计稿处理

如果设计稿来自蓝湖，**必须自动使用蓝湖 MCP 服务**（`mcp_lanhu`）获取切图资源，禁止手动从蓝湖网页下载。

### 6.6 图片资源放置模块

> ⛔ **MANDATORY（强制规则）：图片资源必须放在当前页面所属模块下！**

**判断标准：当前正在编辑的 Screen/Dialog 文件位于哪个模块，图片就放到哪个模块。**

| 页面所在模块 | 图片放置路径 | 命名前缀 |
|-------------|-------------|---------|
| `shared_project` | `shared_project/src/commonMain/composeResources/drawable/` | `project_` |
| `shared_common` | `shared_common/src/commonMain/composeResources/drawable/` | `common_` |
| `shared_base` | `shared_base/src/commonMain/composeResources/drawable/` | `base_` |

#### ❌ WRONG（错误示例）

```
场景：正在开发 shared_project 模块下的 UserProfileScreen.kt

错误：将图片放到 shared_common 模块
shared_common/src/commonMain/composeResources/drawable/project_ic_avatar.webp

错误：将图片放到 shared_base 模块
shared_base/src/commonMain/composeResources/drawable/project_ic_avatar.webp
```

#### ✅ RIGHT（正确示例）

```
场景：正在开发 shared_project 模块下的 UserProfileScreen.kt

正确：图片必须放到 shared_project 模块
shared_project/src/commonMain/composeResources/drawable/project_ic_avatar.webp

正确：使用 project_ 前缀命名
project_ic_avatar.webp
```

> ⚠️ **AI 特别提醒：即使页面中引用了 shared_common 模块的组件，图片资源也必须放在当前页面所在的模块！不要因为组件来自其他模块就把图片放到那个模块！**

### 6.7 资源引用规则

> ⚠️ **强制规范：** 使用图片资源时，**必须同时添加两行 import**，缺一不可！

#### 正确写法

**文件顶部 import 区（必须同时添加这两行）：**
```kotlin
import com.basic.project.R_com_basic_project  // R 类，访问 drawable.xx
import com.basic.project.project_ic_phone     // 具体资源名，直接使用
```

> ⚠️ **AI 特别提醒：`import com.basic.project.project_ic_phone` 这行 import 极易被遗漏！每次使用图片时必须检查是否已添加！**

**使用处（直接写简称）：**
```kotlin
Image(
    painter = painterResource(R_com_basic_project.drawable.project_ic_phone),
    contentDescription = null,
    modifier = Modifier.size(24.dp),
    contentScale = ContentScale.FillWidth
)
```

#### 各模块对应关系

| 模块 | namespace（包名） | R 类名 | import 示例 |
|------|------------------|--------|------------|
| shared_project | `com.basic.project` | `R_com_basic_project` | `import com.basic.project.R_com_basic_project` |
| shared_common | `com.basic.common` | `R_com_basic_common` | `import com.basic.common.R_com_basic_common` |
| shared_base | `com.basic.base` | `R_com_basic_base` | `import com.basic.base.R_com_basic_base` |

#### 绝对禁止

```kotlin
// ❌ 禁止在使用处写全限定名！
Image(
    painter = painterResource(com.basic.project.R_com_basic_project.drawable.project_tab_mine_normal),
    ...
)
```

### 6.8 常见问题

#### 编译器引用图片爆红

**原因：** 缺少 import 语句。

**解决方法：**

1. **执行资源生成**：`./gradlew generateComposeResClass`
2. **在报错文件顶部 import 区添加缺失的 import（两行都要检查）：**
   - 爆红：`R_com_basic_project` → 添加：`import com.basic.project.R_com_basic_project`
   - 爆红：`project_ic_phone` → 添加：`import com.basic.project.project_ic_phone`

> ⚠️ **重点检查：`import com.basic.project.project_ic_phone` 这行极容易漏掉！**

**注意：** 不要因为爆红就去修改文件夹结构或文件名，也不要反复重新切图！

---

## 七、核心组件速查

### 7.1 基础组件（来自 shared_common）

| 组件 | 路径 | 用途 |
|------|------|------|
| `BasicScreen` | `shared_common/.../base/BasicScreen.kt` | 所有页面的基类 |
| `BasicHazeScaffold` | `shared_common/.../base/BasicScaffold.kt` | 毛玻璃骨架布局 |
| `BasicTitleBar` | `shared_common/.../base/BasicTitleBar.kt` | 标准标题栏 |
| `BasicInteraction` | `shared_common/.../base/BasicInteraction.kt` | 加载/错误/空状态管理 |
| `BasicRefreshInteraction` | `shared_common/.../base/BasicRefreshInteraction.kt` | 分页下拉刷新+上拉加载 |
| `BasicDialog` | `shared_common/.../base/BasicDialog.kt` | 普通弹窗基类 |
| `LoadingDialog` | `shared_common/.../base/LoadingDialog.kt` | 内置 Loading 弹窗 |

### 7.2 分页与下拉刷新（来自 shared_base）

| 组件 | 路径 | 用途     |
|------|------|--------|
| `PagingControl` | `shared_base/.../ktx/RefreshLazyListKtx.kt` | 分页控制接口 |
| `RefreshState` | `shared_base/.../ktx/RefreshLazyListKtx.kt` | 刷新状态管理 |
| `BasicRefreshLazyListInteraction` | `shared_common/.../base/BasicRefreshInteraction.kt` | 分页列表组件 |
| `CoordinatorLayout` | `shared_base/.../ktx/CoordinatorLayoutKtx.kt` | 协调者组件  |

### 7.3 分栏组件（来自 shared_base）

| 组件 | 路径 | 用途 |
|------|------|------|
| `HorizontalPagerLifecycle` | `shared_base/.../ktx/HorizontalPagerLifecycle.kt` | 左右滑动分栏（带生命周期） |
| `VerticalPagerLifecycle` | `shared_base/.../ktx/VerticalPagerLifecycle.kt` | 上下滑动分栏（带生命周期） |

### 7.4 弹窗控制

| 方法 | 用途 |
|------|------|
| `DialogController.showNow()` | 普通弹窗 |
| `DialogController.showPriority()` | 优先级弹窗 |
| `DialogController.showMaxPriority()` | 最高优先级弹窗 |

---

## 八、典型页面模板

### 8.1 标准分页列表页

```kotlin
@Router(RouterConstant.xxx)
class XxxScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        val model = rememberMainScreenModel { XxxScreenModel() }
        val pullDownProgress = model.refreshState.progress.collectAsState().value
        
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            canConsumeScrollUp = { pullDownProgress <= 0f },
            top = { BasicTitleBar("标题") },
            center = {
                BasicInteraction(model) { modifier ->
                    BasicRefreshLazyListInteraction(
                        modifier = modifier.fillMaxSize(),
                        state = model.refreshState,
                        dataSize = { model.data.size }
                    ) {
                        items(model.data, key = { it.id }) { item -> ItemLayout(item) }
                    }
                }
            }
        )
    }
}

class XxxScreenModel : BasicScreenModel(), PagingControl {
    val data = mutableStateListOf<Item>()
    override val refreshState = RefreshState(true, true)
    
    override fun onLoad(context: ScreenContext) {
        screenModelScope.launchScope {
            uiLoading("加载中...")
            pagingFirst()
            uiSuccess(data.isEmpty())
        }.catch { code, error, e -> uiError(code, error) }
    }
    
    override suspend fun pagingFirst() { /* 实现 */ }
    override suspend fun pagingMore() { /* 实现 */ }
}
```

### 8.2 普通弹窗

```kotlin
@Router(RouterConstant.xxx)
class XxxDialog : BasicDialog() {
    @Composable
    override fun CreateUI() {
        Column { /* 自定义内容 */ }
    }
}
```

---

## 九、关键约定

1. **页面类必须用 `@Router` 注解注册路由**
2. **ScreenModel 必须继承 `BasicScreenModel`**
3. **有分页时必须实现 `PagingControl` 接口**
4. **`onLoad` 中：失败调用 `uiError`、空数据调用 `uiSuccess(true)`**
5. **`pagingMore` 成功必须调用 `pagingOver()` 标识是否还有更多**
6. **Lazy 列表 item 必须指定 `key`，避免不必要的重组**
7. **文本组件必须设置 `lineHeight` 和 `fontWeight`，图片组件必须设置 `contentScale`**
8. **Color 统一使用十六进制范式 `Color(0xffffffff)`，透明度用 `.copy(alpha)`**
9. **禁止直接使用 `HorizontalPager` / `VerticalPager`，必须用 `HorizontalPagerLifecycle` / `VerticalPagerLifecycle`**

---

## 十、遇到问题时

| 问题类型 | 参考章节 |
|---------|---------|
| 不知道用什么组件 | 第五章 界面开发提问指南 |
| 不知道怎么写页面 | 第四章 界面开发规范 4.1 节 |
| 不知道怎么写弹窗 | 第四章 界面开发规范 4.2 节 |
| 不知道怎么写原生弹窗 | 第四章 界面开发规范 4.3 节 |
| 不知道怎么管理加载/错误/空状态 | 第四章 界面开发规范 4.4 节 |
| 不知道怎么写分页 | 第四章 界面开发规范 4.5 节 |
| 不知道怎么写分栏/Tab 页 | 第四章 界面开发规范 4.6 节 |
| 不知道怎么混用组件 | 第四章 界面开发规范 4.7 节 |
| 不知道文本/图片/颜色怎么写 | 第四章 界面开发规范 4.8 节 |
| 不知道怎么处理切图 | 第六章 切图规范 |

---

## 十一、不要做的事

- 不要读取、引用、复制其他项目的文件或代码，所有实现必须基于本项目
- 不要在 Screen 中直接写业务逻辑，全部放在 ScreenModel 中
- 不要忘记调用 `pagingOver()`
- 不要在 `pagingFirst` / `pagingMore` 中手动处理错误（`initPagingControl` 会自动处理）
- 不要扩展 `BasicScreen`，应该继承它
- 不要把 `WebViewState` / 原生 UI 控件的 state 放在 Composable 内
- 不要在多个共享模块各自生成 iOS framework
- 不要使用旧的 `ProjectRouter.main()` / `ProjectRouter.detail()` 等方法新增页面，优先用 `@Router` + `asRouter(url)`
- 不要修改 `pagingFirst` / `pagingMore` 的签名（由 `initPagingControl` 自动调用）
- 不要在弹窗的 `CreateUI()` 中做耗时操作
- 不要硬编码 HazeScaffold 的内容间距，使用 `LocalHazeScaffoldContentPadding`
- 不要在安全边距基础上再追加额外间距（画蛇添足）
- 不要在 item{} / items{} 中加 Spacer 或 padding 来做间距，用 `verticalArrangement` / `contentPadding`
- 不要直接使用 `HorizontalPager` / `VerticalPager`，必须用 `HorizontalPagerLifecycle` / `VerticalPagerLifecycle`
- 不要使用 `Color(red=, green=, blue=)` 写法，统一用十六进制 `Color(0xffffffff)`
- 不要因为图片引用爆红就修改文件夹结构或文件名或反复重新切图，先执行 `generateComposeResClass`，不行再手动 import
- 不要默认使用 png 格式，优先使用 webp
- 禁止使用Icon组件, IconButton组件

---

## 十二、文档职责分工

| 文档 | 职责 | 不应包含 |
|------|------|---------|
| AGENTS.md | 界面开发规范、切图规范、提问指南、核心组件速查、典型模板 | 模块架构、API 签名细节 |
| 架构api文档.md | 全部 API 速查（路由、SPI、网络、WebView 等） | UI 风格规范 |
| 架构设计文档.md | 模块依赖、启动流程、UI 与导航架构 | 具体 API 用法 |
| cocoapods使用说明.md | Pod 依赖配置 | Gradle 配置 |