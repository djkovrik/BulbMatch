package com.sedsoftware.bulbmatch.data.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SUPPORTED_CATALOG_SCHEMA_VERSION = 1
private const val CONTENT_HASH_ALGORITHM = "SHA-256"
private const val CONTENT_HASH_SCOPE =
    "kotlinx-serialization canonical UnsignedCatalogPayload v1"

const val BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH =
    "catalog/bulbmatch-catalog-development.json"

@Serializable
data class BundledCatalogDocument(
    val schemaVersion: Int,
    val catalogVersion: String,
    val rulesetVersion: String,
    val publishedAt: String,
    val sourceManifestVersion: String,
    val contentHashAlgorithm: String,
    val contentHashScope: String,
    val contentHash: String,
    val release: CatalogReleaseMetadata,
    val reviewedRuleCodes: List<String>,
    val entries: List<BundledCatalogEntry>,
)

@Serializable
data class CatalogReleaseMetadata(
    val state: String,
    val releaseEligible: Boolean,
    val requiredReviewer: String,
    val reviewedAt: String?,
    val decision: String,
)

@Serializable
data class BundledCatalogEntry(
    val canonicalCode: String,
    val commonNameEn: String,
    val commonNameRu: String,
    val aliasesEn: List<String>,
    val aliasesRu: List<String>,
    val diagramId: String,
    val distinguishingHintEn: String,
    val distinguishingHintRu: String,
    val sourceRecordIds: List<String>,
    val reviewState: String,
    val enabledForAssessment: Boolean,
)

@Serializable
private data class UnsignedCatalogPayload(
    val schemaVersion: Int,
    val catalogVersion: String,
    val rulesetVersion: String,
    val publishedAt: String,
    val sourceManifestVersion: String,
    val contentHashAlgorithm: String,
    val contentHashScope: String,
    val release: CatalogReleaseMetadata,
    val reviewedRuleCodes: List<String>,
    val entries: List<BundledCatalogEntry>,
)

enum class CatalogValidationMode {
    Development,
    Production,
}

sealed interface CatalogLoadResult {
    data class Valid(
        val catalog: BundledCatalogDocument,
    ) : CatalogLoadResult

    data class Invalid(
        val failure: CatalogIntegrityFailure,
    ) : CatalogLoadResult
}

sealed interface CatalogIntegrityFailure {
    data object MalformedJson : CatalogIntegrityFailure
    data class UnsupportedSchema(val actual: Int) : CatalogIntegrityFailure
    data class UnsupportedHashContract(
        val algorithm: String,
        val scope: String,
    ) : CatalogIntegrityFailure

    data class HashMismatch(
        val expected: String,
        val actual: String,
    ) : CatalogIntegrityFailure

    data class InvalidMetadata(val field: String) : CatalogIntegrityFailure
    data class DuplicateCanonicalCode(val code: String) : CatalogIntegrityFailure
    data class DuplicateAlias(val alias: String) : CatalogIntegrityFailure
    data class ProductionApprovalRequired(val reason: String) : CatalogIntegrityFailure
}

