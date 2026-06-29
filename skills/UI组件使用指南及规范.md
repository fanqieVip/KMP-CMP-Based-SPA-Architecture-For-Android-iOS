# UI 组件使用指南及规范

> 本文档定义了 Screen、Dialog 及通用 UI 组件的基础开发标准、编码约定、复杂组件的实战指南，并提供了典型页面的标准代码模板。

## 1. 典型页面模板 (Code Templates)

> AI 在开发新页面时，应优先参考此处的结构。

### 1.1 标准分页列表页
适用于：展示数据列表，且支持下拉刷新和上拉加载更多的页面。

```kotlin
@Router(RouterConstant.YOUR_PATH)
class XxxScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        // 1. 创建并记忆 ScreenModel
        val model = rememberMainScreenModel { XxxScreenModel() }
        
        // 2. 获取刷新进度（用于协调 Scaffold 滑动）
        val pullDownProgress = model.refreshState.progress.collectAsState().value
        
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            // 3. 协调滑动消费
            canConsumeScrollUp = { pullDownProgress <= 0f },
            top = { BasicTitleBar("页面标题") },
            center = {
                // 4. 使用 BasicInteraction 管理 loading/error/empty 状态
                BasicInteraction(model) { modifier ->
                    // 5. 使用分页列表组件
                    BasicRefreshLazyListInteraction(
                        modifier = modifier.fillMaxSize(),
                        state = model.refreshState,
                        dataSize = { model.data.size }
                    ) {
                        // 6. 列表内容，必须指定 key
                        items(model.data, key = { it.id }) { item -> 
                            XxxItemLayout(item) 
                        }
                    }
                }
            }
        )
    }
}

class XxxScreenModel : BasicScreenModel(), PagingControl {
    // 数据源
    val data = mutableStateListOf<Item>()
    
    // 分页状态管理
    override val refreshState = RefreshState(true, true)
    
    override fun onLoad(context: ScreenContext) {
        screenModelScope.launchScope {
            // 首次进入显示全屏 Loading
            uiLoading("加载中...")
            pagingFirst()
            // 加载完成后更新状态（检查是否为空）
            uiSuccess(data.isEmpty())
        }.catch { code, error, e -> 
            uiError(code, error) 
        }
    }
    
    override suspend fun pagingFirst() {
        // 调用接口获取第一页数据
    }
    
    override suspend fun pagingMore() {
        // 调用接口获取更多数据
    }
}
```

### 1.2 普通弹窗 (BasicDialog)
适用于：Compose 实现的业务弹窗。

```kotlin
@Router(RouterConstant.YOUR_DIALOG_PATH)
class XxxDialog : BasicDialog() {
    @Composable
    override fun CreateUI() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(Color(0xffffffff), shape = RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "弹窗标题",
                fontSize = 18.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // ... 内容 ...
            Button(onClick = { dismiss() }) {
                Text("关闭")
            }
        }
    }
}
```

### 1.3 原生弹窗 (BasicNativeDialog)
适用于：需要穿透到原生层、或不随 Screen 销毁的弹窗。

```kotlin
class XxxNativeDialog : BasicNativeDialog() {
    @Composable
    override fun CreateUI() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 原生弹窗内容
        }
    }
}
```

---

## 2. 页面 (Screen) 开发规范

### 2.1 基础要求
- **继承基类**：所有业务页面必须继承 `BasicScreen`。
- **注册路由**：必须使用 `@Router(path)` 注解注册路径。
- **组件对齐**：标准页面必须使用 `BasicTitleBar`，严禁手动通过 `statusBarsPadding` 和硬编码高度拼接标题栏。
- **状态管理**：
    - 必须使用 `rememberMainScreenModel` 绑定模型。
    - 模型必须继承 `BasicScreenModel`。
    - **分页规范**：支持分页的页面，其 `onLoad` 方法必须直接调用 `pagingFirst()`，严禁编写重复的初始化加载逻辑（胶水代码）。
    - **禁止**在 Screen 中写复杂的业务逻辑，全部下沉到 ScreenModel。
    - **强制询问交互状态**：在设计确认阶段（阶段二）必须明确询问是否接入 `BasicInteraction`。

### 2.2 生命周期映射
ScreenModel 的生命周期由框架自动管理：
`onInit` (初始化) -> `onLoad` (初次加载) -> `onVisible` (每次可见) -> `onInvisible` (不可见) -> `onDestroyed` (销毁)。

