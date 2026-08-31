package network.bahn.androidcryptowallet.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme

private val TotalBalanceCardHeight = 148.dp
private val TotalBalanceTitleRowHeight = 24.dp
private val TotalBalanceBodyHeight = 76.dp

@Composable
fun TotalBalanceCard(
    totalFiatFormatted: String?,
    assetCount: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(TotalBalanceCardHeight),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TotalBalanceTitleRowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_total_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.size(TotalBalanceTitleRowHeight)) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TotalBalanceBodyHeight),
            ) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Text(
                        text = totalFiatFormatted ?: stringResource(R.string.home_total_placeholder),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (assetCount > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.home_asset_count,
                                assetCount,
                                assetCount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TotalBalanceCardInitialLoadingPreview() {
    WalletTheme {
        TotalBalanceCard(
            totalFiatFormatted = null,
            assetCount = 0,
            isLoading = true,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TotalBalanceCardRefreshingPreview() {
    WalletTheme {
        TotalBalanceCard(
            totalFiatFormatted = null,
            assetCount = 3,
            isLoading = true,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TotalBalanceCardLoadedPreview() {
    WalletTheme {
        TotalBalanceCard(
            totalFiatFormatted = null,
            assetCount = 3,
            isLoading = false,
            modifier = Modifier.padding(20.dp),
        )
    }
}
