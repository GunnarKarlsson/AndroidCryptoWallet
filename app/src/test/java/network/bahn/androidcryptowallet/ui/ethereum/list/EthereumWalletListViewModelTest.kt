package network.bahn.androidcryptowallet.ui.ethereum.list

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
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumWalletListViewModelTest {
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
        assertEquals(EthereumNetwork.SEPOLIA, viewModel.uiState.value.selectedNetwork)
    }

    @Test
    fun showsEmptyAfterCatalogIsReadyWithNoWallets() = runTest {
        val ready = MutableStateFlow(false)
        val wallets = MutableStateFlow(emptyList<EthereumWallet>())
        val viewModel = createViewModel(wallets = wallets, ready = ready)
        val states = mutableListOf<EthereumWalletListUiState>()
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
        val states = mutableListOf<EthereumWalletListUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        val latest = states.last()
        assertEquals(listOf(WALLET), latest.wallets)
        job.cancel()
    }

    @Test
    fun selectingMainnetUpdatesStore() = runTest {
        val network = MutableStateFlow(EthereumNetwork.SEPOLIA)
        val viewModel = createViewModel(network = network)
        viewModel.onNetworkSelected(EthereumNetwork.MAINNET)
        assertEquals(EthereumNetwork.MAINNET, network.value)
    }

    private fun createViewModel(
        network: MutableStateFlow<EthereumNetwork> = MutableStateFlow(EthereumNetwork.SEPOLIA),
        wallets: MutableStateFlow<List<EthereumWallet>> = MutableStateFlow(emptyList()),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ) = EthereumWalletListViewModel(
        walletRepository = FakeEthListWalletRepository(wallets),
        selectedEthereumNetworkStore = FakeEthListNetworkStore(network),
        catalogReadiness = FakeEthCatalogReadiness(ready),
    )
}

private val WALLET = EthereumWallet(
    id = "eth-1",
    network = EthereumNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
)

private class FakeEthListWalletRepository(
    private val wallets: MutableStateFlow<List<EthereumWallet>>,
) : EthereumWalletRepository {
    override fun observeWallets(): Flow<List<EthereumWallet>> = wallets
    override fun observeWallet(id: String): Flow<EthereumWallet?> = emptyFlow()
    override fun generateMnemonic() = error("unused")
    override suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) = error("unused")
}

private class FakeEthListNetworkStore(
    private val network: MutableStateFlow<EthereumNetwork>,
) : SelectedEthereumNetworkStore {
    override val selectedNetwork: Flow<EthereumNetwork> = network
    override suspend fun setNetwork(network: EthereumNetwork) {
        this.network.value = network
    }
}

private class FakeEthCatalogReadiness(
    private val ready: MutableStateFlow<Boolean>,
) : WalletCatalogReadiness {
    override fun observeReady(): Flow<Boolean> = ready
    override suspend fun initialize() {
        ready.value = true
    }
}
