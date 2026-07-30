import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateComposablePreviewPaparazziTestsTask : DefaultTask() {
    @get:Input
    abstract val previewPackages: ListProperty<String>

    @get:Input
    abstract val testPackageName: Property<String>

    @get:Input
    abstract val testClassName: Property<String>

    @get:Input
    abstract val compileSdkVersion: Property<Int>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageName = testPackageName.get()
        val className = testClassName.get()
        val packagePath = packageName.replace('.', '/')
        val outputFile = outputDirectory.file("$packagePath/$className.kt").get().asFile
        val previewPackageLiterals = previewPackages.get()
            .sorted()
            .joinToString(separator = ", ") { "\"$it\"" }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            @file:Suppress("LongMethod")

            package $packageName

            import app.cash.paparazzi.Paparazzi
            import com.sedsoftware.bulbmatch.screenshottests.BulbMatchPaparazziPreviewRule
            import com.sedsoftware.bulbmatch.screenshottests.BulbMatchPreviewContent
            import com.sedsoftware.bulbmatch.screenshottests.BulbMatchSnapshotId
            import com.sedsoftware.bulbmatch.screenshottests.configureComposeResources
            import org.junit.Rule
            import org.junit.Test
            import org.junit.runner.RunWith
            import org.junit.runners.Parameterized
            import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
            import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
            import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

            @RunWith(Parameterized::class)
            class $className(
                private val preview: ComposablePreview<AndroidPreviewInfo>,
            ) {
                companion object {
                    private val cachedPreviews: List<ComposablePreview<AndroidPreviewInfo>> by lazy {
                        AndroidComposablePreviewScanner()
                            .scanPackageTrees(
                                include = listOf($previewPackageLiterals),
                                exclude = emptyList(),
                            )
                            .includePrivatePreviews()
                            .getPreviews()
                            .sortedBy(BulbMatchSnapshotId::create)
                    }

                    @JvmStatic
                    @Parameterized.Parameters(name = "{0}")
                    fun previews(): List<ComposablePreview<AndroidPreviewInfo>> = cachedPreviews
                }

                @get:Rule
                val paparazzi: Paparazzi = BulbMatchPaparazziPreviewRule.createFor(
                    preview = preview,
                    compileSdkVersion = ${compileSdkVersion.get()},
                )

                @Test
                fun snapshot() {
                    paparazzi.configureComposeResources()
                    paparazzi.snapshot(name = BulbMatchSnapshotId.create(preview)) {
                        BulbMatchPreviewContent(preview.previewInfo) {
                            preview()
                        }
                    }
                }
            }
            """.trimIndent(),
        )
    }
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}

val previewPackage = "com.sedsoftware.bulbmatch.compose"
val generatedTestPackage = "com.sedsoftware.bulbmatch.screenshottests.generated"
val generatedTestClass = "GeneratedComposablePreviewPaparazziTest"
val generatedTestDirectory = layout.projectDirectory.dir("build/generated/source/paparazziPreviews/test/kotlin")

val generateComposablePreviewPaparazziTests by tasks.registering(
    GenerateComposablePreviewPaparazziTestsTask::class,
) {
    group = "verification"
    description = "Generates deterministic Paparazzi tests for BulbMatch Compose previews."

    previewPackages.set(listOf(previewPackage))
    testPackageName.set(generatedTestPackage)
    testClassName.set(generatedTestClass)
    compileSdkVersion.set(36)
    outputDirectory.set(generatedTestDirectory)
}

android {
    namespace = "com.sedsoftware.bulbmatch.screenshottests"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs("-Xmx4g")
            }
        }
    }

    sourceSets {
        getByName("test").kotlin.directories.add(generatedTestDirectory.asFile.absolutePath)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared:compose"))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.resources)
    implementation(libs.compose.material3)
    testImplementation(libs.test.junit4)
    testImplementation(libs.test.composable.preview.scanner.android)
    testRuntimeOnly(libs.compose.ui.tooling)
}

tasks.matching {
    it.name == "compileDebugUnitTestKotlin" ||
        it.name == "compileReleaseUnitTestKotlin" ||
        it.name.startsWith("recordPaparazzi") ||
        it.name.startsWith("verifyPaparazzi")
}.configureEach {
    dependsOn(generateComposablePreviewPaparazziTests)
}

tasks.matching {
    it.name == "testDebugUnitTest" ||
        it.name == "testReleaseUnitTest" ||
        it.name.startsWith("recordPaparazzi") ||
        it.name.startsWith("verifyPaparazzi")
}.configureEach {
    notCompatibleWithConfigurationCache(
        "Paparazzi 2.0.0-alpha02 captures non-serializable AGP variant state.",
    )
}

tasks.withType<Test>().configureEach {
    reports.html.required.set(false)
}
