package network.bahn.androidcryptowallet.ui.ethereum.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.ui.chain.WalletListItemTitle
import network.bahn.androidcryptowallet.ui.chain.chainIconRes
import network.bahn.androidcryptowallet.ui.chain.walletListItemLabelRes
import network.bahn.androidcryptowallet.ui.chain.walletsTitleRes
import network.bahn.androidcryptowallet.ui.ethereum.EthereumNetworkDropdown
import network.bahn.androidcryptowallet.ui.ethereum.evmNativeAmountLabel
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun EthereumWalletListScreen(
    onBack: () -> Unit,
    onCreateWallet: () -> Unit,
    onRestoreWallet: () -> Unit,
    onWalletClick: (walletId: String) -> Unit,
    viewModel: EthereumWalletListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EthereumWalletListContent(
        uiState = uiState,
        onBack = onBack,
        onNetworkSelected = viewModel::onNetworkSelected,
        onCreateWallet = onCreateWallet,
        onRestoreWallet = onRestoreWallet,
        onWalletClick = onWalletClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EthereumWalletListContent(
    uiState: EthereumWalletListUiState,
    onBack: () -> Unit,
    onNetworkSelected: (EvmNetwork) -> Unit,
    onCreateWallet: () -> Unit,
    onRestoreWallet: () -> Unit,
    onWalletClick: (walletId: String) -> Unit,
) {
    var showActionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = walletTopAppBarColors(),
                    title = { Text(stringResource(uiState.family.walletsTitleRes)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    },
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
            EthereumNetworkDropdown(
                networks = uiState.availableNetworks,
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
                            EthereumWalletListItem(
                                wallet = wallet,
                                family = uiState.family,
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
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EthereumWalletListItem(
    wallet: EthereumWallet,
    family: EvmFamily,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            WalletListItemTitle(
                name = StringUtils.walletDisplayName(
                    name = wallet.name,
                    fallback = stringResource(family.walletListItemLabelRes),
                ),
                chainIconRes = family.chainIconRes,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (wallet.balanceWei == null) {
                    stringResource(R.string.receive_address_placeholder)
                } else {
                    evmNativeAmountLabel(
                        StringUtils.formatEthereumAmount(wallet.balanceWei),
                        wallet.network.nativeSymbol,
                    )
                },
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = StringUtils.truncateEthereumAddress(wallet.address),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumWalletListLoadingPreview() {
    WalletTheme {
        EthereumWalletListContent(
            uiState = EthereumWalletListUiState(),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onWalletClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumWalletListEmptyPreview() {
    WalletTheme {
        EthereumWalletListContent(
            uiState = EthereumWalletListUiState(isLoading = false),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onWalletClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumWalletListPopulatedPreview() {
    WalletTheme {
        EthereumWalletListContent(
            uiState = EthereumWalletListUiState(
                isLoading = false,
                wallets = listOf(
                    EthereumWallet(
                        id = "1",
                        network = EvmNetwork.SEPOLIA,
                        address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                        name = "Savings",
                    ),
                    EthereumWallet(
                        id = "2",
                        network = EvmNetwork.SEPOLIA,
                        address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                    ),
                ),
            ),
            onBack = {},
            onNetworkSelected = {},
            onCreateWallet = {},
            onRestoreWallet = {},
            onWalletClick = {},
        )
    }
}
