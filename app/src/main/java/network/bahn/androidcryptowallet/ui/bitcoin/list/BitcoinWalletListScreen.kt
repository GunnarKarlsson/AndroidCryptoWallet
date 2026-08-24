package network.bahn.androidcryptowallet.ui.bitcoin.list

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.ui.bitcoin.BitcoinNetworkDropdown
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun BitcoinWalletListScreen(
    onBack: () -> Unit,
    onCreateWallet: () -> Unit,
    onRestoreWallet: () -> Unit,
    onNetworkStatus: () -> Unit,
    onWalletClick: (String) -> Unit,
    viewModel: BitcoinWalletListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BitcoinWalletListContent(
        uiState = uiState,
        onBack = onBack,
        onNetworkSelected = viewModel::onNetworkSelected,
        onCreateWallet = onCreateWallet,
        onRestoreWallet = onRestoreWallet,
        onNetworkStatus = onNetworkStatus,
        onWalletClick = onWalletClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitcoinWalletListContent(
    uiState: BitcoinWalletListUiState,
    onBack: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onCreateWallet: () -> Unit,
    onRestoreWallet: () -> Unit,
    onNetworkStatus: () -> Unit,
    onWalletClick: (String) -> Unit,
) {
    var showActionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.wallets_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showActionsSheet = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_wallet),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            BitcoinNetworkDropdown(
                selectedNetwork = uiState.selectedNetwork,
                onNetworkSelected = onNetworkSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                uiState.wallets.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.wallets, key = { it.id }) { wallet ->
                            BitcoinWalletListItem(
                                wallet = wallet,
                                onClick = { onWalletClick(wallet.id) },
                            )
                        }
                    }
                }
                uiState.isLoading -> {
                    val loadingDescription = stringResource(R.string.wallets_loading)
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.wallets_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionsSheet = false },
            sheetState = sheetState,
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.create_wallet)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showActionsSheet = false
                        onCreateWallet()
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.restore_wallet)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.SettingsBackupRestore,
                        contentDescription = null,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showActionsSheet = false
                        onRestoreWallet()
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.network_status_title)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showActionsSheet = false
                        onNetworkStatus()
                    },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BitcoinWalletListItem(
    wallet: BitcoinWallet,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StringUtils.walletDisplayName(
                        name = wallet.name,
                        fallback = stringResource(R.string.wallet_list_item_label),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (wallet.kind == BitcoinWalletKind.WATCH_ONLY) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.wallet_watch_only),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringUtils.truncateBitcoinAddress(wallet.receiveAddress),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                val confirmed = wallet.confirmedBalanceSatoshis
                Text(
                    text = if (confirmed == null) {
                        stringResource(R.string.receive_address_placeholder)
                    } else {
                        stringResource(
                            R.string.bitcoin_amount,
                            StringUtils.formatBitcoinAmount(confirmed),
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val unconfirmed = wallet.unconfirmedBalanceSatoshis
                if (unconfirmed != null && unconfirmed != 0L) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.unconfirmed_balance,
                            StringUtils.formatBitcoinAmount(unconfirmed),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletListLoadingPreview() {
    WalletTheme {
        BitcoinWalletListContent(
            uiState = BitcoinWalletListUiState(),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onNetworkStatus = {},
            onWalletClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletListEmptyPreview() {
    WalletTheme {
        BitcoinWalletListContent(
            uiState = BitcoinWalletListUiState(isLoading = false),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onNetworkStatus = {},
            onWalletClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinWalletListPopulatedPreview() {
    WalletTheme {
        BitcoinWalletListContent(
            uiState = BitcoinWalletListUiState(
                isLoading = false,
                wallets = listOf(
                    BitcoinWallet(
                        id = "1",
                        network = BitcoinNetwork.TESTNET4,
                        receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
                        name = "Savings",
                        confirmedBalanceSatoshis = 4_225_100,
                        unconfirmedBalanceSatoshis = 12_000,
                    ),
                    BitcoinWallet(
                        id = "2",
                        network = BitcoinNetwork.TESTNET4,
                        receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
                        confirmedBalanceSatoshis = 0,
                        unconfirmedBalanceSatoshis = 0,
                    ),
                    BitcoinWallet(
                        id = "mock:TESTNET4:tb1qp0we5epypgj4acd2c4au58045ruud2pd6heuee",
                        network = BitcoinNetwork.TESTNET4,
                        receiveAddress = "tb1qp0we5epypgj4acd2c4au58045ruud2pd6heuee",
                        scriptType = BitcoinScriptType.EXTERNAL,
                        kind = BitcoinWalletKind.WATCH_ONLY,
                    ),
                ),
            ),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onNetworkStatus = {},
            onWalletClick = {},
        )
    }
}
