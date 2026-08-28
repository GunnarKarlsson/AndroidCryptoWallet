package network.bahn.androidcryptowallet.ui.ethereum.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import network.bahn.androidcryptowallet.ui.chain.receiveClipboardLabelRes
import network.bahn.androidcryptowallet.ui.chain.walletListItemLabelRes
import network.bahn.androidcryptowallet.ui.ethereum.evmNativeAmountLabel
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun EthereumWalletDetailsScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    viewModel: EthereumWalletDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEnter()
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EthereumWalletDetailsEvent.WalletDeleted -> onBack()
            }
        }
    }
    EthereumWalletDetailsContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onRefreshTransactions = viewModel::onRefreshTransactions,
        onLoadMore = viewModel::onLoadMore,
        onSend = onSend,
        onReceive = onReceive,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EthereumWalletDetailsContent(
    uiState: EthereumWalletDetailsUiState,
    onRefresh: () -> Unit,
    onRefreshTransactions: () -> Unit,
    onLoadMore: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.address_copied)
    val address = uiState.address
    val balanceWei = uiState.balanceWei ?: "0"
    val nativeSymbol = uiState.network?.nativeSymbol.orEmpty()
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

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeleting) onDismissDeleteConfirm()
            },
            title = { Text(text = stringResource(R.string.delete_wallet_title)) },
            text = { Text(text = stringResource(R.string.delete_wallet_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(
                        text = stringResource(R.string.delete_wallet_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteConfirm,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = walletTopAppBarColors(),
                title = {
                    Text(
                        text = StringUtils.walletDisplayName(
                            name = uiState.wallet?.name,
                            fallback = stringResource(
                                uiState.family?.walletListItemLabelRes
                                    ?: R.string.ethereum_wallet_list_item_label,
                            ),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                        onClick = onDeleteClick,
                        enabled = !uiState.isDeleting,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_wallet),
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        enabled = !uiState.isDeleting,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit_wallet),
                        )
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
                        copyAddress(context, address, uiState.family)
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                DetailCard(
                    title = stringResource(R.string.wallet_balance),
                    value = evmNativeAmountLabel(
                        StringUtils.formatEthereumAmount(balanceWei),
                        nativeSymbol,
                    ),
                    valueStyle = MaterialTheme.typography.headlineMedium,
                    valueFontFamily = FontFamily.Monospace,
                    caption = StringUtils.formatLastUpdated(
                        updatedAtMillis = uiState.balanceUpdatedAtMillis,
                        neverRefreshed = stringResource(R.string.last_updated_never),
                        lastUpdatedPattern = stringResource(R.string.last_updated),
                    ),
                    errorMessage = uiState.errorMessage,
                    trailing = {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.wallet_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onRefreshTransactions,
                        enabled = !uiState.isLoadingTransactions &&
                            !uiState.isRefreshingTransactions,
                    ) {
                        if (uiState.isRefreshingTransactions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.refresh_transactions),
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
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
                    items(uiState.transactions.size, key = { uiState.transactions[it].hash }) { index ->
                        EthereumTransactionRow(
                            tx = uiState.transactions[index],
                            nativeSymbol = nativeSymbol,
                        )
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
private fun EthereumTransactionRow(
    tx: EvmTransactionSummary,
    nativeSymbol: String,
) {
    val netWei = tx.netWei.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val amount = StringUtils.formatEthereumAmount(tx.netWei)
    val signedAmount = if (netWei > java.math.BigInteger.ZERO) "+$amount" else amount
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
                    text = evmNativeAmountLabel(signedAmount, nativeSymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                EthereumTxStatusMarker(confirmed = tx.confirmed)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = StringUtils.truncateEthereumAddress(tx.hash),
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
private fun EthereumTxStatusMarker(confirmed: Boolean) {
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
    trailing: @Composable (() -> Unit)? = null,
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
                .padding(
                    start = 24.dp,
                    end = if (trailing != null) 8.dp else 24.dp,
                    top = if (trailing != null) 8.dp else 24.dp,
                    bottom = 24.dp,
                ),
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

private fun copyAddress(context: Context, address: String, family: EvmFamily?) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val label = family?.receiveClipboardLabelRes?.let(context::getString)
        ?: context.getString(R.string.receive_clipboard_label_eth)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, address))
}

private fun previewWallet() = EvmWallet(
    id = "1",
    network = EvmNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    name = "Savings",
    balanceWei = "1000000000000000000",
    balanceUpdatedAtMillis = 1_700_000_000_000L,
)

@Preview(showBackground = true)
@Composable
private fun EthereumWalletDetailsWithTransactionsPreview() {
    WalletTheme {
        EthereumWalletDetailsContent(
            uiState = EthereumWalletDetailsUiState(
                wallet = previewWallet(),
                transactions = listOf(
                    EvmTransactionSummary(
                        hash = "0x1bf67e8fd7f28df862dd8c0adf023a0e836c051802f4682c9f15c0e9bf7d722e",
                        confirmed = true,
                        blockTimeSeconds = 1_787_613_816L,
                        netWei = "1000000000000000000",
                        feeWei = "32125058233059",
                    ),
                    EvmTransactionSummary(
                        hash = "0x0972edf7fbab6883e4b52648beb8b2404491f4e2800f7ecd4409e5f4bb782878",
                        confirmed = true,
                        blockTimeSeconds = 1_787_493_624L,
                        netWei = "100000000000000",
                        feeWei = "33661273633823",
                    ),
                ),
            ),
            onRefresh = {},
            onRefreshTransactions = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
            onBack = {},
            onEdit = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumWalletDetailsZeroBalancePreview() {
    WalletTheme {
        EthereumWalletDetailsContent(
            uiState = EthereumWalletDetailsUiState(
                wallet = previewWallet().copy(balanceWei = "0"),
            ),
            onRefresh = {},
            onRefreshTransactions = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
            onBack = {},
            onEdit = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumWalletDetailsScreenPreview() {
    WalletTheme {
        EthereumWalletDetailsContent(
            uiState = EthereumWalletDetailsUiState(
                wallet = previewWallet(),
                isRefreshing = true,
            ),
            onRefresh = {},
            onRefreshTransactions = {},
            onLoadMore = {},
            onSend = {},
            onReceive = {},
            onBack = {},
            onEdit = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
        )
    }
}
