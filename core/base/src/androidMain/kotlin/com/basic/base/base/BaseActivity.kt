package com.basic.base.base

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.basic.base.local.PermissionController
import com.basic.base.utils.isAccessibilityServiceBlockingEnabled
import com.basic.base.utils.suppressAccessibilityTree
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.PermissionsControllerImpl
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

open class BaseActivity : FragmentActivity(), PermissionController {
    companion object {
        private const val APPLICATION_PROCESS_ID = "APPLICATION_PROCESS_ID"
        private const val ANTI_AUTOMATION_LOG_TAG = "AntiAutomation"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!processIsRestart(savedInstanceState)) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            window.suppressAccessibilityTree()
            FileKit.init(this)
            permissionClient.bind(this)
        }
    }
    override val permissionClient: PermissionsController by lazy { PermissionsControllerImpl(applicationContext = applicationContext) }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (
            isAccessibilityServiceBlockingEnabled &&
            event?.actionMasked == MotionEvent.ACTION_DOWN &&
            isSuspectedAutomatedTouch(event)
        ) {
            onSuspectedAutomatedTouch(event)
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * 判断触摸事件是否符合 Android 无障碍 dispatchGesture 的标准注入特征。
     *
     * @param event 待检查的按下事件。
     * @return 同时命中虚拟设备、未知工具类型和固定触摸参数时返回 true。
     */
    private fun isSuspectedAutomatedTouch(event: MotionEvent): Boolean {
        val isVirtualDevice = event.deviceId == 0 ||
            event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD ||
            event.device?.isVirtual == true
        val hasSyntheticPointer = event.getToolType(event.actionIndex) == MotionEvent.TOOL_TYPE_UNKNOWN &&
            event.pressure == 1f &&
            event.size == 1f
        return event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN) &&
            isVirtualDevice &&
            hasSyntheticPointer
    }

    /**
     * 收到疑似脚本注入触摸时执行风控记录。
     *
     * 子类可重写此方法接入上报、验证码或其他业务风控动作；当前事件会被统一吞掉。
     *
     * @param event 被拦截的触摸事件。
     */
    protected open fun onSuspectedAutomatedTouch(event: MotionEvent) {
        Log.w(
            ANTI_AUTOMATION_LOG_TAG,
            "Blocked suspected automated touch: x=${event.x}, y=${event.y}, " +
                "pressure=${event.pressure}, size=${event.size}, source=${event.source}, " +
                "deviceId=${event.deviceId}, toolType=${event.getToolType(event.actionIndex)}"
        )
    }

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge()
    }
}
