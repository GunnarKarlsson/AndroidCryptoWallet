package network.bahn.androidcryptowallet.data.local.secure

interface BitcoinMnemonicStore {
    fun hasWallet(): Boolean

    /** Persist BIP-39 mnemonic + optional passphrase. Never log these values. */
    fun save(mnemonic: String, passphrase: String?)
}
