package com.basic.base

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
actual fun AutoSize(designWidth: Float, content: @Composable (() -> Unit)) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        configuration.screenWidthDp.toFloat()
    } else {
        configuration.screenHeightDp.toFloat()
    }
    val originalDensity = LocalDensity.current

    // 计算缩放比例
    val scale = screenWidthDp / designWidth
    val scaledDensity = originalDensity.density * scale

    // 替换 LocalDensity
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = scaledDensity,
            fontScale = originalDensity.fontScale
        )
    ) {
        content()
    }
}