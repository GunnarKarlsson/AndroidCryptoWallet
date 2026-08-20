package network.bahn.androidcryptowallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WalletLightColorScheme = lightColorScheme(
    primary = WalletBlue,
    onPrimary = Color.White,
    primaryContainer = WalletBlueContainer,
    onPrimaryContainer = WalletOnBackground,
    secondary = WalletBlue,
    onSecondary = Color.White,
    background = WalletBackground,
    onBackground = WalletOnBackground,
    surface = WalletBackground,
    onSurface = WalletOnBackground,
    surfaceVariant = WalletSurfaceVariant,
    onSurfaceVariant = WalletMuted,
    outline = WalletOutline,
    error = WalletError,
    onError = Color.White,
)

@Composable
fun WalletTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = WalletLightColorScheme,
        typography = WalletTypography,
        content = content,
    )
}
