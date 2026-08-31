package network.bahn.androidcryptowallet.data.repository

import network.bahn.androidcryptowallet.BuildConfig
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerEndpoint
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerKind
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import javax.inject.Inject
import javax.inject.Singleton

data class ProviderDefinition(
    val id: String,
    val groupLabel: String,
    val label: String,
    val defaultUrl: String,
)

@Singleton
class DefaultProviderCatalog @Inject constructor() {
    private val definitionsById: Map<String, ProviderDefinition> =
        buildList {
            add(
                ProviderDefinition(
                    id = ProviderIds.BITCOIN_TESTNET4,
                    groupLabel = GROUP_BITCOIN,
                    label = "Testnet4 API",
                    defaultUrl = BuildConfig.MS_TESTNET4_BASE_URL,
                ),
            )
            add(
                ProviderDefinition(
                    id = ProviderIds.BITCOIN_MAINNET,
                    groupLabel = GROUP_BITCOIN,
                    label = "Mainnet API",
                    defaultUrl = BuildConfig.MS_MAINNET_BASE_URL,
                ),
            )
            EvmNetwork.entries.forEach { network ->
                add(evmRpcDefinition(network))
                add(evmExplorerDefinition(network))
            }
        }.associateBy { it.id }

    fun allDefinitions(): List<ProviderDefinition> =
        definitionsById.values.sortedWith(
            compareBy<ProviderDefinition> { it.groupLabel.lowercase() }
                .thenBy { it.label.lowercase() },
        )

    fun definition(id: String): ProviderDefinition =
        definitionsById[id] ?: error("Unknown provider id: $id")

    fun defaultUrl(id: String): String = definition(id).defaultUrl

    fun bitcoinProviderId(network: BitcoinNetwork): String = when (network) {
        BitcoinNetwork.TESTNET4 -> ProviderIds.BITCOIN_TESTNET4
        BitcoinNetwork.MAINNET -> ProviderIds.BITCOIN_MAINNET
    }

    fun evmRpcProviderId(network: EvmNetwork): String =
        ProviderIds.evmRpc(network)

    fun evmExplorerProviderId(network: EvmNetwork): String =
        ProviderIds.evmExplorer(network)

    fun explorerKind(network: EvmNetwork): EvmExplorerKind =
        defaultExplorerEndpoints.getValue(network).kind

    fun defaultExplorerEndpoint(network: EvmNetwork): EvmExplorerEndpoint =
        defaultExplorerEndpoints.getValue(network)

    private fun evmRpcDefinition(network: EvmNetwork): ProviderDefinition =
        ProviderDefinition(
            id = ProviderIds.evmRpc(network),
            groupLabel = network.family.groupLabel(),
            label = "${network.label} RPC",
            defaultUrl = defaultRpcUrls.getValue(network),
        )

    private fun evmExplorerDefinition(network: EvmNetwork): ProviderDefinition =
        ProviderDefinition(
            id = ProviderIds.evmExplorer(network),
            groupLabel = network.family.groupLabel(),
            label = "${network.label} Explorer",
            defaultUrl = defaultExplorerEndpoints.getValue(network).baseUrl,
        )

    private fun EvmFamily.groupLabel(): String = when (this) {
        EvmFamily.ETHEREUM -> GROUP_ETHEREUM
        EvmFamily.BSC -> GROUP_BSC
        EvmFamily.POLYGON -> GROUP_POLYGON
        EvmFamily.ARBITRUM -> GROUP_ARBITRUM
        EvmFamily.BASE -> GROUP_BASE
        EvmFamily.OPTIMISM -> GROUP_OPTIMISM
        EvmFamily.AVALANCHE -> GROUP_AVALANCHE
    }

    internal companion object {
        const val GROUP_BITCOIN = "Bitcoin"
        const val GROUP_ETHEREUM = "Ethereum"
        const val GROUP_BSC = "BSC"
        const val GROUP_POLYGON = "Polygon"
        const val GROUP_ARBITRUM = "Arbitrum"
        const val GROUP_BASE = "Base"
        const val GROUP_OPTIMISM = "Optimism"
        const val GROUP_AVALANCHE = "Avalanche"

        val defaultRpcUrls: Map<EvmNetwork, String> = mapOf(
            EvmNetwork.SEPOLIA to "https://ethereum-sepolia-rpc.publicnode.com",
            EvmNetwork.MAINNET to "https://ethereum.publicnode.com",
            EvmNetwork.BSC_TESTNET to "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
            EvmNetwork.BSC_MAINNET to "https://bsc-dataseed.bnbchain.org",
            EvmNetwork.POLYGON_AMOY to "https://polygon-amoy-bor-rpc.publicnode.com",
            EvmNetwork.POLYGON_MAINNET to "https://polygon-bor-rpc.publicnode.com",
            EvmNetwork.ARBITRUM_SEPOLIA to "https://sepolia-rollup.arbitrum.io/rpc",
            EvmNetwork.ARBITRUM_MAINNET to "https://arb1.arbitrum.io/rpc",
            EvmNetwork.BASE_SEPOLIA to "https://sepolia.base.org",
            EvmNetwork.BASE_MAINNET to "https://mainnet.base.org",
            EvmNetwork.OPTIMISM_SEPOLIA to "https://sepolia.optimism.io",
            EvmNetwork.OPTIMISM_MAINNET to "https://mainnet.optimism.io",
            EvmNetwork.AVALANCHE_FUJI to "https://api.avax-test.network/ext/bc/C/rpc",
            EvmNetwork.AVALANCHE_MAINNET to "https://api.avax.network/ext/bc/C/rpc",
        )

        val defaultExplorerEndpoints: Map<EvmNetwork, EvmExplorerEndpoint> = mapOf(
            EvmNetwork.SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://eth-sepolia.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://eth.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.BSC_TESTNET to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BSC_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_AMOY to EvmExplorerEndpoint(
                baseUrl = "https://api-amoy.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_FUJI to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
        )
    }
}

object ProviderIds {
    const val BITCOIN_TESTNET4 = "bitcoin_testnet4"
    const val BITCOIN_MAINNET = "bitcoin_mainnet"

    fun evmRpc(network: EvmNetwork): String = "evm_${network.name.lowercase()}_rpc"

    fun evmExplorer(network: EvmNetwork): String = "evm_${network.name.lowercase()}_explorer"
}
