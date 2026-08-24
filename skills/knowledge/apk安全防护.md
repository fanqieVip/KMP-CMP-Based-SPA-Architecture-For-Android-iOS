# APK 安全防护知识库

> 本文档沉淀当前项目 Android APK 安全防护能力。涉及敏感字符串保护、VMP 加固、APK 完整性签名、运行期环境校验、自动点击与积分墙防作弊及发布前验收。本文只记录机制、入口和维护规则，不记录真实密钥值。

## 1. 防护分层总览

| 层级 | 能力 | 入口 | 主要作用 |
| --- | --- | --- | --- |
| 编译期字符串保护 | `com.basic.protect-src` + `ProtectSrc(...)` | 各模块 `build.gradle.kts`、业务源码 | 防止密钥、请求头 key、JSBridge 名称、Hook 特征等敏感字符串以明文进入 Android 产物 |
| 发布期 VMP 加固 | `mainVmp` | Android Studio Gradle 面板 `app/tasks/publish_online/mainVmp`、`batchTask.gradle` | 将指定范围 dex 方法转为 VMP so，提高静态反编译和动态 Patch 成本 |
| APK 完整性签名 | `mainVmp` 写入加密 asset | `batchTask.gradle` + `EnvCheckerUtils.kt` | 对 Manifest、resources、dex、so 等关键条目生成清单，运行期重建并比对 |
| 运行期环境校验 | `EnvCheckerUtils.checkEnv()` | `core/base/src/androidMain/.../EnvCheckerUtils.kt` | 检测破签、改包、插件化、Hook、Frida、完整性篡改等风险 |
| 自动点击与积分墙防作弊 | 无障碍树隐藏、手势服务拦截、触摸特征检测 | `ProjectBuildConfig.kt`、`core/base` | 提高 Android 无障碍自动点击器的操作成本 |

安全能力必须组合使用：`ProtectSrc` 负责隐藏字符串，VMP 负责提升核心逻辑逆向成本，完整性签名负责发现产物被改写，`EnvCheckerUtils` 负责运行期判定与熔断。

## 2. 编译期字符串保护

### 2.1 适用场景

推荐使用 `ProtectSrc("...")` 保护以下内容：

- 密钥、盐值、IV、摘要拼接片段等密码学材料。
- 请求头 key、签名字段名、服务端约定字段名、JSBridge 方法名。
- DeepLink scheme、关键路由片段、资产文件伪装名等可被攻击者用于定位逻辑的特征。
- Hook、Frida、Xposed、LSPatch、NPatch、破签工具类名、so 名、文件路径、进程名、线程名等检测特征。
- 安全校验中的算法名、固定头、正则表达式、异常分支判定字符串。

### 2.2 接入入口

需要保护字符串的 Android/KMP 模块必须应用插件：

```kotlin
plugins {
    id("com.basic.protect-src")
}
```

业务代码通过 `core/base` 暴露的 marker 调用：

```kotlin
import com.basic.base.utils.ProtectSrc

val headerKey = ProtectSrc("X-Sign")!!
```

当前项目中已观察到 `core/base`、`core/common`、`project/main` 使用该插件。新增模块如果出现敏感字符串，应同步接入插件。

### 2.3 使用红线

- 只允许直接字符串字面量或 `null`：`ProtectSrc("literal")`、`ProtectSrc(null)`。
- 禁止传变量、字符串拼接、`BuildConfig` 字段或运行期表达式：这些输入无法被编译期稳定改写，插件会报错。
- `ProtectSrc` 是编译期标记，不是运行期加密 API；不要用它保护用户输入或服务端下发的动态数据。
- 插件只保护 Android 产物；iOS/common metadata 阶段仍按普通函数返回原值。
- 不要在 `ProtectSrcRuntime`、`ProtectSrcAes256cbc` 内部反向调用 `ProtectSrc`，避免自举依赖。

## 3. VMP 加固

### 3.1 发布入口

生产发布使用 `app/tasks/publish_online/mainVmp` 执行 VMP 加固。该任务位于 `batchTask.gradle`，核心流程是：

1. 准备 VMP 工具目录、SDK/NDK/CMake/JDK 环境。
2. 从 `VmpConfig.kt` 写出 VMP 运行参数和 `rules.txt`。
3. 对渠道 APK 执行 VMP 加固，生成 `*_vmp.apk`。
4. 向 VMP 后的 APK 写入加密完整性签名 asset。
5. 对齐并执行 V2 签名。

### 3.2 配置入口

VMP 配置统一维护在：

```text
buildSrc/src/main/kotlin/com/frame/basic/buildsrc/VmpConfig.kt
```

核心字段：

| 字段 | 作用 | 维护要求 |
| --- | --- | --- |
| `protectRules` | 控制哪些 class/method 进入 VMP 加密范围 | 必须覆盖安全核心类；新增安全逻辑后要同步检查 |
| `nmmpName` | VMP 核心 so 名 | 建议使用干扰性命名；修改后要确认 BuildKonfig 注入和运行期检测同步 |
| `nmmvmName` | VMP 虚拟机 so 名 | 建议使用干扰性命名；修改后要确认 BuildKonfig 注入和运行期检测同步 |
| `className` | VMP 初始化类 | 建议使用干扰性命名；必须与 VMP 工具要求一致 |

