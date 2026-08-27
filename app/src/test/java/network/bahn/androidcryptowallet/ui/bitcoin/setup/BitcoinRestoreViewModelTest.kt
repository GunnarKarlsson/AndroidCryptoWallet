package network.bahn.androidcryptowallet.ui.bitcoin.setup

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
import network.bahn.androidcryptowallet.domain.model.BitcoinNetworkStatus
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinRestoreViewModelTest {
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
        val walletRepo = FakeRestoreWalletRepository()
        val networkRepo = FakeRestoreNetworkRepository()
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()

        assertEquals(
            listOf(RestoreCall(BitcoinNetwork.TESTNET4, VALID_WORDS, null)),
            walletRepo.restoreCalls,
        )
        assertEquals(listOf(BitcoinNetwork.TESTNET4), networkRepo.setCalls)
        assertEquals(listOf(BitcoinRestoreEvent.WalletRestored), events)
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun blankPhraseDoesNotCallRepository() = runTest {
        val walletRepo = FakeRestoreWalletRepository()
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
        val walletRepo = FakeRestoreWalletRepository(
            restoreError = InvalidBitcoinMnemonicException("invalid"),
        )
        val networkRepo = FakeRestoreNetworkRepository()
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()

        assertEquals("invalid", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isRestoring)
        assertTrue(events.isEmpty())
        assertTrue(networkRepo.setCalls.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun duplicateRestoreStillNavigatesBack() = runTest {
        val walletRepo = FakeRestoreWalletRepository()
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinRestoreEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)

        viewModel.restore()
        viewModel.onMnemonicWordChange(0, MNEMONIC)
        viewModel.restore()

        assertEquals(2, walletRepo.restoreCalls.size)
        assertEquals(
            listOf(BitcoinRestoreEvent.WalletRestored, BitcoinRestoreEvent.WalletRestored),
            events,
        )
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun submittingDisablesRestore() = runTest {
        val gate = CompletableDeferred<Unit>()
        val walletRepo = FakeRestoreWalletRepository(restoreGate = gate)
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
        val viewModel = createViewModel(FakeRestoreWalletRepository())
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
        val walletRepo = FakeRestoreWalletRepository()
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
        val walletRepo = FakeRestoreWalletRepository()
        val networkRepo = FakeRestoreNetworkRepository(
            network = MutableStateFlow(BitcoinNetwork.MAINNET),
        )
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onMnemonicWordChange(0, MNEMONIC)
        viewModel.onPassphraseChange("secret")
        viewModel.onRestoreNetworkSelected(BitcoinNetwork.MAINNET)

        viewModel.restore()

        assertEquals(
            listOf(
                RestoreCall(
                    network = BitcoinNetwork.MAINNET,
                    mnemonicWords = VALID_WORDS,
                    passphrase = "secret",
                ),
            ),
            walletRepo.restoreCalls,
        )
        assertEquals(listOf(BitcoinNetwork.MAINNET), networkRepo.setCalls)
        job.cancel()
    }

    private fun createViewModel(
        walletRepo: FakeRestoreWalletRepository,
        networkRepo: FakeRestoreNetworkRepository = FakeRestoreNetworkRepository(),
    ) = BitcoinRestoreViewModel(
        walletRepository = walletRepo,
        networkStatusRepository = networkRepo,
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"
private val MNEMONIC = VALID_WORDS.joinToString(" ")

private data class RestoreCall(
    val network: BitcoinNetwork,
    val mnemonicWords: List<String>,
    val passphrase: String?,
)

private class FakeRestoreWalletRepository(
    private val restoreError: Exception? = null,
    private val restoreGate: CompletableDeferred<Unit>? = null,
) : BitcoinWalletRepository {
    val restoreCalls = mutableListOf<RestoreCall>()

    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = emptyFlow()
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
    ) {
        restoreGate?.await()
        restoreError?.let { throw it }
        restoreCalls += RestoreCall(network, mnemonicWords, passphrase)
    }
    override suspend fun refreshBalance(walletId: String) = error("unused")
    override suspend fun deleteWallet(walletId: String) = error("unused")
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

private class FakeRestoreNetworkRepository(
    private val network: MutableStateFlow<BitcoinNetwork> = MutableStateFlow(BitcoinNetwork.TESTNET4),
) : BitcoinNetworkStatusRepository {
    val setCalls = mutableListOf<BitcoinNetwork>()

    override fun observeStatus(): Flow<BitcoinNetworkStatus?> = emptyFlow()
    override fun selectedNetwork(): Flow<BitcoinNetwork> = network
    override suspend fun setNetwork(network: BitcoinNetwork) {
        setCalls += network
        this.network.value = network
    }
    override suspend fun refreshBlockHeight() = error("unused")
}
