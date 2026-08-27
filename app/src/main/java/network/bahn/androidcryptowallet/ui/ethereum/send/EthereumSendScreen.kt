package network.bahn.androidcryptowallet.ui.ethereum.send

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.util.StringUtils

@Composable
fun EthereumSendScreen(
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: EthereumSendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EthereumSendEvent.Sent -> onSent()
            }
        }
    }
    EthereumSendContent(
        uiState = uiState,
        onRecipientChange = viewModel::onRecipientChange,
        onAmountChange = viewModel::onAmountChange,
        onGasPresetSelected = viewModel::onGasPresetSelected,
        onRetryFees = viewModel::onRetryFees,
        onSend = viewModel::onSend,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EthereumSendContent(
    uiState: EthereumSendUiState,
    onRecipientChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onGasPresetSelected: (EthereumGasPreset) -> Unit,
    onRetryFees: () -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = walletTopAppBarColors(),
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
                onClick = onSend,
                enabled = uiState.canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.send_eth_action))
                }
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
                value = uiState.recipient,
                onValueChange = onRecipientChange,
                enabled = !uiState.isSubmitting,
                label = { Text(stringResource(R.string.send_recipient_label)) },
                placeholder = { Text(stringResource(R.string.send_recipient_placeholder_eth)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            pastedClipboardText(context)?.let(onRecipientChange)
                        },
                        enabled = !uiState.isSubmitting,
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
                value = uiState.amount,
                onValueChange = onAmountChange,
                enabled = !uiState.isSubmitting,
                label = { Text(stringResource(R.string.send_amount_label)) },
                placeholder = { Text(stringResource(R.string.send_amount_placeholder_eth)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                suffix = {
                    Text(
                        text = stringResource(R.string.send_eth_unit),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Text(
                text = stringResource(R.string.send_fee_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                uiState.isLoadingFees -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.eth_send_fees_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                uiState.feeLoadError != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.feeLoadError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetryFees, enabled = !uiState.isSubmitting) {
                            Text(stringResource(R.string.eth_send_fees_retry))
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EthereumGasPreset.entries.forEach { preset ->
                            EthereumGasPresetCard(
                                preset = preset,
                                gweiLabel = uiState.priorityFeeGweiLabel(preset),
                                selected = uiState.gasPreset == preset,
                                onClick = { onGasPresetSelected(preset) },
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            EthereumSendTotalSummary(uiState = uiState)
            EthereumSendRemainingBalance(uiState = uiState)
            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EthereumGasPresetCard(
    preset: EthereumGasPreset,
    gweiLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(preset.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (gweiLabel != null) {
                    stringResource(R.string.eth_send_fee_rate, gweiLabel)
                } else {
                    stringResource(R.string.receive_address_placeholder)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(preset.etaRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EthereumSendTotalSummary(
    uiState: EthereumSendUiState,
) {
    val feeWei = uiState.estimatedFeeWei
    val amountWei = StringUtils.parseEthereumAmountToWei(uiState.amount)
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
            EthereumSendSummaryRow(
                label = stringResource(R.string.send_fee_row),
                value = if (feeWei == null) {
                    placeholder
                } else {
                    stringResource(
                        R.string.ethereum_amount,
                        StringUtils.formatEthereumAmount(feeWei),
                    )
                },
            )
            EthereumSendSummaryRow(
                label = stringResource(R.string.send_total_label),
                value = if (amountWei == null || feeWei == null) {
                    placeholder
                } else {
                    stringResource(
                        R.string.ethereum_amount,
                        StringUtils.formatEthereumAmount(
                            amountWei.add(java.math.BigInteger(feeWei)).toString(),
                        ),
                    )
                },
                emphasize = true,
            )
        }
    }
}

@Composable
private fun EthereumSendRemainingBalance(
    uiState: EthereumSendUiState,
) {
    val remainingWei = uiState.remainingBalanceWei
    val placeholder = stringResource(R.string.receive_address_placeholder)
    val overspend = uiState.wouldOverspend
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
        ) {
            EthereumSendSummaryRow(
                label = stringResource(R.string.send_remaining_label),
                value = if (remainingWei == null) {
                    placeholder
                } else {
                    stringResource(
                        R.string.ethereum_amount,
                        StringUtils.formatEthereumAmount(remainingWei),
                    )
                },
                emphasize = true,
                overspend = overspend,
            )
        }
    }
}

@Composable
private fun EthereumSendSummaryRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    overspend: Boolean = false,
) {
    val contentColor = if (overspend) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onBackground
    }
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
            color = if (overspend) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontFamily = FontFamily.Monospace,
            color = contentColor,
        )
    }
}

private fun pastedClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context).toString().trim().takeIf { it.isNotEmpty() }
}

private val PreviewFeeData = EthereumFeeData(
    baseFeePerGasWei = "1_000_000_000".replace("_", ""),
    suggestedPriorityFeePerGasWei = "1_500_000_000".replace("_", ""),
)

@Preview(showBackground = true)
@Composable
private fun EthereumSendScreenPreview() {
    WalletTheme {
        EthereumSendContent(
            uiState = EthereumSendUiState(
                availableBalanceWei = "5000000000000000000",
                feeData = PreviewFeeData,
            ),
            onRecipientChange = {},
            onAmountChange = {},
            onGasPresetSelected = {},
            onRetryFees = {},
            onSend = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumSendScreenFilledPreview() {
    WalletTheme {
        EthereumSendContent(
            uiState = EthereumSendUiState(
                recipient = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                amount = "0.010000000000000000",
                gasPreset = EthereumGasPreset.Fast,
                availableBalanceWei = "5000000000000000000",
                feeData = PreviewFeeData,
            ),
            onRecipientChange = {},
            onAmountChange = {},
            onGasPresetSelected = {},
            onRetryFees = {},
            onSend = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumSendScreenSubmittingPreview() {
    WalletTheme {
        EthereumSendContent(
            uiState = EthereumSendUiState(
                recipient = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                amount = "0.010000000000000000",
                isSubmitting = true,
                availableBalanceWei = "5000000000000000000",
                feeData = PreviewFeeData,
            ),
            onRecipientChange = {},
            onAmountChange = {},
            onGasPresetSelected = {},
            onRetryFees = {},
            onSend = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumSendScreenOverspendPreview() {
    WalletTheme {
        EthereumSendContent(
            uiState = EthereumSendUiState(
                recipient = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                amount = "4.900000000000000000",
                gasPreset = EthereumGasPreset.Fast,
                availableBalanceWei = "5000000000000000000",
                feeData = PreviewFeeData,
            ),
            onRecipientChange = {},
            onAmountChange = {},
            onGasPresetSelected = {},
            onRetryFees = {},
            onSend = {},
            onBack = {},
        )
    }
}
