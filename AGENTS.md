# AI Agent 开发导航中心 (Tiered Instruction Hub)

> 本文件作为 AI 在本项目协作的总入口。AI **必须** 首先阅读此文件，掌握分层加载协议，以最小化 Token 消耗并提升决策精度。

## ⚠️ 强制指令 (最高优先级 - 分层加载协议)

**首次进入项目或开始新任务时，AI 必须按顺序执行以下步骤：**

1.  **加载元指令 (Methodology)**：立即读取 `skills/workflows/` 下的所有文件，掌握“如何工作”的状态机逻辑。
2.  **加载知识补丁 (Increment)**：读取 `skills/patches/` 下的文件，同步最新的分布式共识。
3.  **按需加载规约 (Contextual Standards)**：根据当前任务类型（如写 UI、配置 Pod），加载 `skills/standards/` 中相关的规范文件。
4.  **按需检索手册 (On-demand Knowledge)**：**严禁全量加载** `skills/knowledge/`。仅在需要特定 API、组件参数或架构细节时，通过 `grep` 或 `list_files` 定位并读取。

---

## 🛠 分层 Skill 索引

### 1. Workflows (编排层 - 如何工作)
> 这些文档定义了 AI 的互动逻辑与执行阶段，必须驻留内存。

| 任务类型 | 对应文档 | 核心内容 |
| :--- | :--- | :--- |
| **UI 开发流** | [UI开发工作流规范.md](./skills/workflows/UI开发工作流规范.md) | **AI 强制工作流**、嵌套拓扑分析、提问序列 |
| **SDK 集成流** | [SDK集成工作流规范.md](./skills/workflows/SDK集成工作流规范.md) | 侵入分析报告、场景映射矩阵、双端协议审计 |
| **Lib 模版生成** | [Lib模版生成指南.md](./skills/workflows/Lib模版生成指南.md) | 三方 SDK 适配模块一键自动化生成模板 |
| **Project 模版生成** | [Project模版生成指南.md](./skills/workflows/Project模版生成指南.md) | 业务 Project 模块一键自动化生成模板 |
| **IDE 环境同步** | [IDE环境同步指南.md](./skills/workflows/IDE环境同步指南.md) | AS 外部工具与 Xcode Scheme 物理对齐 |
| **启屏页生成** | [启屏页生成指南.md](./skills/workflows/启屏页生成指南.md) | 三端视觉对齐、极简原生打底方案 |
| **Skill 升级/维护** | [AI-Skill自动升级指南.md](./skills/workflows/AI-Skill自动升级指南.md) | **语义间隙感知**、补丁归档与合并机制 |

### 2. Patches (补丁层 - 增量共识)
> 这些文档包含最新的分布式共识与临时规则，必须首先加载。

| 补丁类型 | 对应文档 | 核心内容 |
| :--- | :--- | :--- |
| **最新共识** | [./skills/patches/](./skills/patches/) | 尚未合并到主规约的临时规则与架构补丁 |

### 3. Standards (规约层 - 质量红线)
> 这些文档定义了静态约束，在编码自检阶段加载。

| 规范类型 | 对应文档 | 核心内容 |
| :--- | :--- | :--- |
| **通用代码** | [通用代码规范.md](./skills/standards/通用代码规范.md) | **文件头注释标准**、Git 身份核对、工程美学 |
| **UI 组件规范** | [UI开发规范.md](./skills/standards/UI开发规范.md) | **质量红线**、Text/Image 原子约束、性能禁令 |
| **资源与切图** | [资源与切图规范.md](./skills/standards/资源与切图规范.md) | WebP 强制要求、**R 类双行 Import 规则** |
| **iOS 依赖** | [Pod依赖使用指南与规范.md](./skills/standards/Pod依赖使用指南与规范.md) | 版本锁定、CInterop 故障排查 |

### 4. Project Constraints (项目规约层 - 专属共识)
> 这些文档定义了当前项目特有的规约与动态共识，AI 必须遵守。

| 约束类型 | 对应文档 | 核心内容 |
| :--- | :--- | :--- |
| **项目专属** | [README.md](./skills/project-constraints/README.md) | **项目专属约束**、临时共识、特定业务逻辑红线 |

### 5. Knowledge (知识层 - 查阅手册)
> 这些文档包含 API 细节，仅在需要时通过检索加载。

| 知识领域 | 对应文档 | 核心内容 |
| :--- | :--- | :--- |
| **通用架构** | [架构设计文档.md](./skills/knowledge/架构设计文档.md) | 模块职责图谱、依赖关系、启动流 |
| **交互组件库** | [UI组件手册.md](./skills/knowledge/UI组件手册.md) | 页面模板、`Coordinator` 滑动联动、组件实战 |
| **架构 API** | [架构api文档.md](./skills/knowledge/架构api文档.md) | 路由、网络、SPI、弹窗栈等核心 API 定义 |
| **PPI 适配** | [ppi适配指南.md](./skills/knowledge/ppi适配指南.md) | Compose 物理尺寸适配、设计参数配置与原生控件 Density 隔离 |
| **SDK 集成案例** | [SDK集成案例手册.md](./skills/knowledge/SDK集成案例手册.md) | 同类型 SDK 的接口设计、用户流程、数据流转与模型案例 |
| **APK 安全防护** | [apk安全防护.md](./skills/knowledge/apk安全防护.md) | `ProtectSrc`、VMP 加固、完整性签名、运行期环境校验 |

---

## 🚫 高频禁令汇总 (Don'ts)

- ❌ **禁止 Token 浪费**：严禁无故读取不相关的 `knowledge/` 文件。
- ❌ **禁止自作主张**：必须遵循 `workflows/` 下的原子化确认机制。
- ❌ **禁止遗漏补丁**：必须首先合并 `patches/` 中的共识。

---

## 🎯 工作范围
所有操作必须限定在当前项目根目录下。
