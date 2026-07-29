#if arch(x86_64) && targetEnvironment(simulator)
// Compose Multiplatform 1.11.1 does not publish iosX64 artifacts.
#else
import AVFoundation
import Photos
import PhotosUI
import UIKit
import UniformTypeIdentifiers
import compose

final class IOSImageSourceHost: NSObject, PlatformIosImageSourceHost {
    typealias Completion = (PlatformIosHostImageResult) -> Void

    private let bridgeFactory: IosBridgeFactory
    private weak var presenter: UIViewController?
    private var pendingCompletion: Completion?

    init(bridgeFactory: IosBridgeFactory) {
        self.bridgeFactory = bridgeFactory
    }

    func attachPresenter(_ presenter: UIViewController) {
        self.presenter = presenter
    }

    func currentCameraPermission() -> PlatformCameraPermissionState {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            return bridgeFactory.permissionUnavailable()
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            return bridgeFactory.permissionGranted()
        case .notDetermined:
            return bridgeFactory.permissionDeniedCanAsk()
        case .denied, .restricted:
            return bridgeFactory.permissionDeniedOpenSettings()
        @unknown default:
            return bridgeFactory.permissionUnavailable()
        }
    }

    func requestCameraPermission(
        completion: @escaping (PlatformCameraPermissionState) -> Void
    ) {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            completeOnMain {
                completion(self.bridgeFactory.permissionUnavailable())
            }
            return
        }
        guard AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined else {
            completeOnMain {
                completion(self.currentCameraPermission())
            }
            return
        }
        AVCaptureDevice.requestAccess(for: .video) { [weak self] _ in
            guard let self else { return }
            self.completeOnMain {
                completion(self.currentCameraPermission())
            }
        }
    }

    func presentCamera(completion: @escaping Completion) {
        completeOnMain {
            guard self.pendingCompletion == nil else {
                completion(
                    self.bridgeFactory.imageFailure(code: .cameraNotReady)
                )
                return
            }
            guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
                completion(
                    self.bridgeFactory.imageFailure(code: .cameraUnavailable)
                )
                return
            }
            guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else {
                completion(
                    self.bridgeFactory.imageFailure(code: .permissionDenied)
                )
                return
            }
            guard let presenter = self.topPresenter() else {
                completion(
                    self.bridgeFactory.imageFailure(code: .cameraNotReady)
                )
                return
            }

            let picker = UIImagePickerController()
            picker.sourceType = .camera
            picker.mediaTypes = [UTType.image.identifier]
            picker.allowsEditing = false
            picker.delegate = self
            self.pendingCompletion = completion
            presenter.present(picker, animated: true)
        }
    }

    func presentSingleImagePicker(completion: @escaping Completion) {
        completeOnMain {
            guard self.pendingCompletion == nil else {
                completion(
                    self.bridgeFactory.imageFailure(code: .cameraNotReady)
                )
                return
            }
            guard let presenter = self.topPresenter() else {
                completion(
                    self.bridgeFactory.imageFailure(code: .unreadableImage)
                )
                return
            }

            var configuration = PHPickerConfiguration(photoLibrary: .shared())
            configuration.filter = .images
            configuration.selectionLimit = 1
            configuration.preferredAssetRepresentationMode = .current
            let picker = PHPickerViewController(configuration: configuration)
            picker.delegate = self
            self.pendingCompletion = completion
            presenter.present(picker, animated: true)
        }
    }

    func openApplicationSettings() -> Bool {
        guard let url = URL(string: UIApplication.openSettingsURLString),
              UIApplication.shared.canOpenURL(url)
        else {
            return false
        }
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
        return true
    }

    private func topPresenter() -> UIViewController? {
        func top(from controller: UIViewController?) -> UIViewController? {
            if let presented = controller?.presentedViewController {
                return top(from: presented)
            }
            if let navigation = controller as? UINavigationController {
                return top(from: navigation.visibleViewController)
            }
            if let tabs = controller as? UITabBarController {
                return top(from: tabs.selectedViewController)
            }
            return controller
        }
        return top(from: presenter)
    }

    private func finish(_ result: PlatformIosHostImageResult) {
        let completion = pendingCompletion
        pendingCompletion = nil
        completion?(result)
    }

    private func encodedBytes(from image: UIImage) -> KotlinByteArray? {
        guard let data = image.jpegData(compressionQuality: 0.92)
            ?? image.pngData()
        else {
            return nil
        }
        return kotlinBytes(from: data)
    }

    private func kotlinBytes(from data: Data) -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(data.count))
        data.withUnsafeBytes { rawBuffer in
            let bytes = rawBuffer.bindMemory(to: UInt8.self)
            for index in bytes.indices {
                result.set(
                    index: Int32(index),
                    value: Int8(bitPattern: bytes[index])
                )
            }
        }
        return result
    }

    private func completeOnMain(_ work: @escaping () -> Void) {
        if Thread.isMainThread {
            work()
        } else {
            DispatchQueue.main.async(execute: work)
        }
    }
}

extension IOSImageSourceHost:
    UIImagePickerControllerDelegate,
    UINavigationControllerDelegate
{
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true) {
            self.finish(self.bridgeFactory.imageCancelled())
        }
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        guard let image = info[.originalImage] as? UIImage else {
            picker.dismiss(animated: true) {
                self.finish(
                    self.bridgeFactory.imageFailure(code: .unreadableImage)
                )
            }
            return
        }
        picker.dismiss(animated: true) {
            DispatchQueue.global(qos: .userInitiated).async {
                let encoded = self.encodedBytes(from: image)
                self.completeOnMain {
                    if let encoded {
                        self.finish(
                            self.bridgeFactory.imageSuccess(encodedImage: encoded)
                        )
                    } else {
                        self.finish(
                            self.bridgeFactory.imageFailure(code: .unreadableImage)
                        )
                    }
                }
            }
        }
    }
}

extension IOSImageSourceHost: PHPickerViewControllerDelegate {
    func picker(
        _ picker: PHPickerViewController,
        didFinishPicking results: [PHPickerResult]
    ) {
        picker.dismiss(animated: true) {
            guard let provider = results.first?.itemProvider else {
                self.finish(self.bridgeFactory.imageCancelled())
                return
            }
            guard let imageType = provider.registeredTypeIdentifiers.first(
                where: {
                    UTType($0)?.conforms(to: .image) == true
                }
            ) else {
                self.finish(
                    self.bridgeFactory.imageFailure(code: .unreadableImage)
                )
                return
            }
            provider.loadDataRepresentation(
                forTypeIdentifier: imageType
            ) { data, _ in
                self.completeOnMain {
                    guard let data, !data.isEmpty else {
                        self.finish(
                            self.bridgeFactory.imageFailure(code: .unreadableImage)
                        )
                        return
                    }
                    self.finish(
                        self.bridgeFactory.imageSuccess(
                            encodedImage: self.kotlinBytes(from: data)
                        )
                    )
                }
            }
        }
    }
}
#endif
