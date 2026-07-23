# UI 开发工作流规范

> 本文档定义了 AI 在参与项目开发时的协作流程、互动规则及标准提问模板。其核心作用是**在编码前通过结构化沟通，确定页面的主要结构、导航模式及交互骨架**。

## 1. AI 强制工作流 (必选)

AI 在接收到新页面/功能的编写指令时，**必须**严格遵循以下四个阶段，禁止跳过：

1.  **阶段一：需求与设计分析** —— 仔细阅读设计稿或文字需求。
2. **按指南提问 (核心)** —— 按照本指南的模板，向用户确认不确定项。
3. **制定实施计划** —— 在得到用户全部确认后，列出 Todo 列表。
4. **编码实施** —— 按照计划分步实现。

### 1.1 嵌套拓扑分析 (针对复杂页面)
当识别到页面存在多层嵌套时，在进入阶段二前，必须先输出 `[UI 嵌套拓扑树]`。
1. **状态标注**：必须在每个拥有独立 `ScreenModel` 的节点后标注 `(Interaction?)` 以对齐交互职责。
2. **递归确认**：必须针对每一个标注了 `(Interaction?)` 的层级，原子化确认其是否需要独立的 `BasicInteraction` 状态托管。
3. **层级归属原则**：若存在 Layer 1 (Bottom Nav) 则 Layer 2 (Top Tabs) 必须下沉为 Layer 1 内部 Page 的组件，严禁在根 Scaffold 层级与其并列。
4. **分层分段实施计划 (Layered Implementation Plan)**：
   - 当检测到 $\ge 2$ 层分栏嵌套时，AI 禁止输出全量 Todo，必须改为输出 `[分层实施路线图]`。
   - **执行原则**：每一层嵌套（Page / Tab）被视为一个独立的子工作流，必须闭环执行“确认 -> 计划 -> 编码”。
5. **层级独立性准则 (Layer Independence Rule)**：嵌套拓扑中的每一层（Layer X）都必须被视为一个**完全独立的 UI 单元**。AI 严禁将外层容器的结构决策（如“已使用 Scaffold”）通过直觉或惯性传递给内层子页面。
6. **职责边界感知识别 (Responsibility Boundary Perception)**：
   - **框架层组件 (Layer 1)**：`Pager` (根容器), `Scaffold` (全局导航), `Interaction` (全屏占位)。
   - **业务层组件 (Layer 2+)**：`Scaffold` (局部标题栏), `Coordinator` (页面内滚动), `Paging`/`List` (具体业务加载)。
   - **红线**：严禁在框架层确认过程中混入业务层组件确认，反之亦然。
7. **显式闭环交棒 (Explicit Handoff)**：当当前层级（Layer X）完成其职责范围内的组件确认后，AI 必须**显式宣告该层闭环**，并请求开启下一层的工作流。
   - **强制宣告范式**：“Layer X [名称] 的骨架确认已闭环。现在申请启动 Layer X+1 [名称] 的子工作流。”

### 1.2 工作流中断恢复协议 (Context Resume Protocol)
- **状态锚定**：当 UI 开发工作流（阶段一至四）被非当前 UI 任务（如补丁创建、Bug 修复、环境同步）中断时，AI 必须记录当前所处阶段及**嵌套层级坐标**。
- **强制询问回复**：中断任务完成后，AI 严禁直接继续 UI 工作流。必须首先询问：“**当前 [页面名] 的 UI 开发工作流处于 [嵌套层级] 的阶段 [X]，是否回到该工作流继续执行？**”。
- **禁止跳过**：在未得到用户确认前，严禁进入下一阶段（尤其是编码实施阶段）。

## 2. 交互互动规则

- ⚠️ **原子化提问优先级 (Mandatory Priority)**：必须严格按以下顺序发起 `ask_user`，严禁越级：
    1. **分栏组件 (Pager)**：优先确认是否多 Tab 嵌套。
    2. **脚手架组件 (Scaffold)**：确认骨架模式。
    3. **主交互组件 (Interaction)**：确认全屏状态管理。
    4. **协调者组件 (Coordinator)**：确认头部折叠逻辑。
    5. **分页组件 (Paging)**：确认数据加载模式。
    6. **其他组件**。
