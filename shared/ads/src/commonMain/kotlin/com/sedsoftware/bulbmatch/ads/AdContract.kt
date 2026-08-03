package com.sedsoftware.bulbmatch.ads

import com.sedsoftware.bulbmatch.domain.AdFrequencyState

enum class AdPlacement {
    ResultInline,
    HistorySticky,
    ReferenceSticky,
    MatchExitInterstitial,
}

enum class AdPlatform {
    Android,
    Ios,
}

enum class AdBuildMode {
    PreviewOrTest,
    DebugDevice,
    Release,
}

data class AdUnitSet(
    val resultInline: String,
    val historySticky: String,
    val referenceSticky: String,
    val matchExitInterstitial: String,
) {
    fun id(placement: AdPlacement): String = when (placement) {
        AdPlacement.ResultInline -> resultInline
        AdPlacement.HistorySticky -> historySticky
        AdPlacement.ReferenceSticky -> referenceSticky
        AdPlacement.MatchExitInterstitial -> matchExitInterstitial
    }
}

data class AdConfiguration(
    val enabled: Boolean,
    val units: AdUnitSet?,
    val userConsent: Boolean = false,
    val locationTracking: Boolean = false,
    val ageRestricted: Boolean = false,
) {
    init {
        require(!enabled || units != null)
        require(!userConsent) {
            "BulbMatch's approved AppSpec does not authorize affirmative ad consent."
        }
        require(!locationTracking) {
            "BulbMatch must not enable advertising location signals."
        }
    }
}

object BulbMatchAdConfiguration {
    private val androidProduction = AdUnitSet(
        resultInline = "R-M-19664981-1",
        historySticky = "R-M-19664981-2",
        referenceSticky = "R-M-19664981-3",
        matchExitInterstitial = "R-M-19664981-4",
    )
    private val iosProduction = AdUnitSet(
        resultInline = "R-M-19664982-1",
        historySticky = "R-M-19664982-2",
        referenceSticky = "R-M-19664982-3",
        matchExitInterstitial = "R-M-19664982-4",
    )
    fun forBuild(
        platform: AdPlatform,
        mode: AdBuildMode,
        debugUnits: AdUnitSet? = null,
    ): AdConfiguration = when (mode) {
        AdBuildMode.PreviewOrTest -> AdConfiguration(enabled = false, units = null)
        AdBuildMode.DebugDevice -> AdConfiguration(
            enabled = true,
            units = requireNotNull(debugUnits) {
                "Debug ad units must be supplied by the platform debug build."
            },
        )
        AdBuildMode.Release -> AdConfiguration(
            enabled = true,
            units = when (platform) {
                AdPlatform.Android -> androidProduction
                AdPlatform.Ios -> iosProduction
            }.also(::validateProduction),
        )
    }

    fun validateProduction(units: AdUnitSet) {
        val ids = AdPlacement.entries.map(units::id)
        require(ids.all(String::isNotBlank))
        require(ids.distinct().size == ids.size)
        require(ids.none { it.startsWith("demo-") })
        require(ids.all { it.matches(Regex("R-M-\\d+-\\d+")) })
    }
}

object InterstitialEligibilityPolicy {
    private const val MIN_COOLDOWN_MS = 10L * 60L * 1_000L

    fun isEligible(
        state: AdFrequencyState,
        nowEpochMs: Long,
        resultIsCompatible: Boolean,
        explicitResultExit: Boolean,
        destructiveDialogPending: Boolean,
    ): Boolean {
        if (!resultIsCompatible || !explicitResultExit || destructiveDialogPending) return false
        if (state.completedCompatibleMatches < 2) return false
        val last = state.lastInterstitialEpochMs
        if (last == null) return true
        return state.compatibleMatchesSinceInterstitial >= 3 &&
            nowEpochMs - last >= MIN_COOLDOWN_MS
    }
}

sealed interface AdOutcome {
    data object Impression : AdOutcome
    data object DismissedWithoutImpression : AdOutcome
    data object Unavailable : AdOutcome
    data object LoadFailed : AdOutcome
    data object ShowFailed : AdOutcome
}
