package de.gematik.epa.poc.privacy

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.ElementDefinition
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
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
    val setterName = "set${property.capitalizeAsciiOnly()}"
    this::class.members.first() { it.name == setterName }.call(this, value)
}

fun Base.getValuesForPath(path: String): List<Any> {
    return FhirPath.evaluate(this, path)
}

val ElementDefinition.typeName: String?
    get() = this.type.firstOrNull()?.code

