Profile: EPAResearchMedication
Id: epa-research-medication
Parent: Medication

// Medication contains no privacy labels and will be transferred as is

// Extensions
* extension contains 
    RxPrescriptionProcessIdentifierExtension named rxPrescriptionProcessIdentifier 0..1 MS and
    MedicationIsVaccineExtension named isVaccine 0..1 MS and
    DrugCategoryExtension named drugCategory 0..1 MS and
    ExtensionNormgroesseDeBasis named normSizeCode 0..1 MS and
    MedicationFormulationPackagingExtension named packaging 0..1 MS and
    MedicationManufacturingInstructionsExtension named manufacturingInstructions 0..1 MS and
    EPAMedicationTypeExtension named type 0..1 MS
* extension[isVaccine]
  * valueBoolean MS
* extension[drugCategory]
  * valueCoding MS
    * system MS
    * code MS
* extension[normSizeCode] ^short = "Package size according to N-designation"
* extension[normSizeCode] ^definition = "Description of the therapy-appropriate package size (e.g., N1)"
  * valueCode MS
* extension[packaging]
  * valueString MS
* extension[manufacturingInstructions]
  * valueString MS
