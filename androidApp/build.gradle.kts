import com.android.build.api.artifact.SingleArtifact
import com.android.bundle.Config.BundleConfig
import com.android.bundle.Config.UncompressNativeLibraries.PageAlignment
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
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

abstract class ValidateNativeLibraryPageSizeTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val archiveFile: RegularFileProperty

    @get:Input
    abstract val minimumPageSizeBytes: Property<Long>

    @TaskAction
    fun validate() {
        val minimumAlignment = minimumPageSizeBytes.get()
        val failures = mutableListOf<String>()
        var nativeLibraryCount = 0

        ZipFile(archiveFile.get().asFile).use { archive ->
            val entries = archive.entries().asSequence()
                .filter {
                    !it.isDirectory &&
                        it.name.endsWith(".so") &&
                        abiFromNativeLibraryPath(it.name) in SUPPORTED_16_KB_ABIS
                }
                .toList()
            val packagedAbis = entries.mapNotNull { abiFromNativeLibraryPath(it.name) }.toSet()
            check(packagedAbis == SUPPORTED_16_KB_ABIS) {
                "Archive must contain native libraries for all 16 KB-capable ABIs: " +
                    "expected=$SUPPORTED_16_KB_ABIS, actual=$packagedAbis"
            }

            entries.forEach { entry ->
                nativeLibraryCount += 1
                val alignments = archive.getInputStream(entry).use { stream ->
                    elfLoadAlignments(stream.readBytes(), entry.name)
                }
                val invalid = alignments.filter { it < minimumAlignment }
                if (invalid.isNotEmpty()) {
                    failures += "${entry.name}: LOAD alignments=${alignments.joinToString()}"
                }
            }
        }

        check(failures.isEmpty()) {
            "Native libraries are not compatible with ${minimumAlignment / 1024} KB pages:\n" +
                failures.joinToString("\n")
        }
        logger.lifecycle(
            "Validated $nativeLibraryCount native libraries for ${minimumAlignment / 1024} KB ELF alignment",
        )
    }

    private fun abiFromNativeLibraryPath(path: String): String? {
        val segments = path.split('/')
        val libIndex = segments.indexOf("lib")
        if (libIndex < 0) return null
        return segments.getOrNull(libIndex + 1)
    }

    private fun elfLoadAlignments(bytes: ByteArray, entryName: String): List<Long> {
        val format = readElfFormat(bytes, entryName)
        val buffer = ByteBuffer.wrap(bytes).order(format.byteOrder)
        val programHeaderOffset = readProgramHeaderOffset(buffer, format.is64Bit)
        val programHeaderEntrySize = readUnsignedShort(
            buffer,
            if (format.is64Bit) ELF_64_PROGRAM_HEADER_ENTRY_SIZE else ELF_32_PROGRAM_HEADER_ENTRY_SIZE,
        )
        val programHeaderCount = readUnsignedShort(
            buffer,
            if (format.is64Bit) ELF_64_PROGRAM_HEADER_COUNT else ELF_32_PROGRAM_HEADER_COUNT,
        )
        check(programHeaderOffset >= 0 && programHeaderEntrySize > 0 && programHeaderCount > 0) {
            "$entryName has an invalid ELF program-header table"
        }

        val alignments = (0 until programHeaderCount).mapNotNull { index ->
            readLoadAlignment(
                buffer = buffer,
                bytesSize = bytes.size,
                entryName = entryName,
                is64Bit = format.is64Bit,
                headerOffsetLong = programHeaderOffset + index.toLong() * programHeaderEntrySize,
            )
        }
        check(alignments.isNotEmpty()) { "$entryName has no ELF LOAD segments" }
        return alignments
    }

    private fun readElfFormat(bytes: ByteArray, entryName: String): ElfFormat {
        check(bytes.size >= ELF_64_HEADER_SIZE) { "$entryName has a truncated ELF header" }
        check(
            bytes[0] == 0x7f.toByte() &&
                bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'L'.code.toByte() &&
                bytes[3] == 'F'.code.toByte(),
        ) { "$entryName is not an ELF binary" }

        val is64Bit = when (bytes[ELF_CLASS_INDEX].toInt()) {
            ELF_CLASS_32 -> false
            ELF_CLASS_64 -> true
            else -> error("$entryName has an unsupported ELF class")
        }
        val byteOrder = when (bytes[ELF_DATA_INDEX].toInt()) {
            ELF_DATA_LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
            ELF_DATA_BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
            else -> error("$entryName has an unsupported ELF byte order")
        }
        return ElfFormat(is64Bit, byteOrder)
    }

    private fun readProgramHeaderOffset(buffer: ByteBuffer, is64Bit: Boolean): Long =
        if (is64Bit) {
            buffer.getLong(ELF_64_PROGRAM_HEADER_OFFSET)
        } else {
            buffer.getInt(ELF_32_PROGRAM_HEADER_OFFSET).toLong() and UINT_MASK
        }

    private fun readUnsignedShort(buffer: ByteBuffer, offset: Int): Int =
        buffer.getShort(offset).toInt() and USHORT_MASK

    private fun readLoadAlignment(
        buffer: ByteBuffer,
        bytesSize: Int,
        entryName: String,
        is64Bit: Boolean,
        headerOffsetLong: Long,
    ): Long? {
        val alignmentOffset = if (is64Bit) ELF_64_LOAD_ALIGNMENT else ELF_32_LOAD_ALIGNMENT
        val alignmentSize = if (is64Bit) Long.SIZE_BYTES else Int.SIZE_BYTES
        val requiredEnd = headerOffsetLong + alignmentOffset + alignmentSize
        check(requiredEnd <= bytesSize.toLong()) {
            "$entryName has a truncated ELF program-header table"
        }
        val headerOffset = headerOffsetLong.toInt()
        val type = buffer.getInt(headerOffset).toLong() and UINT_MASK
        if (type != ELF_PROGRAM_HEADER_LOAD) return null

        val alignment = if (is64Bit) {
            buffer.getLong(headerOffset + alignmentOffset)
        } else {
            buffer.getInt(headerOffset + alignmentOffset).toLong() and UINT_MASK
        }
        check(alignment >= 0) { "$entryName has an invalid LOAD alignment" }
        return alignment
    }

    private data class ElfFormat(
        val is64Bit: Boolean,
        val byteOrder: ByteOrder,
    )

    private companion object {
        const val ELF_64_HEADER_SIZE = 64
        const val ELF_CLASS_INDEX = 4
        const val ELF_DATA_INDEX = 5
        const val ELF_CLASS_32 = 1
        const val ELF_CLASS_64 = 2
        const val ELF_DATA_LITTLE_ENDIAN = 1
        const val ELF_DATA_BIG_ENDIAN = 2
        const val ELF_32_PROGRAM_HEADER_OFFSET = 28
        const val ELF_64_PROGRAM_HEADER_OFFSET = 32
        const val ELF_32_PROGRAM_HEADER_ENTRY_SIZE = 42
        const val ELF_64_PROGRAM_HEADER_ENTRY_SIZE = 54
        const val ELF_32_PROGRAM_HEADER_COUNT = 44
        const val ELF_64_PROGRAM_HEADER_COUNT = 56
        const val ELF_32_LOAD_ALIGNMENT = 28
        const val ELF_64_LOAD_ALIGNMENT = 48
        const val ELF_PROGRAM_HEADER_LOAD = 1L
        const val UINT_MASK = 0xffffffffL
        const val USHORT_MASK = 0xffff
        val SUPPORTED_16_KB_ABIS = setOf("arm64-v8a", "x86_64")
    }
}

abstract class ValidateAppBundlePageAlignmentTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val bundleConfig = ZipFile(bundleFile.get().asFile).use { bundle ->
            val entry = checkNotNull(bundle.getEntry(BUNDLE_CONFIG_PATH)) {
                "App Bundle has no $BUNDLE_CONFIG_PATH: ${bundleFile.get().asFile}"
            }
            bundle.getInputStream(entry).use(BundleConfig::parseFrom)
        }
        val alignment = bundleConfig.optimizations.uncompressNativeLibraries.alignment
        check(alignment == PageAlignment.PAGE_ALIGNMENT_16K) {
            "App Bundle requests $alignment instead of ${PageAlignment.PAGE_ALIGNMENT_16K}"
        }
        logger.lifecycle("Validated App Bundle page alignment: $alignment")
    }

    private companion object {
        const val BUNDLE_CONFIG_PATH = "BundleConfig.pb"
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

val validateDebugNativeLibrariesFor16Kb = tasks.register<ValidateNativeLibraryPageSizeTask>(
    "validateDebugNativeLibrariesFor16Kb",
) {
    group = "verification"
    description = "Validates 16 KB ELF LOAD alignment for every 64-bit native library in the debug APK."
    dependsOn("assembleDebug")
    archiveFile.set(layout.buildDirectory.file("outputs/apk/debug/androidApp-debug.apk"))
    minimumPageSizeBytes.set(16L * 1024L)
}

val validateDebugAppBundleNativeLibrariesFor16Kb =
    tasks.register<ValidateNativeLibraryPageSizeTask>(
        "validateDebugAppBundleNativeLibrariesFor16Kb",
    ) {
        group = "verification"
        description = "Validates 16 KB ELF LOAD alignment for every 64-bit native library in the debug AAB."
        dependsOn("bundleDebug")
        archiveFile.set(layout.buildDirectory.file("outputs/bundle/debug/androidApp-debug.aab"))
        minimumPageSizeBytes.set(16L * 1024L)
    }

val validateDebugAppBundlePageAlignmentFor16Kb =
    tasks.register<ValidateAppBundlePageAlignmentTask>(
        "validateDebugAppBundlePageAlignmentFor16Kb",
    ) {
        group = "verification"
        description = "Validates that the debug AAB requests 16 KB ZIP alignment from bundletool."
        dependsOn("bundleDebug")
        bundleFile.set(layout.buildDirectory.file("outputs/bundle/debug/androidApp-debug.aab"))
    }

tasks.register("validateDebugPageSizeCompatibility") {
    group = "verification"
    description = "Validates APK/AAB native ELF alignment and the App Bundle 16 KB delivery contract."
    dependsOn(
        validateDebugNativeLibrariesFor16Kb,
        validateDebugAppBundleNativeLibrariesFor16Kb,
        validateDebugAppBundlePageAlignmentFor16Kb,
    )
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
