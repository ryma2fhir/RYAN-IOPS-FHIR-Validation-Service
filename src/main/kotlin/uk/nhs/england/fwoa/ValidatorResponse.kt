package uk.nhs.england.fwoa

import com.google.gson.Gson;

data class ValidatorResponse (
    var isSuccessful: Boolean = false,
    var errorMessages: List<ValidatorErrorMessage>? = null
)


class ValidatorErrorMessage {
    private val severity: String? = null
    private val msg: String? = null
}

