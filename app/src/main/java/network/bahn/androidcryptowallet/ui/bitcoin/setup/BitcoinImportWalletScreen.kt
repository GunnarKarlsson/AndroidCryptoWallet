package network.bahn.androidcryptowallet.ui.bitcoin.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.DebugNextButton
import network.bahn.androidcryptowallet.ui.util.SecureWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitcoinImportWalletScreen(
    mnemonic: String,
    passphrase: String,
    onMnemonicChange: (String) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    SecureWindow()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_wallet_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = { DebugNextButton(onClick = onImport) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.import_wallet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.import_wallet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = mnemonic,
                onValueChange = onMnemonicChange,
                label = { Text(stringResource(R.string.import_mnemonic_label)) },
                placeholder = { Text(stringResource(R.string.import_mnemonic_placeholder)) },
                minLines = 4,
            )
            BitcoinPassphraseField(
                value = passphrase,
                onValueChange = onPassphraseChange,
            )
            Button(
                onClick = onImport,
                enabled = mnemonic.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_wallet))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinImportWalletScreenPreview() {
    WalletTheme {
        BitcoinImportWalletScreen(
            mnemonic = "",
            passphrase = "",
            onMnemonicChange = {},
            onPassphraseChange = {},
            onImport = {},
            onBack = {},
        )
    }
}
