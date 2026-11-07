package chat.oi.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import chat.oi.app.ui.navigation.OiNavHost
import chat.oi.app.ui.theme.OiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Security Invariant (SEC-006 / MASVS-PLATFORM-1):
        // Prevent screen capture, task-switching previews, and spyware screenshot leakage
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        setContent {
            OiTheme {
                OiNavHost()
            }
        }
    }
}
