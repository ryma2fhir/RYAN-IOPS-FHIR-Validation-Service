package uk.nhs.england.fwoa

data class ValidatorResponse (
    var isSuccessful: Boolean = false,
    var errorMessages: List<ValidatorErrorMessage>? = null
)


class ValidatorErrorMessage {
    private val severity: String? = null
    private val msg: String? = null
}

