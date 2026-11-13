/*
 * Copyright 2024-2026, gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */

package de.gematik.epa.poc.privacy

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.utilities.npm.NpmPackage
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * HAPI validation support for a FSH/SUSHI project: knows the resources SUSHI generated and
 * everything in the FHIR packages the project depends on, read from the local package cache.
 */
class SushiProjectValidationSupport(
    private val ctx: FhirContext,
    private val sushiProjectDirectory: Path,
    private val sushiOutputDirectory: Path = sushiProjectDirectory.resolve("fsh-generated/resources"),
    private val packagesCacheDirectory: Path = Paths.get(System.getProperty("user.home"), ".fhir", "packages"),
) : PrePopulatedValidationSupport(ctx) {

    private val loadedPackages = mutableSetOf<FhirPackage>()

    init {
        loadSushiGeneratedResources()
        SushiConfig(sushiProjectDirectory).dependencies.forEach(::loadPackage)
    }

    private fun loadSushiGeneratedResources() {
        Files.walk(sushiOutputDirectory).use { paths ->
            paths.filter { it.isRegularFile() && it.toString().lowercase().endsWith(".json") }
                .forEach { addResource(ctx.newJsonParser().parseResource(it.readText())) }
        }
    }

    private fun loadPackage(fhirPackage: FhirPackage) {
        if (!loadedPackages.add(fhirPackage)) return

        val packagePath = packagesCacheDirectory.resolve("${fhirPackage.name}#${fhirPackage.version}")
        if (!packagePath.exists()) {
            // A missing transitive package must not abort the run when none of its resources are
            // needed; validation against its profiles surfaces as messages in the report instead.
            logger.warn("Package {} is not installed under {}; skipping", fhirPackage, packagesCacheDirectory)
            return
        }
        logger.debug("Loading package {}", fhirPackage)

        val npmPackage = NpmPackage.fromFolder(packagePath.toString())
        val packageFolder = npmPackage.folders.getValue("package")
        for (resourceFile in npmPackage.types.values.flatten().toSet()) {
            val json = String(packageFolder.fetchFile(resourceFile), StandardCharsets.UTF_8)
            addResource(ctx.newJsonParser().parseResource(json))
        }

        transitiveDependencies(packagePath.resolve("package/package.json")).forEach(::loadPackage)
    }

    private fun transitiveDependencies(packageJsonFile: Path): List<FhirPackage> {
        val dependencies = ObjectMapper().readTree(packageJsonFile.toFile())["dependencies"] ?: return emptyList()
        return dependencies.fields().asSequence().map { (name, version) -> FhirPackage(name, version.asText()) }.toList()
    }

    private companion object {
        val logger = LoggerFactory.getLogger(SushiProjectValidationSupport::class.java)
    }
}
