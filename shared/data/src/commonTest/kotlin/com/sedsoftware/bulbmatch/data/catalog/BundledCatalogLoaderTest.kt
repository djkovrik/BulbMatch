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
        val document = pendingDocument()

        val result = loader.load(
            utf8Json = catalogJson.encodeToString(document).encodeToByteArray(),
            mode = CatalogValidationMode.Development,
        )

        assertEquals(document, assertIs<CatalogLoadResult.Valid>(result).catalog)
    }

    @Test
    fun modifiedContentIsRejectedWithoutUnversionedFallback() {
        val document = pendingDocument()
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
        val document = pendingDocument()

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

    @Test
    fun exactApprovedCatalogPassesProductionValidation() {
        val document = approvedDocument()

        val result = loader.load(
            utf8Json = catalogJson.encodeToString(document).encodeToByteArray(),
            mode = CatalogValidationMode.Production,
        )

        assertEquals(document, assertIs<CatalogLoadResult.Valid>(result).catalog)
    }

    @Test
    fun reviewerDateDecisionAndRuleCodesAreReleaseBlocking() {
        val approved = approvedDocument()
        val invalidDocuments = listOf(
            approved.copy(
                release = approved.release.copy(requiredReviewer = "Another Reviewer"),
            ).withHash(),
            approved.copy(
                release = approved.release.copy(reviewedAt = ""),
            ).withHash(),
            approved.copy(
                release = approved.release.copy(decision = "PENDING"),
            ).withHash(),
            approved.copy(reviewedRuleCodes = emptyList()).withHash(),
        )

        invalidDocuments.forEach { document ->
            val result = loader.load(
                utf8Json = catalogJson.encodeToString(document).encodeToByteArray(),
                mode = CatalogValidationMode.Production,
            )
            assertIs<CatalogIntegrityFailure.ProductionApprovalRequired>(
                assertIs<CatalogLoadResult.Invalid>(result).failure,
            )
        }
    }

    @Test
    fun pendingOrDisabledEntryBlocksProduction() {
        val approved = approvedDocument()
        val pending = approved.entries.single().copy(
            reviewState = "PENDING_HUMAN_SIGNOFF",
            enabledForAssessment = false,
        )
        val result = loader.load(
            utf8Json = catalogJson.encodeToString(
                approved.copy(entries = listOf(pending)).withHash(),
            ).encodeToByteArray(),
            mode = CatalogValidationMode.Production,
        )

        assertIs<CatalogIntegrityFailure.ProductionApprovalRequired>(
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    @Test
    fun duplicateNormalizedCodeAndCrossEntryAliasAreRejected() {
        val approved = approvedDocument()
        val first = approved.entries.single()
        val duplicateCode = first.copy(
            canonicalCode = "E-27",
            aliasesEn = listOf("second"),
            aliasesRu = listOf("второй"),
        )
        val duplicateCodeResult = loader.load(
            catalogJson.encodeToString(
                approved.copy(entries = listOf(first, duplicateCode)).withHash(),
            ).encodeToByteArray(),
            CatalogValidationMode.Development,
        )
        assertIs<CatalogIntegrityFailure.DuplicateCanonicalCode>(
            assertIs<CatalogLoadResult.Invalid>(duplicateCodeResult).failure,
        )

        val duplicateAlias = first.copy(
            canonicalCode = "GU10",
            aliasesEn = listOf("E 27"),
            aliasesRu = listOf("ГУ10"),
        )
        val duplicateAliasResult = loader.load(
            catalogJson.encodeToString(
                approved.copy(entries = listOf(first, duplicateAlias)).withHash(),
            ).encodeToByteArray(),
            CatalogValidationMode.Development,
        )
        assertIs<CatalogIntegrityFailure.DuplicateAlias>(
            assertIs<CatalogLoadResult.Invalid>(duplicateAliasResult).failure,
        )
    }

    @Test
    fun unknownJsonFieldIsRejectedByStrictDecoding() {
        val document = approvedDocument()
        val encoded = catalogJson.encodeToString(document)
        val withUnknownField = encoded.replaceFirst(
            "{",
            """{"unexpected":true,""",
        )

        val result = loader.load(
            withUnknownField.encodeToByteArray(),
            CatalogValidationMode.Development,
        )

        assertEquals(
            CatalogIntegrityFailure.MalformedJson,
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    private fun pendingDocument(): BundledCatalogDocument {
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
        return unsigned.withHash()
    }

    private fun approvedDocument(): BundledCatalogDocument {
        val pending = pendingDocument()
        return pending.copy(
            release = CatalogReleaseMetadata(
                state = "APPROVED",
                releaseEligible = true,
                requiredReviewer = "Sergey V.",
                reviewedAt = "2026-08-01T00:00:00Z",
                decision = "APPROVED",
            ),
            reviewedRuleCodes = listOf("VOLTAGE_TARGET_220_240"),
            entries = pending.entries.map { entry ->
                entry.copy(
                    reviewState = "APPROVED",
                    enabledForAssessment = true,
                )
            },
        ).withHash()
    }

    private fun BundledCatalogDocument.withHash(): BundledCatalogDocument =
        copy(contentHash = loader.contentHash(this))
}
