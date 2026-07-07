package com.basic.base.downloader

import com.basic.base.ktx.ApiException
import com.basic.base.ktx.CoroutineJob
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.utils.Md5Utils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.io.buffered
import kotlin.time.Clock

class DownloadTask(
    val url: String,
    val targetDir: PlatformFile,
    val client: HttpClient
) {
    // 内部状态
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()
    private val _progress = MutableStateFlow<DownloadProgressState>(DownloadProgressState(0L, 0L))
    val progress: StateFlow<DownloadProgressState> = _progress.asStateFlow()
    private var downloadJob: CoroutineJob? = null

    // 下载完毕的文件
    private val filePath by lazy {
        targetDir / generateFileName(url)
    }

    // 临时文件
    private val tempFilePath by lazy {
        targetDir / generateTempFileName(url)
    }


    /**
     * 取消下载
     */
    suspend fun cancel() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel()
        }
        _state.emit(DownloadState.Cancel)
    }

    /**
     * 恢复到下载队列可用
     */
    internal suspend fun resume() {
        _state.emit(DownloadState.Idle)
    }

    /**
     * 开始下载
     */
    internal fun start() {
        if (downloadJob?.isActive == true) return
        downloadJob = applicationScope.launchScope(Dispatchers.IO) {
            //开始下载
            updateDownloadState(DownloadState.Downloading)
            //检测文件夹是否存在
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                targetDir.createDirectories()
            }
            //检测文件是否已经下载完成了，就直接返回
            if (filePath.exists() && !filePath.isDirectory()) {
                updateProgressState(filePath.size(), filePath.size(), forceUpdate = true)
                updateDownloadState(DownloadState.Completed(filePath = filePath))
                return@launchScope
            }
            //已下载的大小
            val downloadedSize = if (tempFilePath.exists() && !tempFilePath.isDirectory()) {
                tempFilePath.size()
            } else {
                0L
            }
            client.prepareGet(url) {
                header(HttpHeaders.Range, "bytes=${downloadedSize}-")
                header(HttpHeaders.AcceptCharset, "utf-8")
                header(HttpHeaders.AcceptEncoding, "identity")
                header(HttpHeaders.UserAgent, "Ktor Client")
                timeout {
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 10000
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw Exception("HTTP ${response.status}")
                }
                // 获取实际内容长度
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0
                when (response.status) {
                    HttpStatusCode.OK, HttpStatusCode.PartialContent -> {
                        if (contentLength >= 0) {//支持断点下载
                            if (!(response.status == HttpStatusCode.OK && downloadedSize == contentLength && downloadedSize != 0L)) {
                                val serverSize = contentLength + downloadedSize
                                response.bodyAsChannel().let { channel ->
                                    tempFilePath.sink(true).buffered().use {
                                        var downloaded = downloadedSize
                                        val buffer = ByteArray(4096)
                                        while (true) {
                                            if (!this.isActive) {
                                                break
                                            }
                                            val bytesRead = channel.readAvailable(buffer)
                                            if (bytesRead <= 0) break
                                            it.write(buffer, 0, bytesRead)
                                            downloaded += bytesRead
                                            updateProgressState(downloaded, serverSize)
                                        }
                                    }
                                }
                            }
                        } else {//不支持断点下载
                            if (tempFilePath.exists() && !tempFilePath.isDirectory()) {
                                tempFilePath.delete()
                            }
                            response.bodyAsChannel().let { channel ->
                                tempFilePath.sink().buffered().use {
                                    var downloaded = 0L
                                    val buffer = ByteArray(4096)
                                    while (true) {
                                        if (!this.isActive) {
                                            break
                                        }
                                        val bytesRead = channel.readAvailable(buffer)
                                        if (bytesRead <= 0) break
                                        it.write(buffer, 0, bytesRead)
                                        downloaded += bytesRead
                                        updateProgressState(downloaded, 0)
                                    }
                                }
                            }
                        }
                    }

                    HttpStatusCode.RequestedRangeNotSatisfiable -> {}//已下载完
                    else -> {
                        throw ApiException(response.status.value, response.status.description)
                    }
                }
                if (this.isActive) {
                    if (filePath.exists() && !filePath.isDirectory()) {
                        filePath.delete()
                    }
                    tempFilePath.atomicMove(filePath)
                    updateProgressState(filePath.size(), filePath.size(), forceUpdate = true)
                    updateDownloadState(DownloadState.Completed(filePath))
                } else {
                    updateDownloadState(DownloadState.Cancel)
                }
            }
        }.catch { _, _, e ->
            updateDownloadState(DownloadState.Failed(e))
        }
    }

    private suspend fun updateDownloadState(state: DownloadState) {
        _state.emit(state)
    }

    private var preUpdateTime = 0L
    private suspend fun updateProgressState(downloadedBytes: Long, totalBytes: Long, forceUpdate: Boolean = false) {
        val nowTime = Clock.System.now().toEpochMilliseconds()
        if (nowTime - preUpdateTime < 500 && !forceUpdate) {
            return
        } else {
            preUpdateTime = nowTime
        }
        _progress.emit(DownloadProgressState(downloadedBytes = downloadedBytes, totalBytes = totalBytes))
    }

    /**
     * 解析正式文件名
     */
    private fun generateFileName(url: String): String {
        val fileId = Md5Utils.encode(url)
        val extension = extractFileExtension(url)
        return "$fileId${if (extension.isNotEmpty()) ".$extension" else ""}"
    }

    /**
     * 解析临时文件吗
     */
    private fun generateTempFileName(url: String): String {
        val fileName = generateFileName(url)
        return "$fileName.downloading"
    }

    /**
     * 根据url地址文件解析扩展名
     */
    private fun extractFileExtension(url: String): String {
        val fileName = url.substringAfterLast("/").substringBefore("?")
        val dotIndex = fileName.lastIndexOf(".")

        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            val ext = fileName.substring(dotIndex + 1)
            // 限制扩展名长度和字符
            if (ext.length <= 10 && ext.all { it.isLetterOrDigit() }) {
                ext.lowercase()
            } else {
                ""
            }
        } else {
            ""
        }
    }
}