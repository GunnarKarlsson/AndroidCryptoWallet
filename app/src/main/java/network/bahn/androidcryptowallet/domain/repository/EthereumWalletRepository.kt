package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

interface EthereumWalletRepository {
    /** Wallets for the currently selected network. Public data only. */
    fun observeWallets(): Flow<List<EthereumWallet>>

    /** BIP-39: generate a 12-word mnemonic. Does not persist. */
    fun generateMnemonic(): List<String>

    /**
     * Persist a confirmed BIP-39 mnemonic (optional passphrase) and cache the
     * BIP-44 receive address for [network] only.
     */
    suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )
}
