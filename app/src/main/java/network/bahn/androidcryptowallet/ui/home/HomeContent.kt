package network.bahn.androidcryptowallet.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.WalletTopAppBar
import java.math.BigInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onAddWallet: () -> Unit,
    onHoldingClick: (PortfolioHoldingDestination) -> Unit,
    onNetworkModeSelected: (WalletNetworkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                WalletTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_logo),
                                contentDescription = stringResource(R.string.varna_logo),
                                modifier = Modifier.size(32.dp),
                            )
                            Text(stringResource(R.string.home_title))
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.refresh_balance),
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
            item {
                TotalBalanceCard(
                    totalFiatFormatted = uiState.totalFiatFormatted,
                    assetCount = uiState.assetCount,
                    isLoading = uiState.isTotalLoading,
                )
            }
            if (uiState.isHoldingsLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (!uiState.isHoldingsLoading && uiState.holdings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.home_empty_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            itemsIndexed(uiState.holdings, key = { _, holding -> holding.headline }) { index, holding ->
                PortfolioHoldingRow(
                    holding = holding,
                    onClick = { onHoldingClick(holding.destination) },
                    showDivider = index > 0,
                )
            }
            item {
                OutlinedButton(
                    onClick = onAddWallet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.home_add_wallet))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    WalletTheme {
        HomeContent(
            uiState = HomeUiState(
                holdings = listOf(
                    PortfolioHolding(
                        destination = PortfolioHoldingDestination.Evm(EvmFamily.ARBITRUM),
                        headline = "Arbitrum (ETH)",
                        nativeSymbol = "ETH",
                        balanceWei = BigInteger("120000000000000000"),
                    ),
                    PortfolioHolding(
                        destination = PortfolioHoldingDestination.Bitcoin,
                        headline = "Bitcoin (BTC)",
                        nativeSymbol = "BTC",
                        balanceSatoshis = 100_000L,
                    ),
                ),
                assetCount = 2,
                isTotalLoading = true,
                isHoldingsLoading = false,
            ),
            onRefresh = {},
            onAddWallet = {},
            onHoldingClick = {},
            onNetworkModeSelected = {},
        )
    }
}
