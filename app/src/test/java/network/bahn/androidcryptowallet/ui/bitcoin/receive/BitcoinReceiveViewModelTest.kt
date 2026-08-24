package network.bahn.androidcryptowallet.ui.bitcoin.receive

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
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinReceiveViewModelTest {
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
        val viewModel = createViewModel(FakeReceiveWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals(WALLET.receiveAddress, viewModel.uiState.value.address)
        assertEquals(BitcoinNetwork.TESTNET4.label, viewModel.uiState.value.networkLabel)
        assertEquals(
            "bitcoin:${WALLET.receiveAddress}",
            viewModel.uiState.value.paymentUri,
        )
        job.cancel()
    }

    @Test
    fun missingWalletLeavesReceiveFieldsEmpty() = runTest {
        val viewModel = createViewModel(
            FakeReceiveWalletRepository(wallet = MutableStateFlow(null)),
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
        repo: FakeReceiveWalletRepository,
    ) = BitcoinReceiveViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        walletRepository = repo,
    )
}

private val WALLET = BitcoinWallet(
    id = "wallet-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
)

private class FakeReceiveWalletRepository(
    private val wallet: MutableStateFlow<BitcoinWallet?> = MutableStateFlow(WALLET),
) : BitcoinWalletRepository {
    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = wallet
    override fun generateMnemonic() = error("unused")
    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")
    override suspend fun restoreWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")
    override suspend fun refreshBalance(walletId: String) = error("unused")
    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")
    override suspend fun getCachedTransactions(walletId: String) = error("unused")
    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ) = error("unused")
    override fun isValidAddress(network: BitcoinNetwork, address: String) = error("unused")
    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ) = error("unused")
}
