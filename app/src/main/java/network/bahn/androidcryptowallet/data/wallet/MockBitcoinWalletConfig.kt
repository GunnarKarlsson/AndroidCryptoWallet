package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind

/**
 * Watch-only mock addresses from BuildConfig / local.properties.
 * Empty lists mean no mocks for that network.
 */
data class MockBitcoinWalletConfig(
    val testnet4Addresses: List<String>,
    val mainnetAddresses: List<String>,
) {
    fun desiredWallets(): List<BitcoinWallet> =
        toWallets(BitcoinNetwork.TESTNET4, testnet4Addresses) +
            toWallets(BitcoinNetwork.MAINNET, mainnetAddresses)

    companion object {
        const val ID_PREFIX = "mock:"

        fun fromRaw(testnet4Raw: String, mainnetRaw: String) = MockBitcoinWalletConfig(
            testnet4Addresses = parseAddresses(testnet4Raw),
            mainnetAddresses = parseAddresses(mainnetRaw),
        )

        fun parseAddresses(raw: String): List<String> =
            raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

        fun walletId(network: BitcoinNetwork, address: String): String =
            "$ID_PREFIX${network.name}:$address"

        private fun toWallets(
            network: BitcoinNetwork,
            addresses: List<String>,
        ): List<BitcoinWallet> = addresses.map { address ->
            BitcoinWallet(
                id = walletId(network, address),
                network = network,
                receiveAddress = address,
                derivationIndex = 0,
                scriptType = BitcoinScriptType.EXTERNAL,
                kind = BitcoinWalletKind.WATCH_ONLY,
            )
        }
    }
}
