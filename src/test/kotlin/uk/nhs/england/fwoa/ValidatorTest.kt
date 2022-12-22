package uk.nhs.england.fwoa

import com.google.common.collect.ImmutableList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidatorTest {
    lateinit var INVALID_JSON_VALIDATOR_RESPONSE :String
    lateinit var validator: Validator

    var patientPDS = "{\"resourceType\":\"Patient\",\"id\":\"6aa13139-201b-4a54-9608-b49331a46d90\",\"meta\":{\"versionId\":\"2\",\"security\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-Confidentiality\",\"code\":\"U\",\"display\":\"unrestricted\"}]},\"extension\":[{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NominatedPharmacy\",\"valueReference\":{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y12345\"}}},{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-PreferredDispenserOrganization\",\"valueReference\":{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y23456\"}}},{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-MedicalApplianceSupplier\",\"valueReference\":{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y34567\"}}},{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-DeathNotificationStatus\",\"extension\":[{\"url\":\"deathNotificationStatus\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-DeathNotificationStatus\",\"version\":\"1.0.0\",\"code\":\"2\",\"display\":\"Formal - death notice received from Registrar of Deaths\"}]}},{\"url\":\"systemEffectiveDate\",\"valueDateTime\":\"2010-10-22T00:00:00+00:00\"}]},{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSCommunication\",\"extension\":[{\"url\":\"language\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-HumanLanguage\",\"version\":\"1.0.0\",\"code\":\"fr\",\"display\":\"French\"}]}},{\"url\":\"interpreterRequired\",\"valueBoolean\":true}]},{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-ContactPreference\",\"extension\":[{\"url\":\"PreferredWrittenCommunicationFormat\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-PreferredWrittenCommunicationFormat\",\"code\":\"12\",\"display\":\"Braille\"}]}},{\"url\":\"PreferredContactMethod\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-PreferredContactMethod\",\"code\":\"1\",\"display\":\"Letter\"}]}},{\"url\":\"PreferredContactTimes\",\"valueString\":\"Not after 7pm\"}]},{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-birthPlace\",\"valueAddress\":{\"city\":\"Manchester\",\"district\":\"Greater Manchester\",\"country\":\"GBR\"}}],\"identifier\":[{\"system\":\"https://fhir.nhs.uk/Id/nhs-number\",\"value\":\"9000000009\"}],\"name\":[{\"id\":\"123\",\"use\":\"usual\",\"family\":\"Smith\",\"given\":[\"Jane\"],\"prefix\":[\"Mrs\"],\"suffix\":[\"MBE\"],\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}}],\"telecom\":[{\"id\":\"789\",\"system\":\"phone\",\"value\":\"01632960587\",\"use\":\"home\",\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}},{\"id\":\"OC789\",\"system\":\"other\",\"_system\":{\"extension\":[{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-OtherContactSystem\",\"valueCoding\":{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-OtherContactSystem\",\"code\":\"textphone\",\"display\":\"Minicom (Textphone)\"}}]},\"value\":\"01632960587\",\"use\":\"home\",\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}}],\"gender\":\"female\",\"birthDate\":\"2010-10-22\",\"deceasedDateTime\":\"2010-10-22T00:00:00+00:00\",\"address\":[{\"id\":\"456\",\"extension\":[{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-AddressKey\",\"extension\":[{\"url\":\"type\",\"valueCoding\":{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-AddressKeyType\",\"code\":\"PAF\"}},{\"url\":\"value\",\"valueString\":\"12345678\"}]}],\"use\":\"home\",\"line\":[\"1 Trevelyan Square\",\"Boar Lane\",\"City Centre\",\"Leeds\",\"West Yorkshire\"],\"postalCode\":\"LS1 6AE\",\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}},{\"id\":\"T456\",\"extension\":[{\"url\":\"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-AddressKey\",\"extension\":[{\"url\":\"type\",\"valueCoding\":{\"system\":\"https://fhir.hl7.org.uk/CodeSystem/UKCore-AddressKeyType\",\"code\":\"PAF\"}},{\"url\":\"value\",\"valueString\":\"12345678\"}]}],\"use\":\"temp\",\"text\":\"Student Accommodation\",\"line\":[\"1 Trevelyan Square\",\"Boar Lane\",\"City Centre\",\"Leeds\",\"West Yorkshire\"],\"postalCode\":\"LS1 6AE\",\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}}],\"multipleBirthInteger\":1,\"contact\":[{\"id\":\"C123\",\"relationship\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0131\",\"code\":\"C\",\"display\":\"Emergency Contact\"}]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"01632960587\"}],\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}}],\"generalPractitioner\":[{\"id\":\"254406A3\",\"type\":\"Organization\",\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y12345\",\"period\":{\"start\":\"2020-01-01\",\"end\":\"2021-12-31\"}}}]}"
    var patientA28 = "{\"resourceType\":\"Patient\",\"id\":\"221c028d-e15b-4d60-8035-c460c8231fa3\",\"identifier\":[{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MRN\",\"value\":\"E3843677\"},{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MR\",\"value\":\"900070078\"},{\"system\":\"https://fhir.nhs.uk/Id/nhs-number\",\"value\":\"9333333333\"}],\"name\":[{\"family\":\"SMITH\",\"given\":[\"FREDRICA\"],\"prefix\":[\"MRS\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"+441234567890\",\"use\":\"home\"}],\"gender\":\"female\",\"birthDate\":\"1965-11-12\",\"address\":[{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}],\"contact\":[{\"name\":{\"family\":\"SMITH\",\"given\":[\"FRANCESCA\"],\"prefix\":[\"MRS\"]},\"address\":{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}}],\"generalPractitioner\":[{\"identifier\":{\"system\":\"https://fhir.hl7.org.uk/Id/gmc-number\",\"value\":\"G5612908\"},\"display\":\"Dr Gregory Townley\"},{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y06601\"},\"display\":\"MALTON GP PRACTICE\"}]}"
    var patientErrorInvalidGender = "{\"resourceType\":\"Patient\",\"id\":\"221c028d-e15b-4d60-8035-c460c8231fa3\",\"identifier\":[{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MRN\",\"value\":\"E3843677\"},{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MR\",\"value\":\"900070078\"},{\"system\":\"https://fhir.nhs.uk/Id/nhs-number\",\"value\":\"9333333333\"}],\"name\":[{\"family\":\"SMITH\",\"given\":[\"FREDRICA\"],\"prefix\":[\"MRS\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"+441234567890\",\"use\":\"home\"}],\"gender\":\"hydra\",\"birthDate\":\"1965-11-12\",\"address\":[{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}],\"contact\":[{\"name\":{\"family\":\"SMITH\",\"given\":[\"FRANCESCA\"],\"prefix\":[\"MRS\"]},\"address\":{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}}],\"generalPractitioner\":[{\"identifier\":{\"system\":\"https://fhir.hl7.org.uk/Id/gmp-number\",\"value\":\"G5612908\"},\"display\":\"Dr Gregory Townley\"},{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y06601\"},\"display\":\"MALTON GP PRACTICE\"}]}"
    var patientErrorNoNHSNumberValue = "{\"resourceType\":\"Patient\",\"id\":\"221c028d-e15b-4d60-8035-c460c8231fa3\",\"identifier\":[{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MRN\",\"value\":\"E3843677\"},{\"system\":\"https://fhir.yorkhospitals.nhs.uk/Id/MR\",\"value\":\"900070078\"},{\"system\":\"https://fhir.nhs.uk/Id/nhs-number\"}],\"name\":[{\"family\":\"SMITH\",\"given\":[\"FREDRICA\"],\"prefix\":[\"MRS\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"+441234567890\",\"use\":\"home\"}],\"gender\":\"female\",\"birthDate\":\"1965-11-12\",\"address\":[{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}],\"contact\":[{\"name\":{\"family\":\"SMITH\",\"given\":[\"FRANCESCA\"],\"prefix\":[\"MRS\"]},\"address\":{\"use\":\"home\",\"city\":\"MALTON\",\"district\":\"NORTH YORKSHIRE\",\"postalCode\":\"YO32 5TT\"}}],\"generalPractitioner\":[{\"identifier\":{\"system\":\"https://fhir.hl7.org.uk/Id/gmp-number\",\"value\":\"G5612908\"},\"display\":\"Dr Gregory Townley\"},{\"identifier\":{\"system\":\"https://fhir.nhs.uk/Id/ods-organization-code\",\"value\":\"Y06601\"},\"display\":\"MALTON GP PRACTICE\"}]}"
    fun getInvalidResponse() : ValidatorResponse {
      var validatorResponse = ValidatorResponse(isSuccessful = false)
      val msg = ValidatorErrorMessage()
      msg.msg ="Invalid JSON"
      msg.severity = "error"
      validatorResponse.errorMessages =
          ImmutableList.of(
              msg
          )
      return validatorResponse
    }

    @BeforeAll
    fun setup() {
        validator = Validator(ValidatorConstants.FHIR_R4, null)
        INVALID_JSON_VALIDATOR_RESPONSE = getInvalidResponse().toJson()
    }

    @Test
    fun simple_patient() {
        val resourceText =
            "{\"resourceType\":\"Patient\",\"id\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Generated by <a href=\\\"https://github.com/synthetichealth/synthea\\\">Synthea</a>.Version identifier: master-branch-latest-2-gfd2217b\\n .   Person seed: -5969330820059413579  Population seed: 1614314878171</div>\"},\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName\",\"valueString\":\"Son314 Vandervort697\"},{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-birthPlace\",\"valueAddress\":{\"city\":\"New Bedford\",\"state\":\"Massachusetts\",\"country\":\"US\"}},{\"url\":\"http://synthetichealth.github.io/synthea/disability-adjusted-life-years\",\"valueDecimal\":1.1872597438165626},{\"url\":\"http://synthetichealth.github.io/synthea/quality-adjusted-life-years\",\"valueDecimal\":70.81274025618343}],\"identifier\":[{\"system\":\"https://github.com/synthetichealth/synthea\",\"value\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MR\",\"display\":\"Medical Record Number\"}],\"text\":\"Medical Record Number\"},\"system\":\"http://hospital.smarthealthit.org\",\"value\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"SS\",\"display\":\"Social Security Number\"}],\"text\":\"Social Security Number\"},\"system\":\"http://hl7.org/fhir/sid/us-ssn\",\"value\":\"999-49-6778\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"DL\",\"display\":\"Driver's License\"}],\"text\":\"Driver's License\"},\"system\":\"urn:oid:2.16.840.1.113883.4.3.25\",\"value\":\"S99922723\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PPN\",\"display\":\"Passport Number\"}],\"text\":\"Passport Number\"},\"system\":\"http://standardhealthrecord.org/fhir/StructureDefinition/passportNumber\",\"value\":\"X72123203X\"}],\"name\":[{\"use\":\"official\",\"family\":\"Beier427\",\"given\":[\"Minnie888\"],\"prefix\":[\"Mrs.\"]},{\"use\":\"maiden\",\"family\":\"Jaskolski867\",\"given\":[\"Minnie888\"],\"prefix\":[\"Mrs.\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"555-390-9260\",\"use\":\"home\"}],\"gender\":\"female\",\"birthDate\":\"1949-01-01\",\"address\":[{\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/geolocation\",\"extension\":[{\"url\":\"latitude\",\"valueDecimal\":41.83492774608349},{\"url\":\"longitude\",\"valueDecimal\":-70.58336455010793}]}],\"line\":[\"862 Sauer Station Suite 31\"],\"city\":\"Plymouth\",\"state\":\"Massachusetts\",\"country\":\"US\"}],\"maritalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-MaritalStatus\",\"code\":\"M\",\"display\":\"M\"}],\"text\":\"M\"},\"multipleBirthInteger\":3,\"communication\":[{\"language\":{\"coding\":[{\"system\":\"urn:ietf:bcp:47\",\"code\":\"en-US\",\"display\":\"English\"}],\"text\":\"English\"}}]}"
        validator.validate(resourceText).let { assertTrue(it.isSuccessful) }

    }

    @Test
    fun empty() {
        val resourceText = ""
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)

    }

    @Test
    fun array() {
        val resourceText = "[1,2,3]"
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)

    }

    @Test
    fun null_json() {
        val resourceText = "null"
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)

    }

    @Test
    fun null_java() {
        val resourceText: String? = null
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)

    }

    @Test
    fun number_json() {
        val resourceText = "123"
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)
       // assertEquals(validatorStu3!!.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE)
    }

    @Test
    fun boolean_json() {
        val resourceText = "true"
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)
        //assertEquals(validatorStu3!!.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE)
    }

    @Test
    fun bad_json() {
        val resourceText = "{a:<>}}}"
        assertEquals(validator.validate(resourceText).toJson(), INVALID_JSON_VALIDATOR_RESPONSE)
       // assertEquals(validatorStu3!!.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE)
    }

    @Test
    fun PDSTest() {
        val resourceText = patientPDS
        var result = validator.validate(resourceText)
        result.let { assertTrue(it.isSuccessful) }
    }

    @Test
    fun PatientErrorNoNHSNumberValueTest() {
        val resourceText = patientErrorNoNHSNumberValue
        var result = validator.validate(resourceText)
        println(result.toJson())
        result.let { assertFalse(it.isSuccessful) }
    }

    @Test
    fun PatientErrorInvalidGenderTest() {
        val resourceText = patientErrorInvalidGender
        var result = validator.validate(resourceText)
        println(result.toJson())
        result.let { assertFalse(it.isSuccessful) }
    }

    @Test
    fun PatientA28Test() {
        val resourceText = patientA28
        var result = validator.validate(resourceText)
        result.let { assertTrue(it.isSuccessful) }
    }
}
