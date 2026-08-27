package network.bahn.androidcryptowallet.ui.bitcoin.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.ui.bitcoin.BitcoinNetworkDropdown
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun BitcoinNetworkStatusScreen(
    onBack: () -> Unit,
    viewModel: BitcoinNetworkStatusViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEnter()
    }
    BitcoinNetworkStatusContent(
        uiState = uiState,
        onNetworkSelected = viewModel::onNetworkSelected,
        onRefresh = { viewModel.onRefresh(uiState.selectedNetwork) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitcoinNetworkStatusContent(
    uiState: BitcoinNetworkStatusUiState,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = walletTopAppBarColors(),
                title = { Text(stringResource(R.string.network_status_title)) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BitcoinNetworkDropdown(
                    selectedNetwork = uiState.selectedNetwork,
                    onNetworkSelected = onNetworkSelected,
                    modifier = Modifier.weight(1f),
                )
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
                            contentDescription = stringResource(R.string.refresh_block_height),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            StatusCard(
                title = stringResource(R.string.latest_block),
                value = StringUtils.formatBlockHeight(uiState.blockHeight),
                valueStyle = MaterialTheme.typography.displayMedium,
                caption = StringUtils.formatLastUpdated(
                    updatedAtMillis = uiState.updatedAtMillis,
                    neverRefreshed = stringResource(R.string.last_updated_never),
                    lastUpdatedPattern = stringResource(R.string.last_updated),
                ),
                errorMessage = uiState.errorMessage,
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    valueStyle: TextStyle,
    caption: String?,
    errorMessage: String?,
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

@Preview(showBackground = true)
@Composable
private fun BitcoinNetworkStatusScreenPreview() {
    WalletTheme {
        BitcoinNetworkStatusContent(
            uiState = BitcoinNetworkStatusUiState(
                selectedNetwork = BitcoinNetwork.TESTNET4,
                blockHeight = 894_623,
                updatedAtMillis = 1_700_000_000_000L,
            ),
            onNetworkSelected = {},
            onRefresh = {},
            onBack = {},
        )
    }
}
