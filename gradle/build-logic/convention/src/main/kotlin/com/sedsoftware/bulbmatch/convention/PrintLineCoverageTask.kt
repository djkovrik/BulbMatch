package com.sedsoftware.bulbmatch.convention

import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class PrintLineCoverageTask : DefaultTask() {
    @get:InputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun printCoverage() {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        val document = factory.newDocumentBuilder().parse(reportFile.get().asFile)
        val lineCounter = generateSequence(document.documentElement.firstChild) { it.nextSibling }
            .firstOrNull {
                it.nodeName == "counter" &&
                    it.attributes?.getNamedItem("type")?.textContent == "LINE"
            }
            ?: error("Kover report does not contain a LINE counter.")
        val missed = lineCounter.attributes.getNamedItem("missed").textContent.toLong()
        val covered = lineCounter.attributes.getNamedItem("covered").textContent.toLong()
        val total = missed + covered
        check(total > 0L) { "Kover report does not contain executable lines." }
        val percentage = covered * 100.0 / total

        println(String.format(Locale.US, "%.1f", percentage))
    }
}
