# CocoaPods 使用说明

## 一、集成位置

所有 Pod 库均在 **所在模块** 集成，方便统一生成 Podfile 配置（`iosApp/Podfile`）。

## 二、版本号规范

所有 Pod 库都必须带上版本号，避免意外升级造成不可预知的风险：

```kotlin
cocoapods {
    pod("AFNetworking", version = "4.0.1")
    pod("Alamofire", version = "5.6.4")
}
```

### 查找版本号

如果某些库没有明确说明版本号，可在 `pod install` 安装后执行：

```bash
cat Podfile.lock
```

然后把对应的版本号填写到 `pod{}` 配置中。

## 三、特殊库配置

### 3.1 纯静态库（.a 文件）

有些库可能不是标准的 framework 库（如纯静态库 `.a`），导致编译时无法自动生成 def 转义配置。

**解决方法：**

在 **所在模块** 的以下目录找到需要的 `.h` 文件名：

```
build/cocoapods/synthetic/ios/Pods/$名称/
```

然后配置在 `build.gradle.kts` 的 pod 依赖中：

```kotlin
pod("SomeStaticLib", version = "1.0.0") {
    headers = "SomeStaticLib.h"
}
```

### 3.2 文件夹名与 framework 名不一致

有些库下载后文件夹的名字和 `xxx.framework` 的 `xxx` 不一样，导致编译时无法自动生成 def 转义配置。

**解决方法：**

在以下目录找到 `.framework` 或 `.xcframework` 文件夹：

```
build/cocoapods/synthetic/ios/Pods/$名称/
```

把 `.` 号前面的名字配置在 `build.gradle.kts` 的 pod 依赖中：

```kotlin
pod("SomeFramework", version = "1.0.0") {
    moduleName = ".号前面的名字"
}
```

### 3.3 CInterop 阶段失败

有些库上述问题都没有，但就是卡在 CInterop 阶段，无法生成 `.kib` 文件。

**解决方法：**

绝大部分问题就是找不到 `.h` 文件，直接引入 headers 即可。如果是在 `.framework/.../Headers/` 下的 `.h` 文件：

```kotlin
pod("SomeFramework", version = "1.0.0") {
    headers = "pod名字/xxx.h"
}
```

### 3.4 多个 .h 文件需要导入

如果发现有多个 `.h` 文件需要导入：

**步骤：**

1. 新建文件 `src/nativeInterop/cinterop/xxxx.h`

2. 写入：

```c
#import <xxx/xxx.h>
#import <xxx/yyy.h>
```

> 前面是否加路径可参考第 3.3 点

3. 在 pod 作用域中配置：

```kotlin
pod("SomeFramework", version = "1.0.0") {
    headers = project.file("src/nativeInterop/cinterop/xxxx.h").absolutePath
}
```

## 四、常见问题排查流程

```
Pod 依赖配置问题
    │
    ├── CInterop 阶段失败？
    │       │
    │       ├── 是否是纯静态库？ → 配置 headers = "xxx.h"
    │       │
    │       ├── 文件夹名与 framework 名不一致？ → 配置 moduleName = "xxx"
    │       │
    │       ├── .h 文件在子目录下？ → 配置 headers = "pod名字/xxx.h"
    │       │
    │       └── 多个 .h 文件？ → 创建 cinterop 桥接头文件
    │
    └── Podfile 配置问题？
            │
            └── 未指定版本号？ → 添加 version 参数
```

## 五、注意事项

1. **不要在多个共享模块各自添加 Pod 依赖**，统一在 **所在模块** 管理
2. **不要省略版本号**，必须明确指定
3. **不要修改 `iosApp/Podfile`**，该文件由框架脚本自动生成