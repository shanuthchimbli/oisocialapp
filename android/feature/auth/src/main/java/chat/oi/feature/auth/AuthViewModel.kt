package chat.oi.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.oi.core.domain.model.AccountState
import chat.oi.core.domain.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _viewState = MutableStateFlow(AuthViewState())
    val viewState: StateFlow<AuthViewState> = _viewState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AuthSideEffect>()
    val sideEffect: SharedFlow<AuthSideEffect> = _sideEffect.asSharedFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UpdateCountryCode -> {
                _viewState.update { it.copy(selectedCountryCode = intent.code) }
                validatePhone(_viewState.value.phoneNumber)
            }
            is AuthIntent.UpdatePhoneNumber -> {
                val cleaned = intent.number.filter { it.isDigit() }
                _viewState.update { it.copy(phoneNumber = cleaned) }
                validatePhone(cleaned)
            }
            is AuthIntent.UpdateOtp -> {
                val digitsOnly = intent.otp.filter { it.isDigit() }.take(6)
                _viewState.update {
                    it.copy(
                        otpCode = digitsOnly,
                        isOtpComplete = digitsOnly.length == 6
                    )
                }
                if (digitsOnly.length == 6) {
                    handleIntent(AuthIntent.VerifyOtp)
                }
            }
            AuthIntent.RequestOtp -> requestOtp()
            AuthIntent.VerifyOtp -> verifyOtp()
            AuthIntent.ClearError -> _viewState.update { it.copy(errorMessage = null) }
        }
    }

    private fun validatePhone(number: String) {
        val isValid = when (_viewState.value.selectedCountryCode) {
            "+91" -> number.length == 10 && number.startsWithAny("6", "7", "8", "9")
            "+44" -> number.length == 10
            "+1" -> number.length == 10
            else -> number.length in 7..15
        }
        _viewState.update { it.copy(isPhoneValid = isValid) }
    }

    private fun String.startsWithAny(vararg prefixes: String): Boolean {
        return prefixes.any { this.startsWith(it) }
    }

    private fun requestOtp() {
        val state = _viewState.value
        if (!state.isPhoneValid) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, errorMessage = null) }
            val fullNumber = "${state.selectedCountryCode}${state.phoneNumber}"
            
            _viewState.update {
                it.copy(
                    isLoading = false,
                    remainingCooldownSeconds = 60
                )
            }
            _sideEffect.emit(AuthSideEffect.NavigateToOtp(fullNumber))
        }
    }

    private fun verifyOtp() {
        val state = _viewState.value
        if (state.otpCode.length != 6) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val simulatedUser = User(
                id = "018e69e4-bc23-7188-b769-cfbb98b846e1",
                phoneNumber = "${state.selectedCountryCode}${state.phoneNumber}",
                displayName = "Verified Citizen",
                username = "oi_user",
                accountState = AccountState.PROVISIONAL
            )

            _viewState.update {
                it.copy(
                    isLoading = false,
                    authenticatedUser = simulatedUser
                )
            }
            _sideEffect.emit(AuthSideEffect.NavigateToHome(simulatedUser))
        }
    }
}
