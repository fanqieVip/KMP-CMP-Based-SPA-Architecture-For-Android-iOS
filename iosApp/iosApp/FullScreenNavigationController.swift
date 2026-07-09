//
//  FullScreenNavigationController.swift
//  iosApp
//
//  Created by 范俊 on 2026/07/09.
//

import UIKit

/**
 * 全屏导航控制器。
 *
 * 用于承载 Compose 页面栈：隐藏系统导航栏，避免顶部导航栏占位，同时保留系统边缘滑动返回能力。
 */
final class FullScreenNavigationController: UINavigationController, UIGestureRecognizerDelegate {
    override func viewDidLoad() {
        super.viewDidLoad()
        configureNavigation()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        configureNavigation()
    }

    override func pushViewController(_ viewController: UIViewController, animated: Bool) {
        super.pushViewController(viewController, animated: animated)
        configureNavigation()
    }

    /**
     控制系统边缘返回手势是否可以开始。

     - Parameter gestureRecognizer: 当前触发的手势识别器。
     - Returns: 导航栈内存在可返回页面时返回 true，否则返回 false。
     */
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }

    /**
     允许系统边缘返回手势与 Compose 内部手势共同识别。

     - Parameters:
       - gestureRecognizer: 当前导航控制器的边缘返回手势。
       - otherGestureRecognizer: 页面内其他手势。
     - Returns: 始终返回 true，降低 Compose 根视图抢占边缘手势的概率。
     */
    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    /**
     统一配置导航栏可见性与系统返回手势状态。
     */
    private func configureNavigation() {
        setNavigationBarHidden(true, animated: false)
        interactivePopGestureRecognizer?.isEnabled = true
        interactivePopGestureRecognizer?.delegate = self
    }
}
