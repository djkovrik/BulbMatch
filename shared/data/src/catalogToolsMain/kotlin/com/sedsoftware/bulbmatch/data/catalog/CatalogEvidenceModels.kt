package com.sedsoftware.bulbmatch.data.catalog

import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogSignoffDocument(
    val catalogVersion: String,
    val rulesetVersion: String,
    val safetyFixtureSuiteVersion: String,
    val sourceManifestVersion: String,
    val catalogContentHashAlgorithm: String,
    val catalogContentHash: String,
    val rulesetReviewFileSha256: String,
    val runtimeRulesSourceSha256: String,
    val safetyFixtureSuiteFileSha256: String,
    val reviewedCommit: String,
    val reviewer: String,
    val reviewedAt: String,
    val decision: String,
)

@Serializable
internal data class CatalogSafetyFixtureSuite(
    val suiteVersion: String,
    val catalogVersion: String,
    val rulesetVersion: String,
    val reviewer: String,
    val reviewedAt: String?,
    val reviewDecision: String,
    val fixtures: List<CatalogSafetyFixture>,
)

@Serializable
internal data class CatalogSafetyFixture(
    val fixtureId: String,
    val kind: String,
    val requirements: List<String>,
    val acceptanceScenarios: List<String>,
    val coveredRuleCodes: List<String>,
    val input: CatalogSafetyFixtureInput,
    val expectedOutcome: String,
    val expectedReasonCodes: List<String>,
    val expectedProfileAssertions: List<String> = emptyList(),
    val expectedBaseCode: String? = null,
    val reviewState: String,
)

@Serializable
internal data class CatalogSafetyFixtureInput(
    val baseKind: String? = null,
    val baseValue: String? = null,
    val voltageKind: String? = null,
    val voltageValues: List<CatalogVoltageFixtureValue> = emptyList(),
    val frequencyHz: Double? = null,
    val sourceRatedWatts: Double? = null,
    val fixtureMaximumWatts: Double? = null,
    val lumens: Double? = null,
    val printedEquivalentWatts: Double? = null,
    val dimmability: String? = null,
    val observationDecision: String? = null,
    val searchQuery: String? = null,
)

@Serializable
internal data class CatalogVoltageFixtureValue(
    val minimum: Double,
    val maximum: Double,
)
