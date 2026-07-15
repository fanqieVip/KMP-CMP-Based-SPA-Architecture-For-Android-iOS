/**
 * @Description: Android WebView 键盘兼容性处理工具类，解决键盘弹出导致布局异常的问题
 * @Author:         范俊
 * @CreateDate:     2026/07/15 14:00
 */
package com.basic.base.utils

import android.R
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.yzq.immersion.navigationBarHeight

/**
 * 键盘兼容性处理
 * @param activity 当前 Activity
 */
class KeyboardFix private constructor(private val activity: Activity) {

    companion object {
        /**
         * 开启 Activity 的键盘兼容处理
         * @param activity 目标 Activity
         */
        fun fix(activity: Activity) {
            runCatching { KeyboardFix(activity) }
        }
    }

    private val mChildOfContent: View
    private var usableHeightPrevious = 0
    private val frameLayoutParams: ViewGroup.LayoutParams

    init {
        val content = activity.findViewById<ViewGroup>(R.id.content)
        // 获取 Activity 的根布局（通常是我们在 XML 里写的根节点）
        mChildOfContent = content.getChildAt(0)

        // 监听全局布局树变化
        mChildOfContent.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 如果 Activity 已经被销毁，及时移除监听防止内存泄漏
                if (activity.isFinishing || activity.isDestroyed) {
                    mChildOfContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    return
                }
                possiblyResizeChildOfContent()
            }
        })
        frameLayoutParams = mChildOfContent.layoutParams
    }

    /**
     * 根据高度变化调整布局大小
     */
    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow != usableHeightPrevious) {
            // 获取屏幕区域的绝对总高度（包含状态栏，但不含导航栏）
            val r = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(r)

            // 注意：这里直接取 root 视图的绝对高度，不使用受 density 缩放影响的 View.height
            val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.windowManager.currentWindowMetrics.bounds.height()
            } else {
                val displayMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                displayMetrics.heightPixels
            }

            // 计算键盘占用的绝对物理像素高度
            val heightDifference = screenHeight - usableHeightNow

            // 设定一个阈值（比如全屏的 1/5 ），大于它则代表键盘弹出
            if (heightDifference > screenHeight / 5) {
                // 键盘弹出了：将根布局高度限制在 [总可用高度 - 键盘高度]
                // 此时 WebView 会触发底层的 adjustResize 逻辑，且由于高度变矮，会自动滚动聚焦的输入框
                frameLayoutParams.height = screenHeight - heightDifference + activity.navigationBarHeight
            } else {
                // 键盘收起了：恢复满屏布局高度
                frameLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            mChildOfContent.requestLayout()
            usableHeightPrevious = usableHeightNow
        }
    }

    /**
     * 计算可用高度
     * @return 可用像素高度
     */
    private fun computeUsableHeight(): Int {
        val r = Rect()
        // 使用绝对的可见屏幕坐标框架，规避自定义 PPI 缩放对局部 View 高度测量的干扰
        mChildOfContent.getWindowVisibleDisplayFrame(r)

        // 如果项目开启了沉浸式状态栏/全屏模式，r.top 会变成 0
        // 为了防止导航栏或者状态栏高度的增减导致计算错位，直接返回绝对底部坐标 r.bottom 最为稳定
        return r.bottom
    }
}