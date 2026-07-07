package com.basic.project.ui.screen.filesystem.filekit

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

/**
 * 字典映射选择演示
 */
@Router(RouterConstant.FILE_KIT_DIRECTORY_MAPPING)
class DictionaryMappingScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("原生目录映射")
            },
            center = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).background(Color.Black).padding(20.dp)) {
                    val text = """
                    支持 Android、iOS、macOS 和 JVM 目标平台
                    FileKit 提供对标准平台特定目录的访问：
                    val filesDir: PlatformFile = FileKit.filesDir
                    val cacheDir: PlatformFile = FileKit.cacheDir
                    val databasesDir: PlatformFile = FileKit.databasesDir
                    如果这些目录不存在，则会自动创建，并且在每个平台上都进行了适当的沙盒化处理。
                    平台特定目录行为
                    每个平台都根据其自身约定将这些标准目录映射到不同的位置：
                    安卓
                    filesDir：映射到context.filesDir应用程序的私有内部存储空间。
                    cacheDir：映射到context.cacheDir应用程序的私有缓存目录。
                    databasesDir：映射到databases应用程序内部存储中的一个子目录
                    iOS
                    filesDir：指向应用程序的“文档”目录，该目录已通过 iCloud 备份。
                    cacheDir：指向应用程序的缓存目录，该目录不会被备份，并且可能会被系统清除。
                    databasesDir：映射到databases应用程序“文档”目录中的一个子目录
                    macOS
                    filesDir：映射到~/Library/Application Support/<app-id>/，需要使用应用 ID 初始化 FileKit。
                    cacheDir：映射到~/Library/Caches/<app-id>/
                    databasesDir：映射到databases应用程序支持目录中的一个子目录
                    JVM（桌面）
                    filesDir：映射到特定于平台的应用程序数据位置：
                    Linux：~/.local/share/<app-id>/
                    macOS：~/Library/Application Support/<app-id>/
                    视窗：%APPDATA%/<app-id>/
                    cacheDir：映射到特定于平台的缓存位置：
                    Linux：~/.cache/<app-id>/
                    macOS：~/Library/Caches/<app-id>/
                    视窗：%LOCALAPPDATA%/<app-id>/Cache/
                    databasesDir：映射到databasesfilesDir 中的子目录
                """.trimIndent()
                    Text(text, fontSize = 15.sp, color = Color.White)
                }
            }
        )
    }
}