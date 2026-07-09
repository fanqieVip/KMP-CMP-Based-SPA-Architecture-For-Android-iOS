# UI 开发规范 (UI Standards)

> 本文档定义了 UI 开发的质量红线、组件约束及编码美学，AI 必须以此作为代码审查的最高准则。

## 1. 页面 (Screen) 与 弹窗 (Dialog) 约束

### 1.1 Screen 基础要求
- **继承强制性**：所有业务页面必须继承 `BasicScreen`。
- **路由注册**：必须使用 `@Router(path)` 注解，严禁硬编码跳转。
- **组件标准**：标准页面必须使用 `BasicTitleBar`，严禁手动通过 `statusBarsPadding` 和硬编码高度拼接标题栏。
- **状态分离**：
    - 必须使用 `rememberMainScreenModel` 绑定模型。
    - 禁止在 Composable 函数内直接编写复杂的业务逻辑（如网络请求、数据库操作），全部下沉到 `ScreenModel`。

### 1.2 弹窗与原生宿主选择
- **BasicDialog**：仅限 Compose 实现的轻量业务弹窗，生命周期跟随当前 `BaseScreen` 宿主。
- **BasicNativeDialog**：用于需要脱离 Compose 弹窗栈、覆盖到原生层的弹窗，例如权限申请或需要强制覆盖导航栏的交互。
- **UIContainer.push**：用于需要打开完整 `Screen`，且当前 Compose 单页宿主可能被三方 SDK 原生页面遮挡的场景，例如一键登录原生页上继续打开协议页、说明页或业务确认页。
- **Android Activity 主题强制性**：新建 Android Activity（包括 `NativeActivity` 和业务专属 Activity）时，在 `AndroidManifest.xml` 中**必须**配置 `android:theme="@style/base_activity_anim_theme"`。这确保了原生 Activity 的进出场动画与 Compose Screen 的滑动动画（右进右出）完全一致，维持视觉连续性。
- **禁止滥用原生宿主**：常规页面跳转必须优先使用 `navigator.push(Screen())`。`UIContainer.push(Screen())` 会创建独立宿主和独立导航栈，返回、参数传递和数据同步必须由业务明确处理。

## 2. 通用组件原子规约

### 2.1 文本 (Text)
- **强制设置 `lineHeight`**：默认值必须显式设置为与 `fontSize` 相等，以对齐 iOS/Android 渲染基准线。
- **强制设置 `fontWeight`**：除非设计稿明确标注无粗细要求。
- **协议文案富文本**：登录、注册、一键登录、授权页、注销、关于等场景中，只要文案包含协议、隐私政策、用户协议、运营商协议等可阅读条款，协议项必须使用区别于普通说明文字的颜色突出显示，并且可以点击打开对应链接或触发业务自定义跳转。
- **协议映射完整性**：多协议文案必须逐项保留标题与链接的映射关系，禁止只拼接纯文本后丢失链接；SDK 协议与业务协议同时存在时，应按业务语义明确连接词，例如 `登录即认可《SDK协议》和《用户协议》、《隐私政策》并使用本机号码登录`。
- **平台能力降级记录**：如当前平台缺少精确 range 点击能力，必须至少保证协议区域可点击，并在模块文档中记录限制和后续可改进方向。

### 2.2 图片 (Image)
- **强制设置 `contentScale`**：默认推荐 `ContentScale.FillWidth`。
- **格式限制**：必须优先使用 WebP 格式资源。

### 2.3 颜色 (Color)
- **色值范式**：统一使用十六进制 `Color(0xffxxxxxx)`。
- **透明度控制**：禁止使用 RGB 分量计算透明度，统一使用 `.copy(alpha = ...)`。

### 2.4 列表 (Lazy Layout)
- **唯一 Key**：每个 item 必须指定唯一的 `key`。
- **布局约束**：禁止在 item 内部增加 Spacer 做容器间距，必须使用 `verticalArrangement` 或 `contentPadding`。

### 2.5 输入框 (Input Field)
- **组件标准**：业务场景下的文本输入框**必须默认使用** `ComposeEditText`，严禁直接使用原生 `BasicTextField` 或 `TextField`。
- **状态管理**：输入内容变更回调 `textChange` 必须映射到 `ScreenModel` 中的状态。

## 3. 性能与资源安全

- **WebView 释放**：`WebViewState` 必须持有在 `ScreenModel` 中，且必须在 `onDestroyed()` 中调用 `destroyed()`。
- **Haze 磨砂安全**：严禁在 `HazeScaffold` 的 `top`/`bottom` 插槽内设置不透明背景（会导致硬件加速失效）。

## 4. 关键禁用清单 (Prohibition List)

- ❌ **禁止** 直接使用原生 `HorizontalPager` / `VerticalPager`（必须使用其 `Lifecycle` 后缀版本）。
- ❌ **禁止** 直接使用 `Icon` 和 `IconButton` 组件。
- ❌ **禁止** 直接使用原生 `BasicTextField` 或 `TextField`（必须使用 `ComposeEditText`）。
- ❌ **禁止** 严禁在任何布局中硬编码 `44.dp` / `statusBar` 等数值作为安全边距。
- ❌ **禁止** 在 `onLoad` 中手动 catch 并处理分页错误（必须委托给 `PagingControl` 自动处理）。
