package network.bahn.androidcryptowallet.ui.ethereum.setup

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
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumSetupViewModelTest {
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
        val walletRepo = FakeEthSetupWalletRepository()
        val networkStore = FakeEthSetupNetworkStore(
            network = MutableStateFlow(EvmNetwork.MAINNET),
        )
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EthereumSetupEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onPassphraseChange("secret")

        viewModel.confirm()

        assertEquals(
            listOf(EthCreateCall(EvmNetwork.MAINNET, VALID_WORDS, "secret")),
            walletRepo.createCalls,
        )
        assertEquals(listOf(EvmNetwork.MAINNET), networkStore.setCalls)
        assertEquals(listOf(EthereumSetupEvent.WalletCreated), events)
        assertTrue(viewModel.uiState.value.mnemonicWords.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun createFailureStaysOnScreen() = runTest {
        val walletRepo = FakeEthSetupWalletRepository(
            createError = IllegalStateException("disk full"),
        )
        val networkStore = FakeEthSetupNetworkStore()
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val events = mutableListOf<EthereumSetupEvent>()
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }

        viewModel.confirm()

        assertEquals("disk full", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isCreating)
        assertEquals(VALID_WORDS, viewModel.uiState.value.mnemonicWords)
        assertTrue(events.isEmpty())
        assertTrue(networkStore.setCalls.isEmpty())
        job.cancel()
        eventsJob.cancel()
    }

    @Test
    fun createFailureWithoutMessageUsesFallback() = runTest {
        val walletRepo = FakeEthSetupWalletRepository(createError = IllegalStateException())
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
        val walletRepo = FakeEthSetupWalletRepository(createGate = gate)
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
        val walletRepo = FakeEthSetupWalletRepository()
        val networkStore = FakeEthSetupNetworkStore()
        val viewModel = createViewModel(walletRepo, networkStore)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onCreateNetworkSelected(EvmNetwork.MAINNET)

        viewModel.confirm()

        assertEquals(
            listOf(EthCreateCall(EvmNetwork.MAINNET, VALID_WORDS, null)),
            walletRepo.createCalls,
        )
        assertEquals(listOf(EvmNetwork.MAINNET), networkStore.setCalls)
        job.cancel()
    }

    private fun createViewModel(
        walletRepo: FakeEthSetupWalletRepository,
        networkStore: FakeEthSetupNetworkStore = FakeEthSetupNetworkStore(),
    ) = EthereumSetupViewModel(
        walletRepository = walletRepo,
        selectedEvmNetworkStore = networkStore,
    )
}

private val VALID_WORDS = List(11) { "abandon" } + "about"

private data class EthCreateCall(
    val network: EvmNetwork,
    val mnemonicWords: List<String>,
    val passphrase: String?,
)

private class FakeEthSetupWalletRepository(
    private val createError: Exception? = null,
    private val createGate: CompletableDeferred<Unit>? = null,
) : EthereumWalletRepository {
    val createCalls = mutableListOf<EthCreateCall>()

    override fun observeWallets(): Flow<List<EthereumWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EthereumWallet?> = emptyFlow()
    override fun generateMnemonic() = VALID_WORDS
    override suspend fun createWallet(
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        createGate?.await()
        createError?.let { throw it }
        createCalls += EthCreateCall(network, mnemonicWords, passphrase)
    }

    override suspend fun restoreWallet(
        network: EvmNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) = error("unused")

    override suspend fun deleteWallet(walletId: String) = error("unused")

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")
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

private class FakeEthSetupNetworkStore(
    private val network: MutableStateFlow<EvmNetwork> = MutableStateFlow(EvmNetwork.SEPOLIA),
) : SelectedEvmNetworkStore {
    val setCalls = mutableListOf<EvmNetwork>()

    override fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork> = network
    override suspend fun setNetwork(family: EvmFamily, network: EvmNetwork) {
        setCalls += network
        this.network.value = network
    }
}
