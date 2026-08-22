package network.bahn.androidcryptowallet.ui.bitcoin.send

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
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.SendBitcoinUseCase
import network.bahn.androidcryptowallet.domain.usecase.ValidateBitcoinAddressUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinSendViewModelTest {
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
        val repo = FakeSendWalletRepository(validAddress = false)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("not-an-address")
        viewModel.onAmountChange("0.01")

        viewModel.onSend()

        assertTrue(repo.sendCalls.isEmpty())
        assertEquals(
            "Enter a valid Bitcoin address for this network",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isSubmitting)
        job.cancel()
    }

    @Test
    fun zeroAmountDoesNotCallSend() = runTest {
        val repo = FakeSendWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("tb1qrecipient")
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
    fun amountBelowDustDoesNotCallSend() = runTest {
        val repo = FakeSendWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("tb1qrecipient")
        viewModel.onAmountChange("0.000001")

        viewModel.onSend()

        assertTrue(repo.sendCalls.isEmpty())
        assertEquals(
            "Amount is below the dust limit. Send at least 294 satoshis (0.00000294 BTC).",
            viewModel.uiState.value.errorMessage,
        )
        job.cancel()
    }

    @Test
    fun submittingDisablesSend() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeSendWalletRepository(sendGate = gate)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("tb1qrecipient")
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
        val repo = FakeSendWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinSendEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onRecipientChange("tb1qrecipient")
        viewModel.onAmountChange("0.00001000")
        viewModel.onFeePresetSelected(SendFeePreset.Fast)

        viewModel.onSend()

        assertEquals(
            listOf(
                SendCall(
                    walletId = WALLET.id,
                    recipientAddress = "tb1qrecipient",
                    amountSatoshis = 1_000L,
                    feeRateSatPerVbyte = 10L,
                ),
            ),
            repo.sendCalls,
        )
        assertEquals(listOf(BitcoinSendEvent.Sent), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun sendErrorStaysOnScreen() = runTest {
        val repo = FakeSendWalletRepository(sendError = IllegalStateException("Insufficient funds"))
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("tb1qrecipient")
        viewModel.onAmountChange("0.01")

        viewModel.onSend()

        assertEquals("Insufficient funds", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.canSend)
        job.cancel()
    }

    @Test
    fun watchOnlyCannotSend() = runTest {
        val repo = FakeSendWalletRepository(
            wallet = MutableStateFlow(WALLET.copy(kind = BitcoinWalletKind.WATCH_ONLY)),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onRecipientChange("tb1qrecipient")
        viewModel.onAmountChange("0.01")

        assertTrue(viewModel.uiState.value.isWatchOnly)
        assertFalse(viewModel.uiState.value.canSend)
        viewModel.onSend()
        assertTrue(repo.sendCalls.isEmpty())
        assertEquals(
            "Watch-only wallets cannot send",
            viewModel.uiState.value.errorMessage,
        )
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeSendWalletRepository,
    ) = BitcoinSendViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        observeBitcoinWallet = ObserveBitcoinWalletUseCase(repo),
        sendBitcoin = SendBitcoinUseCase(repo),
        validateBitcoinAddress = ValidateBitcoinAddressUseCase(repo),
    )
}

private val WALLET = BitcoinWallet(
    id = "wallet-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1qrestore",
    confirmedBalanceSatoshis = 50_000L,
)

private data class SendCall(
    val walletId: String,
    val recipientAddress: String,
    val amountSatoshis: Long,
    val feeRateSatPerVbyte: Long,
)

private class FakeSendWalletRepository(
    private val wallet: MutableStateFlow<BitcoinWallet?> = MutableStateFlow(WALLET),
    private val validAddress: Boolean = true,
    private val sendError: Exception? = null,
    private val sendGate: CompletableDeferred<Unit>? = null,
) : BitcoinWalletRepository {
    val sendCalls = mutableListOf<SendCall>()

    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = wallet
    override fun generateMnemonic() = error("unused")
    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")
    override suspend fun refreshBalance(walletId: String) = error("unused")
    override suspend fun getCachedTransactions(walletId: String) = error("unused")
    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ) = error("unused")

    override fun isValidAddress(network: BitcoinNetwork, address: String): Boolean = validAddress

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ): String {
        sendGate?.await()
        sendError?.let { throw it }
        sendCalls += SendCall(walletId, recipientAddress, amountSatoshis, feeRateSatPerVbyte)
        return "txid"
    }
}
