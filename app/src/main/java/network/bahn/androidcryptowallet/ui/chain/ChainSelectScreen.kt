package network.bahn.androidcryptowallet.ui.chain

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.theme.walletTopAppBarColors
import network.bahn.androidcryptowallet.ui.theme.WalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainSelectScreen(
    chains: List<SupportedChain> = SupportedChains,
    onChainSelected: (SupportedChain) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = walletTopAppBarColors(),
                    title = { Text(stringResource(R.string.select_chain_title)) },
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = stringResource(R.string.varna_logo),
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp)
                                .size(32.dp),
                        )
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
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(chains, key = { it.name }) { chain ->
                ChainListItem(
                    chain = chain,
                    onClick = { onChainSelected(chain) },
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun ChainListItem(
    chain: SupportedChain,
    onClick: () -> Unit,
) {
    val label = stringResource(chain.labelRes)
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(
                painter = painterResource(chain.iconRes),
                contentDescription = null,
                tint = Color.Black,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private val SupportedChain.labelRes: Int
    get() = when (this) {
        SupportedChain.BITCOIN -> R.string.chain_bitcoin
        SupportedChain.ETHEREUM -> R.string.chain_ethereum
        SupportedChain.BSC -> R.string.chain_bsc
    }

private val SupportedChain.iconRes: Int
    get() = when (this) {
        SupportedChain.BITCOIN -> R.drawable.ic_chain_bitcoin
        SupportedChain.ETHEREUM -> R.drawable.ic_chain_ethereum
        SupportedChain.BSC -> R.drawable.ic_chain_bsc
    }

@Preview(showBackground = true)
@Composable
private fun ChainSelectScreenPreview() {
    WalletTheme {
        ChainSelectScreen(onChainSelected = {})
    }
}
