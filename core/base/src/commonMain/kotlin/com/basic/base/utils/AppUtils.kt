package com.basic.base.utils

import com.basic.base.local.UIContainer
import io.github.vinceglb.filekit.PlatformFile

/**
 * @Description:    app工具类
 * @Author:         fanj
 * @CreateDate:     2026/7/15 11:39
 * @Version:
 */
expect object AppUtils {
    /**
     * 安装安卓apk
     *
     * @param path apk文件
     * @param uiContainer 宿主
     */
    fun installApk(uiContainer: UIContainer, path: PlatformFile)
}