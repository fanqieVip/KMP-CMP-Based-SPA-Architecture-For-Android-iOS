#### 模块说明 ####
app: 外壳
buildSrc: 构建参数配置
iosApp: ios原生项目
core/base: 架构核心
core/common: 组件化公共依赖
core/native: 安全加密模块（如安卓的ndk库实现）
project/main: 具体项目模块
libs：三方sdk统一存放目录

#### 依赖关系 ####
core/base -> core/common -> project/main -> app -> iosApp
