# UI 组件手册 (Component Handbook)

> 本文档提供了典型页面的标准模板、复杂组件的实战指南及生命周期映射详情，作为 AI 编码时的参考。

## 1. 典型页面模板 (Code Templates)

### 1.1 标准分页列表页
```kotlin
@Router(RouterConstant.YOUR_PATH)
class XxxScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        val model = rememberBaseScreenModel { XxxScreenModel() }
        val pullDownProgress = model.refreshState.progress.collectAsState().value
        
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            canConsumeScrollUp = { pullDownProgress <= 0f },
            top = { BasicTitleBar("页面标题") },
            center = {
                    // 5. 使用分页组件 (支持 List, Grid, StaggeredGrid)
                    BasicRefreshStaggeredGridInteraction(
                        modifier = modifier.fillMaxSize(),
                        columns = StaggeredGridCells.Fixed(2),
                        state = model.refreshState,
                        dataSize = { model.data.size }
                    ) {
                        items(model.data, key = { it.id }) { item -> XxxItemLayout(item) }
                    }
                }
            }
        )
    }
}
```

## 2. 核心生命周期映射
- `onInit` (初始化) -> `onVisible` (用户肉眼可见) -> `onInvisible` (不可见) -> `onDestroyed` (销毁)。

## 3. 复杂组件实战

### 3.1 BasicHazeScaffold 联动
- **`minTopHeight`**：定义折叠后的最小保留高度。
- **滑动优先级**：Scaffold 优先消费向上滑动位移。

### 3.2 CoordinatorLayout (混合滑动)
- **方案 A (固定高度悬停)**：设置 `minHeaderHeight` 为具体 Dp 值。
- **方案 B (随内容吸顶)**：`minHeaderHeight = 0dp`，在 content 中使用 `Column`。
- **关键修饰符**：必须给列表加上 `Modifier.coordinatorMainScroll`。

### 3.3 交互状态托管 (BasicInteraction)
- **多层嵌套策略**：外层负责骨架 Loading，内层列表刷新建议使用 `PopLoadingState` (全屏等待框) 模式。
- **状态同步**：加载完成必须调用 `interactionState.uiSuccess(data.isEmpty())`。

### 3.4 Pager 生命周期首帧对齐

`HorizontalPagerLifecycle` / `VerticalPagerLifecycle` 会根据 `PagerState.currentPage` 为当前页注入 `LocalPageLifecycleVisible = true`，内页通过 `rememberBaseScreenModel(tag)` 接收 `onVisible/onInvisible`。

如果当前选中的 tab 由业务状态保存，例如底部导航当前是会员页，创建 `PagerState` 时必须直接注入初始页：

```kotlin
val tabs = MainTabManager.tabs.collectAsState().value
val currentTab = MainTabManager.currentTab.collectAsState().value
val currentIndex = maxOf(tabs.indexOf(currentTab), 0)
val pagerState = rememberPagerState(initialPage = currentIndex) { tabs.size }

HorizontalPagerLifecycle(pagerState, userScrollEnabled = false) { page ->
    TabContent(tabs[page])
}

LaunchedEffect(currentIndex) {
    pagerState.scrollToPage(currentIndex)
}
```

`LaunchedEffect` 只负责后续 tab 切换同步，不能作为首帧纠偏。否则从某个 tab 打开二级页后返回，宿主重新进入 composition 时 Pager 可能先以默认第 0 页分发生命周期，造成首页 WebView 收到错误的 `pageShow(true)`，随后再切回真实 tab，形成 `onVisible/onInvisible` 抖动。

## 4. 常见布局适配
- **处理 Bottom 遮挡**：通过 `LocalHazeScaffoldContentPadding.current` 获取并应用到内层列表。

## 5. 基础 UI 组件 (core/base)

