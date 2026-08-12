package com.basic.base.utils

import com.basic.base.local.UIContainer
import io.github.vinceglb.filekit.PlatformFile

/**
 * @Description:    App 系统工具能力集合。
 * @Author:         范俊
 * @CreateDate:     2026/08/12 15:00
 */
expect object AppUtils {
    /**
     * 查询系统定位服务是否已开启，不检查应用定位权限。
     *
     * @return 系统定位服务开关已开启时返回 true，否则返回 false。
     */
    fun isLocationServiceEnabled(): Boolean

    /**
     * 安装 Android APK 文件。
     *
     * @param uiContainer 页面宿主。
     * @param path APK 文件。
     */
    fun installApk(uiContainer: UIContainer, path: PlatformFile)
}
