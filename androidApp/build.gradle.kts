import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import javax.xml.parsers.DocumentBuilderFactory

abstract class ValidateReleaseManifestTask : DefaultTask() {
    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifestFile.get().asFile)
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val permissions = document.getElementsByTagName("uses-permission")
            .let { nodes ->
                buildSet {
                    for (index in 0 until nodes.length) {
                        add(nodes.item(index).attributes.getNamedItemNS(androidNamespace, "name").nodeValue)
                    }
                }
            }
        val forbidden = setOf(
            "com.google.android.gms.permission.AD_ID",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        )
        check(permissions.intersect(forbidden).isEmpty()) {
            "Release manifest contains forbidden permissions: ${permissions.intersect(forbidden).sorted()}"
        }
        val providers = document.getElementsByTagName("provider")
            .let { nodes ->
                buildSet {
                    for (index in 0 until nodes.length) {
                        add(nodes.item(index).attributes.getNamedItemNS(androidNamespace, "name").nodeValue)
                    }
                }
            }
        val forbiddenProviders = setOf(
            "com.yandex.mobile.ads.features.debugpanel.data.local.DebugPanelFileProvider",
            "io.appmetrica.analytics.internal.PreloadInfoContentProvider",
        )
        check(providers.intersect(forbiddenProviders).isEmpty()) {
            "Release manifest contains forbidden providers: " +
                providers.intersect(forbiddenProviders).sorted()
        }
    }
}

private val demoBannerAdUnitId = "demo-banner-yandex"
private val demoInterstitialAdUnitId = "demo-interstitial-yandex"
private val androidProductionAdUnitIds = mapOf(
    "resultInline" to "R-M-19664981-1",
    "historySticky" to "R-M-19664981-2",
    "referenceSticky" to "R-M-19664981-3",
    "matchExitInterstitial" to "R-M-19664981-4",
)

val bulbMatchVersionName = providers.gradleProperty("bulbMatchVersionName")
    .getOrElse("1.0.0")
val bulbMatchVersionCode = providers.gradleProperty("bulbMatchVersionCode")
    .getOrElse("1000000")
    .toInt()

val releaseSigningValues = mapOf(
    "ANDROID_KEYSTORE_PATH" to providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull,
    "ANDROID_KEYSTORE_PASSWORD" to providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull,
    "ANDROID_KEY_ALIAS" to providers.environmentVariable("ANDROID_KEY_ALIAS").orNull,
    "ANDROID_KEY_PASSWORD" to providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull,
)
val releaseSigningConfigured = releaseSigningValues.values.any { !it.isNullOrBlank() }

check(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$").matches(bulbMatchVersionName)) {
    "bulbMatchVersionName '$bulbMatchVersionName' must be a stable SemVer value such as 1.0.0"
}
check(bulbMatchVersionCode in 1..2_100_000_000) {
    "bulbMatchVersionCode must be between 1 and 2100000000"
}
check(!releaseSigningConfigured || releaseSigningValues.values.all { !it.isNullOrBlank() }) {
    val missingValues = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys.sorted()
    "Incomplete Android release signing configuration. Missing: ${missingValues.joinToString()}"
}

fun quotedBuildConfigValue(value: String): String = "\"$value\""

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val googleServicesFile = layout.projectDirectory.file("google-services.json")
val hasFirebaseConfiguration = googleServicesFile.asFile.isFile
if (hasFirebaseConfiguration) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.sedsoftware.bulbmatch"
    compileSdk = 36

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 36

        applicationId = "com.sedsoftware.bulbmatch"
        versionCode = bulbMatchVersionCode
        versionName = bulbMatchVersionName
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseSigningValues.getValue("ANDROID_KEYSTORE_PATH")!!)
                storePassword = releaseSigningValues.getValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("ANDROID_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "YANDEX_RESULT_INLINE_AD_UNIT_ID",
                quotedBuildConfigValue(demoBannerAdUnitId),
            )
            buildConfigField(
                "String",
                "YANDEX_HISTORY_STICKY_AD_UNIT_ID",
                quotedBuildConfigValue(demoBannerAdUnitId),
            )
            buildConfigField(
                "String",
                "YANDEX_REFERENCE_STICKY_AD_UNIT_ID",
                quotedBuildConfigValue(demoBannerAdUnitId),
            )
            buildConfigField(
                "String",
                "YANDEX_MATCH_EXIT_INTERSTITIAL_AD_UNIT_ID",
                quotedBuildConfigValue(demoInterstitialAdUnitId),
            )
        }
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            buildConfigField(
                "String",
                "YANDEX_RESULT_INLINE_AD_UNIT_ID",
                quotedBuildConfigValue(androidProductionAdUnitIds.getValue("resultInline")),
            )
            buildConfigField(
                "String",
                "YANDEX_HISTORY_STICKY_AD_UNIT_ID",
                quotedBuildConfigValue(androidProductionAdUnitIds.getValue("historySticky")),
            )
            buildConfigField(
                "String",
                "YANDEX_REFERENCE_STICKY_AD_UNIT_ID",
                quotedBuildConfigValue(androidProductionAdUnitIds.getValue("referenceSticky")),
            )
            buildConfigField(
                "String",
                "YANDEX_MATCH_EXIT_INTERSTITIAL_AD_UNIT_ID",
                quotedBuildConfigValue(androidProductionAdUnitIds.getValue("matchExitInterstitial")),
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val validateReleaseMergedManifest = tasks.register<ValidateReleaseManifestTask>(
    "validateReleaseMergedManifest",
) {
    group = "verification"
    description = "Rejects advertising ID, location, and broad media permissions in release."
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        validateReleaseMergedManifest.configure {
            manifestFile.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared:compose"))
    implementation(project(":shared:app"))
    implementation(project(":shared:data"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:platform"))
    implementation(project(":shared:ads"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.decompose)
    implementation(libs.mvikotlin)
    implementation(libs.mvikotlin.main)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.multiplatformSettings)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.compose.material3)
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
        dependsOn(":shared:ads:validateProductionAdUnits")
        dependsOn(":shared:ads:validateAdSdkReleaseVersion")
        dependsOn(":shared:data:validateProductionCatalog")
        dependsOn("validateCrashReportingConfiguration")
        dependsOn("validateReleaseMergedManifest")
}

tasks.register("validateCrashReportingConfiguration") {
    group = "verification"
    description = "Blocks release until Firebase platform configuration is supplied."
    inputs.property("hasGoogleServicesJson", hasFirebaseConfiguration)
    inputs.file(googleServicesFile).optional()
    doLast {
        val configured = inputs.properties
            .getValue("hasGoogleServicesJson")
            .toString()
            .toBooleanStrict()
        check(configured) {
            "Release blocked: androidApp/google-services.json is required for Crashlytics."
        }
        val configuration = inputs.files.singleFile.readText()
        check(
            Regex("\"package_name\"\\s*:\\s*\"com\\.sedsoftware\\.bulbmatch\"")
                .containsMatchIn(configuration),
        ) {
            "Release blocked: google-services.json has no com.sedsoftware.bulbmatch client."
        }
    }
}
