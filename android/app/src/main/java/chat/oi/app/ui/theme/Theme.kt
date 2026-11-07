package chat.oi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    secondary = Sky500,
    background = Slate900,
    surface = Slate800,
    onPrimary = TextDark,
    onSecondary = White,
    onBackground = White,
    onSurface = White
)

@Composable
fun OiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
