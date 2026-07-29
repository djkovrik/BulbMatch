package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome

data class InterstitialEligibilityInput(
    val outcome: AssessmentOutcome,
    val completedMatchOrdinal: Int?,
    val frequency: AdFrequencyState,
    val nowEpochMs: Long,
    val explicitResultExit: Boolean,
    val saveInProgress: Boolean,
    val pendingDestructiveDialog: Boolean,
)

object InterstitialEligibilityPolicy {
    const val MINIMUM_COMPLETED_ORDINAL = 2
    const val MATCHES_PER_IMPRESSION = 3
    const val COOLDOWN_MILLIS = 10 * 60 * 1_000L

    fun isEligible(input: InterstitialEligibilityInput): Boolean {
        if (input.outcome != AssessmentOutcome.Compatible) return false
        if (!input.explicitResultExit || input.saveInProgress || input.pendingDestructiveDialog) return false
        if ((input.completedMatchOrdinal ?: 0) < MINIMUM_COMPLETED_ORDINAL) return false
        val last = input.frequency.lastInterstitialEpochMs ?: return true
        if (input.frequency.compatibleMatchesSinceInterstitial < MATCHES_PER_IMPRESSION) return false
        return input.nowEpochMs - last >= COOLDOWN_MILLIS
    }
}
