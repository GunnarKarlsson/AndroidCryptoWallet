package network.bahn.androidcryptowallet.ui.evm.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinMnemonicWordInputGrid
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinPassphraseField
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.DebugNextButton
import network.bahn.androidcryptowallet.ui.util.SecureWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmImportWalletScreen(
    mnemonicWords: List<String>,
    passphrase: String,
    isSubmitting: Boolean,
    canRestore: Boolean,
    errorMessage: String?,
    onMnemonicWordChange: (index: Int, value: String) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    SecureWindow()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = walletTopAppBarColors(),
                title = { Text(stringResource(R.string.restore_wallet_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = { DebugNextButton(onClick = onRestore) },
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
                text = stringResource(R.string.restore_wallet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.restore_wallet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BitcoinMnemonicWordInputGrid(
                words = mnemonicWords,
                enabled = !isSubmitting,
                onWordChange = onMnemonicWordChange,
            )
            BitcoinPassphraseField(
                value = passphrase,
                onValueChange = onPassphraseChange,
            )
            Button(
                onClick = onRestore,
                enabled = canRestore,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.restore_wallet))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EvmImportWalletScreenPreview() {
    WalletTheme {
        EvmImportWalletScreen(
            mnemonicWords = List(ETH_RESTORE_MNEMONIC_WORD_COUNT) { "" },
            passphrase = "",
            isSubmitting = false,
            canRestore = false,
            errorMessage = null,
            onMnemonicWordChange = { _, _ -> },
            onPassphraseChange = {},
            onRestore = {},
            onBack = {},
        )
    }
}
