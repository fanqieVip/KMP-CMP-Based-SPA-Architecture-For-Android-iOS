package com.basic.base.ktx

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * 获取状态栏高度
 */
@Composable
fun statusBarHeight(): Dp {
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
}

/**
 * 获取导航栏高度
 */
@Composable
fun navigationBarHeight(): Dp {
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * 获取系统栏顶部总高度
 */
@Composable
fun systemBarsTopHeight(): Dp {
    return WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
}

/**
 * 获取系统栏底部总高度
 */
@Composable
fun systemBarsBottomHeight(): Dp {
    return WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
}
