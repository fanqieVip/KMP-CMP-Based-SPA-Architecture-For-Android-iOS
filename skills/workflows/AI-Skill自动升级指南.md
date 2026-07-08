# AI-Skill 自动升级指南 (AI Self-Evolution & Maintenance)

> 本文档定义了 AI 如何主动发现知识缺位、生成增量补丁，以及如何自动化执行分布式补丁的“归档、合并与清理”工作。

## 1. 触发矩阵 (Trigger Matrix) - 发现知识缺位

AI 应在满足以下“三维感知”条件之一时，主动申请更新补丁（Patch）：

1.  **基础规约锚定 (Base Convention Anchor - 零容忍)**：
    *   一旦用户针对文件头注释（Author/Date/Meta）、命名风格、Import 顺序等“工程美学/元数据”提出纠正。
    *   此类属于低级错误，**首次**触发即必须补丁化。
2.  **架构/模式校准 (Pattern Calibration - 逻辑红线)**：
    *   AI 提出的实现路径被用户以“不符合本项目架构习惯”为由否定。
    *   发现代码库中存在 ≥3 处的统一写法，但现有 Skill 文档未记录。
3.  **高频修正固化 (High-Freq Correction)**：
    *   同一细节规则（如：Text 必须带 lineHeight）被用户纠正 2 次以上。
    *   用户明确给出“这是我们项目的强制规范”的指令时。
4.  **跨端差异补丁 (CPAP - Cross-Platform Anomaly Patch)**：
    *   解决了一个隐晦的、由于 Android/iOS platform 渲染（如 Haze 毛玻璃抖动）或导航栈生命周期不一致引起的 Bug，且该解法具有通用价值。
5.  **纠错即审计 (Correction is Audit)**：
    *   凡是用户对 AI 的“推理逻辑”、“工作流顺序”或“架构理解”提出明确纠正时，AI 必须在下一轮回复中首行执行「补丁触发审计」。
    *   **审计准则**：必须首行公示“正在执行纠错审计...”，比对四个触发维度。审计完成后必须明确告知用户审计结论（已触发/未触发补丁，原因）。

## 2. 补丁生命周期与命名规范

为避免高频协作下的文件名冲突及加载覆盖，必须遵循以下规约：

- **强制命名格式**：`patch_{AgentName}_{YYYYMMDD_HHMMSS_SSS}_{ShortDesc}.md`
    - *示例*：`patch_Claude_20241027_172035_512_InteractionMandatory.md`
- **内容要求**：包含“触发背景”、“修正后的标准模式”、“受影响的 API/组件”。
- **⚠️ 非硬编码准则 (Non-Hardcoding Principle)**：所有 Patch 或 Skill 文档更新必须定义“方法论”与“动态提取路径”，**严禁**将当前环境值（如特定用户名、时间、路径）硬编码进文档。

**申请更新的动作范式：**
> “感知到 [类别] 知识缺位。我建议在 `skills/patches/` 下新建毫秒级隔离补丁 `patch_{Name}_{Timestamp}_{Desc}.md` 以固化该模式，是否同意？”

## 3. 语义间隙感知 (Semantic Gap Awareness)

AI 必须对特定 UI 模式保持“业务敏感度”，禁止无脑复制工具生成的 CSS：

1. **符号敏感 (Symbol Sensitivity)**：识别到 `《 》`、`()内文字`、`下划线` 等符号时，强制开启富文本审计，核对是否存在颜色分层。
2. **场景敏感 (Scenario Sensitivity)**：登录/注册/注销/关于等页面的“协议/政策”描述，默认视为 `AnnotatedString` 处理，禁止使用单色 `Text`。
3. **工具降级补偿 (Tool Fallback Compensation)**：一旦蓝湖工具提示“标注模式”或“Schema 失败”，AI 必须在阶段二中原子化确认富文本颜色与点击跳转路由。
4. **选项完备性审计 (Option Exhaustion Audit)**：AI 在调用 `ask_user` 提供决策选项时，必须自检是否涵盖了“否定现有预设”或“完全自定义”的路径，严禁通过有限选项将用户引导至不适用的框架约束中。

---

## 4. 知识库维护流程 (Maintainer's Guide)

当收到“合并补丁”指令时，AI 按照以下流程执行：

### 4.1 分析与归类
1.  **全量读取**：读取 `skills/patches/` 目录下除 `.gitkeep` 以外的所有 `.md` 文件。
2.  **内容画像**：识别补丁核心内容。
3.  **映射目标**：
    - **UI 展现/组件实战/模板** -> `knowledge/UI组件手册.md`
    - **UI 规范/原子规约/质量红线** -> `standards/UI开发规范.md`
    - **协作流程/提问模板/页面结构** -> `workflows/UI开发工作流规范.md`
    - **图片资源/切图/Import 规范** -> `standards/资源与切图规范.md`
    - **架构设计/模块职责/依赖关系** -> `knowledge/架构设计文档.md`
    - **API 接口/路由/网络/SPI** -> `knowledge/架构api文档.md`
    - **通用代码风格/文件头/Git 规范** -> `standards/通用代码规范.md`
    - **进化机制/补丁规约/自维护** -> `workflows/AI-Skill自动升级指南.md`
    - **环境同步/IDE/构建/CocoaPods** -> `workflows/IDE环境同步指南.md` 或 `standards/Pod依赖使用指南与规范.md`
    - **SDK 集成流程/适配模块** -> `workflows/SDK集成工作流规范.md` 或 `workflows/Lib模版生成指南.md`
    - **Project 项目模块生成** -> `workflows/Project模版生成指南.md`
    - **无法归类的新领域** -> 提议创建新的 `skills/Xxx.md`

### 4.2 拟定合并计划
AI 必须先输出一个合并计划给负责人确认：
- “发现补丁 A，涉及 [分类]，建议合并进 `[目标文档].md` 的「X.X」章节。”
- “发现补丁 B，属于新领域，建议新建 `skills/NewSkill.md`。”

### 4.3 执行合并与清理
1.  **执行合并**：去重、润色并更新目标正式 Skill 文档。
2.  **清理 (Clean Up)**：合并成功后，AI 必须主动申请删除已处理的 Patch 文件。

## 5. 维护禁令
- ❌ **禁止直接覆盖**：严禁在未确认的情况下直接修改正式 Skill 文档。
- ❌ **权限锁死 (Merge Authority Lock)**：AI 发现知识缺位时仅有“创建补丁”提议权。严禁在同一轮对话或自发状态下合并补丁。补丁的“合并、归档、清理”操作**必须且只能**由用户主动发起明确任务指令后方可执行。
- ❌ **禁止保留原件**：已成功合并的补丁必须申请删除，禁止堆积。
