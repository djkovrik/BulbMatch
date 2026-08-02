import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.data"
        compileSdk = 36
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    jvm("catalogTools") {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatformSettings)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatformSettings.test)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        getByName("catalogToolsTest").dependencies {
            implementation(kotlin("test"))
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        register("BulbMatchDatabase") {
            packageName.set("com.sedsoftware.bulbmatch.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

val catalogToolsCompilation = kotlin
    .targets
    .getByName("catalogTools")
    .compilations
    .getByName("main")

val productionCatalogFile = layout.projectDirectory.file(
    "src/commonMain/resources/catalog/bulbmatch-catalog-production.json",
)
val catalogReleasesRoot = rootProject.layout.projectDirectory.dir("spec/catalog/releases")
val runtimeRulesSource = layout.projectDirectory.file(
    "src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/catalog/BundledCatalogRules.kt",
)

tasks.register<JavaExec>("updateProductionCatalogHash") {
    group = "catalog"
    description = "Updates only the canonical contentHash and validates the exact catalog."
    dependsOn(catalogToolsCompilation.compileTaskProvider)
    classpath(catalogToolsCompilation.output.allOutputs)
    classpath(catalogToolsCompilation.runtimeDependencyFiles)
    mainClass.set("com.sedsoftware.bulbmatch.data.catalog.CatalogReleaseToolKt")
    args("update-hash", productionCatalogFile.asFile.absolutePath)
    inputs.file(productionCatalogFile)
    outputs.file(productionCatalogFile)
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("validateProductionCatalog") {
    group = "verification"
    description = "Validates the approved catalog against its exact frozen release bundle."
    dependsOn(catalogToolsCompilation.compileTaskProvider)
    classpath(catalogToolsCompilation.output.allOutputs)
    classpath(catalogToolsCompilation.runtimeDependencyFiles)
    mainClass.set("com.sedsoftware.bulbmatch.data.catalog.CatalogReleaseToolKt")
    args(
        "validate-release",
        productionCatalogFile.asFile.absolutePath,
        catalogReleasesRoot.asFile.absolutePath,
        runtimeRulesSource.asFile.absolutePath,
        productionCatalogFile.asFile.parentFile.absolutePath,
    )
    inputs.file(productionCatalogFile)
    inputs.dir(catalogReleasesRoot)
    inputs.file(runtimeRulesSource)
}

tasks.configureEach {
    if (name == "testAndroidHostTest") {
        dependsOn("catalogToolsTest")
    }
}

tasks.withType<Test>().configureEach {
    if (name == "catalogToolsTest") {
        systemProperty(
            "bulbmatch.projectRoot",
            rootProject.layout.projectDirectory.asFile.absolutePath,
        )
    }
}
