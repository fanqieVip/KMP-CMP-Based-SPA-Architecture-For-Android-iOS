#### 模块说明 ####
app: 外壳
buildSrc: 构建参数配置
iosApp: ios原生项目
shared_base: 架构核心
shared_common: 组件化公共依赖
shared_native: 安全加密模块（如安卓的ndk库实现）
shared_project: 具体项目模块

#### 依赖关系 ####
shared_base -> shared_common -> shared_project -> app -> iosApp
