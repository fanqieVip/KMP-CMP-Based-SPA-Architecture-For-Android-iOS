# PPI 适配指南

> 面向 AI 与开发者阅读。本文描述当前项目已经落地的 Compose Multiplatform PPI 适配架构、参数含义、接入边界和维护要求。

## 1. 解决的问题

Android 与 iOS 的屏幕分辨率、系统 Density、物理 PPI 和逻辑坐标体系不同。直接把设计稿标注值写成 `dp` 或 `sp` 时，相同数值在不同设备上的物理尺寸可能不同；全局修改 Android `Resources.displayMetrics.density` 又会同时影响原生页面和第三方 SDK，侵入范围过大。

当前方案解决以下问题：

- Android 与 iOS 的 Compose UI 使用同一套设计标注值。
- 根据设备物理 PPI 换算 Compose `Density`，尽量保持间距、控件尺寸和字号的物理尺寸一致。
- 不修改平台全局 `Resources`、`UIScreen` 或原生窗口 Density。
- Compose 与原生 UI 的 Density 相互隔离，原生页面、原生控件和第三方 SDK 继续遵循平台默认适配规则。
- 横竖屏切换时 Density 不随窗口宽高变化，控件物理尺寸保持稳定；页面布局仍根据当前窗口约束重新排版。
- Compose 字体与原生互操作容器使用 App 自定义 `fontScale`，默认值为 `1.0`，不读取系统字体无障碍缩放。

该方案追求的是跨设备物理尺寸一致，不是让所有设备显示完全相同数量的内容，也不是把整张设计稿按屏幕宽度等比缩放。物理屏幕更大的设备应当自然展示更多内容，页面仍需使用响应式布局处理可用空间差异。

## 2. 达到的效果

当设备 PPI 数据准确时，同一个 Compose 标注值在不同设备上对应近似相同的物理长度。例如，设计稿中的 `24` 单位间距在 Android 和 iOS 上写成 `24.dp`，最终会分别转换为不同的设备像素数，但肉眼看到的物理间距应当接近。

适配范围如下：

| 场景 | 使用的 Density | 字体缩放 | 处理方式 |
| --- | --- | --- | --- |
| `BaseApp` 内的 Compose 页面 | 设计 Density | App 自定义，默认 `1.0` | `DesignDensityProvider` 全局提供 |
| `NativeDialog` 内的 Compose 内容 | 设计 Density | App 自定义，默认 `1.0` | 独立原生弹窗入口重新提供 |
| `AndroidView` / `UIKitView` 外层互操作区域 | 平台原生 Density | App 自定义，默认 `1.0` | 使用 `NativeDensityProvider` |
| Android 原生 Activity/View | 平台原生 Density | 平台自身规则 | 不修改 `Resources` |
| iOS UIView/UIViewController | 平台原生坐标体系 | 平台自身规则 | 不修改 UIKit 全局配置 |
| 第三方 SDK 原生页面 | 平台原生 Density | 平台自身规则 | 不参与 Compose Density 适配 |
| 自研 WebView H5 | App 设计 Density 换算为 CSS Scale | H5 根字号统一控制 | App 提供 `currentDensity`，H5 动态设置 `rem` 根字号 |
| 第三方 WebView 页面 | WebView 自身规则 | WebView 自身规则 | 不注入设计 Density |

物理尺寸一致依赖设备 PPI 的准确性。Android 厂商上报错误、iOS 新机型尚未收录或模拟器环境不完整时会进入回退逻辑，此时只能保证可用性，不能承诺严格的物理尺寸一致。

## 3. 参数配置

配置文件：

```text
buildSrc/src/main/kotlin/com/frame/basic/buildsrc/DesignConfig.kt
```

当前配置：

```kotlin
object DesignConfig {
    const val baselinePixel: Int = 1080
    const val baselinePpi: Int = 440
    const val measurementPpi: Int = 375
}
```

### 3.1 参数含义

