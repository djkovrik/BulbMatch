package com.sedsoftware.bulbmatch.data.catalog

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BundledCatalogLoaderTest {
    private val loader = BundledCatalogLoader()

    @Test
    fun sha256MatchesPublishedTestVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.digestHex("abc".encodeToByteArray()),
        )
    }

    @Test
    fun validDevelopmentCatalogPassesIntegrityValidation() {
        val document = signedDocument()

        val result = loader.load(
            utf8Json = catalogJson.encodeToString(document).encodeToByteArray(),
            mode = CatalogValidationMode.Development,
        )

        assertEquals(document, assertIs<CatalogLoadResult.Valid>(result).catalog)
    }

    @Test
    fun modifiedContentIsRejectedWithoutUnversionedFallback() {
        val document = signedDocument()
        val modified = document.copy(catalogVersion = "tampered")

        val result = loader.load(
            utf8Json = catalogJson.encodeToString(modified).encodeToByteArray(),
            mode = CatalogValidationMode.Development,
        )

        assertIs<CatalogIntegrityFailure.HashMismatch>(
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    @Test
    fun pendingDevelopmentCatalogCannotBeUsedAsProduction() {
        val document = signedDocument()

        val result = loader.load(
            utf8Json = catalogJson.encodeToString(document).encodeToByteArray(),
            mode = CatalogValidationMode.Production,
        )

        assertIs<CatalogIntegrityFailure.ProductionApprovalRequired>(
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    @Test
    fun malformedJsonIsIsolatedAsFatalCatalogDataFailure() {
        val result = loader.load(
            utf8Json = "{not-json".encodeToByteArray(),
            mode = CatalogValidationMode.Development,
        )

        assertEquals(
            CatalogIntegrityFailure.MalformedJson,
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    private fun signedDocument(): BundledCatalogDocument {
        val unsigned = BundledCatalogDocument(
            schemaVersion = 1,
            catalogVersion = "development-test",
            rulesetVersion = "development-test",
            publishedAt = "2026-07-29",
            sourceManifestVersion = "1",
            contentHashAlgorithm = "SHA-256",
            contentHashScope = "kotlinx-serialization canonical UnsignedCatalogPayload v1",
            contentHash = "",
            release = CatalogReleaseMetadata(
                state = "DEVELOPMENT_PENDING_HUMAN_SIGNOFF",
                releaseEligible = false,
                requiredReviewer = "Sergey V.",
                reviewedAt = null,
                decision = "PENDING",
            ),
            reviewedRuleCodes = emptyList(),
            entries = listOf(
                BundledCatalogEntry(
                    canonicalCode = "E27",
                    commonNameEn = "E27 candidate",
                    commonNameRu = "E27 candidate",
                    aliasesEn = listOf("E27"),
                    aliasesRu = listOf("E27"),
                    diagramId = "base_e27",
                    distinguishingHintEn = "Candidate hint",
                    distinguishingHintRu = "Candidate hint",
                    sourceRecordIds = listOf("APPSPEC-CANDIDATE-LIST"),
                    reviewState = "PENDING_HUMAN_SIGNOFF",
                    enabledForAssessment = false,
                ),
            ),
        )
        return unsigned.copy(contentHash = loader.contentHash(unsigned))
    }
}
