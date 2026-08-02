import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
}

val ocrReleaseFiles = listOf(
    Triple(
        "src/androidMain/assets/Models/det/inference.onnx",
        "a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d",
        4_826_518L,
    ),
    Triple(
        "src/androidMain/assets/Models/rec/inference.onnx",
        "b3018ef2b09a0250b6e0c8e871c927098363e5fd4df890cc68e8358eb0aaf1bd",
        7_887_627L,
    ),
    Triple(
        "src/androidMain/assets/Models/det/inference.yml",
        "98069072e1b6b37d727fd9d9f11725faa46d6ea0de012f2ed26caea011c37699",
        903L,
    ),
    Triple(
        "src/androidMain/assets/Models/rec/inference.yml",
        "025039bac23eb4a308efcefa4d58eab3af440767815c6ba6938468bf6353ee5a",
        4_538L,
    ),
)

val validateOcrModelRelease by tasks.registering {
    group = "verification"
    description = "Validates bundled PaddleOCR assets, manifest, platform floor, and ML Kit removal."
    notCompatibleWithConfigurationCache(
        "The release validator intentionally reads and hashes external binary assets at execution time.",
    )

    val manifest = layout.projectDirectory.file("ocr-model-manifest.json")
    inputs.file(manifest)
    inputs.files(ocrReleaseFiles.map { layout.projectDirectory.file(it.first) })

    doLast {
        val manifestText = manifest.asFile.readText()
        ocrReleaseFiles.forEach { (relativePath, expectedHash, expectedBytes) ->
            val asset = layout.projectDirectory.file(relativePath).asFile
            check(asset.isFile) { "Missing OCR release asset: $relativePath" }
            check(asset.length() == expectedBytes) {
                "Unexpected OCR asset size for $relativePath: ${asset.length()} != $expectedBytes"
            }
            val actualHash = MessageDigest.getInstance("SHA-256")
                .digest(asset.readBytes())
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            check(actualHash == expectedHash) {
                "Unexpected OCR asset SHA-256 for $relativePath: $actualHash"
            }
            check(manifestText.contains("\"bundledPath\": \"$relativePath\"")) {
                "OCR manifest does not name $relativePath"
            }
            check(manifestText.contains("\"sha256\": \"$expectedHash\"")) {
                "OCR manifest does not contain the expected hash for $relativePath"
            }
        }

        val versionCatalog = rootProject.file("gradle/libs.versions.toml").readText()
        check(versionCatalog.contains("android-minSdk = \"26\"")) {
            "PaddleOCR Android release requires minSdk 26"
        }
        check(!versionCatalog.contains("com.google.mlkit:text-recognition")) {
            "ML Kit Android dependency must be removed after PaddleOCR migration"
        }
        check(!rootProject.file("iosApp/Podfile").readText().contains("GoogleMLKit/TextRecognition")) {
            "ML Kit iOS pod must be removed after PaddleOCR migration"
        }
    }
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(validateOcrModelRelease)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.platform"
        compileSdk = 36
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
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
            implementation(libs.onnxruntime.android)
            implementation(libs.opencv.android)
            implementation(libs.firebase.crashlytics)
            implementation(libs.kotlinx.coroutines.android)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
            implementation(libs.test.junit4)
        }
    }
}
