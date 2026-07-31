package com.sedsoftware.bulbmatch.data.catalog

import kotlinx.serialization.decodeFromString
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private const val PRODUCTION_CATALOG_FILENAME = "bulbmatch-catalog-production.json"
private const val OPERATION_INDEX = 0
private const val CATALOG_PATH_INDEX = 1
private const val CATALOG_RELEASES_ROOT_INDEX = 2
private const val RUNTIME_RULES_SOURCE_INDEX = 3
private const val SHIPPING_CATALOG_DIRECTORY_INDEX = 4
private const val UPDATE_HASH_ARGUMENT_COUNT = 2
private const val VALIDATE_RELEASE_ARGUMENT_COUNT = 5
private val fullCommitRegex = Regex("[0-9a-f]{40}")
private val sha256Regex = Regex("[0-9a-f]{64}")

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Expected a catalog tool operation." }
    when (args[OPERATION_INDEX]) {
        "update-hash" -> {
            require(args.size == UPDATE_HASH_ARGUMENT_COUNT) {
                "update-hash expects the production catalog path."
            }
            updateCatalogHash(Path.of(args[CATALOG_PATH_INDEX]))
        }
        "validate-release" -> {
            require(args.size == VALIDATE_RELEASE_ARGUMENT_COUNT) {
                "validate-release expects catalog, releases root, runtime rules, and shipping directory."
            }
            validateRelease(
                catalogPath = Path.of(args[CATALOG_PATH_INDEX]),
                catalogReleasesRoot = Path.of(args[CATALOG_RELEASES_ROOT_INDEX]),
                runtimeRulesSource = Path.of(args[RUNTIME_RULES_SOURCE_INDEX]),
                shippingCatalogDirectory = Path.of(args[SHIPPING_CATALOG_DIRECTORY_INDEX]),
            )
        }
        else -> error("Unknown catalog tool operation: ${args[OPERATION_INDEX]}")
    }
}

private fun updateCatalogHash(catalogPath: Path) {
    require(catalogPath.fileName.toString() == PRODUCTION_CATALOG_FILENAME)
    val original = Files.readString(catalogPath, StandardCharsets.UTF_8)
    require(!original.startsWith('\uFEFF')) { "Production catalog must be UTF-8 without BOM." }
    val document = catalogJson.decodeFromString<BundledCatalogDocument>(original)
    verifyRulesMetadata(document)

    val hash = BundledCatalogLoader().contentHash(document)
    val contentHashLine = Regex(
        pattern = """(?m)^(\s*"contentHash"\s*:\s*")[^"]*("\s*,\s*)$""",
    )
    require(contentHashLine.findAll(original).count() == 1) {
        "Expected exactly one top-level contentHash line."
    }
    val updated = contentHashLine.replace(original) { match ->
        match.groupValues[1] + hash + match.groupValues[2]
    }
    Files.writeString(catalogPath, updated, StandardCharsets.UTF_8)

    val updatedBytes = Files.readAllBytes(catalogPath)
    val development = BundledCatalogLoader().load(
        utf8Json = updatedBytes,
        mode = CatalogValidationMode.Development,
    )
    require(development is CatalogLoadResult.Valid) {
        "Development catalog validation failed after hash update: $development"
    }
    if (document.release.state == "APPROVED") {
        val production = BundledCatalogLoader().load(
            utf8Json = updatedBytes,
            mode = CatalogValidationMode.Production,
        )
        require(production is CatalogLoadResult.Valid) {
            "Production catalog validation failed after hash update: $production"
        }
    }
    println("catalogVersion=${document.catalogVersion} contentHash=$hash")
}

