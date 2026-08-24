import SwiftUI
import ComposeApp
import UIKit

// SceneDelegate，用于接收 Universal Link 和 URL Scheme 回调
class SwiftSceneDelegate: NSObject, UIWindowSceneDelegate {
    func sceneDidBecomeActive(_ scene: UIScene) {
        SwiftAppDelegate.syncKeyWindow()
        if let windowScene = scene as? UIWindowScene {
            syncOrientation(windowScene.interfaceOrientation)
        }
    }

    func windowScene(
        _ windowScene: UIWindowScene,
        didUpdate previousCoordinateSpace: UICoordinateSpace,
        interfaceOrientation previousInterfaceOrientation: UIInterfaceOrientation,
        traitCollection previousTraitCollection: UITraitCollection
    ) {
        syncOrientation(windowScene.interfaceOrientation)
    }

    // 处理 Universal Link (如微信 SDK 要求的地方)
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        AppDelegate.shared.sceneContinueUserActivity(userActivity: userActivity)
    }

    // 处理 URL Scheme (如微信、支付宝支付返回)
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        AppDelegate.shared.sceneOpenURLContexts(urlContexts: URLContexts)
    }

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        SwiftAppDelegate.syncKeyWindow()
        if let windowScene = scene as? UIWindowScene {
            syncOrientation(windowScene.interfaceOrientation)
        }
        AppDelegate.shared.sceneWillConnectToOptions(userActivities: connectionOptions.userActivities, urlContexts: connectionOptions.urlContexts)
    }

    private func syncOrientation(_ orientation: UIInterfaceOrientation) {
        IOSScreenStateHelper.shared.updateWindowOrientation(
            interfaceOrientation: Int64(orientation.rawValue)
        )
    }
}

class SwiftAppDelegate: NSObject, UIApplicationDelegate {
    private weak var currentWindow: UIWindow?

    @objc dynamic var window: UIWindow? {
        get {
            currentWindow ?? SwiftAppDelegate.findKeyWindow()
        }
        set {
            currentWindow = newValue
        }
    }

    static func syncKeyWindow() {
        (UIApplication.shared.delegate as? SwiftAppDelegate)?.window = findKeyWindow()
    }

    private static func findKeyWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap { $0.windows }
        .first { $0.isKeyWindow }
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        AppDelegate.shared.onAppCreate()
        UIViewController.swizzleAllLifecycle()
        NotificationCenter.default.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let sceneConfig = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = SwiftSceneDelegate.self
        return sceneConfig
    }

    @objc private func didEnterBackground() {
        AppDelegate.shared.onAppBackground()
    }

    @objc private func willEnterForeground() {
        AppDelegate.shared.onAppForeground()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(SwiftAppDelegate.self) var delegate
    @SwiftUI.StateObject private var notificationManager = NotificationManager()

    var body: some Scene {
        WindowGroup {
            MainView().environmentObject(notificationManager)
        }
    }
}
