package com.basic.base.base

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityManager
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
            (hasGestureCapableAccessibilityService() || isSuspectedAutomatedTouch(event))
        ) {
            onSuspectedAutomatedTouch(event)
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * 检查当前是否启用了能够向屏幕注入手势的无障碍服务。
     *
     * @return 存在声明了手势执行能力的已启用服务时返回 true。
     */
    private fun hasGestureCapableAccessibilityService(): Boolean {
        val accessibilityManager = getSystemService(AccessibilityManager::class.java)
        return accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { serviceInfo ->
                serviceInfo.capabilities and
                    AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES != 0
            }
    }

    /**
     * 判断触摸事件是否具有脚本注入常见特征。
     *
     * @param event 待检查的按下事件。
     * @return 同时命中压力与接触面积异常，或输入源不是触摸屏时返回 true。
     */
    private fun isSuspectedAutomatedTouch(event: MotionEvent): Boolean {
        val hasSyntheticPressure = event.pressure == 0f || event.pressure == 1f
        val hasNoContactArea = event.size == 0f
        val isNotTouchscreen = event.source and InputDevice.SOURCE_TOUCHSCREEN == 0
        return (hasSyntheticPressure && hasNoContactArea) || isNotTouchscreen
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
                "pressure=${event.pressure}, size=${event.size}, source=${event.source}"
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
