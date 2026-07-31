package com.sedsoftware.bulbmatch.data.catalog

import com.sedsoftware.bulbmatch.data.DefaultCatalogProvider
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.VoltageDisposition
import com.sedsoftware.bulbmatch.domain.VoltageFamilyRule
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultCatalogProviderTest {
    @Test
    fun availableCatalogMapsEntriesAndSupportsNormalizedSearch() {
        val provider = provider(
            bytes = developmentDocument().toJsonBytes(),
            mode = CatalogValidationMode.Development,
        )

        val available = assertIs<CatalogAvailability.Available>(provider.availability.value)
        assertEquals("catalog-test", available.catalog.snapshot.catalogVersion)
        assertEquals(setOf(baseCode("E27")), available.catalog.snapshot.enabledBaseCodes)
        assertEquals(baseCode("E27"), provider.searchEntries("  эдисона ").single().code)
        assertEquals(baseCode("E27"), provider.searchEntries("").single().code)
        assertEquals("Edison screw", provider.entry(baseCode("E27"))?.commonNameEn)
        assertNull(provider.entry(baseCode("GU10")))
    }

    @Test
    fun reviewedRulesetRejectsMissingVoltageRulesAtConstruction() {
        assertFailsWith<IllegalArgumentException> {
            testRuleset(voltageRules = emptyList())
        }
    }

    @Test
    fun productionPendingApprovalExposesInspectableVersionsWithoutEntries() {
        val provider = provider(
            bytes = developmentDocument().toJsonBytes(),
            mode = CatalogValidationMode.Production,
        )

        val invalid = assertIs<CatalogAvailability.Invalid>(provider.availability.value)
        assertEquals("ProductionApprovalRequired", invalid.reasonCode)
        assertEquals("catalog-test", invalid.catalogVersion)
        assertEquals("rules-test", invalid.rulesetVersion)
        assertTrue(invalid.humanApprovalPending)
        assertTrue(provider.searchEntries("E27").isEmpty())
        assertNull(provider.entry(baseCode("E27")))
    }

    @Test
    fun malformedCatalogDoesNotInventInspectableMetadata() {
        val provider = provider("{not-json".encodeToByteArray())

        val invalid = assertIs<CatalogAvailability.Invalid>(provider.availability.value)
        assertEquals("MalformedJson", invalid.reasonCode)
        assertNull(invalid.catalogVersion)
        assertNull(invalid.rulesetVersion)
        assertFalse(invalid.humanApprovalPending)
    }

    @Test
    fun rulesetVersionAndReviewedCodesMustExactlyMatchRuntimeRules() {
        val versionMismatch = provider(
            bytes = developmentDocument().toJsonBytes(),
            ruleset = testRuleset().copy(version = "different-rules"),
        )
        assertEquals(
            "ReviewedRulesetVersionMismatch",
            assertIs<CatalogAvailability.Invalid>(versionMismatch.availability.value).reasonCode,
        )
        assertTrue(versionMismatch.searchEntries("").isEmpty())

        val codeMismatch = provider(
            bytes = developmentDocument().toJsonBytes(),
            ruleset = testRuleset().copy(reviewedRuleCodes = listOf("DIFFERENT_CODE")),
        )
        assertEquals(
            "ReviewedRuleCodesMismatch",
            assertIs<CatalogAvailability.Invalid>(codeMismatch.availability.value).reasonCode,
        )
        assertTrue(codeMismatch.searchEntries("").isEmpty())
    }

    private fun provider(
        bytes: ByteArray,
        mode: CatalogValidationMode = CatalogValidationMode.Development,
        ruleset: ReviewedCatalogRuleset = testRuleset(),
    ) = DefaultCatalogProvider(
        utf8Catalog = bytes,
        mode = mode,
        ruleset = ruleset,
    )

    private fun testRuleset(
        voltageRules: List<VoltageFamilyRule> = listOf(
            VoltageFamilyRule(
                minimumVolts = 220.0,
                maximumVolts = 240.0,
                disposition = VoltageDisposition.InScope,
                reasonCode = "TARGET_REGION",
            ),
        ),
    ) = ReviewedCatalogRuleset(
        version = "rules-test",
        reviewedRuleCodes = listOf("TARGET_REGION"),
        voltageRules = voltageRules,
        targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
        targetFrequency = requireNotNull(
            com.sedsoftware.bulbmatch.domain.FrequencyMarking.from(50.0),
        ),
    )

    private fun developmentDocument(): BundledCatalogDocument {
        val unsigned = BundledCatalogDocument(
            schemaVersion = 1,
            catalogVersion = "catalog-test",
            rulesetVersion = "rules-test",
            publishedAt = "2026-07-29",
            sourceManifestVersion = "sources-test",
            contentHashAlgorithm = "SHA-256",
            contentHashScope = "kotlinx-serialization canonical UnsignedCatalogPayload v1",
            contentHash = "",
            release = CatalogReleaseMetadata(
                state = "PENDING_HUMAN_SIGNOFF",
                releaseEligible = false,
                requiredReviewer = "Sergey V.",
                reviewedAt = null,
                decision = "PENDING",
            ),
            reviewedRuleCodes = listOf("TARGET_REGION"),
            entries = listOf(
                BundledCatalogEntry(
                    canonicalCode = "E27",
                    commonNameEn = "Edison screw",
                    commonNameRu = "Резьбовой цоколь",
                    aliasesEn = listOf("Edison"),
                    aliasesRu = listOf("Эдисона"),
                    diagramId = "base-e27",
                    distinguishingHintEn = "Large threaded base",
                    distinguishingHintRu = "Крупный резьбовой цоколь",
                    sourceRecordIds = listOf("source-e27"),
                    reviewState = "PENDING_HUMAN_SIGNOFF",
                    enabledForAssessment = true,
                ),
            ),
        )
        return unsigned.copy(contentHash = BundledCatalogLoader().contentHash(unsigned))
    }

    private fun BundledCatalogDocument.toJsonBytes(): ByteArray =
        catalogJson.encodeToString(this).encodeToByteArray()

    private fun baseCode(value: String): BaseCode = requireNotNull(BaseCode.from(value))
}
