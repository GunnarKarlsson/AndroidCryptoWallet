package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet

interface BitcoinWalletRepository {
    /** Wallets for the currently selected network. Public data only. */
    fun observeWallets(): Flow<List<BitcoinWallet>>

    fun observeWallet(id: String): Flow<BitcoinWallet?>

    /** BIP-39: generate a 12-word mnemonic. Does not persist. */
    fun generateMnemonic(): List<String>

    /**
     * Persist a confirmed BIP-39 mnemonic (optional passphrase) and cache the
     * BIP-84 receive address for [network] only.
     */
    suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )

    /** Fetch the receive-address balance from the remote data source and cache it on the wallet row. */
    suspend fun refreshBalance(walletId: String)
}
