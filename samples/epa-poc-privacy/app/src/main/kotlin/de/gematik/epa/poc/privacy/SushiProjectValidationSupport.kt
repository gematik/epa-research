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
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Pre-configured HAPI [ca.uhn.fhir.context.support.IValidationSupport]
 * for validating generated FHIR Resources in FSH/SUSHI projects.
 */
class SushiProjectValidationSupport(
    private val ctx: FhirContext,
    private val sushiProjectDirectory: Path,
    private val sushiOutputDirectory: Path = sushiProjectDirectory.resolve("fsh-generated/resources"),
    private val packagesCacheDirectory: Path = Paths.get(System.getProperty("user.home"), ".fhir", "packages")
) : PrePopulatedValidationSupport(ctx) {

    private data class PackageDescriptor(val packageName: String, val packageVersion: String)
    private val packages = mutableSetOf<PackageDescriptor>()

    private val logger = LoggerFactory.getLogger(SushiProjectValidationSupport::class.java)

    init {
        loadSushiGeneratedResources()
        loadDependencies()
    }

    @Throws(IOException::class)
    private fun loadSushiGeneratedResources() {
        Files.walk(sushiOutputDirectory).use { paths ->
            paths.filter(Files::isRegularFile)
                .filter { it.toString().lowercase().endsWith(".json") }
                .forEach { addResourceFromFile(it) }
        }
    }

    private fun loadDependencies() {
        val sushiConfig = SushiConfig(sushiProjectDirectory)
        val sushiDependencies = sushiConfig.dependencies

        for (sushiDependency in sushiDependencies) {
            loadPackage(sushiDependency.packageName, sushiDependency.packageVersion)
        }
    }

    private fun findPackageJsonFile(dir: Path): Path? {
        val file = dir.resolve("package.json")
        return when {
            Files.exists(file) -> file
            dir.parent != null -> findPackageJsonFile(dir.parent)
            else -> null
        }
    }

    private fun resolveDependencies(packageJsonFile: Path) {
        val mapper = ObjectMapper()
        val packageJson = mapper.readTree(packageJsonFile.toFile())
        val dependencies = packageJson["dependencies"] ?: return

        val packageNames = dependencies.fieldNames()
        while (packageNames.hasNext()) {
            val packageName = packageNames.next()
            val packageVersion = dependencies[packageName].asText()
            loadPackage(packageName, packageVersion)
        }
    }

    private fun loadPackage(packageName: String, packageVersion: String) {
        val packageDescriptor = PackageDescriptor(packageName, packageVersion)
        if (packages.contains(packageDescriptor)) {
            logger.debug("Skipping already loaded package $packageName#$packageVersion")
            return
        }
        packages.add(packageDescriptor)
        logger.debug("Loading package $packageName version $packageVersion")
        val packagePath = packagesCacheDirectory.resolve("$packageName#$packageVersion")
        if (!packagePath.toFile().exists()) {
            // Soft-fail: a missing transitive FHIR package would otherwise abort the whole run
            // even when none of the resources it provides are needed. Validation against profiles
            // from that package will still surface as warnings later.
            logger.warn("Package {} version {} is not installed under {}; skipping", packageName, packageVersion, packagesCacheDirectory)
            return
        }
        val npmPackage = NpmPackage.fromFolder(packagePath.toString())
        val packageFolder = npmPackage.folders["package"]

        for (resourceFile in npmPackage.types.values.flatten().toSet()) {
            val input = String(packageFolder!!.fetchFile(resourceFile), StandardCharsets.UTF_8)
            val resource = ctx.newJsonParser().parseResource(input)
            addResource(resource)
        }
        // Resolve the sub-dependencies
        resolveDependencies(packagePath.resolve("package").resolve("package.json"))
    }

    private fun addResourceFromFile(resourceJsonFile: Path) {
        val contents = try {
            Files.readString(resourceJsonFile)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val resource = ctx.newJsonParser().parseResource(contents)
        addResource(resource)
    }
}
