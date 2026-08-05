//
// Created by mac on 2026/4/2.
//

import ComposeApp
import SwiftUI
import UIKit

struct KtorMonitorVC: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.KtorMonitorVC()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/**
 * KtorMonitor 调试页面。
 */
struct KtorMonitorView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topLeading) {
            KtorMonitorVC()
                .ignoresSafeArea()

            Button("返回") {
                dismiss()
            }
            .buttonStyle(.borderedProminent)
            .padding(.top, 12)
            .padding(.leading, 12)
        }
    }
}
