package chat.oi.feature.auth

import chat.oi.core.domain.model.User

data class AuthViewState(
    val selectedCountryCode: String = "+91",
    val phoneNumber: String = "",
    val isPhoneValid: Boolean = false,
    val otpCode: String = "",
    val isOtpComplete: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val remainingCooldownSeconds: Int = 0,
    val authenticatedUser: User? = null
)

sealed interface AuthIntent {
    data class UpdateCountryCode(val code: String) : AuthIntent
    data class UpdatePhoneNumber(val number: String) : AuthIntent
    data class UpdateOtp(val otp: String) : AuthIntent
    data object RequestOtp : AuthIntent
    data object VerifyOtp : AuthIntent
    data object ClearError : AuthIntent
}

sealed interface AuthSideEffect {
    data class NavigateToOtp(val fullPhoneNumber: String) : AuthSideEffect
    data class NavigateToHome(val user: User) : AuthSideEffect
    data class ShowToast(val message: String) : AuthSideEffect
}
