package network.bahn.androidcryptowallet.ui.ethereum.edit

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
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumEditWalletViewModelTest {
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
        val viewModel = createViewModel(FakeEthEditWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals("Ethereum wallet", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.isWalletLoaded)
        job.cancel()
    }

    @Test
    fun prefillsCustomName() = runTest {
        val repo = FakeEthEditWalletRepository(
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
        val repo = FakeEthEditWalletRepository()
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EthereumEditWalletEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onNameChange("Holiday")

        viewModel.onConfirm()

        assertEquals(listOf(RenameCall(WALLET.id, "Holiday")), repo.renameCalls)
        assertEquals(listOf(EthereumEditWalletEvent.Saved), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun blankConfirmSavesBlankForRepositoryToNull() = runTest {
        val repo = FakeEthEditWalletRepository()
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
        val repo = FakeEthEditWalletRepository(
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
        val repo = FakeEthEditWalletRepository(renameGate = gate)
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
        repo: FakeEthEditWalletRepository,
    ) = EthereumEditWalletViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        walletRepository = repo,
    )
}

private val WALLET = EthereumWallet(
    id = "wallet-1",
    network = EvmNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
)

private data class RenameCall(
    val walletId: String,
    val name: String?,
)

private class FakeEthEditWalletRepository(
    private val wallet: MutableStateFlow<EthereumWallet?> = MutableStateFlow(WALLET),
    private val renameError: Exception? = null,
    private val renameGate: CompletableDeferred<Unit>? = null,
) : EthereumWalletRepository {
    val renameCalls = mutableListOf<RenameCall>()

    override fun observeWallets(family: EvmFamily): Flow<List<EthereumWallet>> = emptyFlow()
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

    override suspend fun renameWallet(walletId: String, name: String?) {
        renameGate?.await()
        renameError?.let { throw it }
        renameCalls += RenameCall(walletId, name)
    }

    override suspend fun getCachedTransactions(walletId: String) = error("unused")

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor?,
    ) = error("unused")

    override fun isValidAddress(address: String) = error("unused")

    override suspend fun getFeeData(walletId: String) = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: java.math.BigInteger,
        gasPreset: network.bahn.androidcryptowallet.domain.model.EthereumGasPreset,
    ) = error("unused")
}