如果 VMP 工具版本支持额外的敏感字符、敏感名称或规则配置，应优先收敛到 `VmpConfig.kt`，不要散落在 `batchTask.gradle` 或业务模块中。

### 3.3 加密范围原则

- `EnvCheckerUtils.kt` 必须处于 VMP 保护范围内。
- 包体完整性、证书校验、Hook/Frida 检测、反插件化检测、关键签名生成与比对逻辑应优先进入 VMP。
- 业务密钥拼接、请求签名、风控特征、JSBridge 安全入口等高价值逻辑应按风险纳入 VMP。
- 不要盲目全量扩大范围。VMP 会影响包体、性能、崩溃排查和兼容性，新增规则后必须做 release 产物验证。
- `protectRules` 需要与 `app/proguard-rules.pro` 配套，避免 R8 优化、内联、删除或改名后导致 VMP 规则失效。

## 4. APK 完整性签名

### 4.1 生成侧

`mainVmp` 在 VMP 输出后调用完整性签名写入逻辑。生成侧会扫描 APK 中的关键条目，按固定顺序生成明文清单，再加密写入 asset。

纳入清单的条目包括：

- `AndroidManifest.xml`
- `resources.arsc`
- `classes*.dex`
- `lib/**/*.so`

清单会记录 entry 名称、大小、CRC、压缩方式和 SHA-256。Walle 渠道信息写在 APK Signing Block 中，且在签名后写入，因此不参与该清单计算。

### 4.2 运行期

`EnvCheckerUtils.verifyApkIntegritySignatureOrThrow(...)` 会：

1. 检查完整性签名 asset 是否存在。
2. 如果 asset 缺失但 APK 带有 VMP so 特征，判定为异常，防止删除 asset 绕过加固包校验。
3. 解密 asset，校验固定头格式。
4. 重新扫描当前 APK 的 Manifest、resources、dex、so，构建实际清单。
5. 对比生成侧清单与运行期清单，不一致则触发环境风险处理。

### 4.3 密钥与格式绑定规则

完整性签名的关键密钥、IV、固定头、asset 名和清单格式必须生成侧与运行期同步维护：

- 生成侧：`batchTask.gradle`
- 运行期：`core/base/src/androidMain/kotlin/com/basic/base/utils/EnvCheckerUtils.kt`

项目落地时必须自行替换默认材料，并保证两侧完全一致。修改时不要只改一端；也不要把真实密钥、IV 或签名材料写入文档、README、Issue、提交说明或日志。

## 5. EnvCheckerUtils 运行期环境校验

`EnvCheckerUtils.kt` 是 Android 运行期环境校验核心类，必须被 `ProtectSrc` 和 VMP 双重保护。

当前主要检测项：

| 检测项 | 说明 |
| --- | --- |
| Frida 检测 | 检查 maps、TracerPid、线程名、默认端口、文件路径、进程名等特征 |
| APK 证书校验 | 读取当前签名证书并与构建期注入的校验值比对 |
| Application 类校验 | 检测关键 Application 类是否被替换或移除 |
| 线程数量校验 | 过低线程数视为异常运行环境信号 |
| 已知破签/改包工具检测 | 检测 NP 管理器、Fancy、HiddenInvoke 等类或 so 特征 |
| MultiDex 父类校验 | 检测基础 Application 继承链是否被篡改 |
| 插件化/Patch 环境检测 | 检测 LSPatch、NPatch、LSPosed、DexClassLoader、异常 sourceDir 等 |
| 系统服务代理检测 | 检测 ActivityManager、PackageManager 是否被代理 |
| Hook 堆栈检测 | 通过异常堆栈识别 LSPosed、Xposed、SandHook 等特征 |
| APK 完整性签名校验 | 解密并比对 `mainVmp` 写入的完整性清单 |

`checkEnv()` 当前由 Android 侧公共应用入口在后台协程中调用。它带有 30 秒间隔缓存：校验通过会短期复用结果，校验失败会进入风险处理逻辑。

### 5.1 维护要求

- 新增检测特征字符串必须优先用 `ProtectSrc` 包裹。
- 新增关键校验函数必须确认仍在 VMP 保护范围内。
- 不要输出明确错误原因给攻击者；安全分支里的异常文案应保持模糊。
- 新增高成本检测要考虑调用频率，避免明显拖慢启动或高频业务路径。
- 如果修改 VMP so 名、完整性 asset 名、密钥、IV 或清单格式，必须同步修改生成侧与运行期，并完成 release 包验证。

## 6. 自动点击与积分墙防作弊

### 6.1 什么情况下开启

当 Android 应用存在积分墙、奖励领取等容易被无障碍自动点击器批量操作的场景时开启。开关位于 `ProjectBuildConfig.Build.Android.disableAccessibilityService`，默认值为 `true`。

