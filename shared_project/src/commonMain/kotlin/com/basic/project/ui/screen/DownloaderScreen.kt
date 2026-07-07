package com.basic.project.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.downloader.DownloadManager
import com.basic.base.downloader.DownloadProgressState
import com.basic.base.downloader.DownloadState
import com.basic.base.downloader.DownloadTask
import com.basic.base.ktx.launchScope
import com.basic.base.local.ScreenContext
import com.basic.base.router.Router
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant
import io.github.hristogochev.vortex.model.screenModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Description:    下载器
 * @Author:         fanj
 * @CreateDate:     2026/3/6 10:03
 * @Version:
 */
@Router(RouterConstant.DOWNLOADER)
class DownloaderScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("下载器")
            },
            center = {
                val model = rememberMainScreenModel { DownloaderScreenModel() }
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("下载状态：${model.state}")
                    Text("下载进度：${model.progress}")
                    Button(onClick = {
                        model.start()
                    }) {
                        Text("开始下载", fontSize = 12.sp, color = Color.Black)
                    }
                    Button(onClick = {
                        model.cancel()
                    }) {
                        Text("取消下载", fontSize = 12.sp, color = Color.Black)
                    }
                }
            }
        )
    }
}

/**
 * 下载器状态模型
 */
class DownloaderScreenModel : BasicScreenModel() {
    var state by mutableStateOf<DownloadState?>(null)
        private set
    var progress by mutableStateOf<DownloadProgressState?>(null)
        private set
    private var task: DownloadTask? = null
    override fun onInit(context: ScreenContext) {

    }

    /**
     * 开始下载
     */
    fun start() {
        screenModelScope.launchScope {
            task = DownloadManager.downloadAndGet(
                url = "https://edgedl.me.gvt1.com/android/studio/install/2026.1.1.10/android-studio-quail1-patch2-mac_arm.dmg",
                dir = FileKit.filesDir / "download"
            ).also { downloadTask ->
                launch {
                    downloadTask.state.collectLatest {
                        withContext(Dispatchers.Main) {
                            state = it
                        }
                    }
                }
                launch {
                    downloadTask.progress.collectLatest {
                        withContext(Dispatchers.Main) {
                            progress = it
                        }
                    }
                }
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancel() {
        screenModelScope.launchScope {
            task?.cancel()
        }
    }

    override fun onLoad(context: ScreenContext) {
    }
}