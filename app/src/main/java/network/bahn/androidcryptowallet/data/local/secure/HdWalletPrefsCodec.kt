package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

internal object HdWalletPrefsCodec {
    const val MNEMONIC_PREFIX = "mnemonic_"
    const val PASSPHRASE_PREFIX = "passphrase_"
    const val NETWORK_PREFIX = "network_"

    fun walletIdsFromKeys(keys: Set<String>): List<String> =
        keys.mapNotNull { key ->
            key.takeIf { it.startsWith(MNEMONIC_PREFIX) }?.removePrefix(MNEMONIC_PREFIX)
        }.distinct().sorted()

    fun loadNetwork(
        walletId: String,
        strings: Map<String, String?>,
    ): BitcoinNetwork? {
        val networkName = strings[NETWORK_PREFIX + walletId] ?: return null
        return runCatching { BitcoinNetwork.valueOf(networkName) }.getOrNull()
    }
}
