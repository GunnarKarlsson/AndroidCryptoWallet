package network.bahn.androidcryptowallet.ui.ethereum.send

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
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
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumSendViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidAddressDoesNotCallSend() = runTest {
        val repo = FakeSendEthWalletRepository(validAddress = false)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("not-an-address")
        viewModel.onAmountChange("0.01")

        viewModel.onSend()

        assertTrue(repo.sendCalls.isEmpty())
        assertEquals(
            "Enter a valid Ethereum address",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isSubmitting)
        job.cancel()
    }

    @Test
    fun zeroAmountDoesNotCallSend() = runTest {
        val repo = FakeSendEthWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange(RECIPIENT)
        viewModel.onAmountChange("0")

        viewModel.onSend()

        assertTrue(repo.sendCalls.isEmpty())
        assertEquals(
            "Enter an amount greater than zero",
            viewModel.uiState.value.errorMessage,
        )
        job.cancel()
    }

    @Test
    fun submittingDisablesSend() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeSendEthWalletRepository(sendGate = gate)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange(RECIPIENT)
        viewModel.onAmountChange("0.01")
        assertTrue(viewModel.uiState.value.canSend)

        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertFalse(viewModel.uiState.value.canSend)
        gate.complete(Unit)
        job.cancel()
    }

    @Test
    fun successEmitsNavigateBack() = runTest {
        val repo = FakeSendEthWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EthereumSendEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onRecipientChange(RECIPIENT)
        viewModel.onAmountChange("0.010000000000000000")
        viewModel.onGasPresetSelected(EthereumGasPreset.Fast)

        viewModel.onSend()

        assertEquals(
            listOf(
                EthSendCall(
                    walletId = WALLET.id,
                    recipientAddress = RECIPIENT,
                    amountWei = BigInteger("10000000000000000"),
                    gasPreset = EthereumGasPreset.Fast,
                ),
            ),
            repo.sendCalls,
        )
        assertEquals(listOf(EthereumSendEvent.Sent), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun sendErrorStaysOnScreen() = runTest {
        val repo = FakeSendEthWalletRepository(
            sendError = IllegalStateException("Insufficient funds"),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange(RECIPIENT)
        viewModel.onAmountChange("0.01")

        viewModel.onSend()

        assertEquals("Insufficient funds", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.canSend)
        job.cancel()
    }

    @Test
    fun feeLoadFailureDisablesSend() = runTest {
        val repo = FakeSendEthWalletRepository(
            feeError = IllegalStateException("RPC down"),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals("RPC down", viewModel.uiState.value.feeLoadError)
        assertFalse(viewModel.uiState.value.canSend)
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeSendEthWalletRepository,
    ) = EthereumSendViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        walletRepository = repo,
    )
}

private const val RECIPIENT = "0x2222222222222222222222222222222222222222"

private val WALLET = EthereumWallet(
    id = "eth-wallet-1",
    network = EthereumNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    balanceWei = "5000000000000000000",
)

private val FEE_DATA = EthereumFeeData(
    baseFeePerGasWei = "1000000000",
    suggestedPriorityFeePerGasWei = "1500000000",
)

private data class EthSendCall(
    val walletId: String,
    val recipientAddress: String,
    val amountWei: BigInteger,
    val gasPreset: EthereumGasPreset,
)

private class FakeSendEthWalletRepository(
    private val wallet: MutableStateFlow<EthereumWallet?> = MutableStateFlow(WALLET),
    private val validAddress: Boolean = true,
    private val sendGate: CompletableDeferred<Unit>? = null,
    private val sendError: Throwable? = null,
    private val feeError: Throwable? = null,
    private val feeData: EthereumFeeData = FEE_DATA,
) : EthereumWalletRepository {
    val sendCalls = mutableListOf<EthSendCall>()

    override fun observeWallets(): Flow<List<EthereumWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EthereumWallet?> = wallet
    override fun generateMnemonic() = error("unused")

    override suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: EthereumNetwork,
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

    override fun isValidAddress(address: String): Boolean = validAddress

    override suspend fun getFeeData(walletId: String): EthereumFeeData {
        feeError?.let { throw it }
        return feeData
    }

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: BigInteger,
        gasPreset: EthereumGasPreset,
    ): String {
        sendGate?.await()
        sendError?.let { throw it }
        sendCalls += EthSendCall(walletId, recipientAddress, amountWei, gasPreset)
        return "0xtx"
    }
}
