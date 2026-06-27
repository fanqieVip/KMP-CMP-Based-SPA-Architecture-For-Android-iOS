# UI 开发规范

> 本文档定义了 Screen、Dialog 及通用 UI 组件的基础开发标准和编码约定。

## 1. 页面 (Screen) 开发规范

### 1.1 基础要求
- **继承基类**：所有业务页面必须继承 `BasicScreen`。
- **注册路由**：必须使用 `@Router(path)` 注解注册路径。
- **状态管理**：
    - 必须使用 `rememberMainScreenModel` 绑定模型。
    - 模型必须继承 `BasicScreenModel`。
    - **分页规范**：支持分页的页面，其 `onLoad` 方法必须直接调用 `pagingFirst()`，严禁编写重复的初始化加载逻辑（胶水代码）。
    - **禁止**在 Screen 中写复杂的业务逻辑，全部下沉到 ScreenModel。
    - **强制询问交互状态**：在设计确认阶段（阶段二）必须明确询问是否接入 `BasicInteraction`。

### 1.2 生命周期映射
ScreenModel 的生命周期由框架自动管理：
`onInit` (初始化) -> `onLoad` (初次加载) -> `onVisible` (每次可见) -> `onInvisible` (不可见) -> `onDestroyed` (销毁)。

## 2. 弹窗 (Dialog) 开发规范

### 2.1 BasicDialog (Compose 弹窗)
- 适用于大多数业务场景。
- 生命周期跟随宿主 Screen。

### 2.2 BasicNativeDialog (原生弹窗)
- 适用于权限申请、需要穿透到原生层的交互。
- 独立于 Screen 栈管理。

## 3. 通用 UI 组件规范 (细节决定成败)

### 3.1 文本 (Text)
- **强制设置 `lineHeight`**：值通常等于 `fontSize`。
- **强制设置 `fontWeight`**：除非设计稿明确无粗细要求。
- **目的**：消除 Android 和 iOS 在文本渲染高度上的差异。

### 3.2 图片 (Image)
- **强制设置 `contentScale`**：默认使用 `ContentScale.FillWidth`。

### 3.3 颜色 (Color)
- **范式**：统一使用十六进制 `Color(0xffffffff)`。
- **透明度**：禁止使用 RGB 写法，统一用 `.copy(alpha = 0.5f)`。

### 3.4 列表 (Lazy Layout)
- **Key**：每个 item 必须指定唯一的 `key`，防止不必要的重组和状态丢失。
- **间距**：禁止在 item 内部加 Spacer 做间距，必须使用 `verticalArrangement` 或 `contentPadding`。

## 4. 关键禁用清单 (Don'ts)
- ❌ 禁止直接使用 `HorizontalPager` / `VerticalPager`（用其 `Lifecycle` 后缀版本）。
- ❌ 禁止硬编码 `HazeScaffold` 内容间距（用 `LocalHazeScaffoldContentPadding`）。
- ❌ 禁止使用 `Icon` 和 `IconButton` 组件。
- ❌ 禁止在 `onLoad` 中手动处理分页错误（由分页组件自动处理）。