class BundledCatalogLoader(
    private val json: Json = catalogJson,
) {
    fun load(
        utf8Json: ByteArray,
        mode: CatalogValidationMode,
    ): CatalogLoadResult {
        val document = try {
            json.decodeFromString<BundledCatalogDocument>(utf8Json.decodeToString())
        } catch (_: SerializationException) {
            return CatalogLoadResult.Invalid(CatalogIntegrityFailure.MalformedJson)
        } catch (_: IllegalArgumentException) {
            return CatalogLoadResult.Invalid(CatalogIntegrityFailure.MalformedJson)
        }

        if (document.schemaVersion != SUPPORTED_CATALOG_SCHEMA_VERSION) {
            return CatalogLoadResult.Invalid(
                CatalogIntegrityFailure.UnsupportedSchema(document.schemaVersion),
            )
        }
        if (
            document.contentHashAlgorithm != CONTENT_HASH_ALGORITHM ||
            document.contentHashScope != CONTENT_HASH_SCOPE
        ) {
            return CatalogLoadResult.Invalid(
                CatalogIntegrityFailure.UnsupportedHashContract(
                    algorithm = document.contentHashAlgorithm,
                    scope = document.contentHashScope,
                ),
            )
        }

        val actualHash = contentHash(document)
        if (!document.contentHash.equals(actualHash, ignoreCase = true)) {
            return CatalogLoadResult.Invalid(
                CatalogIntegrityFailure.HashMismatch(
                    expected = document.contentHash,
                    actual = actualHash,
                ),
            )
        }

        validateMetadata(document)?.let { return CatalogLoadResult.Invalid(it) }
        if (mode == CatalogValidationMode.Production) {
            validateProductionApproval(document)?.let {
                return CatalogLoadResult.Invalid(it)
            }
        }
        return CatalogLoadResult.Valid(document)
    }

    fun contentHash(document: BundledCatalogDocument): String {
        val unsigned = UnsignedCatalogPayload(
            schemaVersion = document.schemaVersion,
            catalogVersion = document.catalogVersion,
            rulesetVersion = document.rulesetVersion,
            publishedAt = document.publishedAt,
            sourceManifestVersion = document.sourceManifestVersion,
            contentHashAlgorithm = document.contentHashAlgorithm,
            contentHashScope = document.contentHashScope,
            release = document.release,
            reviewedRuleCodes = document.reviewedRuleCodes,
            entries = document.entries,
        )
        return Sha256.digestHex(json.encodeToString(unsigned).encodeToByteArray())
    }

    private fun validateMetadata(
        document: BundledCatalogDocument,
    ): CatalogIntegrityFailure? {
        val requiredMetadata = mapOf(
            "catalogVersion" to document.catalogVersion,
            "rulesetVersion" to document.rulesetVersion,
            "publishedAt" to document.publishedAt,
            "sourceManifestVersion" to document.sourceManifestVersion,
            "release.requiredReviewer" to document.release.requiredReviewer,
        )
        requiredMetadata.entries.firstOrNull { it.value.isBlank() }?.let {
            return CatalogIntegrityFailure.InvalidMetadata(it.key)
        }

        val canonicalCodes = mutableSetOf<String>()
        val aliasOwners = mutableMapOf<String, String>()
        document.entries.forEach { entry ->
            if (
                entry.canonicalCode.isBlank() ||
                entry.commonNameEn.isBlank() ||
                entry.commonNameRu.isBlank() ||
                entry.diagramId.isBlank() ||
                entry.distinguishingHintEn.isBlank() ||
                entry.distinguishingHintRu.isBlank() ||
                entry.sourceRecordIds.isEmpty() ||
                entry.sourceRecordIds.any(String::isBlank)
            ) {
                return CatalogIntegrityFailure.InvalidMetadata(
                    "entry[${entry.canonicalCode}].required",
                )
            }
            val normalizedCode = normalizeSearchTerm(entry.canonicalCode)
            if (!canonicalCodes.add(normalizedCode)) {
                return CatalogIntegrityFailure.DuplicateCanonicalCode(entry.canonicalCode)
            }
            (entry.aliasesEn + entry.aliasesRu + entry.canonicalCode).forEach { alias ->
                val normalized = normalizeSearchTerm(alias)
                if (normalized.isBlank()) {
                    return CatalogIntegrityFailure.InvalidMetadata(
                        "entry[${entry.canonicalCode}].alias",
                    )
                }
                val previousOwner = aliasOwners[normalized]
                if (previousOwner != null && previousOwner != normalizedCode) {
                    return CatalogIntegrityFailure.DuplicateAlias(alias)
                }
                aliasOwners[normalized] = normalizedCode
            }
        }
        return null
    }

    private fun validateProductionApproval(
        document: BundledCatalogDocument,
    ): CatalogIntegrityFailure? {
        val release = document.release
        if (
            release.state != "APPROVED" ||
            !release.releaseEligible ||
            release.requiredReviewer != "Sergey V." ||
            release.reviewedAt.isNullOrBlank() ||
            release.decision != "APPROVED"
        ) {
            return CatalogIntegrityFailure.ProductionApprovalRequired(
                "Catalog metadata does not contain Sergey V.'s recorded approval.",
            )
        }
        if (document.reviewedRuleCodes.isEmpty()) {
            return CatalogIntegrityFailure.ProductionApprovalRequired(
                "No reviewed rule codes are present.",
            )
        }
        if (document.entries.any { it.reviewState != "APPROVED" || !it.enabledForAssessment }) {
            return CatalogIntegrityFailure.ProductionApprovalRequired(
                "At least one catalog entry is pending or disabled.",
            )
        }
        return null
    }
}

internal val catalogJson = Json {
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = false
    isLenient = false
}

internal fun normalizeSearchTerm(value: String): String = value
    .trim()
    .uppercase()
    .filterNot { it.isWhitespace() || it == '-' || it == '_' || it == '.' }
