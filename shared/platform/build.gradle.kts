import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.platform"
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
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity)
            implementation(libs.androidx.core)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.firebase.crashlytics)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
