//
//  NotificationManager.swift
//  iosApp
//
//  Created by mac on 2026/4/2.
//

import SwiftUI
import UserNotifications

// 1. 创建一个遵循 ObservableObject 和 UNUserNotificationCenterDelegate 的类
class NotificationManager: NSObject, ObservableObject, UNUserNotificationCenterDelegate {

    // 用于存储接收到的通知标识符，供界面监听
    @Published var clickKtorNotification: String?

    override init() {
        super.init()
        // 设置代理
        UNUserNotificationCenter.current().delegate = self
    }

    // 这是核心方法：当通知被点击（前台或后台）时调用
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {

        let request = response.notification.request
        DispatchQueue.main.async {
            //处理通知点击事件
        }

        // 必须调用 completionHandler
        completionHandler()
    }

    // 处理应用在前台收到通知时的展示（可选，默认前台不弹窗）
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // 如果希望前台也显示横幅和声音
        completionHandler([.banner, .sound])
    }
}
