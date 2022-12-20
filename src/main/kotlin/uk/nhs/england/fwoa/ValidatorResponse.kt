package uk.nhs.england.fwoa

import com.google.gson.GsonBuilder


data class ValidatorResponse (
    var isSuccessful: Boolean = false,
    var errorMessages: List<ValidatorErrorMessage>? = null
) {
    fun toJson() : String {
        var gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(this)
    }
}


class ValidatorErrorMessage {
    var severity: String? = null
    var msg: String? = null
}

