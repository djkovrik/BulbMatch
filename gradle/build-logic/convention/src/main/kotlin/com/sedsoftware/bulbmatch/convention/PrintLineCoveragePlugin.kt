package com.sedsoftware.bulbmatch.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class PrintLineCoveragePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register(
            "printLineCoverage",
            PrintLineCoverageTask::class.java,
        ) {
            group = "verification"
            description = "Prints aggregate Kover line coverage as a numeric percentage."
            dependsOn("koverXmlReport")
            reportFile.set(project.layout.buildDirectory.file("reports/kover/report.xml"))
        }
    }
}