| 参数 | 含义 | 当前值 |
| --- | --- | --- |
| `baselinePixel` | 设计基准设备的短边物理像素数，也是原始设计稿的基准宽度 | `1080` |
| `baselinePpi` | 设计基准设备屏幕的物理 PPI | `440` |
| `measurementPpi` | 设计标注采用的逻辑测量宽度 | `375` |

`measurementPpi` 名称沿用项目配置约定，但它在公式中的业务含义是设计标注坐标宽度，不是另一台设备的物理 PPI。设计稿宽度为 `1080px`，标注工具按 `375` 宽度测量时，代码中的 `1.dp` 对应设计稿中的 `1080 / 375 = 2.88px`。

### 3.2 参数来源

配置参数时必须同时确认以下信息：

1. 原始设计稿短边宽度，例如 `1080px`。
2. 设计稿对应基准设备的真实物理 PPI，例如 `440`。
3. 设计师或开发者实际读取标注时采用的逻辑宽度，例如 `375`。

不得仅根据设计稿导出分辨率猜测 `baselinePpi`。PPI 描述的是物理屏幕每英寸像素数，必须来自基准设备的可靠规格。

### 3.3 BuildKonfig 注入

`core/base/build.gradle.kts` 将三个参数注入共享 BuildKonfig：

```kotlin
buildConfigField(FieldSpec.Type.INT, "DESIGN_BASELINE_PIXEL", "${DesignConfig.baselinePixel}")
buildConfigField(FieldSpec.Type.INT, "DESIGN_BASELINE_PPI", "${DesignConfig.baselinePpi}")
buildConfigField(FieldSpec.Type.INT, "DESIGN_MEASUREMENT_PPI", "${DesignConfig.measurementPpi}")
```

业务代码不得复制或重新硬编码这些参数。修改设计基准时只修改 `DesignConfig`，然后重新执行 Gradle 编译生成 BuildKonfig。

## 4. 换算原理

设计基准短边的物理宽度为：

```text
baselinePhysicalWidthInch = baselinePixel / baselinePpi
```

每个设计标注单位对应的物理长度为：

```text
designUnitInch = baselinePixel / baselinePpi / measurementPpi
```

当前设备每个设计单位需要使用的像素数，即 Compose Density，为：

```text
designDensity = devicePpi * baselinePixel / baselinePpi / measurementPpi
```

使用当前参数，在基准设备上：

```text
designDensity = 440 * 1080 / 440 / 375 = 2.88
```

因此设计稿上按 `375` 坐标系测得的数值可以直接作为 Compose `dp`/`sp` 数值。例如：

```kotlin
Modifier.padding(horizontal = 24.dp)
Text(fontSize = 16.sp)
```

不要把原始 `1080px` 设计稿上的像素值直接写成 `dp`。原始像素标注需要先换算：

```text
composeValue = designPixelValue * measurementPpi / baselinePixel
```

例如设计稿中的 `72px`：

```text
72 * 375 / 1080 = 25
```

代码中应写成 `25.dp`，而不是 `72.dp`。

## 5. 架构实现

核心实现位于：

```text
core/base/src/commonMain/kotlin/com/basic/base/ui/DesignDensity.kt
core/base/src/androidMain/kotlin/com/basic/base/ui/DesignDensity.android.kt
core/base/src/iosMain/kotlin/com/basic/base/ui/DesignDensity.ios.kt
```

数据流如下：

```text
DesignConfig
    -> BuildKonfig DESIGN_* 字段
    -> getDevicePhysicalPpi() expect/actual
    -> calculateDesignDensity(devicePpi)
    -> DesignDensityProvider
    -> LocalDensity
    -> Compose dp/sp 布局与文字
```

`LocalNativeDensity` 会保存进入设计适配作用域之前的平台 Density。这样即使 `DesignDensityProvider` 被嵌套调用，也不会基于已经缩放过的 Density 再次缩放；`NativeDensityProvider` 也能恢复真正的平台 Density。

## 6. 平台 PPI 获取

### 6.1 Android

Android 从 `DisplayMetrics.xdpi` 和 `ydpi` 获取物理 PPI：

