#if arch(x86_64) && targetEnvironment(simulator)
// Compose Multiplatform 1.11.1 does not publish iosX64 artifacts.
#else
import FirebaseCore
import FirebaseCrashlytics
import Foundation
import compose

final class IOSCrashReportingHost: PlatformIosCrashReportingHost {
    private let collectionEnabled: Bool

    init(bundle: Bundle = .main) {
        let hasFirebaseConfiguration =
            bundle.path(
                forResource: "GoogleService-Info",
                ofType: "plist"
            ) != nil
#if DEBUG
        collectionEnabled = false
#else
        collectionEnabled = hasFirebaseConfiguration
#endif

        guard hasFirebaseConfiguration else { return }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(
            collectionEnabled
        )
    }

    func recordNonFatal(
        exceptionType: String,
        screenCode: PlatformScreenCode?,
        operationCode: PlatformOperationCode
    ) {
        guard collectionEnabled, FirebaseApp.app() != nil else { return }

        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCustomValue(
            exceptionType,
            forKey: "exception_type"
        )
        crashlytics.setCustomValue(
            screenCode?.name ?? "NONE",
            forKey: "screen_code"
        )
        crashlytics.setCustomValue(
            operationCode.name,
            forKey: "operation_code"
        )
        crashlytics.record(
            error: NSError(
                domain: "com.sedsoftware.bulbmatch.nonfatal",
                code: Int(operationCode.ordinal),
                userInfo: [
                    NSLocalizedDescriptionKey: exceptionType,
                ]
            )
        )
    }
}
#endif
