package network.bahn.androidcryptowallet.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePlaceholderScreen(
    onAddWallet: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = walletTopAppBarColors(),
                    title = { Text(stringResource(R.string.home_title)) },
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = stringResource(R.string.varna_logo),
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp)
                                .size(32.dp),
                        )
                    },
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onAddWallet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_wallet))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePlaceholderScreenPreview() {
    WalletTheme {
        HomePlaceholderScreen(onAddWallet = {})
    }
}
