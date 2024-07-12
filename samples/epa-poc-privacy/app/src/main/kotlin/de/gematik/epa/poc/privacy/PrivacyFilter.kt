package de.gematik.epa.poc.privacy

import org.hl7.fhir.r4.model.*

enum class PrivacyFilterMethod {
    Redact,
    Anonymize,
    Pseudonymize,
}

const val PRIVACY_LABEL_EXTENSION_URL = "https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension"
const val SYSTEM_JOB_NUMBER = "https://gematik.de/fhir/epa-research/sid/job-number-identifier"
const val SYSTEM_SOURCE_PROFILE = "https://gematik.de/fhir/epa-research/sid/source-profile"

val RESOURCE_BLOCK_LIST = listOf(
    "Patient",
    "Practitioner",
    "PractitionerRole",
    "Organization",
    "HealthcareService",
)

enum class PrivacyLogeSeverity {
    INFO,
    ERROR,
}

data class PrivacyFilterLogEntry(
    val severity: PrivacyLogeSeverity,
    val method: PrivacyFilterMethod,
    val path: String,
    val message: String = "",
)

class PrivacyFilterRule(
    val method: PrivacyFilterMethod,
    val profile: StructureDefinition,
    private val element: ElementDefinition,
    private val applyRule: (Resource) -> Unit
) {
    fun filter(resource: Resource): PrivacyFilterLogEntry {
        logger.info("Applying rule {} to {}", method, element.path)
        try {
            applyRule(resource)
        } catch (e: Exception) {
            logger.error("Error applying rule to {}", element.path, e)
            return PrivacyFilterLogEntry(PrivacyLogeSeverity.ERROR, method, element.path, e.message ?: "")
        }
        return PrivacyFilterLogEntry(PrivacyLogeSeverity.INFO, method, element.path, method.toString())
    }
}

val subPathRegex = Regex("""^[^.]+\.(.+)$""")

class PrivacyFilterResult(
    val resource: Resource,
    val log: List<PrivacyFilterLogEntry>,
)

typealias PseudonymizeFunction = (String) -> String

