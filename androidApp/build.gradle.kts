import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val hasFirebaseConfiguration = layout.projectDirectory
    .file("google-services.json")
    .asFile
    .isFile
if (hasFirebaseConfiguration) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.sedsoftware.bulbmatch"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "com.sedsoftware.bulbmatch"
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

tasks.configureEach {
    if (name.contains("Release", ignoreCase = true)) {
        dependsOn(":shared:ads:validateProductionAdUnits")
        dependsOn(":shared:ads:validateAdSdkReleaseVersion")
        dependsOn(":shared:data:validateProductionCatalog")
        dependsOn("validateCrashReportingConfiguration")
    }
}

tasks.register("validateCrashReportingConfiguration") {
    group = "verification"
    description = "Blocks release until Firebase platform configuration is supplied."
    inputs.property("hasGoogleServicesJson", hasFirebaseConfiguration)
    doLast {
        val configured = inputs.properties
            .getValue("hasGoogleServicesJson")
            .toString()
            .toBooleanStrict()
        check(configured) {
            "Release blocked: androidApp/google-services.json is required for Crashlytics."
        }
    }
}
