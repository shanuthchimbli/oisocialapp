package chat.oi.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chat.oi.feature.auth.AuthSideEffect
import chat.oi.feature.auth.AuthViewModel
import chat.oi.feature.auth.OtpVerificationScreen
import chat.oi.feature.auth.PhoneAuthScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun OiNavHost(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.viewState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthSideEffect.NavigateToOtp -> {
                    val encodedPhone = URLEncoder.encode(effect.formattedPhone, StandardCharsets.UTF_8.toString())
                    navController.navigate("otp/$encodedPhone")
                }
                is AuthSideEffect.NavigateToHome -> {
                    navController.navigate("home") {
                        popUpTo("phone_auth") { inclusive = true }
                    }
                }
                is AuthSideEffect.ShowSnackbar -> {}
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "phone_auth",
        modifier = modifier
    ) {
        composable("phone_auth") {
            PhoneAuthScreen(
                state = authState,
                onIntent = authViewModel::handleIntent
            )
        }

        composable(
            route = "otp/{phone}",
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawPhone = backStackEntry.arguments?.getString("phone") ?: ""
            val decodedPhone = URLDecoder.decode(rawPhone, StandardCharsets.UTF_8.toString())

            OtpVerificationScreen(
                state = authState,
                phoneNumber = decodedPhone,
                onIntent = authViewModel::handleIntent
            )
        }

        composable("home") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Oi",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticated as @${authState.authenticatedUser?.username ?: "user"}",
                    fontSize = 16.sp,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Zero media. One person, one account. Pure speed.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
