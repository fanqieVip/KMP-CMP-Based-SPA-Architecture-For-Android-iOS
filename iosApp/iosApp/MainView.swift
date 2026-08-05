import ComposeApp
import SwiftUI
import UIKit

struct NainVC: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let composeVc = MainViewControllerKt.MainVC()
        let nav = FullScreenNavigationController(rootViewController: composeVc)
        return nav
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct MainView: View {
    @EnvironmentObject var notificationManager: NotificationManager
    @SwiftUI.State private var isKtorMonitorActive = false

    var body: some View {
        ZStack {
            NainVC()
                .ignoresSafeArea()
                .navigationBarHidden(true)
            // KtorMonitor 快捷入口按钮
            if BuildConfig_com_basic_base.shared.VERSION_TYPE != VersionStatus.shared.RELEASE {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button("KtorMonitor") {
                            isKtorMonitorActive = true
                        }
                        .buttonStyle(.borderedProminent)
                        .padding()
                        .padding(.bottom, 100)
                    }
                }
            }
        }
        .fullScreenCover(isPresented: $isKtorMonitorActive) {
            KtorMonitorView()
                .navigationBarHidden(true)
                .interactiveDismissDisabled(true)
        }
    }
}
