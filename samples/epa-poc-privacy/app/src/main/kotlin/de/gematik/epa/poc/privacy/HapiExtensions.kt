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
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.ElementDefinition
import java.nio.file.Path
import kotlin.io.path.readText

val FhirPath = FHIRPathEngine(SimpleWorkerContext())
val FhirContextR4 = FhirContext.forR4()

fun IBase.toPrettyString(): String {
    val jsonParser = FhirContextR4.newJsonParser()
    jsonParser.setPrettyPrint(true)
    return jsonParser.encodeToString(this)
}

fun loadResource(path: Path): IBaseResource {
    val parser = FhirContextR4.newJsonParser()
    return parser.parseResource(path.readText())
}

fun Base.setValue(property: String, value: Any?) {
    val setterName = "set" + property.replaceFirstChar { it.uppercaseChar() }
    this::class.members.first { it.name == setterName }.call(this, value)
}

fun Base.getValuesForPath(path: String): List<Any> {
    return FhirPath.evaluate(this, path)
}

val ElementDefinition.typeName: String?
    get() = this.type.firstOrNull()?.code
