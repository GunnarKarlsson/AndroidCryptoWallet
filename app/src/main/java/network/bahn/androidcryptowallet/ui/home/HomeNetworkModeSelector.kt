package network.bahn.androidcryptowallet.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.ui.theme.WalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNetworkModeSelector(
    selectedMode: WalletNetworkMode,
    onModeSelected: (WalletNetworkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        WalletNetworkMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = WalletNetworkMode.entries.size,
                ),
                onClick = { onModeSelected(mode) },
                selected = selectedMode == mode,
                label = {
                    Text(
                        text = when (mode) {
                            WalletNetworkMode.MAINNET ->
                                stringResource(R.string.home_network_mode_mainnet)
                            WalletNetworkMode.TESTNET ->
                                stringResource(R.string.home_network_mode_testnet)
                        },
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeNetworkModeSelectorPreview() {
    WalletTheme {
        HomeNetworkModeSelector(
            selectedMode = WalletNetworkMode.TESTNET,
            onModeSelected = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
