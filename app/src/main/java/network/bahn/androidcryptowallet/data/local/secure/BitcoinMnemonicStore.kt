package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

interface BitcoinMnemonicStore {
    /**
     * Persist BIP-39 mnemonic + optional passphrase and [network] for [walletId].
     * Network is required to re-derive BIP-84 (`m/84'/0'` vs `m/84'/1'`).
     * Never log mnemonic or passphrase values.
     */
    fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: BitcoinNetwork,
    )

    fun listHdWalletIds(): List<String>

    fun loadNetwork(walletId: String): BitcoinNetwork?

    fun loadMnemonic(walletId: String): String?

    fun loadPassphrase(walletId: String): String?
}
