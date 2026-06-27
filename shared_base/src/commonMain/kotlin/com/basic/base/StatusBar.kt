package com.basic.base

expect object StatusBar {
    /**
     * 设置状态栏的图标颜色
     * @param isDark  true=深色文字（适合浅色背景），false=浅色文字（适合深色背景）
     */
    internal fun setStatusBarTextDark(isDark: Boolean)
}