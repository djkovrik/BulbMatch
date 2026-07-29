package com.sedsoftware.bulbmatch.ads

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdContractTest {
    @Test
    fun previewAndTestsNeverInitializeNetworkSdk() {
        assertFalse(
            BulbMatchAdConfiguration.forBuild(
                AdPlatform.Android,
                AdBuildMode.PreviewOrTest,
            ).enabled,
        )
    }

    @Test
    fun releaseUsesOnlyProductionShapedIds() {
        BulbMatchAdConfiguration.forBuild(
            AdPlatform.Android,
            AdBuildMode.Release,
        )
        BulbMatchAdConfiguration.forBuild(
            AdPlatform.Ios,
            AdBuildMode.Release,
        )
    }

    @Test
    fun interstitialRequiresAllApprovedConditions() {
        assertTrue(
            InterstitialEligibilityPolicy.isEligible(
                state = AdFrequencyState(
                    completedCompatibleMatches = 2,
                    lastInterstitialEpochMs = null,
                    compatibleMatchesSinceInterstitial = 2,
                ),
                nowEpochMs = 10L,
                resultIsCompatible = true,
                explicitResultExit = true,
                destructiveDialogPending = false,
            ),
        )
        val eligible = AdFrequencyState(
            completedCompatibleMatches = 3,
            lastInterstitialEpochMs = 1_000L,
            compatibleMatchesSinceInterstitial = 3,
        )
        assertTrue(
            InterstitialEligibilityPolicy.isEligible(
                state = eligible,
                nowEpochMs = 601_000L,
                resultIsCompatible = true,
                explicitResultExit = true,
                destructiveDialogPending = false,
            ),
        )
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                state = eligible,
                nowEpochMs = 600_999L,
                resultIsCompatible = true,
                explicitResultExit = true,
                destructiveDialogPending = false,
            ),
        )
        assertFalse(
            InterstitialEligibilityPolicy.isEligible(
                state = eligible,
                nowEpochMs = 601_000L,
                resultIsCompatible = false,
                explicitResultExit = true,
                destructiveDialogPending = false,
            ),
        )
    }
}
