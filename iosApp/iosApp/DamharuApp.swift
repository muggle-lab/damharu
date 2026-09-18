import SwiftUI
import DamharuShared

@main
struct DamharuApp: App {
    var body: some Scene {
        WindowGroup { DiaryRoot().ignoresSafeArea() }
    }
}

private struct DiaryRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        func value(after flag: String) -> String {
            guard let index = args.firstIndex(of: flag), index + 1 < args.count else { return "" }
            return args[index + 1]
        }
        return MainViewControllerKt.MainViewController(
            showDemoControls: true,
            scenario: value(after: "--scenario"),
            dateOverride: value(after: "--date")
        )
        #else
        return MainViewControllerKt.MainViewController(showDemoControls: false, scenario: "empty", dateOverride: "")
        #endif
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