private fun validateRelease(
    catalogPath: Path,
    catalogReleasesRoot: Path,
    runtimeRulesSource: Path,
    shippingCatalogDirectory: Path,
) {
    val catalogBytes = Files.readAllBytes(catalogPath)
    val catalog = when (
        val loaded = BundledCatalogLoader().load(
            utf8Json = catalogBytes,
            mode = CatalogValidationMode.Production,
        )
    ) {
        is CatalogLoadResult.Valid -> loaded.catalog
        is CatalogLoadResult.Invalid -> error("Production catalog validation failed: ${loaded.failure}")
    }
    verifyRulesMetadata(catalog)
    val normalizedReleasesRoot = catalogReleasesRoot.toAbsolutePath().normalize()
    val releaseRoot = normalizedReleasesRoot.resolve(catalog.catalogVersion).normalize()
    require(releaseRoot.parent == normalizedReleasesRoot) {
        "Catalog version must resolve to one direct child of the releases root."
    }
    require(releaseRoot.fileName.toString() == catalog.catalogVersion)

    val rulesetPath = releaseRoot.resolve("rules/ruleset.md")
    val fixturePath = releaseRoot.resolve("fixtures/safety-fixtures.json")
    val signoffPath = releaseRoot.resolve("approval/catalog-signoff.json")
    val sourcesPath = releaseRoot.resolve("sources")
    listOf(rulesetPath, fixturePath, signoffPath, runtimeRulesSource).forEach { path ->
        require(path.isRegularFile()) { "Missing required frozen file: $path" }
    }

    val fixtures = catalogJson.decodeFromString<CatalogSafetyFixtureSuite>(
        Files.readString(fixturePath, StandardCharsets.UTF_8),
    )
    require(fixtures.catalogVersion == catalog.catalogVersion)
    require(fixtures.rulesetVersion == catalog.rulesetVersion)
    require(fixtures.suiteVersion == catalog.catalogVersion)
    require(fixtures.reviewer == "Sergey V.")
    require(!fixtures.reviewedAt.isNullOrBlank())
    require(fixtures.reviewDecision == "APPROVED")
    require(fixtures.fixtures.isNotEmpty())
    require(fixtures.fixtures.all { it.reviewState == "APPROVED" })

    val signoff = catalogJson.decodeFromString<CatalogSignoffDocument>(
        Files.readString(signoffPath, StandardCharsets.UTF_8),
    )
    require(signoff.catalogVersion == catalog.catalogVersion)
    require(signoff.rulesetVersion == catalog.rulesetVersion)
    require(signoff.safetyFixtureSuiteVersion == fixtures.suiteVersion)
    require(signoff.sourceManifestVersion == catalog.sourceManifestVersion)
    require(signoff.catalogContentHashAlgorithm == "SHA-256")
    require(signoff.catalogContentHash == catalog.contentHash)
    require(signoff.rulesetReviewFileSha256 == sha256GitText(rulesetPath))
    require(signoff.runtimeRulesSourceSha256 == sha256GitText(runtimeRulesSource))
    require(signoff.safetyFixtureSuiteFileSha256 == sha256GitText(fixturePath))
    require(fullCommitRegex.matches(signoff.reviewedCommit))
    require(signoff.reviewer == "Sergey V.")
    require(signoff.reviewedAt == catalog.release.reviewedAt)
    require(signoff.decision == "APPROVED")
    listOf(
        signoff.catalogContentHash,
        signoff.rulesetReviewFileSha256,
        signoff.runtimeRulesSourceSha256,
        signoff.safetyFixtureSuiteFileSha256,
    ).forEach { hash -> require(sha256Regex.matches(hash)) }

    val sourceRecordIds = catalog.entries.flatMap(BundledCatalogEntry::sourceRecordIds).toSet()
    sourceRecordIds.forEach { sourceRecordId ->
        val sourcePath = sourcesPath.resolve("$sourceRecordId.md")
        require(sourcePath.isRegularFile()) { "Missing source record: $sourceRecordId" }
        val sourceText = Files.readString(sourcePath, StandardCharsets.UTF_8)
        require("entryId: $sourceRecordId" in sourceText)
        require("catalogVersion: ${catalog.catalogVersion}" in sourceText)
        require("contentHash: ${catalog.contentHash}" in sourceText)
        require("reviewer: Sergey V." in sourceText)
        require("reviewDecision: APPROVED" in sourceText)
    }

    val rulesText = Files.readString(rulesetPath, StandardCharsets.UTF_8)
    require("Review decision: `APPROVED`" in rulesText)
    BundledCatalogRules.reviewedRuleCodes.forEach { code ->
        require("ruleCode: $code" in rulesText) { "Ruleset is missing rule code $code" }
    }

    val shippingCatalogs = Files.list(shippingCatalogDirectory).use { paths ->
        paths
            .filter { it.isRegularFile() }
            .filter { it.name.startsWith("bulbmatch-catalog-") && it.name.endsWith(".json") }
            .toList()
    }
    require(shippingCatalogs.size == 1) {
        "Expected exactly one shipping catalog, found: ${shippingCatalogs.map(Path::getFileName)}"
    }
    require(shippingCatalogs.single().fileName.toString() == PRODUCTION_CATALOG_FILENAME)

    println(
        "catalogVersion=${catalog.catalogVersion} rulesetVersion=${catalog.rulesetVersion} " +
            "fixtures=${fixtures.fixtures.size} contentHash=${catalog.contentHash}",
    )
}

private fun verifyRulesMetadata(document: BundledCatalogDocument) {
    require(document.rulesetVersion == BundledCatalogRules.VERSION) {
        "Catalog rulesetVersion does not match the runtime reviewed ruleset."
    }
    require(document.reviewedRuleCodes == BundledCatalogRules.reviewedRuleCodes) {
        "Catalog reviewedRuleCodes do not exactly match the runtime reviewed ruleset."
    }
}

private fun sha256GitText(path: Path): String {
    val gitCanonicalBytes = Files.readString(path, StandardCharsets.UTF_8)
        .replace("\r\n", "\n")
        .toByteArray(StandardCharsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(gitCanonicalBytes)
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
