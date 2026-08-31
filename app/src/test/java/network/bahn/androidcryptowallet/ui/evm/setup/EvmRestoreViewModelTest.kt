package network.bahn.androidcryptowallet.ui.evm.setup

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import network.bahn.androidcryptowallet.ui.navigation.savedStateHandleForEvmRestoreGraph
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
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import network.bahn.androidcryptowallet.domain.model.InvalidEvmMnemonicException
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EvmRestoreViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoreEmitsNavigateBackAndSetsNetwork() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository()
        val networkStore = FakeEthRestoreNetworkStore()
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EvmRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()

        assertEquals(
            listOf(EthRestoreCall(EvmNetwork.SEPOLIA, VALID_WORDS, null)),
            walletRepo.restoreCalls,
        )
        assertEquals(listOf(EvmNetwork.SEPOLIA), networkStore.setCalls)
        assertEquals(listOf(EvmRestoreEvent.WalletRestored), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun blankPhraseDoesNotCallRepository() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository()
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onMnemonicWordChange(0, "   ")
        viewModel.restore()

        assertTrue(walletRepo.restoreCalls.isEmpty())
        job.cancel()
    }

    @Test
    fun invalidMnemonicStaysOnScreen() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository(
            restoreError = InvalidEvmMnemonicException("invalid"),
        )
        val networkStore = FakeEthRestoreNetworkStore()
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EvmRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()

        assertEquals("invalid", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isRestoring)
        assertTrue(events.isEmpty())
        assertTrue(networkStore.setCalls.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun duplicateRestoreStillNavigatesBack() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository()
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EvmRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()
        viewModel.onMnemonicWordChange(0, MNEMONIC)
        viewModel.restore()

        assertEquals(2, walletRepo.restoreCalls.size)
        assertEquals(
            listOf(EvmRestoreEvent.WalletRestored, EvmRestoreEvent.WalletRestored),
            events,
        )
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun submittingDisablesRestore() = runTest {
        val gate = CompletableDeferred<Unit>()
        val walletRepo = FakeEthRestoreWalletRepository(restoreGate = gate)
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()

        assertTrue(viewModel.uiState.value.isRestoring)
        gate.complete(Unit)
        job.cancel()
    }

    @Test
    fun pasteIntoFirstSlotFillsTwelveWords() = runTest {
        val viewModel = createViewModel(FakeEthRestoreWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onMnemonicWordChange(0, "  ${VALID_WORDS.joinToString("  ")}  \n")

        assertEquals(VALID_WORDS, viewModel.uiState.value.mnemonicWords)
        assertTrue(viewModel.uiState.value.canRestore)
        job.cancel()
    }

    @Test
    fun incompleteSlotsDoNotCallRepository() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository()
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        VALID_WORDS.dropLast(1).forEachIndexed { index, word ->
            viewModel.onMnemonicWordChange(index, word)
        }

        viewModel.restore()

        assertTrue(walletRepo.restoreCalls.isEmpty())
        assertFalse(viewModel.uiState.value.canRestore)
        job.cancel()
    }

    @Test
    fun restoreUsesSelectedNetworkAndPassphrase() = runTest {
        val walletRepo = FakeEthRestoreWalletRepository()
        val networkStore = FakeEthRestoreNetworkStore(
            network = MutableStateFlow(EvmNetwork.MAINNET),
        )
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)
        viewModel.onPassphraseChange("secret")
        viewModel.onRestoreNetworkSelected(EvmNetwork.MAINNET)

        viewModel.restore()

        assertEquals(
            listOf(
                EthRestoreCall(
                    network = EvmNetwork.MAINNET,
                    mnemonicWords = VALID_WORDS,
                    passphrase = "secret",
                ),
            ),
            walletRepo.restoreCalls,
        )
        assertEquals(listOf(EvmNetwork.MAINNET), networkStore.setCalls)
        job.cancel()
    }

    private fun createViewModel(
        walletRepo: FakeEthRestoreWalletRepository,
        networkStore: FakeEthRestoreNetworkStore = FakeEthRestoreNetworkStore(),
        family: EvmFamily = EvmFamily.ETHEREUM,
    ) = EvmRestoreViewModel(
        savedStateHandle = savedStateHandleForEvmRestoreGraph(family),
        walletRepository = walletRepo,
        selectedEvmNetworkStore = networkStore,
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"
private val MNEMONIC = VALID_WORDS.joinToString(" ")

private data class EthRestoreCall(
    val network: EvmNetwork,
    val mnemonicWords: List<String>,
    val passphrase: String?,
)

private class FakeEthRestoreWalletRepository(
    private val restoreError: Exception? = null,
    private val restoreGate: CompletableDeferred<Unit>? = null,
) : EvmWalletRepository {
    val restoreCalls = mutableListOf<EthRestoreCall>()

    override fun observeWallets(family: EvmFamily): Flow<List<EvmWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EvmWallet?> = emptyFlow()
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
    ) {
        restoreGate?.await()
        restoreError?.let { throw it }
        restoreCalls += EthRestoreCall(network, mnemonicWords, passphrase)
    }

    override suspend fun refreshBalance(walletId: String) = error("unused")

    override suspend fun deleteWallet(walletId: String) = error("unused")

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")
    override suspend fun getCachedTransactions(walletId: String) = error("unused")
    override suspend fun getTransactions(
        walletId: String,
        afterCursor: network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor?,
    ) = error("unused")

    override fun isValidAddress(address: String) = error("unused")

    override suspend fun getFeeData(walletId: String) = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: java.math.BigInteger,
        gasPreset: network.bahn.androidcryptowallet.domain.model.EvmGasPreset,
    ) = error("unused")
}

private class FakeEthRestoreNetworkStore(
    private val network: MutableStateFlow<EvmNetwork> = MutableStateFlow(EvmNetwork.SEPOLIA),
) : SelectedEvmNetworkStore {
    val setCalls = mutableListOf<EvmNetwork>()

    override fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork> = network
    override suspend fun setNetwork(family: EvmFamily, network: EvmNetwork) {
        setCalls += network
        this.network.value = network
    }
}
