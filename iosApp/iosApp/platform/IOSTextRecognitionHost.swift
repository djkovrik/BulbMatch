#if arch(x86_64) && targetEnvironment(simulator)
// Compose Multiplatform 1.11.1 does not publish iosX64 artifacts.
#else
import Foundation
import ImageIO
import MLKitTextRecognition
import MLKitVision
import UIKit
import compose

final class IOSTextRecognitionHost: PlatformIosTextRecognitionHost {
    private let bridgeFactory: IosBridgeFactory
    private let recognitionQueue = DispatchQueue(
        label: "com.sedsoftware.bulbmatch.ocr",
        qos: .userInitiated
    )
    private let lock = NSLock()
    private var recognizer: TextRecognizer?
    private var generation: UInt64 = 0
    private var closed = false

    init(bridgeFactory: IosBridgeFactory) {
        self.bridgeFactory = bridgeFactory
        self.recognizer = TextRecognizer.textRecognizer(
            options: TextRecognizerOptions()
        )
    }

    func recognize(
        encodedImage: KotlinByteArray,
        completion: @escaping (PlatformIosHostRecognitionResult) -> Void
    ) {
        let data = Self.data(from: encodedImage)
        lock.lock()
        let currentGeneration = generation
        let activeRecognizer = closed ? nil : recognizer
        lock.unlock()

        guard !data.isEmpty, let activeRecognizer else {
            completion(
                bridgeFactory.recognitionFailure(code: .unsupportedImage)
            )
            return
        }

        recognitionQueue.async { [weak self] in
            autoreleasepool {
                guard let self else { return }
                guard let image = Self.downsampledImage(from: data),
                      let cgImage = image.cgImage
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

                let visionImage = VisionImage(image: image)
                visionImage.orientation = image.imageOrientation
                do {
                    let recognized = try activeRecognizer.results(
                        in: visionImage
                    )
                    let width = CGFloat(cgImage.width)
                    let height = CGFloat(cgImage.height)
                    let observations = recognized.blocks.flatMap(\.lines)
                        .compactMap { line in
                            Self.observation(
                                line: line,
                                width: width,
                                height: height,
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

    func close() {
        lock.lock()
        guard !closed else {
            lock.unlock()
            return
        }
        closed = true
        generation &+= 1
        recognizer = nil
        lock.unlock()
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

    private static func downsampledImage(from data: Data) -> UIImage? {
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
        guard let image = CGImageSourceCreateThumbnailAtIndex(
            source,
            0,
            options as CFDictionary
        ) else {
            return nil
        }
        return UIImage(cgImage: image, scale: 1, orientation: .up)
    }

    private static func observation(
        line: TextLine,
        width: CGFloat,
        height: CGFloat,
        factory: IosBridgeFactory
    ) -> PlatformTextObservation? {
        let text = line.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, width > 0, height > 0 else {
            return nil
        }
        let frame = line.frame
        let left = normalized(frame.minX / width)
        let top = normalized(frame.minY / height)
        let right = normalized(frame.maxX / width)
        let bottom = normalized(frame.maxY / height)
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
