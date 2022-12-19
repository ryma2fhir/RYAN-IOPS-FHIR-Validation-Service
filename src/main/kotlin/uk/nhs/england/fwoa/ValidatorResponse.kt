package uk.nhs.england.fwoa

import com.google.gson.Gson;

data class ValidatorResponse (
    var isSuccessful: Boolean = false,
    var errorMessages: List<ValidatorErrorMessage>? = null
) {
    companion object {
        fun builder(): Any {

        }
    }
}


class ValidatorErrorMessage {
    private val severity: String? = null
    private val msg: String? = null
}

