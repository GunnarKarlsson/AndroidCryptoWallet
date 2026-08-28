package network.bahn.androidcryptowallet.ui.ethereum.receive

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumReceiveViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesAddressNetworkAndPaymentUri() = runTest {
        val viewModel = createViewModel(FakeReceiveEthWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals(WALLET.address, viewModel.uiState.value.address)
        assertEquals(EvmNetwork.SEPOLIA.label, viewModel.uiState.value.networkLabel)
        assertEquals(
            "ethereum:${WALLET.address}@11155111",
            viewModel.uiState.value.paymentUri,
        )
        job.cancel()
    }

    @Test
    fun missingWalletLeavesReceiveFieldsEmpty() = runTest {
        val viewModel = createViewModel(
            FakeReceiveEthWalletRepository(wallet = MutableStateFlow(null)),
        )
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertNull(viewModel.uiState.value.address)
        assertNull(viewModel.uiState.value.networkLabel)
        assertNull(viewModel.uiState.value.paymentUri)
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeReceiveEthWalletRepository,
    ) = EthereumReceiveViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        walletRepository = repo,
    )
}

private val WALLET = EthereumWallet(
    id = "eth-wallet-1",
    network = EvmNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
)

private class FakeReceiveEthWalletRepository(
    private val wallet: MutableStateFlow<EthereumWallet?> = MutableStateFlow(WALLET),
) : EthereumWalletRepository {
    override fun observeWallets(): Flow<List<EthereumWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EthereumWallet?> = wallet
    override fun generateMnemonic() = error("unused")

    override suspend fun createWallet(
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) = error("unused")
    override suspend fun deleteWallet(walletId: String) = error("unused")
    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")
    override suspend fun getCachedTransactions(walletId: String): EthereumTransactionPage? =
        error("unused")

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage = error("unused")

    override fun isValidAddress(address: String): Boolean = error("unused")
    override suspend fun getFeeData(walletId: String): EthereumFeeData = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: BigInteger,
        gasPreset: EthereumGasPreset,
    ): String = error("unused")
}
