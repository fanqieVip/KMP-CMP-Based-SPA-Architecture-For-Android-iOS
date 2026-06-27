import SwiftUI
import ComposeApp

// SceneDelegate，用于接收 Universal Link 和 URL Scheme 回调
class SwiftSceneDelegate: NSObject, UIWindowSceneDelegate {
    
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        AppDelegate.shared.sceneContinueUserActivity(userActivity: userActivity)
    }
    
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        AppDelegate.shared.sceneOpenURLContexts(urlContexts: URLContexts)
    }

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        AppDelegate.shared.sceneWillConnectToOptions(userActivities: connectionOptions.userActivities, urlContexts: connectionOptions.urlContexts)
    }
}

class SwiftAppDelegate: NSObject, UIApplicationDelegate {
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
