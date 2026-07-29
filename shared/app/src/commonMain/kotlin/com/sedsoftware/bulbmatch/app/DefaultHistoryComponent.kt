package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultHistoryComponent(
    componentContext: ComponentContext,
    private val repository: SavedMatchRepository,
    private val settingsRepository: SettingsRepository,
    private val output: (HistoryComponent.Output) -> Unit,
) : HistoryComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val mutableModel = MutableValue(HistoryComponent.Model(true, emptyList(), false, null, false))
    override val model: Value<HistoryComponent.Model> = mutableModel
    private val detailNavigation = SlotNavigation<DetailConfig>()
    private var observationStarted = false

    private val typedDetailSlot: Value<ChildSlot<DetailConfig, SavedResultComponent>> =
        childSlot(
            source = detailNavigation,
            serializer = DetailConfig.serializer(),
            key = "SavedResultDetail",
            handleBackButton = true,
            childFactory = { config, context ->
                DefaultSavedResultComponent(
                    componentContext = context,
                    id = requireNotNull(SavedMatchId.from(config.id)),
                    repository = repository,
                    onDismiss = detailNavigation::dismiss,
                )
            },
        )
    override val detailSlot: Value<ChildSlot<*, SavedResultComponent>> = typedDetailSlot

    init {
        observeHistory()
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun onRetryRequested() {
        if (!observationStarted) observeHistory()
    }

    override fun onSavedMatchSelected(id: SavedMatchId) {
        output(HistoryComponent.Output.OpenSavedMatch(id))
        detailNavigation.activate(DetailConfig(id.value))
    }

    override fun onDeleteRequested(id: SavedMatchId) {
        mutableModel.update { it.copy(pendingDelete = id) }
    }

    override fun onDeleteConfirmed() {
        val id = mutableModel.value.pendingDelete ?: return
        mutableModel.update { it.copy(pendingDelete = null) }
        scope.launch {
            if (repository.delete(id) is RepositoryResult.Failure) {
                mutableModel.update { it.copy(readError = true) }
            }
        }
    }

    override fun onDeleteCancelled() {
        mutableModel.update { it.copy(pendingDelete = null) }
    }

    override fun onClearAllRequested() {
        mutableModel.update { it.copy(clearAllConfirmationVisible = true) }
    }

    override fun onClearAllConfirmed() {
        mutableModel.update { it.copy(clearAllConfirmationVisible = false) }
        scope.launch {
            val history = repository.clearAll()
            val counters = settingsRepository.resetAdFrequency()
            if (history is RepositoryResult.Failure || counters is RepositoryResult.Failure) {
                mutableModel.update { it.copy(readError = true) }
            }
        }
    }

    override fun onClearAllCancelled() {
        mutableModel.update { it.copy(clearAllConfirmationVisible = false) }
    }

    override fun onStartMatchRequested() = output(HistoryComponent.Output.StartMatch)

    private fun observeHistory() {
        observationStarted = true
        mutableModel.update { it.copy(loading = true, readError = false) }
        repository.observeSummaries()
            .onEach { summaries ->
                mutableModel.update { it.copy(loading = false, summaries = summaries, readError = false) }
            }
            .catch { failure ->
                if (failure is CancellationException) throw failure
                observationStarted = false
                mutableModel.update { it.copy(loading = false, readError = true) }
            }
            .launchIn(scope)
    }

    @Serializable
    private data class DetailConfig(val id: String)
}

private class DefaultSavedResultComponent(
    componentContext: ComponentContext,
    private val id: SavedMatchId,
    private val repository: SavedMatchRepository,
    private val onDismiss: () -> Unit,
) : SavedResultComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val mutableModel = MutableValue(SavedResultComponent.Model(true, null, false, false))
    override val model: Value<SavedResultComponent.Model> = mutableModel

    init {
        load()
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun onBackRequested() = onDismiss()

    override fun onDeleteRequested() {
        mutableModel.update { it.copy(deleteConfirmationVisible = true) }
    }

    override fun onDeleteConfirmed() {
        mutableModel.update { it.copy(deleteConfirmationVisible = false) }
        scope.launch {
            when (repository.delete(id)) {
                is RepositoryResult.Success -> onDismiss()
                is RepositoryResult.Failure -> mutableModel.update { it.copy(unavailable = true) }
            }
        }
    }

    override fun onDeleteCancelled() {
        mutableModel.update { it.copy(deleteConfirmationVisible = false) }
    }

    private fun load() {
        scope.launch {
            when (val result = repository.get(id)) {
                is RepositoryResult.Success ->
                    mutableModel.value = SavedResultComponent.Model(
                        loading = false,
                        savedMatch = result.value,
                        unavailable = result.value == null,
                        deleteConfirmationVisible = false,
                    )
                is RepositoryResult.Failure ->
                    mutableModel.update { it.copy(loading = false, unavailable = true) }
            }
        }
    }
}
