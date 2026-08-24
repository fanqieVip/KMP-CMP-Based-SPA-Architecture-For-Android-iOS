package com.basic.base.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.local.LocalAppState

/**
 * @Description: 隔离 Compose 设计密度与平台原生控件密度
 * @Author:         范俊
 * @CreateDate:     2026/08/20 16:50
 */

// 未识别设备 PPI 时，用平台原生 Density 估算物理 PPI。
private const val STANDARD_DENSITY_DPI = 160f

// 保存进入设计密度作用域前的平台原生 Density，嵌套 Provider 时不会重复缩放。
private val LocalNativeDensity = staticCompositionLocalOf<Density?> { null }

/**
 * 为 Compose 内容提供按设计参数校准的 Density，并应用 App 全局字体缩放倍率。
 *
 * @param content 使用设计 Density 的 Compose 内容。
 */
@Composable
internal fun DesignDensityProvider(content: @Composable () -> Unit) {
    val nativeDensity = LocalNativeDensity.current ?: LocalDensity.current
    val fontScale = LocalAppState.current.fontScale.value
    val devicePpi = remember { getDevicePhysicalPpi() }
        ?: nativeDensity.density * STANDARD_DENSITY_DPI
    val designDensity = remember(nativeDensity.density, devicePpi, fontScale) {
        Density(
            density = calculateDesignDensity(devicePpi),
            fontScale = fontScale
        )
    }
    CompositionLocalProvider(
        LocalNativeDensity provides nativeDensity,
        LocalDensity provides designDensity,
        content = content
    )
}

/**
 * 临时恢复平台原生尺寸 Density，并应用 App 全局字体缩放倍率。
 * 供 AndroidView、UIKitView 等原生互操作组件使用。
 *
 * @param content 使用平台原生 Density 的 Compose 内容。
 */
@Composable
fun NativeDensityProvider(content: @Composable () -> Unit) {
    val nativeDensity = LocalNativeDensity.current ?: LocalDensity.current
    val fontScale = LocalAppState.current.fontScale.value
    val densityWithoutSystemFontScale = remember(nativeDensity.density, fontScale) {
        Density(
            density = nativeDensity.density,
            fontScale = fontScale
        )
    }
    CompositionLocalProvider(
        LocalDensity provides densityWithoutSystemFontScale,
        content = content
    )
}

/**
 * 根据当前设备 PPI、设计设备物理宽度与标注基准计算 Compose Density。
 *
 * @param devicePpi 当前设备每英寸像素数。
 * @return 当前设备每个设计逻辑单位对应的物理像素数。
 */
private fun calculateDesignDensity(devicePpi: Float): Float {
    val baselinePixel = BuildConfig_com_basic_base.DESIGN_BASELINE_PIXEL.toFloat()
    val baselinePpi = BuildConfig_com_basic_base.DESIGN_BASELINE_PPI.toFloat()
    val measurementPpi = BuildConfig_com_basic_base.DESIGN_MEASUREMENT_PPI.toFloat()
    require(devicePpi > 0f && baselinePixel > 0f && baselinePpi > 0f && measurementPpi > 0f) {
        "Design density parameters must be greater than zero."
    }
    return devicePpi * baselinePixel / baselinePpi / measurementPpi
}

/**
 * 获取当前设备的物理屏幕 PPI。
 *
 * @return 可确认的设备 PPI；无法确认时返回 null。
 */
internal expect fun getDevicePhysicalPpi(): Float?
