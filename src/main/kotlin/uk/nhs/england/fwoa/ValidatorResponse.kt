package uk.nhs.england.fwoa

import lombok.Builder
import lombok.Value

@Builder
@Value
class ValidatorResponse {
    private val isSuccessful = false
    private val errorMessages: List<ValidatorErrorMessage>? = null
}

@Builder
@Value
internal class ValidatorErrorMessage {
    private val severity: String? = null
    private val msg: String? = null
}

