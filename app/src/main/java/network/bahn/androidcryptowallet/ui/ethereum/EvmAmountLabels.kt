package network.bahn.androidcryptowallet.ui.ethereum

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import network.bahn.androidcryptowallet.R

@Composable
fun evmNativeAmountLabel(amount: String, nativeSymbol: String): String =
    stringResource(R.string.evm_native_amount, amount, nativeSymbol)
