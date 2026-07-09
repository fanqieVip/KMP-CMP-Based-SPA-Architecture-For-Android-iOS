package com.basic.app

import android.app.Activity
import android.content.res.Configuration
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.ApplicationProxyManager
import com.basic.base.utils.ActivityLifecycleCallbacksImpl
import com.basic.base.utils.ProcessUtils
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.onAdaptListener

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/1/10 23:56
 * @Version:
 */
class Application  : MultiDexApplication(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()
        if (ProcessUtils.isMainProcess(this)) {
            initAutoSize()
        }
        initKoin()
        registerAppLifecycle()
        registerActivityLifecycle()
    }

    private fun registerActivityLifecycle(){
        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImpl())
    }

    private fun registerAppLifecycle() {
        ApplicationProxyManager.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                ApplicationProxyManager.onForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                ApplicationProxyManager.onBackground()
            }
        })
    }
    private fun initAutoSize(){
        AutoSizeConfig.getInstance().apply {
            isCustomFragment = true
            onAdaptListener = object : onAdaptListener {
                override fun onAdaptBefore(target: Any?, activity: Activity?) {
                    activity?.let {
                        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            AutoSizeConfig.getInstance().setBaseOnWidth(false)
                            AutoSizeConfig.getInstance().setDesignHeightInDp(BuildConfig_com_basic_base.DESIGN_SIZE)
                        } else {
                            AutoSizeConfig.getInstance().setBaseOnWidth(true)
                            AutoSizeConfig.getInstance().setDesignWidthInDp(BuildConfig_com_basic_base.DESIGN_SIZE)
                        }
                    }
                }

                override fun onAdaptAfter(target: Any?, activity: Activity?) {
                }
            }
        }
    }
}