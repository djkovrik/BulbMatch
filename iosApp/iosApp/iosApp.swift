import SwiftUI
import compose

@main
struct ComposeApp: App {
    private let rootViewController = MainKt.MainViewController()

    var body: some Scene {
        WindowGroup {
            ContentView(viewController: rootViewController)
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    let viewController: UIViewController

    func makeUIViewController(context: Context) -> UIViewController {
        viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates will be handled by Compose
    }
}
