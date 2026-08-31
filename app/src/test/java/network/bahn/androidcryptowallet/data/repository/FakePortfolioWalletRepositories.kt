package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository

internal class FakeBitcoinWalletRepository(
    private val wallets: List<BitcoinWallet> = emptyList(),
) : BitcoinWalletRepository {
    var refreshBalanceCalls = 0

    override fun observeWallets(): Flow<List<BitcoinWallet>> = flowOf(wallets)

    override fun observeWallet(id: String): Flow<BitcoinWallet?> = flowOf(null)

    override fun generateMnemonic(): List<String> = error("unused")

    override suspend fun createWallet(
        network: network.bahn.androidcryptowallet.domain.model.BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: network.bahn.androidcryptowallet.domain.model.BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) {
        refreshBalanceCalls++
    }

    override suspend fun deleteWallet(walletId: String) = error("unused")

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")

    override suspend fun getCachedTransactions(
        walletId: String,
    ): network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage? = error("unused")

    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ): network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage = error("unused")

    override fun isValidAddress(
        network: network.bahn.androidcryptowallet.domain.model.BitcoinNetwork,
        address: String,
    ): Boolean = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ): String = error("unused")
}

internal class FakeEvmWalletRepository(
    private val walletsByFamily: Map<EvmFamily, List<EvmWallet>> = emptyMap(),
) : EvmWalletRepository {
    var refreshBalanceCalls = 0

    override fun observeWallets(family: EvmFamily): Flow<List<EvmWallet>> =
        flowOf(walletsByFamily[family].orEmpty())

    override fun observeWallet(id: String): Flow<EvmWallet?> = flowOf(null)

    override fun generateMnemonic(): List<String> = error("unused")

    override suspend fun createWallet(
        network: network.bahn.androidcryptowallet.domain.model.EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: network.bahn.androidcryptowallet.domain.model.EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) {
        refreshBalanceCalls++
    }

    override suspend fun deleteWallet(walletId: String) = error("unused")

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")

    override suspend fun getCachedTransactions(
        walletId: String,
    ): network.bahn.androidcryptowallet.domain.model.EvmTransactionPage? = error("unused")

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor?,
    ): network.bahn.androidcryptowallet.domain.model.EvmTransactionPage = error("unused")

    override fun isValidAddress(address: String): Boolean = error("unused")

    override suspend fun getFeeData(
        walletId: String,
    ): network.bahn.androidcryptowallet.domain.model.EvmFeeData = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: java.math.BigInteger,
        gasPreset: network.bahn.androidcryptowallet.domain.model.EvmGasPreset,
    ): String = error("unused")
}