- ⚠️ **强制快照公示 (Mandatory Progress Snapshot)**：在阶段二的原子化提问中，AI 的每一次回复 **必须** 以 `[UI 嵌套拓扑树 - 实时决策快照]` 开头。快照须清晰标注当前 Layer、已确认项 `[x]` 及正在进行的决策项。提问内容必须紧随快照之后。
- ⚠️ **优先级序列递归重置 (Recursive Priority Reset)**：当工作流进入下一嵌套层级（从 Layer 1 进入 Layer 2）时，组件确认的优先级序列**必须强制重置**。AI 必须从头开始执行该层的原子化提问，严禁跳过任何环节。
- ⚠️ **零容忍选项 (Mandatory Optionality)**：所有提问选项中**必须**包含“不使用/完全手动实现”的路径。
- ⚠️ **架构拓扑冲突决策 (Topology Conflict Resolution)**：当页面同时涉及 **协调者组件** 与 **分页/列表组件** 时，AI 必须发起专项确认其父子嵌套层级。
- ⚠️ **交互深挖协议 (Deep-Dive Interaction)**：针对 **Scaffold** 和 **Coordinator**，一旦用户选择“使用”，AI **必须通过开放式问题追问**（严禁提供选项）是否有特殊的交互效果（如滑动监听、吸顶规则等）需要说明。
- ⚠️ **拓扑树优先 (Hierarchy First)**：识别到 3 层及以上嵌套时，必须先公示嵌套拓扑树，获得架构认可后再进行原子化提问。
- ⚠️ **原子化提问 (One at a time)**：提问时必须一个一个确认。在没有得到当前问题的工具反馈前，**严禁**提出下一个问题。
- ⚠️ **简洁化**：能用单选解决的不用多选，能用多选解决的不用输入。
- ⚠️ **降级决策确认协议 (Downgrade Requires Confirmation)**：只要 AI 准备采用任何“降级实现”或替代路径，必须暂停执行并先询问开发者，严禁自作主张替换方案。
    1. 必须说明原方案为什么无法继续、存在什么阻塞或风险。
    2. 必须说明准备降级到什么方案，以及会牺牲哪些还原度、完整度、规范性或校验范围。
    3. 必须提供 2 到 4 个可选方案，且至少包含“不降级/继续原方案但需要处理阻塞”的路径。
    4. 必须等待开发者明确选择后，才能继续执行。
    5. 典型触发包括但不限于：工具不可用或返回降级结果、依赖缺失、环境受限、网络失败、权限不足、设计/接口/文档/资产信息不完整、项目规范与当前实现方式冲突、准备用占位/模拟/手写/绘制/临时资源/静态数据/替代组件替换原计划、准备降低 UI 还原度/交互完整度/资源规范/编译校验范围/测试覆盖范围。
    6. 禁止未经确认自行从切图改为手绘、从 WebP 改为 PNG/SVG、从真实接口改为假数据、从项目组件改为手写组件；禁止在最终回复中才告知已经降级。

## 3. 标准提问模板

### 3.1 分栏 (Pager)
针对多标签切换，我需要确认：
1. 是否需要使用 Pager 分栏？（选项必须包含：不使用）
2. 是否需要双向联动（滑动页面同步切换 Tab）？
3. 各 Tab 内容是否可复用同一个 Composable？
4. Pager 初始选中项来源是什么？若当前 Tab 来自业务状态、全局状态或路由参数，必须在创建 `PagerState` 时通过 `rememberPagerState(initialPage = currentIndex) { count }` 同步注入，禁止先默认第 0 页再在 `LaunchedEffect` 中滚动到目标页。

