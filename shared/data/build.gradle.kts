import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bulbmatch.data"
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

tasks.register("validateProductionCatalog") {
    group = "verification"
    description = "Blocks release until the exact bundled catalog has human approval."
    inputs.file(
        layout.projectDirectory.file(
            "src/commonMain/resources/catalog/bulbmatch-catalog-development.json",
        ),
    )
    doLast {
        val catalog = inputs.files.singleFile.readText(Charsets.UTF_8)
        check(catalog.contains("\"requiredReviewer\": \"Sergey V.\""))
        check(catalog.contains("\"state\": \"APPROVED\"")) {
            "Production catalog is not approved by Sergey V."
        }
        check(catalog.contains("\"releaseEligible\": true"))
        check(catalog.contains("\"decision\": \"APPROVED\""))
        check(!catalog.contains("\"reviewState\": \"PENDING_HUMAN_SIGNOFF\""))
        check(!catalog.contains("\"enabledForAssessment\": false"))
    }
}
