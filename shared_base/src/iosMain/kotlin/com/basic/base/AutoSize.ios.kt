package com.basic.base

import androidx.compose.runtime.Composable

@Composable
actual fun AutoSize(designWidth: Float, content: @Composable (() -> Unit)) {
    content()
}