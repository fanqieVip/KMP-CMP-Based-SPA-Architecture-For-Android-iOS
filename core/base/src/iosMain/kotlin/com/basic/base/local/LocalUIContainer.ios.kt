package com.basic.base.local

import platform.UIKit.UIViewController

actual typealias UIContainer = UIViewController

actual fun UIContainer.pop() {
    dismissViewControllerAnimated(false, null)
}