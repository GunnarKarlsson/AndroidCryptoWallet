package network.bahn.androidcryptowallet.ui.bitcoin.send

import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.util.StringUtils

private enum class SendFeePreset(
    @StringRes val labelRes: Int,
    @StringRes val rateRes: Int,
    @StringRes val etaRes: Int,
    val satPerVByte: Long,
) {
    Slow(R.string.send_fee_slow, R.string.send_fee_slow_rate, R.string.send_fee_slow_eta, 2),
    Normal(R.string.send_fee_normal, R.string.send_fee_normal_rate, R.string.send_fee_normal_eta, 5),
    Fast(R.string.send_fee_fast, R.string.send_fee_fast_rate, R.string.send_fee_fast_eta, 10),
}

@Composable
fun BitcoinSendScreen(
    onBack: () -> Unit,
) {
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var feePreset by remember { mutableStateOf(SendFeePreset.Normal) }
    BitcoinSendContent(
        recipient = recipient,
        amount = amount,
        feePreset = feePreset,
        onRecipientChange = { recipient = it },
        onAmountChange = { value ->
            if (value.isEmpty() || value.matches(BTC_AMOUNT_PATTERN)) {
                amount = value
            }
        },
        onFeePresetSelected = { feePreset = it },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitcoinSendContent(
    recipient: String,
    amount: String,
    feePreset: SendFeePreset,
    onRecipientChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onFeePresetSelected: (SendFeePreset) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notAvailableMessage = stringResource(R.string.send_placeholder)
    val canSend = recipient.isNotBlank() && amount.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.send_title)) },
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
        bottomBar = {
            Button(
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar(notAvailableMessage) }
                },
                enabled = canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.send_action))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = recipient,
                onValueChange = onRecipientChange,
                label = { Text(stringResource(R.string.send_recipient_label)) },
                placeholder = { Text(stringResource(R.string.send_recipient_placeholder)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            pastedClipboardText(context)?.let(onRecipientChange)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(R.string.paste_address),
                        )
                    }
                },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.send_amount_label)) },
                placeholder = { Text(stringResource(R.string.send_amount_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                suffix = {
                    Text(
                        text = stringResource(R.string.send_btc_unit),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Text(
                text = stringResource(R.string.send_fee_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SendFeePreset.entries.forEach { preset ->
                    FeePresetCard(
                        preset = preset,
                        selected = feePreset == preset,
                        onClick = { onFeePresetSelected(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SendTotalSummary(
                amount = amount,
                feePreset = feePreset,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeePresetCard(
    preset: SendFeePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(preset.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(preset.rateRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(preset.etaRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SendTotalSummary(
    amount: String,
    feePreset: SendFeePreset,
) {
    val feeSatoshis = feePreset.satPerVByte * ESTIMATED_TX_VBYTES
    val amountSatoshis = StringUtils.parseBitcoinAmountToSatoshis(amount)
    val placeholder = stringResource(R.string.receive_address_placeholder)
    Card(
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SendSummaryRow(
                label = stringResource(R.string.send_fee_row),
                value = stringResource(
                    R.string.bitcoin_amount,
                    StringUtils.formatBitcoinAmount(feeSatoshis),
                ),
            )
            SendSummaryRow(
                label = stringResource(R.string.send_total_label),
                value = if (amountSatoshis == null) {
                    placeholder
                } else {
                    stringResource(
                        R.string.bitcoin_amount,
                        StringUtils.formatBitcoinAmount(amountSatoshis + feeSatoshis),
                    )
                },
                emphasize = true,
            )
        }
    }
}

@Composable
private fun SendSummaryRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private val BTC_AMOUNT_PATTERN = Regex("^\\d*\\.?\\d{0,8}$")
private const val ESTIMATED_TX_VBYTES = 141L

private fun pastedClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context).toString().trim().takeIf { it.isNotEmpty() }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinSendScreenPreview() {
    WalletTheme {
        BitcoinSendScreen(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BitcoinSendScreenFilledPreview() {
    WalletTheme {
        BitcoinSendContent(
            recipient = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
            amount = "0.01000000",
            feePreset = SendFeePreset.Fast,
            onRecipientChange = {},
            onAmountChange = {},
            onFeePresetSelected = {},
            onBack = {},
        )
    }
}
