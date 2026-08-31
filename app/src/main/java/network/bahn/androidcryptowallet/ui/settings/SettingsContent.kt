package network.bahn.androidcryptowallet.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.ProviderSetting
import network.bahn.androidcryptowallet.ui.theme.WalletTheme
import network.bahn.androidcryptowallet.ui.theme.WalletTopAppBar
import network.bahn.androidcryptowallet.ui.util.StringUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onProviderClick: (ProviderSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                WalletTopAppBar(
                    title = { Text(stringResource(R.string.tab_settings)) },
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
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.groups.forEach { group ->
                item(key = "header-${group.label}") {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(group.providers, key = { it.id }) { provider ->
                    ProviderSettingRow(
                        provider = provider,
                        onClick = { onProviderClick(provider) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSettingRow(
    provider: ProviderSetting,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = provider.label,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = StringUtils.truncateProviderUrl(provider.currentUrl),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            if (provider.isOverridden) {
                Text(
                    text = stringResource(R.string.settings_provider_custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    WalletTheme {
        SettingsContent(
            uiState = SettingsUiState(
                groups = listOf(
                    ProviderGroup(
                        label = "Bitcoin",
                        providers = listOf(
                            ProviderSetting(
                                id = "bitcoin_testnet4",
                                groupLabel = "Bitcoin",
                                label = "Testnet4 API",
                                currentUrl = "https://mempool.space/testnet4/api/",
                                defaultUrl = "https://mempool.space/testnet4/api/",
                                isOverridden = false,
                            ),
                        ),
                    ),
                ),
            ),
            onProviderClick = {},
        )
    }
}
