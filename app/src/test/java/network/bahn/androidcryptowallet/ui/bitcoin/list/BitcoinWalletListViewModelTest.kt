package network.bahn.androidcryptowallet.ui.bitcoin.list

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
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletsUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveSelectedBitcoinNetworkUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveWalletCatalogReadyUseCase
import network.bahn.androidcryptowallet.domain.usecase.SetBitcoinNetworkUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinWalletListViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.wallets.isEmpty())
    }

    @Test
    fun staysLoadingWhileCatalogIsNotReadyAndWalletsAreEmpty() = runTest {
        val ready = MutableStateFlow(false)
        val wallets = MutableStateFlow(emptyList<BitcoinWallet>())
        val viewModel = createViewModel(wallets = wallets, ready = ready)
        val states = mutableListOf<BitcoinWalletListUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        val latest = states.last()
        assertTrue(latest.isLoading)
        assertTrue(latest.wallets.isEmpty())
        job.cancel()
    }

    @Test
    fun showsEmptyAfterCatalogIsReadyWithNoWallets() = runTest {
        val ready = MutableStateFlow(false)
        val wallets = MutableStateFlow(emptyList<BitcoinWallet>())
        val viewModel = createViewModel(wallets = wallets, ready = ready)
        val states = mutableListOf<BitcoinWalletListUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        ready.value = true

        val latest = states.last()
        assertFalse(latest.isLoading)
        assertTrue(latest.wallets.isEmpty())
        job.cancel()
    }

    @Test
    fun showsWalletsEvenIfCatalogIsStillLoading() = runTest {
        val ready = MutableStateFlow(false)
        val wallets = MutableStateFlow(listOf(WALLET))
        val viewModel = createViewModel(wallets = wallets, ready = ready)
        val states = mutableListOf<BitcoinWalletListUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        val latest = states.last()
        assertEquals(listOf(WALLET), latest.wallets)
        job.cancel()
    }

    private fun createViewModel(
        network: MutableStateFlow<BitcoinNetwork> = MutableStateFlow(BitcoinNetwork.TESTNET4),
        wallets: MutableStateFlow<List<BitcoinWallet>> = MutableStateFlow(emptyList()),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ) = BitcoinWalletListViewModel(
        observeSelectedBitcoinNetwork = ObserveSelectedBitcoinNetworkUseCase(
            FakeListNetworkRepository(network),
        ),
        observeBitcoinWallets = ObserveBitcoinWalletsUseCase(
            FakeListWalletRepository(wallets),
        ),
        observeWalletCatalogReady = ObserveWalletCatalogReadyUseCase(
            FakeCatalogReadiness(ready),
        ),
        setBitcoinNetwork = SetBitcoinNetworkUseCase(
            FakeListNetworkRepository(network),
        ),
    )
}

private val WALLET = BitcoinWallet(
    id = "hd-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1qrestore",
)

private class FakeListWalletRepository(
    private val wallets: MutableStateFlow<List<BitcoinWallet>>,
) : BitcoinWalletRepository {
    override fun observeWallets(): Flow<List<BitcoinWallet>> = wallets
    override fun observeWallet(id: String) = emptyFlow<BitcoinWallet?>()
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
}

private class FakeListNetworkRepository(
    private val network: MutableStateFlow<BitcoinNetwork>,
) : BitcoinNetworkStatusRepository {
    override fun observeStatus(): Flow<BitcoinNetworkStatus?> = emptyFlow()
    override fun selectedNetwork(): Flow<BitcoinNetwork> = network
    override suspend fun setNetwork(network: BitcoinNetwork) {
        this.network.value = network
    }
    override suspend fun refreshBlockHeight() = error("unused")
}

private class FakeCatalogReadiness(
    private val ready: MutableStateFlow<Boolean>,
) : WalletCatalogReadiness {
    override fun observeReady(): Flow<Boolean> = ready
    override suspend fun initialize() {
        ready.value = true
    }
}
