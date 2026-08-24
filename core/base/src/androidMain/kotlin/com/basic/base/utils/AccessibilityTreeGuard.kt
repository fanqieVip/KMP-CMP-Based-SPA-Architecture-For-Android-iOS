package com.basic.base.utils

import android.view.View
import android.view.Window
import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.constant.VersionStatus

/**
 * 当前构建是否需要执行无障碍服务屏蔽。
 *
 * 配置开关开启且运行包为生产环境时才返回 true。
 */
internal val isAccessibilityServiceBlockingEnabled: Boolean
    get() = BuildConfig_com_basic_base.DISABLE_ACCESSIBILITY_SERVICE &&
        BuildConfig_com_basic_base.VERSION_TYPE == VersionStatus.RELEASE

/**
 * 向无障碍框架隐藏当前窗口及其全部后续子节点。
 *
 * 该标记会由 View 树继承，因此在 ComposeView 创建之前调用也能覆盖其虚拟语义节点。
 *
 * @Description: 统一隐藏 Android 窗口的无障碍节点树，避免业务页面逐个配置 Compose Semantics。
 * @Author:         范俊
 * @CreateDate:     2026/08/24 17:07
 */
internal fun Window.suppressAccessibilityTree() {
    if (isAccessibilityServiceBlockingEnabled) {
        decorView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
}