- 两个值都有效时使用几何平均值，降低单轴误差影响。
- 只有一个值有效时使用有效轴。
- 合理范围校验为 `100..1000` PPI。
- 无有效值时返回 `null`，由公共层使用 `nativeDensity * 160` 估算。

Android 厂商可能上报不准确的 `xdpi/ydpi`。新增设备测试时若发现整套 Compose UI 统一偏大或偏小，应优先核对设备报告值，不要通过页面局部倍率掩盖问题。

### 6.2 iOS

iOS 没有公开的物理 PPI API。当前实现通过 `uname` 获取 Apple 硬件型号标识，再映射到已知 PPI；模拟器优先读取 `SIMULATOR_MODEL_IDENTIFIER`。

维护要求：

- 新 Apple 设备发布后，需要补充硬件标识与官方 PPI。
- PPI 应以 Apple 官方技术规格为准，第三方设备库只适合辅助获取型号标识，不能作为唯一数值依据。
- 未识别型号返回 `null`，公共层回退到 `nativeDensity * 160`。
- 回退结果是估算值，新设备未收录期间不能保证严格的物理尺寸一致。

后续若接入专用 PPI 数据源，应保留“精确结果/估算结果”的区分和未知机型回退能力，不应把 `nativeScale` 直接当作物理 PPI。

## 7. Compose 根入口

### 7.1 BaseApp

`BaseApp` 是普通 Compose 页面树的统一适配入口：

```kotlin
DesignDensityProvider {
    CompositionLocalProvider(...) {
        MaterialTheme {
            // App Compose content
        }
    }
}
```

业务页面只要从 `BaseApp` 进入，就会自动获得设计 Density，不需要在每个 Screen 重复包装。

### 7.2 NativeDialog

`NativeDialog` 运行在独立的原生弹窗容器中，不一定继承当前 `BaseApp` 的 CompositionLocal，因此必须在 `NativeDialog.Content` 根部重新使用 `DesignDensityProvider`。

新增其他独立 Compose 宿主时也必须检查 CompositionLocal 是否与 `BaseApp` 连续。独立 `ComposeView`、`ComposeUIViewController` 或原生弹窗如果创建了新的 Composition，应在其 Compose 根部提供设计 Density。

## 8. 原生控件特殊处理

### 8.1 为什么需要恢复原生 Density

`AndroidView` 和 `UIKitView` 内部的原生控件遵循平台坐标体系，但它们的 Compose 外层测量、Modifier 尺寸和约束仍会读取 `LocalDensity`。如果直接放在设计 Density 作用域中，原生控件外层尺寸可能被按设计 Density 换算，与其内部原生布局产生比例不一致。

所有原生互操作组件必须使用 `NativeDensityProvider` 包裹：

```kotlin
NativeDensityProvider {
    AndroidView(
        factory = { context -> NativeView(context) },
        modifier = modifier
    )
}
```

```kotlin
NativeDensityProvider {
    UIKitView(
        factory = { NativeUIView() },
        modifier = modifier
    )
}
```

当前 `NativeWebView.android.kt` 和 `NativeWebView.ios.kt` 已按此规则处理。

### 8.2 NativeDensityProvider 的边界

`NativeDensityProvider` 会：

- 恢复进入 `DesignDensityProvider` 前保存的平台原生尺寸 Density。
- 使用 `LocalAppState` 配置的 `fontScale`，默认值为 `1.0`。
- 只影响当前 Compose 子树，不修改 Android `Resources` 或 UIKit 全局配置。

原生控件内部如果自行读取系统字体设置，仍可能跟随平台字体策略。`NativeDensityProvider` 固定的是 Compose 互操作作用域的 `LocalDensity.fontScale`，不是对第三方原生 SDK 的全局字体拦截。

### 8.3 全原生页面和第三方 SDK

全原生 Activity、UIViewController 或第三方 SDK 页面不需要添加任何适配代码。它们不在 Compose `LocalDensity` 作用域内，继续使用系统原生 Density。

禁止为了统一 Compose 视觉效果而修改以下全局对象：

