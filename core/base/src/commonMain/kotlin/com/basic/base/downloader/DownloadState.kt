package com.basic.base.downloader

import io.github.vinceglb.filekit.PlatformFile

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Cancel : DownloadState()
    data class Completed(val filePath: PlatformFile) : DownloadState()
    data class Failed(val e: Throwable) : DownloadState()
}


data class DownloadProgressState(val downloadedBytes: Long, val totalBytes: Long) {
    val percent: Float
        get() = if (totalBytes == 0L) 0f
        else downloadedBytes * 100f / totalBytes
}