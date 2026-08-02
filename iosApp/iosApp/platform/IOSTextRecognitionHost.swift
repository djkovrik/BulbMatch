#if arch(x86_64) && targetEnvironment(simulator)
// Compose Multiplatform 1.11.1 does not publish iosX64 artifacts.
#else
import Foundation
import ImageIO
import UIKit
import compose

private actor IOSPaddleOCRController {
    private var sessionManager: ORTSessionManager?
    private var engine: OCREngine?
    private var closed = false

    func recognize(_ image: CGImage) async throws -> OCRRunResult {
        guard !closed else {
            throw CancellationError()
        }
        if let engine {
            return try await engine.run(image)
        }

        let manager = ORTSessionManager()
        try await manager.loadModels(executionProvider: .cpu)
        guard !closed else {
            throw CancellationError()
        }
        let loadedEngine = try OCREngine(sessionManager: manager)
        sessionManager = manager
        engine = loadedEngine
        return try await loadedEngine.run(image)
    }

    func close() {
        closed = true
        engine = nil
        sessionManager = nil
    }
}

final class IOSTextRecognitionHost: PlatformIosTextRecognitionHost {
    private let bridgeFactory: IosBridgeFactory
    private let recognitionQueue = DispatchQueue(
        label: "com.sedsoftware.bulbmatch.ocr",
        qos: .userInitiated
    )
    private let lock = NSLock()
    private var controller: IOSPaddleOCRController?
    private var generation: UInt64 = 0
    private var closed = false

    init(bridgeFactory: IosBridgeFactory) {
        self.bridgeFactory = bridgeFactory
        self.controller = IOSPaddleOCRController()
    }

    func recognize(
        encodedImage: KotlinByteArray,
        completion: @escaping (PlatformIosHostRecognitionResult) -> Void
    ) {
        let data = Self.data(from: encodedImage)
        lock.lock()
        let currentGeneration = generation
        let activeController = closed ? nil : controller
        lock.unlock()

        guard !data.isEmpty, let activeController else {
            completion(
                bridgeFactory.recognitionFailure(code: .unsupportedImage)
            )
            return
        }

        recognitionQueue.async { [weak self] in
            autoreleasepool {
                guard let self else { return }
                guard let cgImage = Self.downsampledImage(from: data)
                else {
                    self.completeIfActive(generation: currentGeneration) {
                        completion(
                            self.bridgeFactory.recognitionFailure(
                                code: .unsupportedImage
                            )
                        )
                    }
                    return
                }

                Task {
                    do {
                        let recognized = try await activeController.recognize(cgImage)
                        let observations = recognized.results.compactMap { result in
                            Self.observation(
                                result: result,
                                width: CGFloat(cgImage.width),
                                height: CGFloat(cgImage.height),
                                factory: self.bridgeFactory
                            )
                        }
                        self.completeIfActive(generation: currentGeneration) {
                            completion(
                                self.bridgeFactory.recognitionSuccess(
                                    observations: observations
                                )
                            )
                        }
                    } catch {
                        self.completeIfActive(generation: currentGeneration) {
                            completion(
                                self.bridgeFactory.recognitionFailure(
                                    code: .recognitionFailed
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    func close() {
        lock.lock()
        guard !closed else {
            lock.unlock()
            return
        }
        closed = true
        generation &+= 1
        let activeController = controller
        controller = nil
        lock.unlock()
        if let activeController {
            Task { await activeController.close() }
        }
    }

    private func completeIfActive(
        generation expectedGeneration: UInt64,
        completion: @escaping () -> Void
    ) {
        lock.lock()
        let active = !closed && generation == expectedGeneration
        lock.unlock()
        guard active else { return }
        DispatchQueue.main.async(execute: completion)
    }

    private static func data(from bytes: KotlinByteArray) -> Data {
        var data = Data(count: Int(bytes.size))
        data.withUnsafeMutableBytes { rawBuffer in
            let destination = rawBuffer.bindMemory(to: UInt8.self)
            for index in destination.indices {
                destination[index] = UInt8(
                    bitPattern: bytes.get(index: Int32(index))
                )
            }
        }
        return data
    }

    private static func downsampledImage(from data: Data) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(
            data as CFData,
            nil
        ) else {
            return nil
        }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: 2048,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        return CGImageSourceCreateThumbnailAtIndex(
            source,
            0,
            options as CFDictionary
        )
    }

    private static func observation(
        result: OCRResult,
        width: CGFloat,
        height: CGFloat,
        factory: IosBridgeFactory
    ) -> PlatformTextObservation? {
        let text = result.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, width > 0, height > 0 else {
            return nil
        }
        let xs = result.polygon.compactMap { $0.count >= 2 ? CGFloat($0[0]) : nil }
        let ys = result.polygon.compactMap { $0.count >= 2 ? CGFloat($0[1]) : nil }
        guard let minX = xs.min(), let maxX = xs.max(),
              let minY = ys.min(), let maxY = ys.max() else {
            return nil
        }
        let left = normalized(minX / width)
        let top = normalized(minY / height)
        let right = normalized(maxX / width)
        let bottom = normalized(maxY / height)
        guard left <= right, top <= bottom else {
            return nil
        }
        return factory.textObservation(
            text: text,
            left: left,
            top: top,
            right: right,
            bottom: bottom
        )
    }

    private static func normalized(_ value: CGFloat) -> Float {
        Float(min(max(value, 0), 1))
    }
}
#endif
