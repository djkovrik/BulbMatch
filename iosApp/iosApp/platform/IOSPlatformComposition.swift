#if arch(x86_64) && targetEnvironment(simulator)
// Compose Multiplatform 1.11.1 does not publish iosX64 artifacts.
#else
import UIKit
import compose

final class IOSPlatformComposition {
    let rootViewController: UIViewController

    private let imageSourceHost: IOSImageSourceHost
    private let textRecognitionHost: IOSTextRecognitionHost
    private let crashReportingHost: IOSCrashReportingHost
    private let appController: IosAppController

    init() {
        let factory = IosBridgeFactory()
        let imageSourceHost = IOSImageSourceHost(bridgeFactory: factory)
        let textRecognitionHost = IOSTextRecognitionHost(
            bridgeFactory: factory
        )
        let crashReportingHost = IOSCrashReportingHost()
        let appController = IosAppController(
            imageSourceHost: imageSourceHost,
            textRecognitionHost: textRecognitionHost,
            crashReportingHost: crashReportingHost
        )

        self.imageSourceHost = imageSourceHost
        self.textRecognitionHost = textRecognitionHost
        self.crashReportingHost = crashReportingHost
        self.appController = appController
        self.rootViewController = appController.viewController
        imageSourceHost.attachPresenter(rootViewController)
    }

    func onForegroundResume() {
        appController.onForegroundResume()
    }

    deinit {
        appController.close()
    }
}
#endif
