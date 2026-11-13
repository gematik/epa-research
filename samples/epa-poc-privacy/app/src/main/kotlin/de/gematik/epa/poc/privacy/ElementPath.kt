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

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.ElementDefinition
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition

/** The profile uses a construct the filter cannot apply. Raised while reading the profile, never per resource. */
class UnsupportedProfileException(message: String) : IllegalArgumentException(message)

/**
 * Where a profile element lives inside a resource instance.
 *
 * Built from an ElementDefinition id such as
 * `Medication.ingredient.item[x]:itemCodeableConcept.coding:atc-de`, which is the only
 * place that carries both the choice type and the slice an element belongs to.
 */
internal class ElementPath private constructor(private val steps: List<Step>) {

    /** A value together with the parent and property it hangs on, which is what removing or replacing it needs. */
    class Match(val parent: Base, val property: String, val value: Base)

    private class Step(
        val property: String,
        val choiceType: String?,
        val belongsToSlice: ((Base) -> Boolean)?,
    ) {
        fun select(parent: Base): List<Base> =
            parent.listChildrenByName(property)
                .filter { choiceType == null || it.fhirType().equals(choiceType, ignoreCase = true) }
                .filter { belongsToSlice == null || belongsToSlice.invoke(it) }
    }

    val leafProperty: String get() = steps.last().property

    fun find(resource: Resource): List<Match> =
        findParents(resource).flatMap { parent ->
            steps.last().select(parent).map { Match(parent, leafProperty, it) }
        }

    fun findParents(resource: Resource): List<Base> =
        steps.dropLast(1).fold(listOf<Base>(resource)) { parents, step -> parents.flatMap(step::select) }

    companion object {
        private const val CHOICE_SUFFIX = "[x]"

        fun of(element: ElementDefinition, profile: StructureDefinition): ElementPath {
            val segments = element.id.split('.')
            val steps = segments.indices.drop(1).map { index ->
                parseStep(segments[index], segments.take(index + 1).joinToString("."), profile)
            }
            if (steps.isEmpty()) throw UnsupportedProfileException("${element.id}: a privacy label on the resource root is not supported")
            return ElementPath(steps)
        }

        private fun parseStep(segment: String, elementId: String, profile: StructureDefinition): Step {
            val name = segment.substringBefore(':')
            val sliceName = segment.substringAfter(':', "").ifEmpty { null }
            if (!name.endsWith(CHOICE_SUFFIX)) {
                return Step(name, null, sliceName?.let { sliceMembership(name, elementId, profile) })
            }
            // A slice on a choice element names the type: item[x]:itemCodeableConcept.
            val property = name.removeSuffix(CHOICE_SUFFIX)
            return Step(property, sliceName?.removePrefix(property), null)
        }

        private fun sliceMembership(property: String, sliceId: String, profile: StructureDefinition): (Base) -> Boolean {
            if (property in EXTENSION_PROPERTIES) {
                val urls = profile.differential.element.first { it.id == sliceId }.extensionProfileUrls
                return { (it as? Extension)?.url in urls }
            }

            val fixedValues = profile.differential.element
                .filter { it.id.startsWith("$sliceId.") }
                .mapNotNull { child ->
                    val fixed = (child.fixed ?: child.pattern) as? PrimitiveType<*> ?: return@mapNotNull null
                    child.id.removePrefix("$sliceId.").split('.') to fixed.valueAsString
                }
            if (fixedValues.isEmpty()) {
                throw UnsupportedProfileException("$sliceId: slice has no fixed or pattern value to recognise its entries by")
            }
            return { candidate ->
                fixedValues.all { (relativePath, expected) ->
                    relativePath
                        .fold(listOf(candidate)) { bases, name -> bases.flatMap { it.listChildrenByName(name) } }
                        .any { (it as? PrimitiveType<*>)?.valueAsString == expected }
                }
            }
        }
    }
}

internal val EXTENSION_PROPERTIES = setOf("extension", "modifierExtension")

/** Canonical URLs of the extension definitions an `extension` slice admits. */
internal val ElementDefinition.extensionProfileUrls: Set<String>
    get() = type.flatMap { it.profile }.map { it.value }.toSet()
