package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

interface EthereumMnemonicStore {
    /**
     * Persist BIP-39 mnemonic + optional passphrase and [network] for [walletId].
     * Never log mnemonic or passphrase values.
     */
    fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: EthereumNetwork,
    )

    fun listHdWalletIds(): List<String>

    /** Remove mnemonic, passphrase, and network prefs for [walletId]. Idempotent. */
    fun delete(walletId: String)

    fun loadNetwork(walletId: String): EthereumNetwork?

    fun loadMnemonic(walletId: String): String?

    fun loadPassphrase(walletId: String): String?
}
