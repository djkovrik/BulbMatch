import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.ads"
        compileSdk = 36
        minSdk = 24
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
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

tasks.register("validateAdSdkReleaseVersion") {
    group = "verification"
    description = "Blocks release while the documented Yandex CMP 8.2.0 artifact is unavailable."
    inputs.properties(
        mapOf(
            "documentedVersion" to "8.2.0",
            "resolvedVersion" to libs.versions.yandex.mobile.ads.kmp.get(),
        ),
    )
    doLast {
        val documented = inputs.properties.getValue("documentedVersion").toString()
        val resolved = inputs.properties.getValue("resolvedVersion").toString()
        check(resolved == documented) {
            "Release blocked: Yandex documents CMP $documented, but Maven Central currently resolves only $resolved."
        }
    }
}
