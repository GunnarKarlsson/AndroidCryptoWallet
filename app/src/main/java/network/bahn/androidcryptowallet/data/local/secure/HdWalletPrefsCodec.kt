package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinHdWalletPublic
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType

internal object HdWalletPrefsCodec {
    const val MNEMONIC_PREFIX = "mnemonic_"
    const val PASSPHRASE_PREFIX = "passphrase_"
    const val NETWORK_PREFIX = "network_"
    const val ADDRESS_PREFIX = "address_"
    const val INDEX_PREFIX = "index_"
    const val SCRIPT_PREFIX = "script_"

    fun walletIdsFromKeys(keys: Set<String>): List<String> =
        keys.mapNotNull { key ->
            key.takeIf { it.startsWith(NETWORK_PREFIX) }?.removePrefix(NETWORK_PREFIX)
        }.distinct().sorted()

    fun loadPublic(
        walletId: String,
        strings: Map<String, String?>,
        ints: Map<String, Int>,
    ): BitcoinHdWalletPublic? {
        val networkName = strings[NETWORK_PREFIX + walletId] ?: return null
        val address = strings[ADDRESS_PREFIX + walletId] ?: return null
        val scriptName = strings[SCRIPT_PREFIX + walletId] ?: return null
        val network = runCatching { BitcoinNetwork.valueOf(networkName) }.getOrNull() ?: return null
        val scriptType = runCatching { BitcoinScriptType.valueOf(scriptName) }.getOrNull()
            ?: return null
        return BitcoinHdWalletPublic(
            id = walletId,
            network = network,
            receiveAddress = address,
            derivationIndex = ints[INDEX_PREFIX + walletId] ?: 0,
            scriptType = scriptType,
        )
    }
}