### 5.1 CenterTitleLayout (居中布局)
- **职责**：确保中间部分绝对居中，且不与左右插槽重叠。
- **参数**：
    - `left`: 左侧插槽 `(Modifier) -> Unit`
    - `right`: 右侧插槽 `(Modifier) -> Unit`
    - `center`: 中间插槽 `(Modifier) -> Unit`

### 5.2 常用小工具
- **留白**：使用 `WidthSpacer(value: Dp)` 或 `HeightSpacer(value: Dp)`。
- **富文本链接**：`AnnotatedString.Builder.appendLinkText(text, style, click)` 快速添加可点击链接。

### 5.3 文字与输入
- **ComposeEditText**：统一封装的输入框，支持：
    - 自动数字/手机号过滤 (`isDigitsOnly`)。
    - `maxLength` 限制。
    - `startIcon` 装饰位。
    - `enable` 状态切换（禁用时自动降级为 `Text` 展示）。
    - 支持 `String`、`AnnotatedString` 或 `TextFieldValue` 作为数据源。

### 5.4 原生 Screen 宿主

单页模式下，如果当前可见层是三方 SDK 的原生页面，普通 `navigator.push(...)` 和 Compose 弹窗仍会渲染在下层 Compose 宿主内，用户可能看不见。需要打开完整 Compose 页面时，使用 `LocalUIContainer.current.push(Screen())` 创建新的原生宿主页面。

```kotlin
val uiContainer = LocalUIContainer.current

Text(
    text = "打开原生 Screen",
    modifier = Modifier.click {
        uiContainer.push(NativeScreenScreen())
    }
)
```

如果业务需要关闭原生转场或禁止系统返回，可显式传参：

```kotlin
uiContainer.push(
    screen = NativeScreenScreen(),
    useAnimation = false,
    disablePhysicalBack = true
)
```

选择规则：

- 普通业务页面：优先使用 `navigator.push(Screen())`。
- 原生弹窗容器：使用 `BasicNativeDialog().show(uiContainer)`。
- 原生页面宿主：使用 `uiContainer.push(Screen())`。
- 无原生转场：仅在业务明确要求时使用 `useAnimation = false`。
- 禁止系统返回/侧滑返回：仅在强制流程中使用 `disablePhysicalBack = true`；iOS 嵌套原生页关闭后会重新按当前页策略恢复侧滑手势状态。

## 6. 弹窗与内存泄漏防护 (Dialog & Memory Leaks)

### 6.1 核心挑战
在 KMP 架构中，`Dialog` 实例通常具有较长的生命周期。如果弹窗持有外部 Lambda（如 `onDismiss` 或 `onConfirm` 回调），而这些 Lambda 捕获了 `Activity` 或 `ScreenModel` 的引用，一旦弹窗被意外持有（或销毁时机过晚），极易造成内存泄漏。

### 6.2 自动清理机制 (`by autoClear`)
框架在 `Dialog` 和 `NativeDialog` 基类中提供了 `autoClear` 属性委托。**所有作为类成员的函数回调，必须使用此委托。**

#### 标准用法：
```kotlin
class MyDialog(
    private val tag: String, 
    onDismiss: (tag: String) -> Unit // 构造函数参数
) : BasicDialog() {

    // 1. 使用 by autoClear 包装回调。在弹窗关闭后，此引用会自动置空，切断引用链。
    private val dismissCallback by autoClear(onDismiss)

    @Composable
    override fun CreateUI() {
        Button(onClick = { dismiss() }) { Text("关闭") }
    }

    override fun onDismiss() {
        // 2. 通过委托访问回调
        dismissCallback?.invoke(tag)
    }
}
```

### 6.3 最佳实践红线
- ❌ **严禁**：在 `Dialog` 子类中直接定义 `val callback: () -> Unit`。
- ✅ **强制**：使用 `private val callback by autoClear(initialBlock)`。
- ✅ **作用域绑定**：尽可能将复杂的业务逻辑封装在 `ScreenModel` 中，利用 `rememberHostScreenModel` 共享模型，而不是通过层层 Lambda 传递。
