package network.bahn.androidcryptowallet.data.local.secure

interface BitcoinMnemonicStore {
    /** Persist BIP-39 mnemonic + optional passphrase for [walletId]. Never log these values. */
    fun save(walletId: String, mnemonic: String, passphrase: String?)
}
