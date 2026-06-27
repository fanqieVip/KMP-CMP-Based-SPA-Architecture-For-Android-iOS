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

struct KtorMonitorView: View {
    var body: some View {
        KtorMonitorVC()
            .ignoresSafeArea()
    }
}
