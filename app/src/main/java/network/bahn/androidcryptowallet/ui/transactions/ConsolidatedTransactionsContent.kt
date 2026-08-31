package network.bahn.androidcryptowallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.ui.home.HomeNetworkModeSelector
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.WalletTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolidatedTransactionsContent(
    uiState: ConsolidatedTransactionsUiState,
    onRefresh: () -> Unit,
    onTransactionClick: (ConsolidatedTransaction) -> Unit,
    onNetworkModeSelected: (WalletNetworkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                WalletTopAppBar(
                    title = { Text(stringResource(R.string.tab_transactions)) },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.refresh_transactions),
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeNetworkModeSelector(
                    selectedMode = uiState.networkMode,
                    onModeSelected = onNetworkModeSelected,
                )
            }
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.consolidated_transactions_loading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                uiState.transactions.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.consolidated_transactions_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    items(uiState.transactions, key = { tx -> tx.id }) { transaction ->
                        ConsolidatedTransactionRow(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsolidatedTransactionsContentPreview() {
    WalletTheme {
        ConsolidatedTransactionsContent(
            uiState = ConsolidatedTransactionsUiState(
                transactions = listOf(
                    ConsolidatedTransaction.Bitcoin(
                        id = "btc:abc:wallet-1",
                        walletId = "wallet-1",
                        walletName = "Savings",
                        chainLabel = "Bitcoin Testnet4 (BTC)",
                        timestampSeconds = 1_700_000_000L,
                        confirmed = true,
                        isIncoming = true,
                        txReference = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                        netSatoshis = 100_000L,
                    ),
                ),
            ),
            onRefresh = {},
            onTransactionClick = {},
            onNetworkModeSelected = {},
        )
    }
}