class PrivacyFilter(
    private val profile: StructureDefinition,
    private val pseudonymize: PseudonymizeFunction = { Integer.toHexString(it.hashCode()) }
){
    private val rules: List<PrivacyFilterRule> = buildList {
        // TODO: implement snapshot support, so that privacy labels from other profiles are also considered
        profile.differential.element.forEach { element ->
            var privacyLabel = element.extension.find {
                it.url == PRIVACY_LABEL_EXTENSION_URL
            }

            // add automatic redact extension if max is 0
            if (privacyLabel == null && element.max == "0") {
                privacyLabel = Extension(
                    PRIVACY_LABEL_EXTENSION_URL,
                )
                privacyLabel.addExtension("obligationPolicy", Coding("http://terminology.hl7.org/CodeSystem/v3-ObservationValue", "REDACT", "redact"))
                logger.info("Automatically adding redact extension {}", privacyLabel)
            }


            if (privacyLabel == null) {
                return@forEach
            }

            logger.info("Found element with privacy label: {}", element.path)

            findPrivacyFilterRule(profile, element, privacyLabel)?.let {
                add(it)
            }

        }
    }

    fun filter(resource: Resource): PrivacyFilterResult {
        val filteredResource = resource.copy()
        filteredResource.meta.tag = filteredResource.meta.tag.filter {
            it.system != SYSTEM_SOURCE_PROFILE
        }

        filteredResource.meta.profile.forEach {
            filteredResource.meta.tag.add(Coding(SYSTEM_SOURCE_PROFILE, it.value, null))
        }

        filteredResource.meta.profile.clear()
        filteredResource.meta.profile.add(CanonicalType(profile.url))



        val log = mutableListOf<PrivacyFilterLogEntry>()
        rules.forEach {
            log.add(it.filter(filteredResource))
        }

        /*
        if (log.size > 0) {
            filteredResource.meta.security.add(
                Coding("http://terminology.hl7.org/CodeSystem/v3-ObservationValue", "PSEUDED", "pseudonymized")
            )
        }
        */

        return PrivacyFilterResult(filteredResource, log)
    }

    fun findPrivacyFilterRule(profile: StructureDefinition, element: ElementDefinition, privacyLabel: Extension): PrivacyFilterRule? {
        val matchResult = subPathRegex.find(element.path) ?: throw IllegalArgumentException("Invalid path: ${element.path}")
        val subPath = matchResult.groupValues[1]
        val obligationPolicy = privacyLabel.getExtensionByUrl("obligationPolicy")?.value as? Coding ?: throw IllegalArgumentException("obligationPolicy not found or is invalid")
        val nullValue = privacyLabel.getExtensionByUrl("nullValue")?.value?.toString() ?: "<anonymized>"

        val method = when (obligationPolicy.code) {
            "PSEUD" -> PrivacyFilterMethod.Pseudonymize
            "ANONY" -> PrivacyFilterMethod.Anonymize
            "REDACT" -> PrivacyFilterMethod.Redact
            else -> throw IllegalArgumentException("Unknown obligationPolicy: ${obligationPolicy.code}")
        }

        val applyRule: (resource: Resource) -> Unit = if (subPath == "id" && method == PrivacyFilterMethod.Pseudonymize) {
            {
                val pseud = pseudonymize(it.id)
                logger.info("Pseudonymizing id: '{}' => {}", it.id, pseud)
                it.setValue(subPath, pseud)
            }
        } else if (subPath.endsWith(".identifier") && method == PrivacyFilterMethod.Pseudonymize) {
            {
                val basePath = subPath.substringBeforeLast(".")
                val baseValue = FhirPath.evaluate(it, basePath)
                    .filterIsInstance<Reference>()
                    .firstOrNull()
                    ?.let { baseValue ->
                        val originalIdentifier = baseValue.identifier
                        val pseud = pseudonymize(originalIdentifier.value)
                        baseValue.identifier.system = SYSTEM_JOB_NUMBER
                        baseValue.identifier.value = pseud
                }
            }
        } else if ("." in subPath && method == PrivacyFilterMethod.Redact) {
            {
                val basePath = subPath.substringBeforeLast(".")
                val lastPath = subPath.substringAfterLast(".")
                val baseValue = FhirPath.evaluate(it, basePath).filterIsInstance<Base>().firstOrNull()
                baseValue?.setValue(lastPath, null)
            }
        } else if (subPath.endsWith(".reference") && method == PrivacyFilterMethod.Pseudonymize) {
            {
                val basePath = subPath.substringBeforeLast(".")
                val baseValue = FhirPath.evaluate(it, basePath).filterIsInstance<Base>().firstOrNull() as Reference?
                baseValue?.reference?.let {
                    baseValue.reference = pseudonymize(it)
                    logger.info("Pseudonymized reference: {} => {}", it, baseValue.reference)
                }
            }
        } else if ("." in subPath && method == PrivacyFilterMethod.Pseudonymize) {
            {
                val basePath = subPath.substringBeforeLast(".")
                val lastPath = subPath.substringAfterLast(".")
                val baseValue = FhirPath.evaluate(it, basePath).filterIsInstance<Base>().firstOrNull()
                val value = baseValue?.getValuesForPath(lastPath)?.firstOrNull()
                baseValue?.setValue(lastPath, pseudonymize(value.toString()))
            }
        } else if ("." !in subPath && (method == PrivacyFilterMethod.Redact)) {
            { it.setValue(subPath, null) }
        } else if (method == PrivacyFilterMethod.Anonymize) {
            { it.setValue(subPath, nullValue) }
        } else {
            { throw IllegalArgumentException("Unsupported method $method for $subPath") }
        }

        return PrivacyFilterRule(method, profile, element, applyRule)
    }

}