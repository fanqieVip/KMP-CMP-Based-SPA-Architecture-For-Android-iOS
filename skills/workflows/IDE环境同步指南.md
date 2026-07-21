# IDE 环境同步指南 (macOS)

> 本文档定义了 Android Studio (AS) 与 Xcode 之间构建环境同步的标准配置。AI 必须以此为准绳进行环境核对。

## 1. 核心规则：最小干预原则 (Idempotency)

**AI 执行准则**：
- **核对优先**：在执行任何修复前，必须先进行全量属性比对。
- **无差异不修改**：若当前 XML 配置在逻辑上（ActionID、顺序、脚本内容）与本指南完全一致，**严禁**执行 `replace_file_content` 操作，以维持 IDE 原始文件的稳定性。

## 2. 环境对应关系矩阵 (Configuration Matrix)

AI 必须确保 AS 与 Xcode 之间的配置严格按下表对应：

| 环境目标 | AS 运行配置名 | AS 外部工具分组 | AS 外部工具名称 | Xcode 对应 Scheme | Xcode 编译配置 | 环境属性值 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **开发环境** | `iosApp-debug` | `GRADLE_ENV` | `GRADLE_ENV_DEBUG` | `iosApp-debug` | `Debug` | `VERSION_STATUS_DEVELOP` |
| **测试环境** | `iosApp-beta` | `GRADLE_ENV` | `GRADLE_ENV_BETA` | `iosApp-beta` | `Release` | `VERSION_STATUS_BETA` |
| **预发环境** | `iosApp-alpha` | `GRADLE_ENV` | `GRADLE_ENV_ALPHA` | `iosApp-alpha` | `Release` | `VERSION_STATUS_ALPHA` |
| **生产环境** | `iosApp-release`| `GRADLE_ENV` | `GRADLE_ENV_RELEASE`| `iosApp-release`| `Release` | `VERSION_STATUS_RELEASE` |

## 3. Android Studio (AS) 标准配置

### 3.1 外部工具定义 (External Tools)
必须在 AS 中定义以下外部工具（Program: `/bin/bash`）：

| 分组 (Group) | 名称 (Name) | 脚本参数 (Arguments) | 工作目录 |
| :--- | :--- | :--- | :--- |
| **GRADLE_ENV** | **GRADLE_ENV_DEBUG** | `-c "sh $ProjectFileDir$/iosApp/env/env_config_debug.sh"` | `$ProjectFileDir$` |
| **GRADLE_ENV** | **GRADLE_ENV_BETA** | `-c "sh $ProjectFileDir$/iosApp/env/env_config_beta.sh"` | `$ProjectFileDir$` |
| **GRADLE_ENV** | **GRADLE_ENV_ALPHA** | `-c "sh $ProjectFileDir$/iosApp/env/env_config_alpha.sh"` | `$ProjectFileDir$` |
| **GRADLE_ENV** | **GRADLE_ENV_RELEASE**| `-c "sh $ProjectFileDir$/iosApp/env/env_config_release.sh"` | `$ProjectFileDir$` |

### 3.2 运行配置核对 (Run Configurations)
**AI 核对逻辑**：
- 扫描 `.idea/runConfigurations/` 下的 XML。
- **关键点：ActionID 构造**。`actionId` 必须符合 `Tool_{Group}_{Name}` 格式。
- **关键点：引用存在性 (AI 全局探测)**：
    - **定义检测**：AI 将尝试扫描 `~/Library/Application Support/Google/AndroidStudio*/tools/{Group}.xml`。
    - **核对标准**：确认该 XML 文件中包含对应的 `<tool name="{Name}">` 节点，且脚本参数与本指南一致。
- **关键点：执行顺序**。在 `<method v="2">` 节点内，`<option name="ToolBeforeRunTask" ... />` 必须位于 `BuildBeforeRunTask` 之前。

## 4. Xcode 标准配置 (Pre-actions)

Xcode 在 Build 之前必须执行脚本生成 `env_config.properties`。

| Scheme | 对应脚本内容 (scriptText) |
| :--- | :--- |
| **iosApp-debug** | `echo "VERSION_STATUS_DEVELOP" > "${PROJECT_DIR}/env/env_config.properties"; exit 0` |
| **iosApp-beta** | `echo "VERSION_STATUS_BETA" > "${PROJECT_DIR}/env/env_config.properties"; exit 0` |
| **iosApp-alpha** | `echo "VERSION_STATUS_ALPHA" > "${PROJECT_DIR}/env/env_config.properties"; exit 0` |
| **iosApp-release**| `echo "VERSION_STATUS_RELEASE" > "${PROJECT_DIR}/env/env_config.properties"; exit 0` |

## 5. 任务指令：同步 IDE 配置

当收到该指令时，AI 执行以下流程：
1. **对比**：读取当前项目 XML 配置与本指南的差异。
2. **报告**：输出 [IDE 环境核对报告] 表格。
3. **修复**：仅针对标记为 `[不匹配]` 的项，由 AI 提供修复方案。
