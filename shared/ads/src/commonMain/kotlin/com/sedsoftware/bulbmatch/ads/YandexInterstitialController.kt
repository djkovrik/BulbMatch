package com.sedsoftware.bulbmatch.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yandex.mobile.ads.kmp.common.AdError
import com.yandex.mobile.ads.kmp.common.AdRequest
import com.yandex.mobile.ads.kmp.common.ImpressionData
import com.yandex.mobile.ads.kmp.compose.rememberInterstitialAdLoader
import com.yandex.mobile.ads.kmp.interstitial.InterstitialAd
import com.yandex.mobile.ads.kmp.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.kmp.interstitial.InterstitialAdLoader
import kotlinx.coroutines.CancellationException

class BulbMatchInterstitialController internal constructor(
    private val configuration: AdConfiguration,
    private val loader: InterstitialAdLoader,
) {
    private var loadedAd: InterstitialAd? = null

    suspend fun preload() {
        val units = configuration.units
        if (!configuration.enabled || units == null || loadedAd != null) return
        loadedAd = try {
            loader.loadAd(
                AdRequest(
                    adUnitId = units.id(AdPlacement.MatchExitInterstitial),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Completes the already-requested navigation on every path. The impression
     * callback is separate so persistence counters reset only after that event.
     */
    fun showOrContinue(
        onImpression: () -> Unit,
        onComplete: (AdOutcome) -> Unit,
    ) {
        val ad = loadedAd
        loadedAd = null
        if (ad == null) {
            onComplete(AdOutcome.Unavailable)
            return
        }
        var impressionRecorded = false
        var completed = false
        val completeOnce: (AdOutcome) -> Unit = { outcome ->
            if (!completed) {
                completed = true
                onComplete(outcome)
            }
        }
        ad.setAdEventListener(
            object : InterstitialAdEventListener {
                override fun onAdShown() = Unit

                override fun onAdFailedToShow(adError: AdError) {
                    ad.setAdEventListener(null)
                    completeOnce(AdOutcome.ShowFailed)
                }

                override fun onAdDismissed() {
                    ad.setAdEventListener(null)
                    completeOnce(
                        if (impressionRecorded) {
                            AdOutcome.Impression
                        } else {
                            AdOutcome.DismissedWithoutImpression
                        },
                    )
                }

                override fun onAdClicked() = Unit

                override fun onAdImpression(impressionData: ImpressionData?) {
                    if (!impressionRecorded) {
                        impressionRecorded = true
                        onImpression()
                    }
                }
            },
        )
        try {
            ad.show()
        } catch (_: Exception) {
            ad.setAdEventListener(null)
            completeOnce(AdOutcome.ShowFailed)
        }
    }

    fun cancelLoading() {
        loader.cancelLoading()
    }
}

@Composable
fun rememberBulbMatchInterstitialController(
    configuration: AdConfiguration,
): BulbMatchInterstitialController {
    val loader = rememberInterstitialAdLoader()
    return remember(configuration, loader) {
        BulbMatchInterstitialController(
            configuration = configuration,
            loader = loader,
        )
    }
}
