package com.basic.base.di.service

expect class IosNSUserActivity
expect class IosUIOpenURLContext
expect class AndroidIntent
interface ApplicationService {
    fun onCreate()
    fun onBackground()
    fun onForeground()
    fun iosSceneContinueUserActivity(userActivity: IosNSUserActivity){}
    fun iosSceneOpenURLContexts(urlContexts: Set<IosUIOpenURLContext>){}
    fun iosSceneWillConnectToOptions(userActivities: Set<IosNSUserActivity>, urlContexts: Set<IosUIOpenURLContext>){}
    fun androidMainActivityOnCreate(intent: AndroidIntent?){}
    fun androidMainActivityOnNewIntent(intent: AndroidIntent?){}
}