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
            throw RuntimeException("Package $packageName version $packageVersion is not installed.")
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
