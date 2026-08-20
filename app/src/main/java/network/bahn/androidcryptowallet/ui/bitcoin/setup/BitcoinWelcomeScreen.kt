package network.bahn.androidcryptowallet.ui.bitcoin.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.DebugNextButton

@Composable
fun BitcoinWelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DebugNextButton(
                onClick = onCreateWallet,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Icon(
                imageVector = Icons.Filled.CurrencyBitcoin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onCreateWallet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.create_wallet))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onImportWallet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_wallet))
            }
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWelcomeScreenPreview() {
    WalletTheme {
        BitcoinWelcomeScreen(
            onCreateWallet = {},
            onImportWallet = {},
        )
    }
}
