package com.sedsoftware.bulbmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.arkivanov.decompose.retainedComponent
import com.sedsoftware.bulbmatch.ads.AdBuildMode
import com.sedsoftware.bulbmatch.ads.AdOutcome
import com.sedsoftware.bulbmatch.ads.AdPlacement
import com.sedsoftware.bulbmatch.ads.AdPlatform
import com.sedsoftware.bulbmatch.ads.AdUnitSet
import com.sedsoftware.bulbmatch.ads.BulbMatchAdConfiguration
import com.sedsoftware.bulbmatch.ads.BulbMatchAdsInitializer
import com.sedsoftware.bulbmatch.ads.BulbMatchBanner
import com.sedsoftware.bulbmatch.ads.rememberBulbMatchInterstitialController
import com.sedsoftware.bulbmatch.compose.App
import com.sedsoftware.bulbmatch.compose.BulbMatchSlots
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class AppActivity : ComponentActivity() {
    private lateinit var rootHolder: AndroidRootHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        rootHolder = retainedComponent {
            AndroidRootHolder.create(it, this)
        }
        rootHolder.bridge.attach(this)
        setContent {
            AndroidBulbMatchApp(rootHolder)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::rootHolder.isInitialized) rootHolder.bridge.onForegroundResume()
    }

    override fun onDestroy() {
        if (::rootHolder.isInitialized) rootHolder.bridge.detach(this)
        super.onDestroy()
    }
}

@Composable
private fun AppActivity.AndroidBulbMatchApp(holder: AndroidRootHolder) {
    val configuration = remember {
        BulbMatchAdConfiguration.forBuild(
            platform = AdPlatform.Android,
            mode = if (BuildConfig.DEBUG) {
                AdBuildMode.DebugDevice
            } else {
                AdBuildMode.Release
            },
            debugUnits = if (BuildConfig.DEBUG) {
                AdUnitSet(
                    resultInline = BuildConfig.YANDEX_RESULT_INLINE_AD_UNIT_ID,
                    historySticky = BuildConfig.YANDEX_HISTORY_STICKY_AD_UNIT_ID,
                    referenceSticky = BuildConfig.YANDEX_REFERENCE_STICKY_AD_UNIT_ID,
                    matchExitInterstitial = BuildConfig.YANDEX_MATCH_EXIT_INTERSTITIAL_AD_UNIT_ID,
                )
            } else {
                null
            },
        )
    }
    val interstitial = rememberBulbMatchInterstitialController(configuration)
    val scope = rememberCoroutineScope()
    BulbMatchAdsInitializer(configuration)
    LaunchedEffect(interstitial) {
        interstitial.preload()
    }
    DisposableEffect(holder.bridge, interstitial) {
        holder.bridge.interstitialPresenter = { completion ->
            interstitial.showOrContinue(
                onImpression = {},
                onComplete = { outcome ->
                    completion(outcome == AdOutcome.Impression)
                    scope.launch { interstitial.preload() }
                },
            )
        }
        onDispose {
            holder.bridge.interstitialPresenter = null
            interstitial.cancelLoading()
        }
    }

    App(
        root = holder.root,
        onThemeChanged = { ThemeChanged(it) },
        slots = BulbMatchSlots(
            cameraPreview = { AndroidCameraPreview(holder.bridge) },
            imagePreview = { AndroidImagePreview(it) },
            resultBanner = {
                BulbMatchBanner(configuration, AdPlacement.ResultInline)
            },
            historyBanner = {
                BulbMatchBanner(configuration, AdPlacement.HistorySticky)
            },
            referenceBanner = {
                BulbMatchBanner(configuration, AdPlacement.ReferenceSticky)
            },
        ),
        formatEpochMillis = { epochMillis ->
            DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                resources.configuration.locales[0] ?: Locale.getDefault(),
            ).format(Date(epochMillis))
        },
        onOpenPrivacyPolicy = {
            openExternalUri("https://sedsoftware.com/apps/bulbmatch/policy.html")
        },
        onOpenSourceSummary = {},
        onEmailSupport = { openExternalUri("mailto:info@sedsoftware.com") },
    )
}

private fun AppActivity.openExternalUri(value: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
