package network.bahn.androidcryptowallet.ui.bitcoin.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.DebugNextButton
import network.bahn.androidcryptowallet.ui.util.SecureWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitcoinCreateWalletScreen(
    words: List<String>,
    passphrase: String,
    onPassphraseChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    SecureWindow()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.recovery_phrase_copied)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_wallet)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = { DebugNextButton(onClick = onContinue) },
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
            BitcoinSetupStepHeader(
                stepLabel = stringResource(R.string.recovery_phrase_step),
                progress = 0.5f,
            )
            Text(
                text = stringResource(R.string.recovery_phrase_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.recovery_phrase_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BitcoinMnemonicWordGrid(words = words)
            BitcoinPassphraseField(
                value = passphrase,
                onValueChange = onPassphraseChange,
            )
            OutlinedButton(
                onClick = {
                    copyPhrase(context, words)
                    scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.copy_recovery_phrase))
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.written_it_down))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun copyPhrase(context: Context, words: List<String>) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("recovery phrase", words.joinToString(" ")))
}

@Preview(showBackground = true)
@Composable
private fun BitcoinCreateWalletScreenPreview() {
    WalletTheme {
        BitcoinCreateWalletScreen(
            words = BitcoinPlaceholderMnemonic.WORDS,
            passphrase = "",
            onPassphraseChange = {},
            onContinue = {},
            onBack = {},
        )
    }
}
