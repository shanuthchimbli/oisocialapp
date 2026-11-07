package chat.oi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import chat.oi.feature.chat.ChatViewModel
import chat.oi.feature.chat.ui.ChatListScreen
import chat.oi.feature.chat.ui.ConversationScreen
import chat.oi.feature.status.StatusViewModel
import chat.oi.feature.status.ui.StatusScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object PhoneAuth : Screen("auth/phone")
    data object OtpVerification : Screen("auth/otp/{phone}") {
        fun createRoute(phone: String): String = "auth/otp/${URLEncoder.encode(phone, "UTF-8")}"
    }
    data object ChatList : Screen("chats")
    data object Status : Screen("status")
    data object Conversation : Screen("conversation/{convId}/{username}") {
        fun createRoute(convId: String, username: String): String = "conversation/$convId/$username"
    }
}

@Composable
fun OiNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val statusViewModel: StatusViewModel = viewModel()

    LaunchedEffect(Unit) {
        authViewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthSideEffect.NavigateToOtp -> {
                    navController.navigate(Screen.OtpVerification.createRoute(effect.fullPhoneNumber))
                }
                is AuthSideEffect.NavigateToHome -> {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                    }
                }
                is AuthSideEffect.ShowToast -> {}
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.PhoneAuth.route
    ) {
        // 1. Phone Input Screen
        composable(Screen.PhoneAuth.route) {
            val state by authViewModel.viewState.collectAsState()
            PhoneAuthScreen(
                state = state,
                onIntent = authViewModel::handleIntent
            )
        }

        // 2. OTP Verification Screen
        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawPhone = backStackEntry.arguments?.getString("phone") ?: ""
            val fullPhone = URLDecoder.decode(rawPhone, "UTF-8")
            val state by authViewModel.viewState.collectAsState()

            OtpVerificationScreen(
                fullPhoneNumber = fullPhone,
                state = state,
                onIntent = authViewModel::handleIntent
            )
        }

        // 3. Chat List Home Screen
        composable(Screen.ChatList.route) {
            val state by chatViewModel.viewState.collectAsState()
            ChatListScreen(
                state = state,
                onIntent = chatViewModel::handleIntent,
                onOpenConversation = { convId, username ->
                    navController.navigate(Screen.Conversation.createRoute(convId, username))
                },
                onNavigateStatus = {
                    navController.navigate(Screen.Status.route)
                }
            )
        }

        // 4. Conversation Screen (1:1 Text Only)
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("convId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val state by chatViewModel.viewState.collectAsState()

            ConversationScreen(
                participantUsername = username,
                state = state,
                onIntent = chatViewModel::handleIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Status Screen (140-char text broadcast)
        composable(Screen.Status.route) {
            val state by statusViewModel.viewState.collectAsState()
            StatusScreen(
                state = state,
                onIntent = statusViewModel::handleIntent,
                onNavigateChats = { navController.popBackStack() }
            )
        }
    }
}
