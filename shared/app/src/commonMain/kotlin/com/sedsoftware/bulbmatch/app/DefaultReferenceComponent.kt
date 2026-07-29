package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable

internal class DefaultReferenceComponent(
    componentContext: ComponentContext,
    private val catalogProvider: CatalogProvider,
    private val output: (ReferenceComponent.Output) -> Unit,
) : ReferenceComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val mutableModel = MutableValue(buildModel(""))
    override val model: Value<ReferenceComponent.Model> = mutableModel
    private val detailNavigation = SlotNavigation<DetailConfig>()

    private val typedDetailSlot: Value<ChildSlot<DetailConfig, ReferenceDetailComponent>> =
        childSlot(
            source = detailNavigation,
            serializer = null,
            key = "ReferenceDetail",
            handleBackButton = true,
            childFactory = { config, _ ->
                val code = requireNotNull(BaseCode.from(config.code))
                val entry = requireNotNull(catalogProvider.entry(code))
                object : ReferenceDetailComponent {
                    override val entry = entry
                    override fun onBackRequested() = detailNavigation.dismiss()
                    override fun onUseBaseRequested() {
                        detailNavigation.dismiss()
                        output(ReferenceComponent.Output.UseBase(code))
                    }
                }
            },
        )
    override val detailSlot: Value<ChildSlot<*, ReferenceDetailComponent>> = typedDetailSlot

    init {
        catalogProvider.availability
            .onEach { mutableModel.value = buildModel(mutableModel.value.query) }
            .launchIn(scope)
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun onQueryChanged(query: String) {
        mutableModel.value = buildModel(query)
    }

    override fun onEntrySelected(code: BaseCode) {
        detailNavigation.activate(DetailConfig(code.value))
    }

    override fun onClearQueryRequested() {
        mutableModel.value = buildModel("")
    }

    private fun buildModel(query: String): ReferenceComponent.Model =
        ReferenceComponent.Model(
            query = query,
            entries = catalogProvider.searchEntries(query),
            catalogAvailability = catalogProvider.availability.value,
        )

    @Serializable
    private data class DetailConfig(val code: String)
}
