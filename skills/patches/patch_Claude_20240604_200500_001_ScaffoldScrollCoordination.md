# Patch: Scaffold 滚动联动能力增强与架构决策校准

## 触发背景
AI 在处理“标题栏划出、局部吸顶”需求时，错误地将其归类为必须使用 `CoordinatorLayout` 的场景，忽视了 `BasicHazeScaffold` 原生具备的嵌套滑动处理能力。这导致了架构选择的偏差。

## 修正后的标准模式

### 1. BasicHazeScaffold 联动能力认知
明确 `BasicHazeScaffold` 通过 `NestedScrollConnection` 实现了顶部区域的压缩与重叠控制：
- **`minTopHeight`**：定义折叠后的最小保留高度（适用于搜索栏吸顶等场景）。
- **`maxTopOverlap`**：定义允许的最大重叠高度。
- **滑动消费逻辑**：向上滑动时外层（Scaffold）优先消费直至达到 `minTopHeight`，向下滑动时内部优先消费直至内部不消费后 Scaffold 恢复高度。

### 2. 架构决策优先级更新
针对“标题栏折叠/划出”场景：
- **优先方案**：使用 `BasicHazeScaffold` 并配置 `minTopHeight`。
- **降级方案**：仅在涉及更复杂的“随滚动改变 Alpha”、“多级联动”或“特定组件间距变化”时才考虑 `CoordinatorLayout`。

### 3. 文档扩充要求
在 `UI组件使用指南及规范.md` 中新增「5.3 Scaffold 滚动联动实战」章节，并更新 `UI开发工作流规范.md` 中的提问逻辑，确保在脚手架确认阶段识别此类联动需求。

## 受影响的文档
- `UI组件使用指南及规范.md` (新增实战章节)
- `UI开发工作流规范.md` (3.2 章节描述)
