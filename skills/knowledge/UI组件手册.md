# UI 组件手册 (Component Handbook)

> 本文档提供了典型页面的标准模板、复杂组件的实战指南及生命周期映射详情，作为 AI 编码时的参考。

## 1. 典型页面模板 (Code Templates)

### 1.1 标准分页列表页
```kotlin
@Router(RouterConstant.YOUR_PATH)
class XxxScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        val model = rememberMainScreenModel { XxxScreenModel() }
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
- `onInit` (初始化) -> `onLoad` (初次加载数据) -> `onVisible` (用户肉眼可见) -> `onInvisible` (不可见) -> `onDestroyed` (销毁)。

## 3. 复杂组件实战

### 3.1 BasicHazeScaffold 联动
- **`minTopHeight`**：定义折叠后的最小保留高度。
- **滑动优先级**：Scaffold 优先消费向上滑动位移。

### 3.2 CoordinatorLayout (混合滑动)
- **方案 A (固定高度悬停)**：设置 `minHeaderHeight` 为具体 Dp 值。
- **方案 B (随内容吸顶)**：`minHeaderHeight = 0dp`，在 content 中使用 `Column`。
- **关键修饰符**：必须给列表加上 `Modifier.coordinatorMainScroll`。

### 3.3 交互状态托管 (BasicInteraction)
- **多层嵌套策略**：外层负责骨架 Loading，内层列表刷新建议使用 `popLoading` (弹窗) 模式。
- **状态同步**：加载完成必须调用 `uiSuccess(data.isEmpty())`。

## 4. 常见布局适配
- **处理 Bottom 遮挡**：通过 `LocalHazeScaffoldContentPadding.current` 获取并应用到内层列表。

## 5. 基础 UI 组件 (shared_base)

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