- Android `Resources.displayMetrics.density`、`densityDpi`、`scaledDensity`。
- Activity 或 Application 的全局 Configuration Density。
- iOS `UIScreen.scale`、`nativeScale` 或 UIKit 全局坐标配置。

这些修改会污染原生页面和第三方 SDK，破坏当前方案的隔离边界。

## 9. 自研 WebView H5 适配

### 9.1 适配边界

自研 H5 与原生 WebView 外壳采用分层适配：

```text
Compose WebView 容器
    -> NativeDensityProvider
    -> 使用平台原生 Density

自研 H5 页面
    -> App 提供 currentDensity
    -> H5 除以 window.devicePixelRatio
    -> 动态设置 rem 根字号
```

`NativeWebView.android.kt` 和 `NativeWebView.ios.kt` 继续使用 `NativeDensityProvider`，确保 WebView 原生容器和平台内部实现不受 Compose 设计 Density 影响。App 不修改 WebView 自身 Density，只需向自研 H5 提供获取当前设计 Density 的方法。

第三方 H5、第三方 SDK WebView 和无法修改源码的网页不接入此协议，继续使用 WebView 默认规则。

### 9.2 App Density 方法约定

App 需要提供获取当前设计 Density 的方法，H5 在初始化根字号时调用：

```typescript
getCurrentDensity(): Promise<number>
```

`getCurrentDensity()` 返回的 `currentDensity` 必须满足：

- 类型为有限正数。
- 单位是“每个设计标注单位对应的设备物理像素数”。
- 数值与 Compose `DesignDensityProvider` 当前使用的设计 Density 一致。
- App 每次根据当前设备计算并注入，H5 不得硬编码某台设备的 Density。
- App 返回原始 `currentDensity`，不要提前除以 DPR，也不要返回百分比。

H5 的实际 CSS 缩放倍率为：

```text
designScale = currentDensity / window.devicePixelRatio
```

原因是 `currentDensity` 描述物理像素，而 H5 尺寸使用 CSS px；`window.devicePixelRatio` 描述一个 CSS px 对应的设备像素数。

例如：

```text
currentDensity = 2.89185
window.devicePixelRatio = 3

designScale = 2.89185 / 3
            = 0.96395
```

不得直接把 `2.89185` 用作 CSS `transform: scale(...)` 或根字号倍率，否则会把物理像素倍率再次当作 CSS 倍率，造成重复放大。

### 9.3 根字号初始化实现

当前自研 H5 使用以下实现：

```typescript
const BASE_ROOT_FONT_SIZE_PX = 50
const DENSITY_BRIDGE_TIMEOUT_MS = 500

function applyRootFontSize(scale = 1) {
  const normalizedScale = Number.isFinite(scale) && scale > 0 ? scale : 1
  document.documentElement.style.fontSize = `${BASE_ROOT_FONT_SIZE_PX * normalizedScale}px`
  document.documentElement.style.setProperty('--design-scale', String(normalizedScale))
}

async function initRootFontSize() {
  applyRootFontSize()
  if (!hasAppBridge()) {
    return
  }

  try {
    const currentDensity = await Promise.race([
      getCurrentDensity(),
      new Promise<undefined>((resolve) => {
        window.setTimeout(resolve, DENSITY_BRIDGE_TIMEOUT_MS)
      }),
    ])
    const devicePixelRatio = window.devicePixelRatio || 1
    if (typeof currentDensity === 'number' && Number.isFinite(currentDensity) && currentDensity > 0) {
      applyRootFontSize(currentDensity / devicePixelRatio)
    }
  } catch {
    // Keep the fixed browser baseline when the native method is unavailable.
  }
}
```

`initRootFontSize()` 必须在 H5 应用初始化阶段调用。初始化流程如下：

1. 立即执行 `applyRootFontSize()`，将根字号设置为浏览器基准值 `50px`。
2. 检查 App Density 方法是否可用；普通浏览器或方法不可用时直接保留 `50px`。
3. App 环境中并行等待 `getCurrentDensity()` 和 `500ms` 超时。
4. App 方法在超时前返回有限正数时，读取 `window.devicePixelRatio`。
5. 计算 `currentDensity / devicePixelRatio`，更新根字号和 `--design-scale`。
6. App 方法超时、抛出异常或返回非法数据时保持 `50px`，页面继续可用。

