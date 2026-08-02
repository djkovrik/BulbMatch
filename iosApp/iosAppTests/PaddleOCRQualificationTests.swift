import ImageIO
import Vision
import XCTest
@testable import BulbMatch

final class PaddleOCRQualificationTests: XCTestCase {
    func testBundledPaddleModelsAgainstAppleVisionControl() async throws {
        let fixtureURL = try XCTUnwrap(
            Bundle(for: Self.self).url(
                forResource: "cyrillic_e27_clean",
                withExtension: "png"
            )
        )
        let source = try XCTUnwrap(CGImageSourceCreateWithURL(fixtureURL as CFURL, nil))
        let image = try XCTUnwrap(CGImageSourceCreateImageAtIndex(source, 0, nil))

        let manager = ORTSessionManager()
        try await manager.loadModels(executionProvider: .cpu)
        let paddle = try OCREngine(sessionManager: manager)
        let paddleText = try await paddle.run(image).results.map(\.text).joined(separator: " ")
        let visionText = try recognizeWithVision(image)

        XCTAssertTrue(paddleText.contains("220"), paddleText)
        XCTAssertTrue(paddleText.uppercased().contains("Е27") || paddleText.uppercased().contains("E27"), paddleText)
        XCTAssertTrue(visionText.contains("220"), visionText)

        let attachment = XCTAttachment(
            string: "PaddleOCR: \(paddleText)\nApple Vision control: \(visionText)"
        )
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func recognizeWithVision(_ image: CGImage) throws -> String {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["ru-RU", "en-US"]
        try VNImageRequestHandler(cgImage: image).perform([request])
        return (request.results ?? [])
            .compactMap { $0.topCandidates(1).first?.string }
            .joined(separator: " ")
    }
}
