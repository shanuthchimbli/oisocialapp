package chat.oi.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel()
    }

    @Test
    fun testIndianPhoneNumberValidation() {
        viewModel.handleIntent(AuthIntent.UpdateCountryCode("+91"))

        // Invalid: too short
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("98765"))
        assertFalse(viewModel.viewState.value.isPhoneValid)

        // Invalid: starts with 1
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("1234567890"))
        assertFalse(viewModel.viewState.value.isPhoneValid)

        // Valid: 10 digits starting with 9
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("9876543210"))
        assertTrue(viewModel.viewState.value.isPhoneValid)
    }

    @Test
    fun testUnitedKingdomPhoneNumberValidation() {
        viewModel.handleIntent(AuthIntent.UpdateCountryCode("+44"))

        // Valid: 10 digits
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("7911123456"))
        assertTrue(viewModel.viewState.value.isPhoneValid)

        // Invalid: 8 digits
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("79111234"))
        assertFalse(viewModel.viewState.value.isPhoneValid)
    }

    @Test
    fun testUnitedStatesPhoneNumberValidation() {
        viewModel.handleIntent(AuthIntent.UpdateCountryCode("+1"))

        // Valid: 10 digits
        viewModel.handleIntent(AuthIntent.UpdatePhoneNumber("2025550143"))
        assertTrue(viewModel.viewState.value.isPhoneValid)
    }

    @Test
    fun testOtpInputLimitsTo6Digits() {
        viewModel.handleIntent(AuthIntent.UpdateOtp("123456789"))
        assertEquals("123456", viewModel.viewState.value.otpCode)
        assertTrue(viewModel.viewState.value.isOtpComplete)
    }
}
