package com.basic.base.datastore

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings

actual val settings: Settings by lazy { NSUserDefaultsSettings.Factory().create(settingsFileName) }