package com.basic.base.datastore

import com.blankj.utilcode.util.Utils
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val settings: Settings by lazy { SharedPreferencesSettings.Factory(Utils.getApp()).create(settingsFileName) }