## 3. 弹窗 (Dialog) 开发规范

### 3.1 BasicDialog (Compose 弹窗)
- 适用于大多数业务场景。
- 生命周期跟随宿主 Screen。

### 3.2 BasicNativeDialog (原生弹窗)
- 适用于权限申请、需要穿透到原生层的交互。
- 独立于 Screen 栈管理。

## 4. 通用 UI 组件规范

### 4.1 文本 (Text)
- **强制设置 `lineHeight`**：值通常等于 `fontSize`。
- **强制设置 `fontWeight`**：除非设计稿明确无粗细要求。
- **目的**：消除 Android 和 iOS 在文本渲染高度上的差异。

### 4.2 图片 (Image)
- **强制设置 `contentScale`**：默认使用 `ContentScale.FillWidth`。

### 4.3 颜色 (Color)
- **范式**：统一使用十六进制 `Color(0xffffffff)`。
- **透明度**：禁止使用 RGB 写法，统一用 `.copy(alpha = 0.5f)`。

### 4.4 列表 (Lazy Layout)
- **Key**：每个 item 必须指定唯一的 `key`，防止不必要的重组和状态丢失。
- **间距**：禁止在 item 内部加 Spacer 做间距，必须使用 `verticalArrangement` 或 `contentPadding`。

## 5. 复杂组件实战指南

### 5.1 交互状态与分页联动 (BasicInteraction & PagingControl)
- **BasicInteraction**：负责页面整体状态 `loading -> success/error/empty`。
    - 首次加载成功：`uiSuccess(data.isEmpty())`。
    - 首次加载失败：`uiError(code, error)`。
    - **多层嵌套准则**：外层负责骨架加载，内层负责数据刷新。建议内层使用 `localLoading` 或 `popLoading` 策略。
- **PagingControl**：负责列表层面的局部状态（下拉刷新与上拉加载）。
    - 必须在 ScreenModel 中实现 `PagingControl` 接口。
    - 加载完成调用 `pagingOver(isOver)`，框架会自动处理 Loading 消失。

### 5.2 分栏组件 (Pager) 高级用法
- **生命周期同步**：Pager 内每个 Tab 的 `ScreenModel` **必须**在 Tab 内容 Composable 内部创建。
- **嵌套滑动协调**：当 `BasicHazeScaffold` 嵌套 Pager 再嵌套列表时，使用状态提升。父页面定义 `canConsumeScrollUp` 状态并监听内页 `refreshState.progress`。
- **递归开发协议 (Recursive Development)**：针对多层 Pager 嵌套，AI 必须遵循“分层闭环”原则。每一层嵌套需独立执行“确认-计划-编码”，外层实现后方可申请启动子层的工作流。

### 5.3 CoordinatorLayout 悬停方案
- **方案 A (固定高度悬停)**：设置 `minHeaderHeight` 为悬停区高度。
- **方案 B (随内容吸顶)**：`minHeaderHeight = 0dp`，在 content 中使用 `Column` 包裹 `[吸顶组件, 列表]`。
- **滚动源标记**：必须给列表组件加上 `Modifier.coordinatorMainScroll`，头部如需响应滑动需加 `Modifier.coordinatorDragProxy`。

### 5.4 常见布局适配
- **处理 Bottom 遮挡**：在 `center` 闭包内通过 `LocalHazeScaffoldContentPadding.current` 获取边距，并应用到列表的 `contentPadding` 上。
- **WebView 资源释放**：`WebViewState` 必须放在 `ScreenModel` 中。**强制要求**在 `ScreenModel.onDestroyed()` 中调用 `webviewState.destroyed()`。

## 6. 关键禁用清单 (Don'ts)
- ❌ 禁止直接使用 `HorizontalPager` / `VerticalPager`（用其 `Lifecycle` 后缀版本）。
- ❌ 禁止硬编码 `HazeScaffold` 内容间距（用 `LocalHazeScaffoldContentPadding`）。
- ❌ 禁止使用 `Icon` 和 `IconButton` 组件。
- ❌ 禁止在 `onLoad` 中手动处理分页错误（由分页组件自动处理）。
- ❌ 严禁在 `HazeScaffold` 的 `top` / `bottom` 插槽内设置不透明背景（会导致毛玻璃失效）。
- ❌ 严禁在任何布局中使用 `44.dp` / `statusBar` 等硬编码数字作为安全边距。
