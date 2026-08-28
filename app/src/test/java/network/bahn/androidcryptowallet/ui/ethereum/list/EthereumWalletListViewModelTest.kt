package network.bahn.androidcryptowallet.ui.ethereum.list

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import network.bahn.androidcryptowallet.ui.navigation.savedStateHandleForEvmWalletList
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
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
        assertEquals(EvmNetwork.SEPOLIA, viewModel.uiState.value.selectedNetwork)
        assertEquals(EvmFamily.ETHEREUM, viewModel.uiState.value.family)
        assertEquals(EvmNetwork.networksFor(EvmFamily.ETHEREUM), viewModel.uiState.value.availableNetworks)
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
        val network = MutableStateFlow(EvmNetwork.SEPOLIA)
        val viewModel = createViewModel(network = network)
        viewModel.onNetworkSelected(EvmNetwork.MAINNET)
        assertEquals(EvmNetwork.MAINNET, network.value)
    }

    private fun createViewModel(
        family: EvmFamily = EvmFamily.ETHEREUM,
        network: MutableStateFlow<EvmNetwork> = MutableStateFlow(EvmNetwork.SEPOLIA),
        wallets: MutableStateFlow<List<EthereumWallet>> = MutableStateFlow(emptyList()),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ) = EthereumWalletListViewModel(
        savedStateHandle = savedStateHandleForEvmWalletList(family),
        walletRepository = FakeEthListWalletRepository(wallets),
        selectedEvmNetworkStore = FakeEthListNetworkStore(network),
        catalogReadiness = FakeEthCatalogReadiness(ready),
    )
}

private val WALLET = EthereumWallet(
    id = "eth-1",
    network = EvmNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
)

private class FakeEthListWalletRepository(
    private val wallets: MutableStateFlow<List<EthereumWallet>>,
) : EthereumWalletRepository {
    override fun observeWallets(family: EvmFamily): Flow<List<EthereumWallet>> = wallets
    override fun observeWallet(id: String): Flow<EthereumWallet?> = emptyFlow()
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

private class FakeEthListNetworkStore(
    private val network: MutableStateFlow<EvmNetwork>,
) : SelectedEvmNetworkStore {
    override fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork> = network
    override suspend fun setNetwork(family: EvmFamily, network: EvmNetwork) {
        require(network.family == family)
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