`Promise.race` 只负责限制 App Density 方法的等待时间，不应取消或修改原生 Density。

### 9.4 `50px` 基准与设计标注换算

`BASE_ROOT_FONT_SIZE_PX = 50` 对应当前 H5 的 `750` rem 坐标约定。在 `375` 逻辑测量宽度下：

```text
375 / 7.5 = 50px
```

设计标注值转换为 rem：

```text
remValue = measurementValue / 50
```

例如：

```text
14 设计单位 -> 0.28rem
24 设计单位 -> 0.48rem
48 设计单位 -> 0.96rem
```

当 `currentDensity = 2.89185`、`devicePixelRatio = 3` 时：

```text
rootFontSize = 50 * (2.89185 / 3)
             = 48.1975 CSS px
```

以 `0.48rem` 的 24 单位间距为例：

```text
CSS length = 0.48 * 48.1975
           = 23.1348 CSS px

physical length = 23.1348 * 3
                = 69.4044 device px

Compose length = 24 * 2.89185
               = 69.4044 device px
```

两端最终使用相同的物理像素长度。

动态注入方案启用后，禁止再使用以下规则控制根字号：

```scss
html {
  font-size: calc(100vw / 7.5);
}
```

根字号只能由 `applyRootFontSize()` 统一设置。否则页面会重新按视口宽度缩放，与 App 提供的 PPI Scale 形成重复或覆盖关系。

### 9.5 rem 与响应式单位的职责

接入 PPI 适配后，`rem` 不再承担“把整张页面按视口宽度等比缩放”的职责，而是表示需要与 Compose 保持物理尺寸一致的设计单位。

适合使用 `rem` 的属性：

- 字体大小。
- 内边距和外边距。
- 按钮高度。
- 圆角。
- 图标尺寸。
- 固定高度组件。

需要铺满父容器或跟随可用空间变化的布局，应继续使用：

- 百分比，例如 `width: 100%`。
- Flex 或 Grid 的弹性轨道。
- `vw`、`vh`、`vmin` 等视口单位。
- `min()`、`max()`、`clamp()` 和媒体查询。

安全区 `env(safe-area-inset-*)` 已经由浏览器按 CSS px 提供，不要再次乘以 `--design-scale`。全屏宽度、流式列表和响应式网格也不要为了 PPI 对齐全部改成固定 rem。

`--design-scale` 用于少量无法通过 rem 表达的特殊计算、调试和组件适配。已经使用 rem 的尺寸不得再次乘以 `--design-scale`。

### 9.6 H5 样式基线

推荐保留以下基础样式：

```scss
html {
  text-size-adjust: 100%;
  -webkit-text-size-adjust: 100%;
}

body {
  margin: 0;
  font-size: 0.28rem;
}
```

其中 `0.28rem` 对应 `14` 个设计标注单位。`text-size-adjust` 用于避免 WebView 对文本进行额外自动放大，保证字号只受根 rem 和项目自定义规则控制。

## 10. 字体规则

当前设计 Density 和 `NativeDensityProvider` 都读取 `LocalAppState.current.fontScale`，默认使用：

```kotlin
fontScale = 1f
```

因此 Compose `sp` 不跟随 Android/iOS 系统字体无障碍倍率，而是由 App 自己统一配置。业务通过 `LocalAppState.current.setFontScale(...)` 修改倍率，不要重新读取系统 `fontScale`，也不要在业务页面逐个修改字号。

建议的统一公式为：

```text
effectiveFontScale = customAppFontScale
```

在全局自定义参数正式落地前，固定值保持为 `1.0`。

## 11. 横竖屏与响应式布局

`baselinePixel` 表示设计基准设备的短边像素，但运行时 Density 只由设备 PPI 和设计参数决定，不读取当前窗口长边或短边。因此：

