package de.gematik.epa.poc.privacy

import ca.uhn.fhir.validation.ValidationResult
import gg.jte.CodeResolver
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationDispense
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText


val logger = LoggerFactory.getLogger("de.gematik.epa.poc.privacy")

val codeResolver: CodeResolver = ResourceCodeResolver("templates", TestCaseResult::class.java.classLoader)
var templateEngine: TemplateEngine = TemplateEngine.create(codeResolver, ContentType.Html)

/**
 * Extension for FHIR Validator to find the privacy aware profile for a given resource. Uses simple if-else for now.
 */
fun Validator.findPrivacyAwareProfile(resource: Resource): StructureDefinition? {
    val privacyAwareProfileUri = when (resource ) {
        is Medication -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication"
        is MedicationRequest -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-request"
        is MedicationDispense -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-dispense"
        is MedicationStatement -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-statement"
        else -> {
            logger.error("No privacy aware profile found for resource: {}", resource)
            return null
        }
    }
    return fetchProfile(privacyAwareProfileUri)
}

fun main() {
    val config = Yaml.decodeFromString<Config>(Path("testcases-config.yaml").readText())
    logger.info("Loaded config: {}", config)

    // TODO: experiment with encryption later
    // val encKey = Crypto.deriveKey("always use secret passwords in production", "and take cale of salt")

    val pseudonyms = mutableMapOf<String, String>()
    val pseudonymizeFunction = { s: String -> pseudonyms.computeIfAbsent(s.substringAfterLast("/")) { UUID.randomUUID().toString() } }

    val validators = mutableMapOf<String,Validator>()

    config.testCaseList.forEach { testCaseConfig ->
        pseudonyms.clear()
        logger.info("Running testcase: {}", testCaseConfig.id)
        val validator = validators.computeIfAbsent(testCaseConfig.sushiProjectPath) {
            Validator(FhirContextR4, Path(it))
        }

        val bundle = loadResource(Path(testCaseConfig.sushiProjectPath).resolve(testCaseConfig.bundlePath)) as Bundle

        var filteredBundle = Bundle()

        filteredBundle.id = bundle.id
        filteredBundle.type = bundle.type

        var filterLog = mutableListOf<PrivacyFilterLogEntry>()

        var validationResults = mutableListOf<ValidationResult>()
        var filteredValidationResults = mutableListOf<ValidationResult>()

        for (entry in bundle.entry) {
            // skip blocked resources
            if (RESOURCE_BLOCK_LIST.contains(entry.resource.resourceType.name)) {
                logger.info("Skipping blocked resource: {}", entry.resource.id)
                continue
            }
            val subResource = entry.resource
            if (subResource == null) {
                logger.error("Resource is null")
                continue
            }
            logger.info("Processing resource: {}", subResource.id)
            val validationResult = validator.validateWithoutTerminology(subResource)
            validationResults.add(validationResult)

            val filteredResource = validator.findPrivacyAwareProfile(subResource)?.let { profile ->
                logger.info("Found privacy aware profile: {}", profile.url)
                val privacyFilter = PrivacyFilter(profile, pseudonymizeFunction)
                val filterResult = privacyFilter.filter(subResource)
                filterLog.addAll(filterResult.log)
                filterResult.resource
            } ?: subResource

            filteredBundle.addEntry().resource = filteredResource

            val filteredValidationResult = validator.validateWithoutTerminology(filteredResource)
            filteredValidationResults.add(filteredValidationResult)

        }

        // load sources
        testCaseConfig.sources.forEach { source ->
            val path = Path(testCaseConfig.sushiProjectPath).resolve(source.path)
            source.content = path.readText()
            logger.info("Loaded FSH source: $path")
        }

        val testcaseResult = TestCaseResult(
            testCaseConfig = testCaseConfig,
            sources = testCaseConfig.sources,
            bundle = bundle,
            filteredBudle = filteredBundle,
            filterLog = filterLog,
            validationResults = validationResults,
            filtereValidationResults = filteredValidationResults,
            pseudonyms = pseudonyms
        )

        val outputPath = Path("../reports/${testCaseConfig.id}.html")
        writeTestCase(config, testcaseResult, outputPath)
        logger.info("Testcase written to: {}", outputPath)
    }

}

fun writeTestCase(config: Config, testCase: TestCaseResult, path: Path) {
    val output = StringOutput()
    templateEngine.render(
        "Testcase.kte",
        mapOf(
            "config" to config,
            "testCase" to testCase
        ),
        output
    )
    path.writeText(output.toString())
}