生命周期红线：
- 使用 `HorizontalPagerLifecycle` / `VerticalPagerLifecycle` 时，`LocalPageLifecycleVisible` 依赖 `PagerState.currentPage` 分发内页 `onVisible/onInvisible`。如果恢复页面、返回二级页或重建 composition 时 `PagerState` 首帧落在默认第 0 页，会短暂触发第 0 个 tab 的可见生命周期，再切回真实 tab，导致 WebView `pageShow(true)`、埋点、刷新等副作用串到错误 tab。
- 因此，任何有默认选中 tab、可恢复选中 tab 或外部状态驱动 tab 的分栏页面，都必须先同步计算 `currentIndex`，再作为 `rememberPagerState(initialPage = currentIndex)` 的初始值；`LaunchedEffect(currentIndex) { scrollToPage/animateScrollToPage(...) }` 只能作为后续切换同步，不得承担首帧纠偏职责。

### 3.2 页面骨架与标题栏
针对页面骨架，我需要确认：
1. 骨架模式：
    - 标准骨架（标题栏+中心区+底栏）
    - **沉浸式/自定义骨架 (Zero Scaffold)**：不使用 `BasicHazeScaffold`，手动适配状态栏。
2. 是否使用项目标准的 `BasicTitleBar`？（禁止手写标题栏）
3. 是否需要毛玻璃效果？（顶部/底部/都需要）
4. **(追加询问 - 若使用 `BasicHazeScaffold`)** surfaceModifier 配置方式：
    - **推荐：传 Color 方法**：使用 `topSurfaceColor` / `bottomSurfaceColor`，默认值以公共组件源码声明为准。
    - 原始方法：通过 `topSurfaceModifier` / `bottomSurfaceModifier` 自定义毛玻璃效果。
    - **低版本兼容**：通过 `legacyTopModifier` / `legacyBottomModifier` 提供不支持或禁止毛玻璃效果的兜底修饰器（`(Modifier) -> Modifier`）。
    - **启用规则**：通过 `hazeRule` 配置启用规则（ALL, NONE, ANDROID_ONLY, IOS_ONLY）。
5. **(追加询问 - 若使用传 Color 方法)** 使用默认配置还是自定义颜色？
    - 默认配置：使用公共组件当前源码中的 `topSurfaceColor` / `bottomSurfaceColor` 默认值
    - 自定义配置：明确 top/bottom 各自颜色
6. 状态栏文字颜色：深色还是浅色？
7. 是否有标题栏？若有，左侧是否需要返回键，右侧是否有操作项？
8. **(追加询问 - 若使用 Scaffold)** 是否有特殊的交互效果（如标题栏随滚动折叠、吸顶等）需要说明？
    - *架构提示*：简单的标题栏划出或局部吸顶应优先使用 `BasicHazeScaffold` 的 `minTopHeight` 配置，而非 `CoordinatorLayout`。

### 3.3 主交互组件
1. 是否需要接入 `BasicInteraction` 主交互组件（Loading/Empty/Error/Success）？（选项必须包含：不使用）

### 3.4 复杂滚动 (CoordinatorLayout)
1. 悬停方案：是 Header 固定高度悬停，还是内容吸顶悬停？（选项必须包含：不使用）
2. 头部最小高度 (minHeaderHeight) 是多少？
3. **(追加询问 - 若使用 Coordinator)** 是否有特殊的交互效果（如吸顶规则、联动偏移等）需要说明？
4. **(架构决策 - 若同时存在分页列表)** 请确认组件嵌套层级：是「协调者包裹分页列表（Header 随列表滚动）」还是「分页列表内部包含折叠头部」？

### 3.5 列表与分页
1. 是列表 (List) 还是网格 (Grid)？（选项必须包含：不使用）
2. **(强制)** 是否需要分页加载（必须使用 `PagingControl`）？
3. 是否需要下拉刷新？是否需要上拉分页？
4. 分页结束的判断逻辑（hasMore 字段 / 空数组）？

## 4. 故障处理指南

- **遇到爆红**：先查 `standards/资源与切图规范.md` 中的 Import 规则，再执行编译任务。
- **遇到组件冲突**：查 `knowledge/UI组件手册.md` 中的嵌套滑动方案或 `standards/UI开发规范.md`。
- **不确定选哪个组件**：**必须中断工作并询问开发者**，禁止凭感觉选择。
- **遇到需要降级**：凡是无法按原方案、原工具、原规范或原技术路径继续，必须按「降级决策确认协议」先询问开发者，禁止自行采用替代实现。
