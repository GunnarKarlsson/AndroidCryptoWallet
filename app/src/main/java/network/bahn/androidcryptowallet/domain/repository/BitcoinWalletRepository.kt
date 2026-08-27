package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
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

    /**
     * Restore from a BIP-39 mnemonic (optional passphrase) for [network].
     * If a wallet already exists for the derived receive address, this is a no-op.
     * Otherwise this calls [createWallet].
     */
    suspend fun restoreWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )

    /** Fetch the receive-address balance from the remote data source and cache it on the wallet row. */
    suspend fun refreshBalance(walletId: String)

    /**
     * Permanently remove [walletId]: encrypted mnemonic/passphrase/network keys first,
     * then the Room row (transaction and cache rows cascade). Idempotent if already gone.
     */
    suspend fun deleteWallet(walletId: String)

    /**
     * Persist a display name for [walletId]. Blank or whitespace-only [name] is stored as null
     * so the UI can fall back to the default label.
     */
    suspend fun renameWallet(walletId: String, name: String?)

    /**
     * Previously fetched transactions for this wallet, or null if they have never been fetched.
     * An empty list means a fetch ran and the address had no transactions.
     */
    suspend fun getCachedTransactions(walletId: String): BitcoinTransactionPage?

    /**
     * Fetch a live transaction page for the wallet receive address and persist it.
     * [afterTxid] null replaces the cached first page; otherwise the page is appended.
     */
    suspend fun getTransactions(
        walletId: String,
        afterTxid: String? = null,
    ): BitcoinTransactionPage

    fun isValidAddress(
        network: BitcoinNetwork,
        address: String,
    ): Boolean

    /**
     * Build, sign, and broadcast a payment from an HD wallet's receive address.
     * Returns the broadcast txid.
     */
    suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ): String
}
