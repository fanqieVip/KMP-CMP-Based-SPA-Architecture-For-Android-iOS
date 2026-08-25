package com.basic.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.basic.base.ApplicationProxyManager
import com.basic.base.utils.ActivityLifecycleCallbacksImpl
import com.basic.base.utils.ProcessUtils

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/1/10 23:56
 * @Version:
 */
class Application  : MultiDexApplication(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()
        initKoin()
        registerAppLifecycle()
        registerActivityLifecycle()
    }

    private fun registerActivityLifecycle(){
        ActivityLifecycleCallbacksImpl().also {
            registerActivityLifecycleCallbacks(it)
            registerComponentCallbacks(it)
        }
    }

    private fun registerAppLifecycle() {
        ApplicationProxyManager.onCreate(ProcessUtils.isMainProcess(this))
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
}
