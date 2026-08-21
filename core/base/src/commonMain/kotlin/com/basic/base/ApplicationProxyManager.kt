package com.basic.base

import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.basic.base.utils.initializeMmkv
import com.basic.base.di.service.AndroidIntent
import com.basic.base.di.service.ApplicationService
import com.basic.base.di.service.IosNSUserActivity
import com.basic.base.di.service.IosUIOpenURLContext
import com.basic.base.local.appState
import com.basic.base.local.autoCheckNetworkPermission
import com.basic.base.spi.SPIRegisterCenter
import com.basic.base.utils.initNapier
import io.github.vinceglb.filekit.coil.addPlatformFileSupport

/**
 * 应用生命周期同步
 * @Author:         fanj
 * @CreateDate:     2026/1/12 12:17
 * @Version:
 */
object ApplicationProxyManager : ApplicationService {
    private val proxies by lazy { SPIRegisterCenter.all<ApplicationService>() }
    override fun onCreate() {
        initializeMmkv()
        initNapier()
        initCoil()
        appState.autoCheckNetworkPermission()
        proxies.forEach { proxy ->
            runCatching { proxy.onCreate() }
        }
    }

    override fun onBackground() {
        appState.updateAppIsForeground(false)
        proxies.forEach { proxy ->
            runCatching { proxy.onBackground() }
        }
    }

    override fun onForeground() {
        appState.updateAppIsForeground(true)
        proxies.forEach { proxy ->
            runCatching { proxy.onForeground() }
        }
    }

    override fun iosSceneContinueUserActivity(userActivity: IosNSUserActivity) {
        proxies.forEach { proxy ->
            runCatching { proxy.iosSceneContinueUserActivity(userActivity) }
        }
    }

    override fun iosSceneOpenURLContexts(urlContexts: Set<IosUIOpenURLContext>) {
        proxies.forEach { proxy ->
            runCatching { proxy.iosSceneOpenURLContexts(urlContexts) }
        }
    }

    override fun iosSceneWillConnectToOptions(
        userActivities: Set<IosNSUserActivity>,
        urlContexts: Set<IosUIOpenURLContext>
    ) {
        proxies.forEach { proxy ->
            runCatching { proxy.iosSceneWillConnectToOptions(userActivities, urlContexts) }
        }
    }

    override fun androidMainActivityOnCreate(intent: AndroidIntent?) {
        proxies.forEach { proxy ->
            runCatching { proxy.androidMainActivityOnCreate(intent) }
        }
    }

    override fun androidMainActivityOnNewIntent(intent: AndroidIntent?) {
        proxies.forEach { proxy ->
            runCatching { proxy.androidMainActivityOnNewIntent(intent) }
        }
    }

    private fun initCoil() {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    addPlatformFileSupport()
                }
                .build()
        }
    }
}
