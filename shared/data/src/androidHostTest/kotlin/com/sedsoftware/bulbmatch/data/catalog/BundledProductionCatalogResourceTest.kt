package com.sedsoftware.bulbmatch.data.catalog

import kotlin.test.Test
import kotlin.test.assertIs

class BundledProductionCatalogResourceTest {
    @Test
    fun packagedUtf8ResourceHasValidSchemaAndHash() {
        val bytes = readResource()

        val result = BundledCatalogLoader().load(
            utf8Json = bytes,
            mode = CatalogValidationMode.Development,
        )

        assertIs<CatalogLoadResult.Valid>(result, result.toString())
    }

    private fun readResource(): ByteArray {
        val classLoader = checkNotNull(javaClass.classLoader)
        return checkNotNull(
            classLoader.getResourceAsStream(BUNDLED_CATALOG_RESOURCE_PATH),
        ) {
            "Missing bundled catalog resource: $BUNDLED_CATALOG_RESOURCE_PATH"
        }.use { it.readBytes() }
    }
}
