package com.sedsoftware.bulbmatch.ads

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var entranceAnimationPlayed by rememberSaveable(configuration, placement) {
        mutableStateOf(false)
    }
    // Lazy layouts retain saveable item state while an ad scrolls out of composition.
    // Capture the decision for this composition so finishing the fade cannot remove
    // animateContentSize while its size spring is still settling.
    val animateEntrance = remember(configuration, placement) {
        !entranceAnimationPlayed
    }
    if (loadState == BannerLoadState.Failed) return

    val bannerAlpha by animateFloatAsState(
        targetValue = if (!animateEntrance || loadState == BannerLoadState.Loaded) 1f else 0f,
        animationSpec = if (animateEntrance) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        } else {
            snap()
        },
        finishedListener = {
            if (loadState == BannerLoadState.Loaded) entranceAnimationPlayed = true
        },
        label = "banner_appearance",
    )
    val entranceModifier = if (animateEntrance) {
        Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            alignment = when (placement) {
                AdPlacement.ResultInline -> Alignment.TopCenter
                AdPlacement.HistorySticky,
                AdPlacement.ReferenceSticky,
                -> Alignment.BottomCenter
                AdPlacement.MatchExitInterstitial -> error("Interstitial is not a banner.")
            },
        )
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().then(entranceModifier)) {
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
                .alpha(bannerAlpha),
        )
    }
}

private enum class BannerLoadState {
    Loading,
    Loaded,
    Failed,
}
