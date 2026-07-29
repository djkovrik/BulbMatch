package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterstitialEligibilityPolicyTest {
    @Test
    fun firstCompatibleMatchIsNeverEligible() {
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 1,
                    frequency = AdFrequencyState(1, null, 3),
                ),
            ),
        )
    }

    @Test
    fun compatibleResultRequiresThreeMatchesSinceImpression() {
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 4,
                    frequency = AdFrequencyState(4, 1_000L, 2),
                ),
            ),
        )
        assertTrue(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 4,
                    frequency = AdFrequencyState(4, 1_000L, 3),
                    now = 1_000L + InterstitialEligibilityPolicy.COOLDOWN_MILLIS,
                ),
            ),
        )
    }

    @Test
    fun firstPossibleImpressionIsSecondCompatibleMatch() {
        assertTrue(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 2,
                    frequency = AdFrequencyState(2, null, 2),
                ),
            ),
        )
    }

    @Test
    fun cooldownIsInclusiveAtTenMinutes() {
        val last = 1_000L
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 5,
                    frequency = AdFrequencyState(5, last, 3),
                    now = last + InterstitialEligibilityPolicy.COOLDOWN_MILLIS - 1,
                ),
            ),
        )
        assertTrue(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(
                    ordinal = 5,
                    frequency = AdFrequencyState(5, last, 3),
                    now = last + InterstitialEligibilityPolicy.COOLDOWN_MILLIS,
                ),
            ),
        )
    }

    @Test
    fun conflictsSavingAndDialogsAreNeverEligible() {
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                eligibleInput(outcome = AssessmentOutcome.PotentialConflict),
            ),
        )
        assertFalse(InterstitialEligibilityPolicy.isEligible(eligibleInput(saveInProgress = true)))
        assertFalse(InterstitialEligibilityPolicy.isEligible(eligibleInput(pendingDialog = true)))
        assertFalse(InterstitialEligibilityPolicy.isEligible(eligibleInput(explicitExit = false)))
    }

    private fun eligibleInput(
        outcome: AssessmentOutcome = AssessmentOutcome.Compatible,
        ordinal: Int = 3,
        frequency: AdFrequencyState = AdFrequencyState(3, null, 3),
        now: Long = 1_000_000L,
        explicitExit: Boolean = true,
        saveInProgress: Boolean = false,
        pendingDialog: Boolean = false,
    ) = InterstitialEligibilityInput(
        outcome = outcome,
        completedMatchOrdinal = ordinal,
        frequency = frequency,
        nowEpochMs = now,
        explicitResultExit = explicitExit,
        saveInProgress = saveInProgress,
        pendingDestructiveDialog = pendingDialog,
    )
}
