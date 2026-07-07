package com.basic.base.downloader

import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DownloadManager {
    private const val maxTasks: Int = 4
    private val activeQueue = arrayListOf<DownloadTask>()
    private val client: HttpClient by lazy { HttpClient { expectSuccess = false } }
    private val waitingQueue = ArrayDeque<DownloadTask>()
    private val lock = Mutex()
    private val taskNotify = Channel<Unit>()

    init {
        applicationScope.launchScope(Dispatchers.IO) {
            while (true) {
                taskNotify.receive()
                lock.withLock {
                    if (waitingQueue.isNotEmpty() && activeQueue.size <= maxTasks){
                        val task = waitingQueue.removeLast()
                        if (task.state.value == DownloadState.Idle) {
                            activeQueue.add(task)
                            launch(Dispatchers.Default) {
                                task.start()
                                task.state.takeWhile {
                                    when (it) {
                                        is DownloadState.Failed, is DownloadState.Completed, is DownloadState.Cancel -> {
                                            lock.withLock {
                                                activeQueue.remove(task)
                                            }
                                            false
                                        }
                                        else -> true
                                    }
                                }.collect {}
                            }
                        }
                    }

                }
            }
        }
    }

    suspend fun downloadAndGet(url: String, dir: PlatformFile): DownloadTask {
        return lock.withLock {
            var task = waitingQueue.find { it.url == url && it.targetDir == dir }?.also {
                it.resume()
            }
            if (task == null) {
                task = activeQueue.find { it.url == url && it.targetDir == dir }
            }
            if (task == null){
                task = DownloadTask(url = url, targetDir = dir, client = client)
                waitingQueue.add(task)
                taskNotify.send(Unit)
            }
            task
        }
    }
}