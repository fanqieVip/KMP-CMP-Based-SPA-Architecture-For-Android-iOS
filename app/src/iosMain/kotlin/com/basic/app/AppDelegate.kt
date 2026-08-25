package com.basic.app

import com.basic.base.ApplicationProxyManager
import com.basic.base.di.service.IosNSUserActivity
import com.basic.base.di.service.IosUIOpenURLContext
import platform.UIKit.UIViewController

object AppDelegate {
    fun onAppCreate() {
        initKoin()
        ApplicationProxyManager.onCreate(isMainProcess = true)
    }

    fun onAppBackground() {
        ApplicationProxyManager.onBackground()
    }

    fun onAppForeground() {
        ApplicationProxyManager.onForeground()
    }

    fun onUICreate(viewController: UIViewController) {
    }

    fun onUIShow(viewController: UIViewController) {
    }

    fun onUIHidden(viewController: UIViewController) {
    }

    fun onUIMove(viewController: UIViewController) {
    }

    fun sceneContinueUserActivity(userActivity: IosNSUserActivity) {
        ApplicationProxyManager.iosSceneContinueUserActivity(userActivity)
    }

    fun sceneOpenURLContexts(urlContexts: Set<IosUIOpenURLContext>) {
        ApplicationProxyManager.iosSceneOpenURLContexts(urlContexts)
    }

    fun sceneWillConnectToOptions(
        userActivities: Set<IosNSUserActivity>,
        urlContexts: Set<IosUIOpenURLContext>
    ) {
        ApplicationProxyManager.iosSceneWillConnectToOptions(userActivities, urlContexts)
    }
}
