package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val catalogProvider: CatalogProvider,
    private val settingsRepository: SettingsRepository,
    private val savedMatchRepository: SavedMatchRepository,
    private val output: (SettingsComponent.Output) -> Unit,
    private val onDismiss: () -> Unit,
) : SettingsComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val mutableModel = MutableValue(
        SettingsComponent.Model(
            locale = settingsRepository.localeOverride.value,
            theme = settingsRepository.themeOverride.value,
            clearing = false,
            clearConfirmationVisible = false,
            errorCode = null,
            catalogAvailability = catalogProvider.availability.value,
        ),
    )
    override val model: Value<SettingsComponent.Model> = mutableModel

    init {
        settingsRepository.localeOverride
            .onEach { locale -> mutableModel.update { it.copy(locale = locale) } }
            .launchIn(scope)
        settingsRepository.themeOverride
            .onEach { theme -> mutableModel.update { it.copy(theme = theme) } }
            .launchIn(scope)
        catalogProvider.availability
            .onEach { catalog -> mutableModel.update { it.copy(catalogAvailability = catalog) } }
            .launchIn(scope)
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun onLanguageSelected(value: LocaleOverride) {
        scope.launch {
            when (settingsRepository.setLocaleOverride(value)) {
                is RepositoryResult.Success -> output(SettingsComponent.Output.LanguageChanged(value))
                is RepositoryResult.Failure -> mutableModel.update { it.copy(errorCode = "language_write_failed") }
            }
        }
    }

    override fun onThemeSelected(value: ThemeOverride) {
        scope.launch {
            when (settingsRepository.setThemeOverride(value)) {
                is RepositoryResult.Success -> output(SettingsComponent.Output.ThemeChanged(value))
                is RepositoryResult.Failure -> mutableModel.update { it.copy(errorCode = "theme_write_failed") }
            }
        }
    }

    override fun onClearLocalDataRequested() {
        mutableModel.update { it.copy(clearConfirmationVisible = true, errorCode = null) }
    }

    override fun onClearLocalDataConfirmed() {
        if (mutableModel.value.clearing) return
        mutableModel.update { it.copy(clearing = true, clearConfirmationVisible = false, errorCode = null) }
        scope.launch {
            val history = savedMatchRepository.clearAll()
            val counters = settingsRepository.resetAdFrequency()
            if (history is RepositoryResult.Success && counters is RepositoryResult.Success) {
                mutableModel.update { it.copy(clearing = false) }
                output(SettingsComponent.Output.LocalDataCleared)
            } else {
                mutableModel.update { it.copy(clearing = false, errorCode = "clear_local_data_failed") }
            }
        }
    }

    override fun onClearLocalDataCancelled() {
        mutableModel.update { it.copy(clearConfirmationVisible = false) }
    }

    override fun onBackRequested() = onDismiss()
}