- 横竖屏切换不会改变同一控件的物理尺寸。
- Density 不会因为平板、分屏或窗口宽度变化而反复缩放。
- 横屏后可用宽度和高度会变化，Compose 会根据约束重新布局。
- 该方案不会自动把竖屏页面整体压缩到横屏短边中。

页面仍必须使用 `fillMaxWidth`、`weight`、`BoxWithConstraints`、窗口尺寸分类、可滚动容器等响应式手段处理空间变化。PPI 适配负责尺寸单位，响应式布局负责内容排布，两者不能互相替代。

## 12. 新增代码检查清单

新增或修改 UI 时检查：

- 设计标注是否基于 `measurementPpi` 坐标系，而不是直接使用原始设计像素。
- 页面是否从 `BaseApp` 的 Composition 进入。
- 独立 Compose 宿主是否重新提供 `DesignDensityProvider`。
- `NativeDialog` 等独立弹窗是否处于设计 Density 作用域。
- 每个 `AndroidView`、`UIKitView` 是否由 `NativeDensityProvider` 直接包裹。
- App 是否提供获取 `currentDensity` 的方法，H5 是否将结果除以 `window.devicePixelRatio`。
- H5 根字号是否只由 `applyRootFontSize()` 设置，未继续使用 `100vw / 7.5`。
- H5 固定设计尺寸是否使用 rem，铺满和弹性布局是否使用百分比或视口单位。
- App Density 方法缺失、超时、异常和返回非法值时是否都能回退到 `50px` 根字号。
- 全原生页面和第三方 SDK 是否保持平台默认 Density。
- 是否错误地修改了 Android 全局 `Resources` Density。
- 是否错误地把 iOS `scale/nativeScale` 当作 PPI。
- 是否错误依赖系统字体无障碍倍率；当前项目使用 App 自定义倍率，默认值为 `1.0`。
- 横竖屏、手机、平板和分屏场景是否仍具备响应式布局能力。

## 13. 验证建议

至少覆盖以下验证矩阵：

| 平台 | 设备类型 | 验证内容 |
| --- | --- | --- |
| Android | 不同分辨率与 PPI 的真机 | Compose 控件物理尺寸、系统原生控件尺寸、字体倍率 |
| Android | 厂商修改显示大小的设备 | `xdpi/ydpi` 可信度与回退表现 |
| iOS | 326/401/458/460/476 PPI 设备 | 型号映射、Compose 物理尺寸 |
| iOS | iPad 与 iPad mini | 264/326 PPI 差异、横竖屏布局 |
| 双端 | 横竖屏切换 | Density 不突变、页面不溢出、NativeView 比例正常 |
| 双端 | NativeDialog | 独立 Composition 中的尺寸与普通页面一致 |
| 双端 | 自研 WebView H5 | App Density、DPR 换算、rem 物理尺寸 |
| 双端 | App Density 方法异常 | 方法不可用、超过 `500ms`、异常和非法返回值均回退到 `50px` |
| 双端 | 第三方 WebView/SDK | 原生内容继续使用平台默认适配 |

推荐使用实际尺子或固定物理参照物测量关键控件，不要只比较截图像素。截图像素不同是预期结果，目标是不同 PPI 设备上的物理长度接近。

## 14. 已知限制

- Android `xdpi/ydpi` 由设备系统上报，部分厂商数据可能不准确。
- iOS 精确 PPI 依赖硬件型号数据，新设备需要持续维护。
- 未识别设备使用原生 Density 推算 PPI，只是可用性回退。
- 外接显示器、桌面窗口化、远程显示和特殊缩放模式不在当前保证范围内。
- App 自定义 `fontScale` 不读取系统字体无障碍设置，默认值为 `1.0`。
- 自研 H5 获取 App Density 超过 `500ms` 或返回非法值时会使用固定 `50px` 基线，本次页面生命周期内不保证再次自动校准。
- 物理尺寸一致不等于视觉截图像素一致，也不等于所有设备显示相同内容量。

出现全局统一偏大或偏小时，应依次检查设计参数、设备 PPI、回退路径和 Provider 边界，不要在业务组件中增加局部缩放倍率。
