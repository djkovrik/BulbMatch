import SwiftUI

#if arch(x86_64) && targetEnvironment(simulator)

@main
struct ComposeApp: App {
    var body: some Scene {
        WindowGroup {
            VStack(spacing: 16) {
                Image(systemName: "desktopcomputer")
                    .font(.system(size: 44))
                Text("BulbMatch Intel Simulator")
                    .font(.title2.bold())
                Text(
                    "Compose Multiplatform 1.11.1 has no iosX64 runtime. "
                    + "Use an arm64 Simulator or a physical iPhone for product QA."
                )
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            }
            .padding(32)
            .accessibilityElement(children: .combine)
        }
    }
}

#else
import compose

@main
struct ComposeApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let composition = IOSPlatformComposition()

    var body: some Scene {
        WindowGroup {
            ContentView(viewController: composition.rootViewController)
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        composition.onForegroundResume()
                    }
                }
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
#endif
