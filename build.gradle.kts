import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.paparazzi).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.sqldelight).apply(false)
    alias(libs.plugins.google.services).apply(false)
    alias(libs.plugins.firebase.crashlytics).apply(false)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.coverage)
}

detekt {
    parallel = true
    buildUponDefaultConfig = true
    baseline = file("$projectDir/detekt/baseline.xml")
    config.setFrom(file("$projectDir/detekt/config.yml"))
    source.setFrom(files("$projectDir/shared", "$projectDir/androidApp"))
    basePath.set(projectDir)
}

tasks.withType<Detekt>().configureEach {
    jvmTarget.set("17")
    exclude("**/build/**")
    exclude("**/com/paddle/ocr/**")
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget.set("17")
    exclude("**/build/**")
    exclude("**/com/paddle/ocr/**")
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.sedsoftware.bulbmatch.domain.*",
                    "com.sedsoftware.bulbmatch.data.*",
                    "com.sedsoftware.bulbmatch.app.*",
                    "com.sedsoftware.bulbmatch.platform.*",
                    "com.sedsoftware.bulbmatch.ads.*",
                )
            }
            excludes {
                classes(
                    "com.sedsoftware.bulbmatch.*.*Fake*",
                    "com.sedsoftware.bulbmatch.*.*Preview*",
                    "com.sedsoftware.bulbmatch.data.db.*",
                    "com.sedsoftware.bulbmatch.platform.Android*",
                    "com.sedsoftware.bulbmatch.platform.Ios*",
                    "com.sedsoftware.bulbmatch.ads.Yandex*",
                )
            }
        }
        total {
            verify {
                rule("Minimum line coverage") {
                    minBound(70)
                }
            }
        }
    }
}

dependencies {
    kover(project(":shared:domain"))
    kover(project(":shared:data"))
    kover(project(":shared:platform"))
    kover(project(":shared:app"))
    kover(project(":shared:ads"))
}
