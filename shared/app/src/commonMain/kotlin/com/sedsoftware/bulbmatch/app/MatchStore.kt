package com.sedsoftware.bulbmatch.app

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.ExperimentalMviKotlinApi
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseAliasIndex
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.Dimmability
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FieldOrigin
import com.sedsoftware.bulbmatch.domain.FixtureMaximumPower
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.Kelvin
import com.sedsoftware.bulbmatch.domain.Lumens
import com.sedsoftware.bulbmatch.domain.ObservedField
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.ShapeCode
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import com.sedsoftware.bulbmatch.domain.Watts
import kotlinx.coroutines.launch

internal interface MatchStore : Store<MatchStore.Intent, MatchStore.State, MatchStore.Label> {
    sealed interface Intent {
        data object StartManual : Intent
        data class StartReferencePrefill(val baseCode: BaseCode) : Intent
        data class StartOcrReview(val observations: List<ObservedField>) : Intent
        data class FieldTextChanged(val field: FieldKey, val value: String) : Intent
        data class KnownBaseSelected(val code: BaseCode) : Intent
        data class UnknownBaseSelected(val rawText: String) : Intent
        data object BaseCleared : Intent
        data class ObservationConfirmed(val field: FieldKey) : Intent
        data class ObservationRejected(val field: FieldKey) : Intent
        data object Assess : Intent
        data object ClearSession : Intent
    }

    data class State(
        val mode: MatchFormComponent.Mode = MatchFormComponent.Mode.Manual,
        val rawValues: Map<FieldKey, String> = emptyMap(),
        val origins: Map<FieldKey, FieldOrigin> = emptyMap(),
        val observations: List<ObservedField> = emptyList(),
        val input: ConfirmedMatchInput = ConfirmedMatchInput(),
        val validationErrors: Map<FieldKey, String> = emptyMap(),
        val assessment: Assessment? = null,
        val completedMatchOrdinal: Int? = null,
        val assessing: Boolean = false,
    ) {
        val hasDraft: Boolean
            get() = observations.isNotEmpty() || rawValues.values.any(String::isNotBlank) || assessment != null
    }

    sealed interface Label {
        data class AssessmentReady(
            val assessment: Assessment,
            val completedMatchOrdinal: Int?,
        ) : Label
    }
}

