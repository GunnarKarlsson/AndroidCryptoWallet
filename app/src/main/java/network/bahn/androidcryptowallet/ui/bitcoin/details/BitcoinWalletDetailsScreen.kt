package network.bahn.androidcryptowallet.ui.bitcoin.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun BitcoinWalletDetailsScreen(
    onBack: () -> Unit,
    viewModel: BitcoinWalletDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEnter()
    }
    BitcoinWalletDetailsContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitcoinWalletDetailsContent(
    uiState: BitcoinWalletDetailsUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.address_copied)
    val address = uiState.receiveAddress
    val unconfirmed = uiState.unconfirmedBalanceSatoshis ?: 0L

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.wallet_details_title))
                        if (uiState.isWatchOnly) {
                            Text(
                                text = stringResource(R.string.wallet_watch_only),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing,
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.refresh_balance),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            DetailCard(
                title = stringResource(R.string.wallet_balance),
                value = stringResource(
                    R.string.bitcoin_amount,
                    StringUtils.formatBitcoinAmount(uiState.confirmedBalanceSatoshis ?: 0L),
                ),
                valueStyle = MaterialTheme.typography.headlineMedium,
                valueFontFamily = FontFamily.Monospace,
                caption = when {
                    unconfirmed != 0L -> stringResource(
                        R.string.unconfirmed_balance,
                        StringUtils.formatBitcoinAmount(unconfirmed),
                    )
                    else -> StringUtils.formatLastUpdated(
                        updatedAtMillis = uiState.balanceUpdatedAtMillis,
                        neverRefreshed = stringResource(R.string.last_updated_never),
                        lastUpdatedPattern = stringResource(R.string.last_updated),
                    )
                },
                errorMessage = uiState.errorMessage,
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailCard(
                title = stringResource(R.string.network_label),
                value = uiState.network?.label
                    ?: stringResource(R.string.receive_address_placeholder),
                valueStyle = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailCard(
                title = stringResource(R.string.receive_address),
                value = address ?: stringResource(R.string.receive_address_placeholder),
                valueStyle = MaterialTheme.typography.bodyLarge,
                valueFontFamily = FontFamily.Monospace,
                trailing = {
                    IconButton(
                        onClick = {
                            if (address == null) return@IconButton
                            copyAddress(context, address)
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        },
                        enabled = address != null,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_receive_address),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    valueStyle: TextStyle,
    caption: String? = null,
    errorMessage: String? = null,
    valueFontFamily: FontFamily? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = valueStyle,
                fontFamily = valueFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (caption != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun copyAddress(context: Context, address: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("bitcoin address", address))
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletDetailsZeroBalancePreview() {
    WalletTheme {
        BitcoinWalletDetailsContent(
            uiState = BitcoinWalletDetailsUiState(
                wallet = BitcoinWallet(
                    id = "1",
                    network = BitcoinNetwork.TESTNET4,
                    receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
                    confirmedBalanceSatoshis = 0,
                    unconfirmedBalanceSatoshis = 0,
                    balanceUpdatedAtMillis = 1_700_000_000_000L,
                ),
            ),
            onRefresh = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletDetailsScreenPreview() {
    WalletTheme {
        BitcoinWalletDetailsContent(
            uiState = BitcoinWalletDetailsUiState(
                wallet = BitcoinWallet(
                    id = "1",
                    network = BitcoinNetwork.TESTNET4,
                    receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
                    confirmedBalanceSatoshis = 4_225_100,
                    unconfirmedBalanceSatoshis = 0,
                    balanceUpdatedAtMillis = 1_700_000_000_000L,
                ),
            ),
            onRefresh = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletDetailsWatchOnlyPreview() {
    WalletTheme {
        BitcoinWalletDetailsContent(
            uiState = BitcoinWalletDetailsUiState(
                wallet = BitcoinWallet(
                    id = "mock:TESTNET4:tb1qwatch",
                    network = BitcoinNetwork.TESTNET4,
                    receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
                    scriptType = BitcoinScriptType.EXTERNAL,
                    kind = BitcoinWalletKind.WATCH_ONLY,
                    confirmedBalanceSatoshis = 50_000,
                    unconfirmedBalanceSatoshis = 0,
                    balanceUpdatedAtMillis = 1_700_000_000_000L,
                ),
            ),
            onRefresh = {},
            onBack = {},
        )
    }
}
