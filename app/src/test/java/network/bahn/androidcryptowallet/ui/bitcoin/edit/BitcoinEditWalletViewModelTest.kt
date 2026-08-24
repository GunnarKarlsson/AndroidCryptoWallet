package network.bahn.androidcryptowallet.ui.bitcoin.edit

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
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinEditWalletViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun prefillsDefaultWhenNameMissing() = runTest {
        val viewModel = createViewModel(FakeEditWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals("Bitcoin wallet", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.isWalletLoaded)
        job.cancel()
    }

    @Test
    fun prefillsCustomName() = runTest {
        val repo = FakeEditWalletRepository(
            wallet = MutableStateFlow(WALLET.copy(name = "Savings")),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals("Savings", viewModel.uiState.value.name)
        job.cancel()
    }

    @Test
    fun confirmEmitsNavigateBack() = runTest {
        val repo = FakeEditWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinEditWalletEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onNameChange("Holiday")

        viewModel.onConfirm()

        assertEquals(listOf(RenameCall(WALLET.id, "Holiday")), repo.renameCalls)
        assertEquals(listOf(BitcoinEditWalletEvent.Saved), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun blankConfirmSavesBlankForRepositoryToNull() = runTest {
        val repo = FakeEditWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onNameChange("   ")

        viewModel.onConfirm()

        assertEquals(listOf(RenameCall(WALLET.id, "   ")), repo.renameCalls)
        job.cancel()
    }

    @Test
    fun errorStaysOnScreen() = runTest {
        val repo = FakeEditWalletRepository(
            renameError = IllegalStateException("Wallet not found"),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onNameChange("Savings")

        viewModel.onConfirm()

        assertEquals("Wallet not found", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.canConfirm)
        job.cancel()
    }

    @Test
    fun submittingDisablesConfirm() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeEditWalletRepository(renameGate = gate)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onNameChange("Savings")
        assertTrue(viewModel.uiState.value.canConfirm)

        viewModel.onConfirm()

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertFalse(viewModel.uiState.value.canConfirm)
        gate.complete(Unit)
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeEditWalletRepository,
    ) = BitcoinEditWalletViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        walletRepository = repo,
    )
}

private val WALLET = BitcoinWallet(
    id = "wallet-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1qrestore",
)

private data class RenameCall(
    val walletId: String,
    val name: String?,
)

private class FakeEditWalletRepository(
    private val wallet: MutableStateFlow<BitcoinWallet?> = MutableStateFlow(WALLET),
    private val renameError: Exception? = null,
    private val renameGate: CompletableDeferred<Unit>? = null,
) : BitcoinWalletRepository {
    val renameCalls = mutableListOf<RenameCall>()

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
    override suspend fun renameWallet(walletId: String, name: String?) {
        renameGate?.await()
        renameError?.let { throw it }
        renameCalls += RenameCall(walletId, name)
    }
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
