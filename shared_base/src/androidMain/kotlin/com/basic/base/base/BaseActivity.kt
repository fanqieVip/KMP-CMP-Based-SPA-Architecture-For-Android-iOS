package com.basic.base.base

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.basic.base.local.PermissionController
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.PermissionsControllerImpl
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

open class BaseActivity : FragmentActivity(), PermissionController {
    companion object {
        private const val APPLICATION_PROCESS_ID = "APPLICATION_PROCESS_ID"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!processIsRestart(savedInstanceState)) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            FileKit.init(this)
            permissionClient.bind(this)
        }
    }
    override val permissionClient: PermissionsController by lazy { PermissionsControllerImpl(applicationContext = applicationContext) }

    /**
     * 是否发生了进程重启
     * 如果页面重建或者进程重启则重定向到启动页
     * @return 是否发生了页面重建或者进程重启，如果发生了则不再进行后续操作
     */
    private fun processIsRestart(savedInstanceState: Bundle?): Boolean {
        val preProcessId = savedInstanceState?.getInt(APPLICATION_PROCESS_ID)
        val curProcessId = android.os.Process.myPid()
        if (preProcessId != null && preProcessId != android.os.Process.myPid()) {
            //跳转到启动页
            overridePendingTransition(0, 0)
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                // 下面这个Flag至关重要，会清空栈里所有的Activity
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(this)
            }
            android.os.Process.killProcess(curProcessId)
            System.exit(0)
            return true
        } else {
            savedInstanceState?.putInt(APPLICATION_PROCESS_ID, curProcessId)
            return false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //记录当前进程id
        outState.putInt(APPLICATION_PROCESS_ID, android.os.Process.myPid())
        super.onSaveInstanceState(outState)
    }
}