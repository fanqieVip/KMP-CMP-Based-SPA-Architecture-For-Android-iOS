package com.basic.base.di.service

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * ui最底层配置
 * 处理toast基础样式或者全局注入参数等
 */
interface UIConfigService {
    /**
     * 全局吐司样式
     */
    fun toastUi(isVisible: Boolean, text: String): @Composable BoxScope.() -> Unit

    /**
     * 最低层配置整体风格或者注入参数
     */
    @Composable
    fun RootUiConfig(content: @Composable () -> Unit) = content()

    /**
     * 全屏弹窗样式
     */
    @Composable
    fun PopLoadingUi(text: String?)
}