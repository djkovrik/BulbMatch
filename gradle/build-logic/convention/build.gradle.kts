plugins {
    `kotlin-dsl`
}

group = "com.sedsoftware.bulbmatch.convention"

gradlePlugin {
    plugins {
        register("printLineCoverage") {
            id = "bulbmatch.config.printcoverage"
            implementationClass = "com.sedsoftware.bulbmatch.convention.PrintLineCoveragePlugin"
        }
    }
}
