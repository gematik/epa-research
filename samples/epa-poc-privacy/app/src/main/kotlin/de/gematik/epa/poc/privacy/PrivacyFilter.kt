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

import org.hl7.fhir.r4.model.BackboneElement
import org.hl7.fhir.r4.model.BackboneType
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Element
import org.hl7.fhir.r4.model.ElementDefinition
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.r4.model.Type
import org.slf4j.LoggerFactory

const val PRIVACY_LABEL_EXTENSION_URL = "https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension"
const val SYSTEM_JOB_NUMBER = "https://gematik.de/fhir/epa-research/sid/job-number-identifier"
const val SYSTEM_SOURCE_PROFILE = "https://gematik.de/fhir/epa-research/sid/source-profile"
const val SYSTEM_PSEUDED = "http://terminology.hl7.org/CodeSystem/v3-ObservationValue"
const val CODE_PSEUDED = "PSEUDED"

enum class PrivacyFilterMethod(val obligationPolicy: String) {
    Redact("REDACT"),
    Anonymize("ANONY"),
    Pseudonymize("PSEUD"),
}

enum class PrivacyLogSeverity {
    INFO,
    ERROR,
}

data class PrivacyFilterLogEntry(
    val severity: PrivacyLogSeverity,
    /** Null for entries that do not stem from a privacy rule. */
    val method: PrivacyFilterMethod?,
    val path: String,
    val message: String,
    val affectedValues: Int = 0,
)

/** [resource] is null when any rule failed: a partially filtered resource must never leave the filter. */
class PrivacyFilterResult(
    val resource: Resource?,
    val log: List<PrivacyFilterLogEntry>,
)

typealias PseudonymizeFunction = (String) -> String

/** Finds the research profile a resource has to be filtered by; null when there is none for it. */
typealias ResearchProfileLookup = (Resource) -> StructureDefinition?

/**
 * Turns a resource into its pseudonymised counterpart as described by a research profile:
 * every element carrying a privacy label is redacted, anonymised or pseudonymised, elements
 * the profile forbids (`max = 0`) are removed, and extensions the profile does not declare are dropped.
 * Contained resources are filtered like standalone ones, by the research profile for their resource type.
 *
 * Only the profile's differential is read, so the research profiles must state all of their
 * privacy rules themselves rather than inherit them.
 */
