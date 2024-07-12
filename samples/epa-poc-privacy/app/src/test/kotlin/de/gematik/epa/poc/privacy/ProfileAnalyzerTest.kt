package de.gematik.epa.poc.privacy

import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.StructureDefinition
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ProfileAnalyzerTest {
    @Test
    fun testFindAllProfiles() {
        val validator = Validator(FhirContextR4, Path("../../../../dev-epa-medication/src/fhir"))
        val l = validator.validationSupport.fetchAllNonBaseStructureDefinitions<StructureDefinition>() ?: emptyList()
        val s = l.distinctBy { it.url }.filter { !it.url.startsWith("http://hl7.org/") }.sortedBy { it.name }.sortedBy { it.type }

        println("|Name                                       |Type                                       |URL|")
        println("|-------------------------------------------|-------------------------------------------|---|")
        s.forEach {
            println("|${it.name.padEnd(40)}|${it.type.padEnd(40)}|${it.url}|")
        }

    }
}