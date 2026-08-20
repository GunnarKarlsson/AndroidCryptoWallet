package network.bahn.androidcryptowallet.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import network.bahn.androidcryptowallet.BuildConfig
import network.bahn.androidcryptowallet.R

@Composable
fun DebugNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = stringResource(R.string.debug_next),
        )
    }
}