class PrivacyFilter(
    private val profile: StructureDefinition,
    private val pseudonymize: PseudonymizeFunction,
    private val researchProfileFor: ResearchProfileLookup = { null },
) {
    /**
     * In the order of the profile, except that pseudonymisation comes last: it replaces its element
     * as a whole, and rules on the sub-elements should still find and report what they remove.
     */
    private val rules: List<Rule> = profile.differential.element.mapNotNull(::ruleFor)
        .sortedBy { it.method == PrivacyFilterMethod.Pseudonymize }

    /** Extension URLs the profile admits, keyed by the path of the `extension` element they are sliced on. */
    private val declaredExtensionUrls: Map<String, Set<String>> = profile.differential.element
        .filter { it.sliceName != null && it.max != "0" && it.path.substringAfterLast('.') in EXTENSION_PROPERTIES }
        .groupBy({ it.path }, { it.extensionProfileUrls })
        .mapValues { (_, urls) -> urls.flatten().toSet() }

    /** One filter per research profile, so that the profile of a contained type is read only once. */
    private val containedFilters = mutableMapOf<String, PrivacyFilter>()

    fun filter(resource: Resource): PrivacyFilterResult {
        require(resource.fhirType() == profile.type) {
            "Profile ${profile.url} is for ${profile.type}, not ${resource.fhirType()}"
        }
        val filtered = resource.copy()
        val log = pseudonymizeInPlace(filtered, isContained = false)
        val failed = log.any { it.severity == PrivacyLogSeverity.ERROR }
        return PrivacyFilterResult(filtered.takeUnless { failed }, log)
    }

    private fun pseudonymizeInPlace(resource: Resource, isContained: Boolean): List<PrivacyFilterLogEntry> {
        val log = rules.map { it.apply(resource) }.toMutableList()
        (resource as? DomainResource)?.contained?.forEach { log += pseudonymizeContained(it) }
        removeUndeclaredExtensions(resource, profile.type)
        declareAsPseudonymized(resource, isContained)
        return log
    }

    private fun pseudonymizeContained(contained: Resource): List<PrivacyFilterLogEntry> {
        val containedPath = "${profile.type}.contained"
        val containedProfile = researchProfileFor(contained)
        if (containedProfile == null) {
            val message = "no research profile for contained ${contained.fhirType()}"
            logger.error("{}: {}", containedPath, message)
            return listOf(PrivacyFilterLogEntry(PrivacyLogSeverity.ERROR, null, containedPath, message))
        }
        val containedFilter = containedFilters.getOrPut(containedProfile.url) {
            if (containedProfile.url == profile.url) this else PrivacyFilter(containedProfile, pseudonymize, researchProfileFor)
        }
        return containedFilter.pseudonymizeInPlace(contained, isContained = true)
            .map { it.copy(path = "$containedPath > ${it.path}") }
    }

    private fun ruleFor(element: ElementDefinition): Rule? {
        val label = element.getExtensionByUrl(PRIVACY_LABEL_EXTENSION_URL)
        val method = when {
            label != null -> methodOf(label, element)
            // The cardinality already mandates absence, with or without a label.
            element.max == "0" -> PrivacyFilterMethod.Redact
            else -> return null
        }
        val dummyValue = label?.getExtensionByUrl("dummyValue")?.value
        if (method == PrivacyFilterMethod.Anonymize && dummyValue == null) {
            throw UnsupportedProfileException("${element.id}: ANONY needs a dummyValue")
        }
        return Rule(element.id, method, ElementPath.of(element, profile), dummyValue)
    }

    private fun methodOf(label: Extension, element: ElementDefinition): PrivacyFilterMethod {
        val policy = (label.getExtensionByUrl("obligationPolicy")?.value as? Coding)?.code
        return PrivacyFilterMethod.entries.find { it.obligationPolicy == policy }
            ?: throw UnsupportedProfileException("${element.id}: unknown obligationPolicy '$policy'")
    }

    private inner class Rule(
        private val elementId: String,
        val method: PrivacyFilterMethod,
        private val path: ElementPath,
        private val dummyValue: Type?,
    ) {
        fun apply(resource: Resource): PrivacyFilterLogEntry =
            try {
                val affected = when (method) {
                    PrivacyFilterMethod.Redact -> redactIn(resource)
                    PrivacyFilterMethod.Anonymize -> anonymizeIn(resource)
                    PrivacyFilterMethod.Pseudonymize -> pseudonymizeIn(resource)
                }
                logger.info("{} {}: {} value(s)", method, elementId, affected)
                val outcome = if (affected == 0) "not present" else "$affected value(s)"
                PrivacyFilterLogEntry(PrivacyLogSeverity.INFO, method, elementId, "$method, $outcome", affected)
            } catch (e: Exception) {
                logger.error("{} {} failed", method, elementId, e)
                PrivacyFilterLogEntry(PrivacyLogSeverity.ERROR, method, elementId, e.message ?: e.toString())
            }

        private fun redactIn(resource: Resource): Int =
            path.find(resource).onEach { it.parent.removeValue(it.property, it.value) }.size

        /**
         * Writes the placeholder wherever the parent element exists, also when the element itself
         * was empty: the profile fixes the element to the placeholder, so absence gives nothing away
         * that the placeholder would hide.
         */
        private fun anonymizeIn(resource: Resource): Int =
            path.findParents(resource).onEach { parent ->
                parent.listChildrenByName(path.leafProperty).forEach { parent.removeValue(path.leafProperty, it) }
                parent.setProperty(parent.declaredName(path.leafProperty), dummyValue!!.copy())
            }.size

        private fun pseudonymizeIn(resource: Resource): Int =
            path.find(resource).onEach { match ->
                // Replaced as a whole so that nothing next to the identifier (reference, display) survives.
                val replacement: Type = when (val value = match.value) {
                    is Reference -> Reference().setIdentifier(jobNumberFor(value.identifier))
                    is Identifier -> jobNumberFor(value)
                    else -> error("PSEUD applies to Reference and Identifier, not ${value.fhirType()}")
                }
                match.parent.removeValue(match.property, match.value)
                match.parent.setProperty(match.parent.declaredName(match.property), replacement)
            }.size

        private fun jobNumberFor(identifier: Identifier): Identifier {
            val original = identifier.value ?: error("no identifier value to derive the job number from")
            return Identifier().setSystem(SYSTEM_JOB_NUMBER).setValue(pseudonymize(original))
        }
    }

    /**
     * Extensions can sit on any element, including primitives, so the whole tree is walked.
     * Extensions that are kept are kept whole; contained resources are filtered on their own.
     */
    private fun removeUndeclaredExtensions(base: Base, path: String) {
        for ((property, extensions) in base.extensionLists()) {
            val declared = declaredExtensionUrls["$path.$property"].orEmpty()
            extensions.removeAll { it.url !in declared }
        }
        for (property in base.children()) {
            if (property.name in EXTENSION_PROPERTIES) continue
            property.values
                .filterNot { it is Resource }
                .forEach { removeUndeclaredExtensions(it, "$path.${property.name}") }
        }
    }

    private fun declareAsPseudonymized(resource: Resource, isContained: Boolean) {
        val meta = resource.meta
        val sourceProfiles = meta.profile.map { Coding(SYSTEM_SOURCE_PROFILE, it.value, null) }
        meta.tag = meta.tag.filter { it.system != SYSTEM_SOURCE_PROFILE } + sourceProfiles
        meta.profile = listOf(CanonicalType(profile.url))
        // FHIR forbids security labels on contained resources (dom-5); the container's label covers them.
        if (!isContained && meta.security.none { it.system == SYSTEM_PSEUDED && it.code == CODE_PSEUDED }) {
            meta.addSecurity(SYSTEM_PSEUDED, CODE_PSEUDED, "pseudonymized")
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(PrivacyFilter::class.java)
    }
}

/** HAPI lists a choice element as `item` but removes and sets it as `item[x]`. */
private fun Base.declaredName(property: String): String = getNamedProperty(property).name

private fun Base.removeValue(property: String, value: Base) = removeChild(declaredName(property), value)

/** Read from the typed accessors because `children()` omits `extension` on HAPI's BackboneType (e.g. Dosage). */
private fun Base.extensionLists(): Map<String, MutableList<Extension>> = buildMap {
    when (val base = this@extensionLists) {
        is Element -> put("extension", base.extension)
        is DomainResource -> put("extension", base.extension)
    }
    when (val base = this@extensionLists) {
        is BackboneElement -> put("modifierExtension", base.modifierExtension)
        is BackboneType -> put("modifierExtension", base.modifierExtension)
        is DomainResource -> put("modifierExtension", base.modifierExtension)
    }
}
