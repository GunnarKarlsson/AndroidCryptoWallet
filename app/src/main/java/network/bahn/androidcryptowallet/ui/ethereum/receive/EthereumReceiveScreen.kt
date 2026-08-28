package network.bahn.androidcryptowallet.ui.ethereum.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.chain.receiveClipboardLabelRes
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.util.QrCodeBitmap

@Composable
fun EthereumReceiveScreen(
    onBack: () -> Unit,
    viewModel: EthereumReceiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EthereumReceiveContent(
        uiState = uiState,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EthereumReceiveContent(
    uiState: EthereumReceiveUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.address_copied)
    val clipboardLabel = stringResource(
        uiState.family?.receiveClipboardLabelRes ?: R.string.receive_clipboard_label_eth,
    )
    val address = uiState.address
    val qrBitmap = remember(uiState.paymentUri) {
        uiState.paymentUri?.let(QrCodeBitmap::encode)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = walletTopAppBarColors(),
                title = { Text(stringResource(R.string.receive_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EthereumReceiveQrCard(bitmap = qrBitmap)
            Text(
                text = stringResource(
                    R.string.network_value,
                    uiState.networkLabel ?: stringResource(R.string.receive_address_placeholder),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = address ?: stringResource(R.string.receive_address_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (address == null) return@OutlinedButton
                        copyEthereumAddress(context, address, clipboardLabel)
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    },
                    enabled = address != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.receive_copy))
                }
                Button(
                    onClick = {
                        if (address == null) return@Button
                        shareEthereumAddress(context, address)
                    },
                    enabled = address != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.receive_share))
                }
            }
        }
    }
}

@Composable
private fun EthereumReceiveQrCard(bitmap: Bitmap?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.receive_qr_content_description),
                modifier = Modifier
                    .padding(16.dp)
                    .size(240.dp),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        } else {
            Spacer(modifier = Modifier.size(272.dp))
        }
    }
}

private fun copyEthereumAddress(context: Context, address: String, label: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, address))
}

private fun shareEthereumAddress(context: Context, address: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, address)
    }
    context.startActivity(
        Intent.createChooser(send, context.getString(R.string.receive_share_chooser)),
    )
}

@Preview(showBackground = true)
@Composable
private fun EthereumReceiveScreenPreview() {
    WalletTheme {
        EthereumReceiveContent(
            uiState = EthereumReceiveUiState(
                address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                networkLabel = "Sepolia",
                paymentUri = "ethereum:0x9858EfFD232B4033E47d90003D41EC34EcaEda94@11155111",
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumReceiveScreenMainnetPreview() {
    WalletTheme {
        EthereumReceiveContent(
            uiState = EthereumReceiveUiState(
                address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                networkLabel = "Mainnet",
                paymentUri = "ethereum:0x9858EfFD232B4033E47d90003D41EC34EcaEda94@1",
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EthereumReceiveScreenEmptyPreview() {
    WalletTheme {
        EthereumReceiveContent(
            uiState = EthereumReceiveUiState(),
            onBack = {},
        )
    }
}
