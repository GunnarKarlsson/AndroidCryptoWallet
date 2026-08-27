package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

interface EthereumWalletRepository {
    /** Wallets for the currently selected network. Public data only. */
    fun observeWallets(): Flow<List<EthereumWallet>>

    fun observeWallet(id: String): Flow<EthereumWallet?>

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

    /**
     * Restore from a BIP-39 mnemonic (optional passphrase) for [network].
     * If a wallet already exists for the derived address, this is a no-op.
     * Otherwise this calls [createWallet].
     */
    suspend fun restoreWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )

    suspend fun refreshBalance(walletId: String)

    /**
     * Permanently remove [walletId]: encrypted mnemonic/passphrase/network keys first,
     * then the Room row. Idempotent if already gone.
     */
    suspend fun deleteWallet(walletId: String)
}
