package network.bahn.androidcryptowallet.ui.ethereum.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme

@Composable
fun EthereumEditWalletScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EthereumEditWalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EthereumEditWalletEvent.Saved -> onSaved()
            }
        }
    }
    EthereumEditWalletContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onConfirm = viewModel::onConfirm,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EthereumEditWalletContent(
    uiState: EthereumEditWalletUiState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_wallet_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onConfirm,
                enabled = uiState.canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.confirm_wallet_name))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.wallet_name_label)) },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { message ->
                    { Text(message) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumEditWalletDefaultPreview() {
    WalletTheme {
        EthereumEditWalletContent(
            uiState = EthereumEditWalletUiState(
                name = "Ethereum wallet",
                isWalletLoaded = true,
            ),
            onNameChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumEditWalletNamedPreview() {
    WalletTheme {
        EthereumEditWalletContent(
            uiState = EthereumEditWalletUiState(
                name = "Savings",
                isWalletLoaded = true,
            ),
            onNameChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumEditWalletSubmittingPreview() {
    WalletTheme {
        EthereumEditWalletContent(
            uiState = EthereumEditWalletUiState(
                name = "Savings",
                isSubmitting = true,
                isWalletLoaded = true,
            ),
            onNameChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}
