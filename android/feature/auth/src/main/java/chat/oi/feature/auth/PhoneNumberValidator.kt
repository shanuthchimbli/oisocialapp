package chat.oi.feature.auth

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

internal object PhoneNumberValidator {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private val supportedRegions = mapOf(
        "+91" to "IN",
        "+44" to "GB",
        "+1" to "US"
    )

    fun isValid(countryCallingCode: String, nationalNumber: String): Boolean {
        val region = supportedRegions[countryCallingCode] ?: return false
        if (nationalNumber.isEmpty() || nationalNumber.any { !it.isDigit() }) return false

        return try {
            val parsed = phoneNumberUtil.parse("$countryCallingCode$nationalNumber", null)
            phoneNumberUtil.isValidNumber(parsed) &&
                phoneNumberUtil.getRegionCodeForNumber(parsed) == region &&
                phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164) ==
                "$countryCallingCode$nationalNumber"
        } catch (_: NumberParseException) {
            false
        }
    }
}
