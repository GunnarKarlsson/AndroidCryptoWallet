package network.bahn.androidcryptowallet.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.ui.chain.chainIconRes
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils
import java.math.BigInteger

@Composable
fun PortfolioHoldingRow(
    holding: PortfolioHolding,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    if (showDivider) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = holding.chainName(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(holding.destination.iconRes()),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = formatHoldingAmount(holding),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun PortfolioHolding.chainName(): String = headline.substringBefore(" (")

private fun formatHoldingAmount(holding: PortfolioHolding): String = when {
    holding.balanceSatoshis != null -> {
        val amount = StringUtils.formatBitcoinAmount(holding.balanceSatoshis)
        "${amount} ${holding.nativeSymbol}"
    }
    holding.balanceWei != null -> {
        val amount = StringUtils.formatEvmAmount(holding.balanceWei.toString())
        "${amount} ${holding.nativeSymbol}"
    }
    else -> "—"
}

private fun PortfolioHoldingDestination.iconRes(): Int = when (this) {
    PortfolioHoldingDestination.Bitcoin -> R.drawable.ic_chain_bitcoin
    is PortfolioHoldingDestination.Evm -> family.chainIconRes
}

@Preview(showBackground = true)
@Composable
private fun PortfolioHoldingRowPreview() {
    WalletTheme {
        PortfolioHoldingRow(
            holding = PortfolioHolding(
                destination = PortfolioHoldingDestination.Evm(EvmFamily.ETHEREUM),
                headline = "Ethereum (ETH)",
                nativeSymbol = "ETH",
                balanceWei = BigInteger("1000000000000000000"),
            ),
            onClick = {},
            showDivider = false,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
