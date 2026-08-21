package network.bahn.androidcryptowallet.ui.bitcoin.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun BitcoinWalletDetailsScreen(
    onBack: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    viewModel: BitcoinWalletDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEnter()
    }
    BitcoinWalletDetailsContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onLoadMore = viewModel::onLoadMore,
        onSend = onSend,
        onReceive = onReceive,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitcoinWalletDetailsContent(
    uiState: BitcoinWalletDetailsUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.address_copied)
    val address = uiState.receiveAddress
    val unconfirmed = uiState.unconfirmedBalanceSatoshis ?: 0L
    val listState = rememberLazyListState()

    LaunchedEffect(listState, uiState.hasMoreTransactions) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            lastVisible >= info.totalItemsCount - 2
        }.collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }

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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            item {
                WalletAddressHeader(
                    address = address,
                    networkLabel = uiState.network?.label,
                    onCopy = {
                        if (address == null) return@WalletAddressHeader
                        copyAddress(context, address)
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
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
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onReceive,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.receive_title))
                    }
                    Button(
                        onClick = onSend,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.send_title))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Text(
                    text = stringResource(R.string.wallet_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            when {
                uiState.isLoadingTransactions && uiState.transactions.isEmpty() -> {
                    item {
                        val loadingDescription = stringResource(R.string.wallet_transactions_loading)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.semantics {
                                    contentDescription = loadingDescription
                                },
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                uiState.transactions.isEmpty() && uiState.transactionsErrorMessage == null -> {
                    item {
                        Text(
                            text = stringResource(R.string.wallet_transactions_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    items(uiState.transactions, key = { it.txid }) { tx ->
                        TransactionRow(tx = tx)
                    }
                }
            }
            uiState.transactionsErrorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (uiState.isLoadingMoreTransactions) {
                item {
                    val loadingMore = stringResource(R.string.wallet_transactions_loading_more)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .semantics { contentDescription = loadingMore },
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: BitcoinTransactionSummary) {
    val amount = StringUtils.formatBitcoinAmount(tx.netSatoshis)
    val signedAmount = if (tx.netSatoshis > 0L) "+$amount" else amount
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bitcoin_amount, signedAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TxStatusMarker(confirmed = tx.confirmed)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = StringUtils.truncateBitcoinAddress(tx.txid),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tx.confirmed && tx.blockTimeSeconds != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringUtils.formatDateTime(tx.blockTimeSeconds * 1_000L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TxStatusMarker(confirmed: Boolean) {
    val label = if (confirmed) {
        stringResource(R.string.tx_status_confirmed)
    } else {
        stringResource(R.string.tx_status_mempool)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (confirmed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun WalletAddressHeader(
    address: String?,
    networkLabel: String?,
    onCopy: () -> Unit,
) {
    val displayAddress = address ?: stringResource(R.string.receive_address_placeholder)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCopy,
                enabled = address != null,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.copy_receive_address),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = displayAddress,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = 13.sp,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.network_value,
                networkLabel ?: stringResource(R.string.receive_address_placeholder),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 40.dp),
        )
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun previewWallet() = BitcoinWallet(
    id = "1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
    confirmedBalanceSatoshis = 4_225_100,
    unconfirmedBalanceSatoshis = 0,
    balanceUpdatedAtMillis = 1_700_000_000_000L,
)

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletDetailsZeroBalancePreview() {
    WalletTheme {
        BitcoinWalletDetailsContent(
            uiState = BitcoinWalletDetailsUiState(
                wallet = previewWallet().copy(
                    confirmedBalanceSatoshis = 0,
                    unconfirmedBalanceSatoshis = 0,
                ),
                isLoadingTransactions = false,
            ),
            onRefresh = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
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
                wallet = previewWallet(),
                isLoadingTransactions = true,
            ),
            onRefresh = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
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
                wallet = previewWallet().copy(
                    id = "mock:TESTNET4:tb1qwatch",
                    scriptType = BitcoinScriptType.EXTERNAL,
                    kind = BitcoinWalletKind.WATCH_ONLY,
                    confirmedBalanceSatoshis = 50_000,
                ),
                isLoadingTransactions = false,
                transactions = listOf(
                    BitcoinTransactionSummary(
                        txid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        confirmed = false,
                        blockTimeSeconds = null,
                        netSatoshis = 50_000,
                        feeSatoshis = 200,
                    ),
                    BitcoinTransactionSummary(
                        txid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        confirmed = true,
                        blockTimeSeconds = 1_700_000_000L,
                        netSatoshis = -12_345,
                        feeSatoshis = 150,
                    ),
                ),
            ),
            onRefresh = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletDetailsTransactionsEmptyPreview() {
    WalletTheme {
        BitcoinWalletDetailsContent(
            uiState = BitcoinWalletDetailsUiState(
                wallet = previewWallet(),
                isLoadingTransactions = false,
            ),
            onRefresh = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
            onBack = {},
        )
    }
}
