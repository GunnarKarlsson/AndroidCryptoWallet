package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinHdWalletPublic

interface BitcoinMnemonicStore {
    /**
     * Persist BIP-39 mnemonic + optional passphrase and a public snapshot for [public].id.
     * Never log mnemonic or passphrase values.
     */
    fun save(
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    )

    fun listHdWalletIds(): List<String>

    fun loadPublic(walletId: String): BitcoinHdWalletPublic?

    fun loadMnemonic(walletId: String): String?

    fun loadPassphrase(walletId: String): String?
}
