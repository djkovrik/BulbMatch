package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultReferenceComponentTest {
    private val fixture = AppComponentTestFixture()

    @BeforeTest
    fun setUp() = fixture.setUp()

    @AfterTest
    fun tearDown() = fixture.tearDown()

    @Test
    fun searchDetailAndUseBaseStayInsidePublicComponentContract() = runTest(fixture.dispatcher) {
        val catalog = testCatalogProvider()
        val outputs = mutableListOf<ReferenceComponent.Output>()
        val component = DefaultReferenceComponent(
            componentContext = fixture.componentContext(),
            catalogProvider = catalog,
            output = outputs::add,
        )
        fixture.dispatcher.scheduler.advanceUntilIdle()

        component.onQueryChanged("эдисон")
        assertEquals(listOf(testBaseCode()), component.model.value.entries.map { it.code })
        component.onClearQueryRequested()
        assertEquals("", component.model.value.query)

        component.onEntrySelected(testBaseCode())
        val detail = assertNotNull(component.detailSlot.value.child?.instance)
        assertEquals(testBaseCode(), detail.entry.code)
        detail.onBackRequested()
        assertNull(component.detailSlot.value.child)

        component.onEntrySelected(testBaseCode())
        assertNotNull(component.detailSlot.value.child?.instance).onUseBaseRequested()
        assertNull(component.detailSlot.value.child)
        assertEquals(
            listOf<ReferenceComponent.Output>(ReferenceComponent.Output.UseBase(testBaseCode())),
            outputs,
        )

        catalog.setAvailability(CatalogAvailability.Invalid("bad_hash"))
        fixture.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(component.model.value.entries.isEmpty())
        assertEquals(CatalogAvailability.Invalid("bad_hash"), component.model.value.catalogAvailability)
    }
}
