package com.sedsoftware.bulbmatch.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.yandex.mobile.ads.kmp.YandexAds
import com.yandex.mobile.ads.kmp.banner.Banner
import com.yandex.mobile.ads.kmp.banner.BannerAdSize
import com.yandex.mobile.ads.kmp.banner.BannerEvents
import com.yandex.mobile.ads.kmp.banner.rememberBannerAdState
import com.yandex.mobile.ads.kmp.common.AdRequest

/**
 * Call only after the first product content is renderable. Disabled preview/test
 * configurations do not touch the network SDK.
 */
@Composable
fun BulbMatchAdsInitializer(
    configuration: AdConfiguration,
) {
    LaunchedEffect(configuration) {
        if (!configuration.enabled) return@LaunchedEffect
        YandexAds.setUserConsent(configuration.userConsent)
        YandexAds.setLocationTracking(configuration.locationTracking)
        YandexAds.setAgeRestricted(configuration.ageRestricted)
        YandexAds.setAppAdAnalyticsReporting(false)
        YandexAds.initialize()
    }
}

/**
 * The request carries only the approved placement ID. A failed slot is removed
 * from layout and is never retried while this composition remains alive.
 */
@Composable
fun BulbMatchBanner(
    configuration: AdConfiguration,
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    onImpression: () -> Unit = {},
) {
    require(placement != AdPlacement.MatchExitInterstitial)
    val units = configuration.units
    if (!configuration.enabled || units == null) return

    var loadState by remember(configuration, placement) {
        mutableStateOf(BannerLoadState.Loading)
    }
    if (loadState == BannerLoadState.Failed) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth.coerceAtLeast(1.dp)
        val size = when (placement) {
            AdPlacement.ResultInline -> BannerAdSize.Inline(
                width = width,
                maxHeight = 120.dp,
            )
            AdPlacement.HistorySticky,
            AdPlacement.ReferenceSticky,
            -> BannerAdSize.Sticky(width = width)
            AdPlacement.MatchExitInterstitial -> error("Interstitial is not a banner.")
        }
        val events = remember(onImpression) {
            BannerEvents(
                onAdLoaded = { loadState = BannerLoadState.Loaded },
                onAdFailedToLoad = { loadState = BannerLoadState.Failed },
                onImpression = { onImpression() },
            )
        }
        val state = rememberBannerAdState(
            adSize = size,
            events = events,
        )
        val adUnitId = units.id(placement)
        LaunchedEffect(state, adUnitId) {
            state.loadAd(AdRequest(adUnitId = adUnitId))
        }
        Banner(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (loadState == BannerLoadState.Loaded) 1f else 0f),
        )
    }
}

private enum class BannerLoadState {
    Loading,
    Loaded,
    Failed,
}
