//
//  UIViewController+LifecycleMonitor.swift
//  iosApp
//
//  Created by mac on 2026/3/31.
//
import UIKit
import ObjectiveC
import ComposeApp

extension UIViewController {
    @objc func swizzled_supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
        let defaultMask = self.swizzled_supportedInterfaceOrientations()
        let resolvedMask = OrientationState.shared.supportedOrientationMask(defaultMask: Int64(defaultMask.rawValue))
        return UIInterfaceOrientationMask(rawValue: UInt(resolvedMask))
    }

    @objc func swizzled_preferredInterfaceOrientationForPresentation() -> UIInterfaceOrientation {
        let defaultOrientation = self.swizzled_preferredInterfaceOrientationForPresentation()
        let resolvedOrientation = OrientationState.shared.preferredInterfaceOrientation(defaultOrientation: Int64(defaultOrientation.rawValue))
        return UIInterfaceOrientation(rawValue: Int(resolvedOrientation)) ?? defaultOrientation
    }

    @objc func swizzled_viewDidLoad() {
        self.swizzled_viewDidLoad()
        AppDelegate.shared.onUICreate(viewController: self)
    }

    @objc func swizzled_viewDidAppear(_ animated: Bool) {
        self.swizzled_viewDidAppear(animated)
        AppDelegate.shared.onUIShow(viewController: self)
    }

    @objc func swizzled_viewDidDisappear(_ animated: Bool) {
        self.swizzled_viewDidDisappear(animated)
        AppDelegate.shared.onUIHidden(viewController: self)
    }
    @objc func swizzled_didMove(_ parent: UIViewController?) {
        self.swizzled_didMove(parent)
        if(parent == nil){
            AppDelegate.shared.onUIMove(viewController: self)
        }
    }

}

extension UIViewController {
    static func swizzleAllLifecycle() {
        // 保证线程安全且只执行一次
        struct Static {
            static let onceToken: Int = 0
        }
        
        // --- 交换 viewDidLoad ---
        if let original = class_getInstanceMethod(self, #selector(viewDidLoad)),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_viewDidLoad)) {
            method_exchangeImplementations(original, swizzled)
        }
        
        // --- 交换 viewDidAppear ---
        if let original = class_getInstanceMethod(self, #selector(viewDidAppear(_:))),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_viewDidAppear(_:))) {
            method_exchangeImplementations(original, swizzled)
        }
        
        // --- 交换 viewDidDisappear ---
        if let original = class_getInstanceMethod(self, #selector(viewDidDisappear(_:))),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_viewDidDisappear(_:))) {
            method_exchangeImplementations(original, swizzled)
        }
        // --- 交换 viewDidDisappear ---
        if let original = class_getInstanceMethod(self, #selector(didMove(toParent: ))),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_didMove(_:))) {
            method_exchangeImplementations(original, swizzled)
        }

        // --- 交换 supportedInterfaceOrientations ---
        if let original = class_getInstanceMethod(self, #selector(getter: supportedInterfaceOrientations)),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_supportedInterfaceOrientations)) {
            method_exchangeImplementations(original, swizzled)
        }

        // --- 交换 preferredInterfaceOrientationForPresentation ---
        if let original = class_getInstanceMethod(self, #selector(getter: preferredInterfaceOrientationForPresentation)),
           let swizzled = class_getInstanceMethod(self, #selector(swizzled_preferredInterfaceOrientationForPresentation)) {
            method_exchangeImplementations(original, swizzled)
        }
    }
}
