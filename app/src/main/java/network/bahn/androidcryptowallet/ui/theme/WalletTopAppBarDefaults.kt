package network.bahn.androidcryptowallet.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun walletTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Black,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)

/**
 * Use on a bottom-navigation shell [androidx.compose.material3.Scaffold] that has no [topBar].
 * Tab destinations own their [TopAppBar] and must consume status-bar insets themselves.
 * Without this, [ScaffoldDefaults.contentWindowInsets] pads tab content for the status bar
 * and the nested [TopAppBar] pads again.
 */
@OptIn(ExperimentalMaterial3Api::class)
val walletShellContentWindowInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        colors = walletTopAppBarColors(),
        windowInsets = TopAppBarDefaults.windowInsets,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}
