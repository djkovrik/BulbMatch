import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.ads"
        compileSdk = 36
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(compose.animation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.yandex.mobile.ads.compose)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.register("validateProductionAdUnits") {
    group = "verification"
    description = "Validates the eight AppSpec-approved production Yandex ad units."
    inputs.properties(
        mapOf(
            "android.resultInline" to "R-M-19664981-1",
            "android.historySticky" to "R-M-19664981-2",
            "android.referenceSticky" to "R-M-19664981-3",
            "android.matchExitInterstitial" to "R-M-19664981-4",
            "ios.resultInline" to "R-M-19664982-1",
            "ios.historySticky" to "R-M-19664982-2",
            "ios.referenceSticky" to "R-M-19664982-3",
            "ios.matchExitInterstitial" to "R-M-19664982-4",
        ),
    )
    doLast {
        val units = inputs.properties.mapValues { it.value.toString() }
        check(units.size == 8)
        check(units.values.distinct().size == 8)
        units.forEach { (key, value) ->
            check(value.isNotBlank()) { "$key is blank." }
            check(!value.startsWith("demo-")) { "$key uses a provider test ID." }
            check(Regex("R-M-\\d+-\\d+").matches(value)) {
                "$key does not match the approved Yandex production ID shape."
            }
        }
    }
}

val approvedAdSdkVersion = "8.1.0"
val appSpecVersion = providers.fileContents(
    rootProject.layout.projectDirectory.file("spec/app-spec/app-spec.json"),
).asText.map { contents ->
    Regex("\"sdkVersion\"\\s*:\\s*\"([^\"]+)\"")
        .find(contents)
        ?.groupValues
        ?.get(1)
        ?: "no version"
}
val iosPodfileVersion = providers.fileContents(
    rootProject.layout.projectDirectory.file("iosApp/Podfile"),
).asText.map { contents ->
    Regex("pod 'YandexMobileAds', '([^']+)'")
        .find(contents)
        ?.groupValues
        ?.get(1)
        ?: "no version"
}
val iosPodfileLockVersion = providers.fileContents(
    rootProject.layout.projectDirectory.file("iosApp/Podfile.lock"),
).asText.map { contents ->
    Regex("- YandexMobileAds \\(([^)]+)\\):")
        .find(contents)
        ?.groupValues
        ?.get(1)
        ?: "no version"
}

tasks.register("validateAdSdkReleaseVersion") {
    group = "verification"
    description = "Validates the approved Yandex CMP/native SDK version across AppSpec, Gradle, and CocoaPods."
    inputs.property("approvedVersion", approvedAdSdkVersion)
    inputs.property("gradleVersion", libs.versions.yandex.mobile.ads.kmp.get())
    inputs.property("appSpecVersion", appSpecVersion)
    inputs.property("iosPodfileVersion", iosPodfileVersion)
    inputs.property("iosPodfileLockVersion", iosPodfileLockVersion)
    doLast {
        val approved = inputs.properties.getValue("approvedVersion").toString()

        val configuredVersions = mapOf(
            "AppSpec" to inputs.properties.getValue("appSpecVersion").toString(),
            "Gradle" to inputs.properties.getValue("gradleVersion").toString(),
            "Podfile" to inputs.properties.getValue("iosPodfileVersion").toString(),
            "Podfile.lock" to inputs.properties.getValue("iosPodfileLockVersion").toString(),
        )
        configuredVersions.forEach { (source, version) ->
            check(version == approved) {
                "Release blocked: approved Yandex Ads SDK is $approved, but $source configures $version."
            }
        }
    }
}
