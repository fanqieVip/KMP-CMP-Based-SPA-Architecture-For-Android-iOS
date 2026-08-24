package com.basic.base.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.basic.base.StatusBar
import com.basic.base.ktx.applicationScope
import com.basic.base.local.WindowOrientation
import com.basic.base.local.appState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity生命周期监听
 *
 * @author fanj
 * @since 4/20/21 9:10 AM
 */
@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ActivityLifecycleCallbacksImpl : Application.ActivityLifecycleCallbacks, ComponentCallbacks {

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        ActivityStackManager.addActivityToStack(activity)
        appState.updateWindowOrientation(
            when (activity.resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> WindowOrientation.PORTRAIT
                Configuration.ORIENTATION_LANDSCAPE -> WindowOrientation.LANDSCAPE
                else -> WindowOrientation.UNKNOWN
            }
        )
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {
        if (activity.isFinishing) {
            ActivityStackManager.popActivityToStack(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        ActivityStackManager.popActivityToStack(activity)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        applicationScope.launch {
            delay(10)
            appState.updateWindowOrientation(
                when (newConfig.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> WindowOrientation.PORTRAIT
                    Configuration.ORIENTATION_LANDSCAPE -> WindowOrientation.LANDSCAPE
                    else -> WindowOrientation.UNKNOWN
                }
            )
            StatusBar.setStatusBarTextDark(appState.statusBarTextIsDark.value)
        }
    }

    override fun onLowMemory() = Unit
}
