package com.basic.base.local

import android.app.Activity

actual typealias UIContainer = Activity

actual fun UIContainer.pop() {
    finish()
}