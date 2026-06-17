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

import org.hl7.fhir.r4.model.*

enum class PrivacyFilterMethod {
    Redact,
    Anonymize,
    Pseudonymize,
}

const val PRIVACY_LABEL_EXTENSION_URL = "https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension"
const val SYSTEM_JOB_NUMBER = "https://gematik.de/fhir/epa-research/sid/job-number-identifier"
const val SYSTEM_SOURCE_PROFILE = "https://gematik.de/fhir/epa-research/sid/source-profile"
const val SYSTEM_PSEUDED = "http://terminology.hl7.org/CodeSystem/v3-ObservationValue"
const val CODE_PSEUDED = "PSEUDED"

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
        logger.info("Applying rule {} to {}", method, element.id)
        try {
            applyRule(resource)
        } catch (e: Exception) {
            logger.error("Error applying rule to {}", element.id, e)
            return PrivacyFilterLogEntry(PrivacyLogeSeverity.ERROR, method, element.id, e.message ?: "")
        }
        return PrivacyFilterLogEntry(PrivacyLogeSeverity.INFO, method, element.id, method.toString())
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
) {
    private val resourceTypeName: String = profile.type

    private val allowedExtensionUrls: Set<String> = profile.differential.element
        .filter { it.path == "$resourceTypeName.extension" && it.sliceName != null }
        .flatMap { slice -> slice.type.flatMap { t -> t.profile.map { p -> p.value } } }
        .toSet()

    private val rules: List<PrivacyFilterRule> = buildList {
        profile.differential.element.forEach { element ->
            var privacyLabel = element.extension.find { it.url == PRIVACY_LABEL_EXTENSION_URL }

            // auto-add REDACT when max=0 (the cardinality already mandates absence; the label makes it explicit)
            if (privacyLabel == null && element.max == "0") {
                privacyLabel = Extension(PRIVACY_LABEL_EXTENSION_URL).apply {
                    addExtension("obligationPolicy", Coding(SYSTEM_PSEUDED, "REDACT", "redact"))
                }
            }

            if (privacyLabel == null) return@forEach

            findPrivacyFilterRule(element, privacyLabel)?.let { add(it) }
        }
    }

    fun filter(resource: Resource): PrivacyFilterResult {
        val filteredResource = resource.copy()

        // meta.tag: drop any prior source-profile tag, then move each meta.profile entry into a fresh source-profile tag
        filteredResource.meta.tag = filteredResource.meta.tag.filter { it.system != SYSTEM_SOURCE_PROFILE }
        filteredResource.meta.profile.forEach {
            filteredResource.meta.tag.add(Coding(SYSTEM_SOURCE_PROFILE, it.value, null))
        }
        filteredResource.meta.profile.clear()
        filteredResource.meta.profile.add(CanonicalType(profile.url))

        val log = mutableListOf<PrivacyFilterLogEntry>()
        rules.forEach { log.add(it.filter(filteredResource)) }

        applyExtensionAllowlist(filteredResource)
        stampPseudedSecurity(filteredResource)

        return PrivacyFilterResult(filteredResource, log)
    }

    private fun applyExtensionAllowlist(resource: Resource) {
        val domain = resource as? DomainResource ?: return
        val iter = domain.extension.iterator()
        while (iter.hasNext()) {
            if (iter.next().url !in allowedExtensionUrls) iter.remove()
        }
    }

    private fun stampPseudedSecurity(resource: Resource) {
        val already = resource.meta.security.any { it.system == SYSTEM_PSEUDED && it.code == CODE_PSEUDED }
        if (!already) {
            resource.meta.security.add(Coding(SYSTEM_PSEUDED, CODE_PSEUDED, "pseudonymized"))
        }
    }

    private fun findPrivacyFilterRule(element: ElementDefinition, privacyLabel: Extension): PrivacyFilterRule? {
        // Use element.id (carries slice annotations + polymorphic-type slices) and normalize to a
        // runtime-navigable FHIRPath. element.id on its own loses the slice context and uses
        // [x] for polymorphic fields, which FHIRPath cannot evaluate.
        val idSubPath = element.id.removePrefix("$resourceTypeName.")
        val subPath = sliceIdToFhirPath(idSubPath)
        val obligationPolicy = privacyLabel.getExtensionByUrl("obligationPolicy")?.value as? Coding
            ?: throw IllegalArgumentException("obligationPolicy not found or is invalid")
        val dummyValue = (privacyLabel.getExtensionByUrl("dummyValue")?.value as? StringType)?.value ?: "<anonymized>"

        val method = when (obligationPolicy.code) {
            "PSEUD" -> PrivacyFilterMethod.Pseudonymize
            "ANONY" -> PrivacyFilterMethod.Anonymize
            "REDACT" -> PrivacyFilterMethod.Redact
            else -> throw IllegalArgumentException("Unknown obligationPolicy: ${obligationPolicy.code}")
        }

        val applyRule = buildRuleAction(method, subPath, dummyValue, element) ?: return null
        return PrivacyFilterRule(method, profile, element, applyRule)
    }

    private fun buildRuleAction(
        method: PrivacyFilterMethod,
        subPath: String,
        dummyValue: String,
        element: ElementDefinition,
    ): ((Resource) -> Unit)? = when {
        // slice-targeted REDACT: remove matching entries from the parent list
        method == PrivacyFilterMethod.Redact && element.sliceName != null && element.max == "0" -> {
            buildSliceRedact(element)
        }

        // REDACT on the resource itself (e.g. requester) — top-level field
        method == PrivacyFilterMethod.Redact && "." !in subPath -> { resource ->
            resource.setValue(subPath, null)
        }

        // REDACT on a nested field (e.g. subject.reference, dosageInstruction.text)
        method == PrivacyFilterMethod.Redact -> { resource ->
            val basePath = subPath.substringBeforeLast(".")
            val lastPath = subPath.substringAfterLast(".")
            FhirPath.evaluate(resource, basePath).filterIsInstance<Base>().forEach {
                it.setValue(lastPath, null)
            }
        }

        // ANONY on a nested field (e.g. performer.actor.display = "ANONYMIZED")
        method == PrivacyFilterMethod.Anonymize && "." in subPath -> { resource ->
            val basePath = subPath.substringBeforeLast(".")
            val lastPath = subPath.substringAfterLast(".")
            FhirPath.evaluate(resource, basePath).filterIsInstance<Base>().forEach {
                it.setValue(lastPath, dummyValue)
            }
        }

        // ANONY on a top-level field
        method == PrivacyFilterMethod.Anonymize -> { resource ->
            resource.setValue(subPath, dummyValue)
        }

        // PSEUD on the resource id
        method == PrivacyFilterMethod.Pseudonymize && subPath == "id" -> { resource ->
            resource.id = pseudonymize(resource.id)
        }

        // PSEUD on a Reference's identifier (e.g. subject.identifier on MedicationRequest)
        method == PrivacyFilterMethod.Pseudonymize && subPath.endsWith(".identifier") -> { resource ->
            val basePath = subPath.substringBeforeLast(".")
            val ref = FhirPath.evaluate(resource, basePath).filterIsInstance<Reference>().firstOrNull()
            if (ref != null) applyIdentifierPseud(ref)
        }

        // PSEUD on a Reference's reference string
        method == PrivacyFilterMethod.Pseudonymize && subPath.endsWith(".reference") -> { resource ->
            val basePath = subPath.substringBeforeLast(".")
            val ref = FhirPath.evaluate(resource, basePath).filterIsInstance<Reference>().firstOrNull()
            ref?.reference?.let { ref.reference = pseudonymize(it) }
        }

        // PSEUD on a top-level Reference (e.g. subject on MedicationDispense)
        method == PrivacyFilterMethod.Pseudonymize && "." !in subPath -> { resource ->
            val ref = FhirPath.evaluate(resource, subPath).filterIsInstance<Reference>().firstOrNull()
            if (ref != null) {
                ref.reference = null
                applyIdentifierPseud(ref)
            }
        }

        // PSEUD on a nested non-identifier/non-reference primitive (legacy)
        method == PrivacyFilterMethod.Pseudonymize -> { resource ->
            val basePath = subPath.substringBeforeLast(".")
            val lastPath = subPath.substringAfterLast(".")
            FhirPath.evaluate(resource, basePath).filterIsInstance<Base>().forEach { base ->
                val value = base.getValuesForPath(lastPath).firstOrNull()
                base.setValue(lastPath, pseudonymize(value.toString()))
            }
        }

        else -> null
    }

    private fun applyIdentifierPseud(ref: Reference) {
        val original = ref.identifier?.value
        val pseud = if (original != null) pseudonymize(original) else null
        ref.identifier = Identifier().apply {
            system = SYSTEM_JOB_NUMBER
            value = pseud
        }
    }

    private fun buildSliceRedact(element: ElementDefinition): (Resource) -> Unit {
        // Use element.id (carries slice annotations like ':itemCodeableConcept' for polymorphic types)
        // and normalize to a FHIRPath the runtime resource can navigate.
        val resourcePrefix = "$resourceTypeName."
        val idSubPath = element.id.removePrefix(resourcePrefix) // "code.coding:atc-de" or "ingredient.item[x]:itemCodeableConcept.coding:atc-de"
        val containerFhirPath = sliceIdToFhirPath(idSubPath) // "code.coding" or "ingredient.itemCodeableConcept.coding"
        val parentSubPath = if ("." in containerFhirPath) containerFhirPath.substringBeforeLast(".") else ""
        val listProperty = containerFhirPath.substringAfterLast(".")
        val sliceIdPrefix = "${element.id}." // "<Type>.code.coding:atc-de."

        val constraints: List<Pair<String, String>> = profile.differential.element
            .filter { it.id?.startsWith(sliceIdPrefix) == true }
            .mapNotNull { sibling ->
                val relPath = sibling.id.removePrefix(sliceIdPrefix)
                primitiveFixedOrPattern(sibling)?.let { relPath to it }
            }

        if (constraints.isEmpty()) {
            return { logger.warn("Cannot apply slice REDACT for {}: no primitive discriminator found", element.id) }
        }

        return { resource ->
            val parents: List<Base> = if (parentSubPath.isEmpty()) {
                listOf(resource)
            } else {
                FhirPath.evaluate(resource, parentSubPath).filterIsInstance<Base>()
            }
            parents.forEach { parent ->
                val getterName = "get" + listProperty.replaceFirstChar { it.uppercaseChar() }
                val getter = parent::class.members.firstOrNull { it.name == getterName } ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                val list = getter.call(parent) as? MutableList<Base> ?: return@forEach
                list.removeAll { entry ->
                    constraints.all { (path, value) ->
                        FhirPath.evaluate(entry, path).any { v ->
                            (v as? PrimitiveType<*>)?.valueAsString == value
                        }
                    }
                }
            }
        }
    }

    private fun primitiveFixedOrPattern(element: ElementDefinition): String? {
        val raw = element.fixed ?: element.pattern ?: return null
        return (raw as? PrimitiveType<*>)?.valueAsString
    }

    /**
     * Normalize a StructureDefinition `id` sub-path to a navigable FHIRPath:
     * - polymorphic-type slices collapse to the abstract field name:
     *   `item[x]:itemCodeableConcept` → `item` (HAPI's FHIRPath returns the typed value directly)
     * - drop any other slice annotations: `coding:atc-de` → `coding`
     */
    private fun sliceIdToFhirPath(idSubPath: String): String =
        idSubPath
            .replace(Regex("""(\w+)\[x\]:[\w-]+""")) { it.groupValues[1] }
            .replace(Regex(""":[\w-]+"""), "")
}