防护仅在“开关开启且为生产环境”时生效；开发、测试和预发环境不执行，避免影响调试和 UI 自动化。没有积分作弊风险，或产品必须支持 TalkBack 等无障碍功能时，应关闭该开关。

### 6.2 作用

- 隐藏 Activity、NativeActivity 和 NativeDialog 的无障碍节点树，使自动化工具无法按控件文字、ID 或层级定位。
- 检测已启用且具备手势执行能力的无障碍服务，阻止其使用 `dispatchGesture` 自动点击。
- 通过压力、接触面积和输入源组合特征，补充拦截部分模拟触摸事件。

### 6.3 副作用与边界

- TalkBack、Switch Access 等合法辅助功能无法读取页面。
- 具备手势执行能力的无障碍服务开启期间，自动点击和用户手指点击都会被拦截。
- Appium、UIAutomator 等依赖无障碍节点的生产包测试会失效。
- 不能保证阻止 ADB、Root、Hook 或已知坐标的测试注入。

### 6.4 核心处理方式

1. `core/base` 通过 BuildKonfig 注入 `DISABLE_ACCESSIBILITY_SERVICE`，运行期与 `VersionStatus.RELEASE` 共同判断是否启用。
2. `AccessibilityTreeGuard.kt` 在 Window 根节点设置 `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS`，无需逐个 Composable 添加 Modifier。
3. `BaseActivity.dispatchTouchEvent` 检查 `CAPABILITY_CAN_PERFORM_GESTURES` 及触摸组合特征，命中后吞掉事件。
4. `AndroidNativeDialog` 单独处理自己的 Window，避免弹窗节点树遗漏。

## 7. 发布与验收清单

发布或修改 APK 安全能力后，至少按以下清单验收：

| 检查项 | 通过标准 |
| --- | --- |
| `ProtectSrc` 插件接入 | 含敏感字符串的 Android/KMP 模块已应用 `com.basic.protect-src` |
| 字符串保护范围 | 密钥、请求头 key、JSBridge 名、Hook/Frida/Patch 特征等已使用 `ProtectSrc("...")` |
| 编译期约束 | 不存在 `ProtectSrc(value)`、拼接表达式或 `BuildConfig` 动态输入 |
| VMP 规则 | `VmpConfig.protectRules` 覆盖 `EnvCheckerUtils` 和高价值安全逻辑 |
| R8 配套 | `app/proguard-rules.pro` 与 VMP 规则对齐，关键类未被优化到规则失效 |
| 完整性签名 | VMP 后 APK 中存在完整性签名 asset，且运行期能解密并重建清单 |
| 生成/运行一致性 | 完整性签名密钥、IV、固定头、asset 名、清单格式两侧一致 |
| 证书校验 | 构建期注入的 APK 签名校验值来自当前发布证书 |
| 运行期调用 | `checkEnv()` 在 Android 启动链路中执行，且敏感业务可按需增加二次触发 |
| 自动点击配置 | `disableAccessibilityService` 已按产品策略配置，且仅在开关开启与生产环境同时满足时生效 |
| 自动点击覆盖 | `BaseActivity`、`NativeActivity`、`AndroidNativeDialog` 以及新增独立 Window 均已核对 |
| 产物验证 | Release/VMP APK 反编译后，被保护字符串不再以明文出现，VMP so 与完整性校验均存在 |

建议验证命令按任务风险选择：

```bash
./gradlew :buildSrc:compileKotlin
./gradlew :core:base:compileAndroidMain
./gradlew :core:common:compileAndroidMain
./gradlew :project:main:compileAndroidMain
```

VMP 与完整性签名必须以实际 release/VMP 产物为准，普通 debug 包不能替代发布验收。

## 8. 常见风险

- 只使用 `ProtectSrc`，但未执行 VMP：字符串更难被直接搜索，但核心校验逻辑仍可能被静态分析和 Patch。
- 只执行 VMP，未保护特征字符串：攻击者仍可通过明文特征快速定位安全逻辑。
- 修改 `batchTask.gradle` 的完整性参数但忘记同步 `EnvCheckerUtils.kt`：运行期会误判或校验失效。
- 删除或放松 `EnvCheckerUtils` 的 VMP 规则：运行期校验逻辑更容易被定位和篡改。
- 将真实密钥、IV、签名材料写进文档或日志：安全材料扩散后，即使代码保护仍会降低防护收益。
- VMP 规则扩大后未做真机和 release 验证：可能引入启动失败、so 加载失败或兼容性问题。
- 只隐藏无障碍节点树但未拦截手势服务：自动点击器仍可使用固定坐标执行 `dispatchGesture`。
- 把 `pressure == 1f` 单独视为脚本点击：大量真实设备会被误拦截，导致页面完全不可操作。
- 未评估无障碍副作用就开启生产防护：TalkBack、Switch Access 和依赖节点树的生产自动化测试会失效。
