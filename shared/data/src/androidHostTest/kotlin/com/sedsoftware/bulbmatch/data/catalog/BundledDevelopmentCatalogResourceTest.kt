package com.sedsoftware.bulbmatch.data.catalog

import kotlin.test.Test
import kotlin.test.assertIs

class BundledDevelopmentCatalogResourceTest {
    @Test
    fun packagedUtf8ResourceHasValidSchemaAndHash() {
        val bytes = readResource()

        val result = BundledCatalogLoader().load(
            utf8Json = bytes,
            mode = CatalogValidationMode.Development,
        )

        assertIs<CatalogLoadResult.Valid>(result, result.toString())
    }

    @Test
    fun packagedDevelopmentResourceCannotPassProductionValidation() {
        val bytes = readResource()

        val result = BundledCatalogLoader().load(
            utf8Json = bytes,
            mode = CatalogValidationMode.Production,
        )

        assertIs<CatalogIntegrityFailure.ProductionApprovalRequired>(
            assertIs<CatalogLoadResult.Invalid>(result).failure,
        )
    }

    private fun readResource(): ByteArray {
        val classLoader = checkNotNull(javaClass.classLoader)
        return checkNotNull(
            classLoader.getResourceAsStream(BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH),
        ) {
            "Missing bundled catalog resource: $BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH"
        }.use { it.readBytes() }
    }
}
