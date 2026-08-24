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
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinSetupViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun confirmEmitsWalletCreatedAndSetsNetwork() = runTest {
        val walletRepo = FakeSetupWalletRepository()
        val networkRepo = FakeSetupNetworkRepository(
            network = MutableStateFlow(BitcoinNetwork.MAINNET),
        )
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinSetupEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onPassphraseChange("secret")

        viewModel.confirm()

        assertEquals(
            listOf(CreateCall(BitcoinNetwork.MAINNET, VALID_WORDS, "secret")),
            walletRepo.createCalls,
        )
        assertEquals(listOf(BitcoinNetwork.MAINNET), networkRepo.setCalls)
        assertEquals(listOf(BitcoinSetupEvent.WalletCreated), events)
        assertTrue(viewModel.uiState.value.mnemonicWords.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun createFailureStaysOnScreen() = runTest {
        val walletRepo = FakeSetupWalletRepository(
            createError = IllegalStateException("disk full"),
        )
        val networkRepo = FakeSetupNetworkRepository()
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<BitcoinSetupEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }

        viewModel.confirm()

        assertEquals("disk full", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isCreating)
        assertEquals(VALID_WORDS, viewModel.uiState.value.mnemonicWords)
        assertTrue(events.isEmpty())
        assertTrue(networkRepo.setCalls.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun createFailureWithoutMessageUsesFallback() = runTest {
        val walletRepo = FakeSetupWalletRepository(createError = IllegalStateException())
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.confirm()

        assertEquals("Could not create wallet", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isCreating)
        job.cancel()
    }

    @Test
    fun submittingDisablesCreate() = runTest {
        val gate = CompletableDeferred<Unit>()
        val walletRepo = FakeSetupWalletRepository(createGate = gate)
        val viewModel = createViewModel(walletRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.confirm()

        assertTrue(viewModel.uiState.value.isCreating)
        viewModel.confirm()
        gate.complete(Unit)
        assertEquals(1, walletRepo.createCalls.size)
        job.cancel()
    }

    @Test
    fun confirmUsesExplicitlySelectedNetwork() = runTest {
        val walletRepo = FakeSetupWalletRepository()
        val networkRepo = FakeSetupNetworkRepository()
        val viewModel = createViewModel(walletRepo, networkRepo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onCreateNetworkSelected(BitcoinNetwork.MAINNET)

        viewModel.confirm()

        assertEquals(
            listOf(CreateCall(BitcoinNetwork.MAINNET, VALID_WORDS, null)),
            walletRepo.createCalls,
        )
        assertEquals(listOf(BitcoinNetwork.MAINNET), networkRepo.setCalls)
        job.cancel()
    }

    private fun createViewModel(
        walletRepo: FakeSetupWalletRepository,
        networkRepo: FakeSetupNetworkRepository = FakeSetupNetworkRepository(),
    ) = BitcoinSetupViewModel(
        walletRepository = walletRepo,
        networkStatusRepository = networkRepo,
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"

private data class CreateCall(
    val network: BitcoinNetwork,
    val mnemonicWords: List<String>,
    val passphrase: String?,
)

private class FakeSetupWalletRepository(
    private val createError: Exception? = null,
    private val createGate: CompletableDeferred<Unit>? = null,
) : BitcoinWalletRepository {
    val createCalls = mutableListOf<CreateCall>()

    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = emptyFlow()
    override fun generateMnemonic() = VALID_WORDS
    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        createGate?.await()
        createError?.let { throw it }
        createCalls += CreateCall(network, mnemonicWords, passphrase)
    }
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

private class FakeSetupNetworkRepository(
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