internal class MatchStoreProvider(
    private val storeFactory: StoreFactory,
    private val compatibilityEngine: CompatibilityEngine,
    private val catalogProvider: CatalogProvider,
    private val settingsRepository: SettingsRepository,
) {
    @OptIn(ExperimentalMviKotlinApi::class)
    fun provide(): MatchStore =
        object : MatchStore,
            Store<MatchStore.Intent, MatchStore.State, MatchStore.Label> by storeFactory.create<
                MatchStore.Intent,
                Nothing,
                Msg,
                MatchStore.State,
                MatchStore.Label,
            >(
                name = "BulbMatchStore",
                initialState = MatchStore.State(),
                executorFactory = coroutineExecutorFactory {
                    onIntent<MatchStore.Intent.StartManual> {
                        dispatch(Msg.SessionStarted(MatchStore.State()))
                    }
                    onIntent<MatchStore.Intent.StartReferencePrefill> { intent ->
                        dispatch(
                            Msg.SessionStarted(
                                MatchStore.State(
                                    mode = MatchFormComponent.Mode.ReferencePrefill,
                                    rawValues = mapOf(FieldKey.Base to intent.baseCode.value),
                                    origins = mapOf(FieldKey.Base to FieldOrigin.Manual),
                                    input = ConfirmedMatchInput(base = ConfirmedBase.Known(intent.baseCode)),
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.StartOcrReview> { intent ->
                        val rawValues = intent.observations
                            .groupBy(ObservedField::fieldKey)
                            .mapValues { (_, values) ->
                                values.firstNotNullOfOrNull(ObservedField::parsedCandidate)
                                    ?: values.first().rawText
                            }
                        val observedKeys = intent.observations.mapTo(linkedSetOf(), ObservedField::fieldKey)
                        dispatch(
                            Msg.SessionStarted(
                                MatchStore.State(
                                    mode = MatchFormComponent.Mode.OcrReview,
                                    rawValues = rawValues,
                                    origins = observedKeys.associateWith { FieldOrigin.Detected },
                                    observations = intent.observations,
                                    input = ConfirmedMatchInput(observationKeys = observedKeys),
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.FieldTextChanged> { intent ->
                        dispatch(
                            Msg.FieldChanged(
                                updateField(
                                    state = state(),
                                    field = intent.field,
                                    raw = intent.value,
                                    catalogAvailability = catalogProvider.availability.value,
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.KnownBaseSelected> { intent ->
                        val state = state()
                        dispatch(
                            Msg.FieldChanged(
                                state.copy(
                                    rawValues = state.rawValues + (FieldKey.Base to intent.code.value),
                                    origins = state.origins + (FieldKey.Base to updatedOrigin(state, FieldKey.Base)),
                                    input = state.input.copy(base = ConfirmedBase.Known(intent.code)),
                                    validationErrors = state.validationErrors - FieldKey.Base,
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.UnknownBaseSelected> { intent ->
                        val state = state()
                        val unknown = ConfirmedBase.Unknown.from(intent.rawText)
                        dispatch(
                            Msg.FieldChanged(
                                state.copy(
                                    rawValues = state.rawValues + (FieldKey.Base to intent.rawText),
                                    origins = state.origins + (FieldKey.Base to updatedOrigin(state, FieldKey.Base)),
                                    input = state.input.copy(base = unknown ?: ConfirmedBase.Missing),
                                    validationErrors = updateError(
                                        state.validationErrors,
                                        FieldKey.Base,
                                        if (unknown == null) "invalid_unknown_base" else null,
                                    ),
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.BaseCleared> {
                        val state = state()
                        dispatch(
                            Msg.FieldChanged(
                                state.copy(
                                    rawValues = state.rawValues - FieldKey.Base,
                                    input = state.input.copy(base = ConfirmedBase.Missing),
                                    validationErrors = state.validationErrors - FieldKey.Base,
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.ObservationConfirmed> { intent ->
                        val state = state()
                        if (intent.field == FieldKey.FixtureMaximumPower) {
                            dispatch(
                                Msg.FieldChanged(
                                    state.copy(
                                        validationErrors = state.validationErrors +
                                            (intent.field to "fixture_maximum_manual_only"),
                                    ),
                                ),
                            )
                        } else {
                            val parsedState = updateField(
                                state = state,
                                field = intent.field,
                                raw = state.rawValues[intent.field].orEmpty(),
                                catalogAvailability = catalogProvider.availability.value,
                            ).copy(
                                origins = state.origins,
                            )
                            val requiredValuePresent = when (intent.field) {
                                FieldKey.Base -> parsedState.input.base != ConfirmedBase.Missing
                                FieldKey.Voltage -> parsedState.input.voltage != VoltageEvidence.Missing
                                else -> true
                            }
                            if (parsedState.validationErrors[intent.field] != null || !requiredValuePresent) {
                                return@onIntent
                            }
                            dispatch(
                                Msg.FieldChanged(
                                    parsedState.copy(
                                        input = parsedState.input.copy(
                                            reviewedFields = parsedState.input.reviewedFields + intent.field,
                                            rejectedObservations = parsedState.input.rejectedObservations - intent.field,
                                        ),
                                        origins = parsedState.origins + (
                                            intent.field to if (state.origins[intent.field] == FieldOrigin.Detected) {
                                                FieldOrigin.Detected
                                            } else {
                                                FieldOrigin.Edited
                                            }
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    onIntent<MatchStore.Intent.ObservationRejected> { intent ->
                        val state = clearField(state(), intent.field)
                        dispatch(
                            Msg.FieldChanged(
                                state.copy(
                                    input = state.input.copy(
                                        reviewedFields = state.input.reviewedFields - intent.field,
                                        rejectedObservations = state.input.rejectedObservations + intent.field,
                                    ),
                                    validationErrors = state.validationErrors - intent.field,
                                ),
                            ),
                        )
                    }
                    onIntent<MatchStore.Intent.Assess> {
                        val current = state()
                        if (current.assessing || current.assessment != null) return@onIntent
                        val catalog = (catalogProvider.availability.value as? CatalogAvailability.Available)?.catalog
                            ?: return@onIntent
                        if (!current.canAssess(catalogProvider.availability.value)) return@onIntent
                        dispatch(Msg.Assessing)
                        launch {
                            val assessment = compatibilityEngine.assess(current.input, catalog.snapshot)
                            val ordinal = if (assessment is Assessment.Compatible) {
                                when (val result = settingsRepository.recordCompatibleMatch()) {
                                    is RepositoryResult.Success -> result.value.completedCompatibleMatches
                                    is RepositoryResult.Failure -> null
                                }
                            } else {
                                null
                            }
                            dispatch(Msg.Assessed(assessment, ordinal))
                            publish(MatchStore.Label.AssessmentReady(assessment, ordinal))
                        }
                    }
                    onIntent<MatchStore.Intent.ClearSession> {
                        dispatch(Msg.SessionStarted(MatchStore.State()))
                    }
                },
                reducer = { msg ->
                    when (msg) {
                        is Msg.SessionStarted -> msg.state
                        is Msg.FieldChanged -> msg.state.copy(assessment = null, completedMatchOrdinal = null)
                        Msg.Assessing -> copy(assessing = true)
                        is Msg.Assessed -> copy(
                            assessment = msg.assessment,
                            completedMatchOrdinal = msg.completedMatchOrdinal,
                            assessing = false,
                        )
                    }
                },
            ) {}

    private sealed interface Msg {
        data class SessionStarted(val state: MatchStore.State) : Msg
        data class FieldChanged(val state: MatchStore.State) : Msg
        data object Assessing : Msg
        data class Assessed(val assessment: Assessment, val completedMatchOrdinal: Int?) : Msg
    }
}

private fun MatchStore.State.canAssess(catalogAvailability: CatalogAvailability): Boolean =
    catalogAvailability is CatalogAvailability.Available &&
        validationErrors.isEmpty() &&
        input.unhandledObservationKeys.isEmpty() &&
        input.base != ConfirmedBase.Missing &&
        input.voltage != VoltageEvidence.Missing

private fun updatedOrigin(
    state: MatchStore.State,
    field: FieldKey,
): FieldOrigin =
    if (field in state.input.observationKeys) FieldOrigin.Edited else FieldOrigin.Manual

private fun updateError(
    errors: Map<FieldKey, String>,
    field: FieldKey,
    error: String?,
): Map<FieldKey, String> =
    if (error == null) errors - field else errors + (field to error)

private fun updateField(
    state: MatchStore.State,
    field: FieldKey,
    raw: String,
    catalogAvailability: CatalogAvailability,
): MatchStore.State {
    val values = state.rawValues + (field to raw)
    val origin = updatedOrigin(state, field)
    var input = state.input
    var error: String? = null
    when (field) {
        FieldKey.Base -> {
            val knownCode = (catalogAvailability as? CatalogAvailability.Available)
                ?.catalog
                ?.entries
                ?.let(BaseAliasIndex::from)
                ?.findExact(raw)
            input = input.copy(
                base = knownCode?.let(ConfirmedBase::Known) ?: ConfirmedBase.Missing,
            )
            error = if (raw.isBlank() || knownCode != null) null else "select_known_or_unknown_base"
        }
        FieldKey.Voltage -> {
            val parsed = raw.parseVoltage()
            input = input.copy(voltage = parsed?.let(VoltageEvidence::Marking) ?: VoltageEvidence.Missing)
            error = if (raw.isBlank() || parsed != null) null else "invalid_voltage"
        }
        FieldKey.Frequency -> {
            val parsed = raw.parseNumber()?.let(FrequencyMarking::from)
            input = input.copy(frequency = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_frequency"
        }
        FieldKey.SourceRatedPower -> {
            val parsed = raw.parseNumber()?.let(Watts::from)
            input = input.copy(sourceRatedPower = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_power"
        }
        FieldKey.PrintedEquivalentPower -> {
            val parsed = raw.parseNumber()?.let(Watts::from)
            input = input.copy(printedEquivalentPower = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_power"
        }
        FieldKey.LuminousFlux -> {
            val parsed = raw.parseNumber()?.let(Lumens::from)
            input = input.copy(luminousFlux = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_lumens"
        }
        FieldKey.ColorTemperature -> {
            val parsed = raw.parseNumber()?.let(Kelvin::from)
            input = input.copy(colorTemperature = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_kelvin"
        }
        FieldKey.Shape -> {
            val parsed = ShapeCode.from(raw)
            input = input.copy(shape = parsed)
            error = if (raw.isBlank() || parsed != null) null else "invalid_shape"
        }
        FieldKey.Dimmability -> {
            val parsed = when (raw.trim().lowercase()) {
                "", "unknown", "неизвестно" -> Dimmability.Unknown
                "yes", "y", "да" -> Dimmability.Yes
                "no", "n", "нет" -> Dimmability.No
                else -> null
            }
            input = input.copy(dimmability = parsed ?: Dimmability.Unknown)
            error = if (parsed == null) "invalid_dimmability" else null
        }
        FieldKey.FixtureMaximumPower -> {
            val parsed = raw.parseNumber()?.let(Watts::from)
            input = input.copy(fixtureMaximumPower = parsed?.let(FixtureMaximumPower::manual))
            error = when {
                field in state.input.observationKeys -> "fixture_maximum_manual_only"
                raw.isBlank() || parsed != null -> null
                else -> "invalid_fixture_power"
            }
        }
    }
    return state.copy(
        rawValues = values,
        origins = state.origins + (field to origin),
        input = input,
        validationErrors = updateError(state.validationErrors, field, error),
    )
}

private fun clearField(
    state: MatchStore.State,
    field: FieldKey,
): MatchStore.State {
    val input = when (field) {
        FieldKey.Base -> state.input.copy(base = ConfirmedBase.Missing)
        FieldKey.Voltage -> state.input.copy(voltage = VoltageEvidence.Missing)
        FieldKey.Frequency -> state.input.copy(frequency = null)
        FieldKey.SourceRatedPower -> state.input.copy(sourceRatedPower = null)
        FieldKey.PrintedEquivalentPower -> state.input.copy(printedEquivalentPower = null)
        FieldKey.LuminousFlux -> state.input.copy(luminousFlux = null)
        FieldKey.ColorTemperature -> state.input.copy(colorTemperature = null)
        FieldKey.Shape -> state.input.copy(shape = null)
        FieldKey.Dimmability -> state.input.copy(dimmability = Dimmability.Unknown)
        FieldKey.FixtureMaximumPower -> state.input.copy(fixtureMaximumPower = null)
    }
    return state.copy(rawValues = state.rawValues - field, input = input)
}

private fun String.parseVoltage(): VoltageMarking? {
    val normalized = trim()
        .replace('–', '-')
        .replace('—', '-')
        .replace('−', '-')
        .replace("volts", "", ignoreCase = true)
        .replace("volt", "", ignoreCase = true)
        .replace("v", "", ignoreCase = true)
        .replace("в", "", ignoreCase = true)
        .trim()
    val parts = normalized.split('-').map(String::trim).filter(String::isNotEmpty)
    return when (parts.size) {
        1 -> parts[0].replace(',', '.').toDoubleOrNull()?.let(VoltageMarking::nominal)
        2 -> {
            val minimum = parts[0].replace(',', '.').toDoubleOrNull() ?: return null
            val maximum = parts[1].replace(',', '.').toDoubleOrNull() ?: return null
            VoltageMarking.range(minimum, maximum)
        }
        else -> null
    }
}

private fun String.parseNumber(): Double? {
    val numeric = trim()
        .replace(',', '.')
        .takeWhile { it.isDigit() || it == '.' || it == '+' || it == '-' }
    return numeric.toDoubleOrNull()
}
