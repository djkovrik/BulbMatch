package com.sedsoftware.bulbmatch.ads

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun debugUsesOnlyPlatformSuppliedTestIds() {
        val units = AdUnitSet(
            resultInline = "test-result",
            historySticky = "test-history",
            referenceSticky = "test-reference",
            matchExitInterstitial = "test-exit",
        )
        val configuration = BulbMatchAdConfiguration.forBuild(
            platform = AdPlatform.Android,
            mode = AdBuildMode.DebugDevice,
            debugUnits = units,
        )

        assertTrue(configuration.enabled)
        assertEquals(units, configuration.units)
    }

    @Test
    fun releaseUsesOnlyProductionShapedIds() {
        val android = BulbMatchAdConfiguration.forBuild(
            AdPlatform.Android,
            AdBuildMode.Release,
        )
        val ios = BulbMatchAdConfiguration.forBuild(
            AdPlatform.Ios,
            AdBuildMode.Release,
        )
        assertEquals(
            listOf(
                "R-M-19664981-1",
                "R-M-19664981-2",
                "R-M-19664981-3",
                "R-M-19664981-4",
            ),
            AdPlacement.entries.map(requireNotNull(android.units)::id),
        )
        assertEquals(
            listOf(
                "R-M-19664982-1",
                "R-M-19664982-2",
                "R-M-19664982-3",
                "R-M-19664982-4",
            ),
            AdPlacement.entries.map(requireNotNull(ios.units)::id),
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
