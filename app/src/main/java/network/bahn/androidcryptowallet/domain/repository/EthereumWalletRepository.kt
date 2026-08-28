package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import java.math.BigInteger

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
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )

    /**
     * Restore from a BIP-39 mnemonic (optional passphrase) for [network].
     * If a wallet already exists for the derived address, this is a no-op.
     * Otherwise this calls [createWallet].
     */
    suspend fun restoreWallet(
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    )

    suspend fun refreshBalance(walletId: String)

    /**
     * Permanently remove [walletId]: encrypted mnemonic/passphrase/network keys first,
     * then the Room row. Idempotent if already gone.
     */
    suspend fun deleteWallet(walletId: String)

    /**
     * Update the display name for [walletId]. Blank/whitespace becomes null
     * (UI falls back to "Ethereum wallet").
     */
    suspend fun renameWallet(walletId: String, name: String?)

    /**
     * Previously fetched transactions for this wallet, or null if they have never been fetched.
     * An empty list means a fetch ran and the address had no transactions.
     */
    suspend fun getCachedTransactions(walletId: String): EthereumTransactionPage?

    /**
     * Fetch a live transaction page for the wallet address and persist it.
     * [afterCursor] null replaces the cached first page; otherwise the page is appended.
     */
    suspend fun getTransactions(
        walletId: String,
        afterCursor: EthereumTransactionPaginationCursor? = null,
    ): EthereumTransactionPage

    fun isValidAddress(address: String): Boolean

    /** Live EIP-1559 fee oracle for the wallet's network. */
    suspend fun getFeeData(walletId: String): EthereumFeeData

    /**
     * Build, sign, and broadcast a native ETH transfer from an HD wallet.
     * Returns the broadcast transaction hash.
     */
    suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: BigInteger,
        gasPreset: EthereumGasPreset,
    ): String
}
