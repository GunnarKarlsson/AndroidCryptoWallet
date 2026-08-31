package network.bahn.androidcryptowallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.ui.evm.evmNativeAmountLabel
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils
import java.math.BigInteger

@Composable
fun ConsolidatedTransactionRow(
    transaction: ConsolidatedTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                    text = amountLabel(transaction),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TxStatusMarker(confirmed = transaction.confirmed)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = walletAndChainLabel(transaction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = txReferenceLabel(transaction),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val timestampSeconds = transaction.timestampSeconds
            if (transaction.confirmed && timestampSeconds != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringUtils.formatDateTime(timestampSeconds * 1_000L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun amountLabel(transaction: ConsolidatedTransaction): String = when (transaction) {
    is ConsolidatedTransaction.Bitcoin -> {
        val amount = StringUtils.formatBitcoinAmount(transaction.netSatoshis)
        val signedAmount = if (transaction.netSatoshis > 0L) "+$amount" else amount
        stringResource(R.string.bitcoin_amount, signedAmount)
    }
    is ConsolidatedTransaction.Evm -> {
        val netWei = transaction.netWei.toBigIntegerOrNull() ?: BigInteger.ZERO
        val amount = StringUtils.formatEvmAmount(transaction.netWei)
        val signedAmount = if (netWei > BigInteger.ZERO) "+$amount" else amount
        evmNativeAmountLabel(signedAmount, transaction.nativeSymbol)
    }
}

@Composable
private fun walletAndChainLabel(transaction: ConsolidatedTransaction): String {
    val walletLabel = StringUtils.walletDisplayName(
        name = transaction.walletName,
        fallback = stringResource(R.string.consolidated_tx_default_wallet),
    )
    return stringResource(
        R.string.consolidated_tx_wallet_chain,
        walletLabel,
        transaction.chainLabel,
    )
}

@Composable
private fun txReferenceLabel(transaction: ConsolidatedTransaction): String = when (transaction) {
    is ConsolidatedTransaction.Bitcoin -> StringUtils.truncateBitcoinAddress(transaction.txReference)
    is ConsolidatedTransaction.Evm -> StringUtils.truncateEvmAddress(transaction.txReference)
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

@Preview(showBackground = true)
@Composable
private fun ConsolidatedTransactionRowPreview() {
    WalletTheme {
        ConsolidatedTransactionRow(
            transaction = ConsolidatedTransaction.Bitcoin(
                id = "btc:abc:wallet-1",
                walletId = "wallet-1",
                walletName = "Savings",
                chainLabel = "Bitcoin (BTC)",
                timestampSeconds = 1_700_000_000L,
                confirmed = true,
                isIncoming = true,
                txReference = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                netSatoshis = 100_000L,
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
