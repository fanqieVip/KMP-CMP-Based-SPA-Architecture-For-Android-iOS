package com.basic.base
import androidx.compose.runtime.Composable

/**
 * 自适应屏幕ppi
 */
@Composable
expect fun AutoSize(
    designWidth: Float = 375f,
    content: @Composable () -> Unit
)