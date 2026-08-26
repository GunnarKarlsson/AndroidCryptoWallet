package network.bahn.androidcryptowallet.ui.ethereum.details

import androidx.lifecycle.SavedStateHandle
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
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumWalletDetailsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onEnterSkipsBalanceFetchWhenCachedIncludingZero() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(balanceWei = "0"),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(0, repo.refreshBalanceCalls)
        job.cancel()
    }

    @Test
    fun onEnterFetchesBalanceWhenNeverCached() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    balanceWei = null,
                    balanceUpdatedAtMillis = null,
                ),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(1, repo.refreshBalanceCalls)
        job.cancel()
    }

    @Test
    fun toolbarRefreshFetchesBalanceEvenWhenCachedZero() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(balanceWei = "0"),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        assertEquals(0, repo.refreshBalanceCalls)

        viewModel.onRefresh()

        assertEquals(1, repo.refreshBalanceCalls)
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeEthDetailsWalletRepository = FakeEthDetailsWalletRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
    ) = EthereumWalletDetailsViewModel(
        savedStateHandle = savedStateHandle,
        walletRepository = repo,
    )
}

private val WALLET = EthereumWallet(
    id = "wallet-1",
    network = EthereumNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    balanceWei = "1000000000000000000",
    balanceUpdatedAtMillis = 1_700_000_000_000L,
)

private class FakeEthDetailsWalletRepository(
    private val wallet: MutableStateFlow<EthereumWallet?> = MutableStateFlow(WALLET),
) : EthereumWalletRepository {
    var refreshBalanceCalls = 0

    override fun observeWallets(): Flow<List<EthereumWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EthereumWallet?> = wallet
    override fun generateMnemonic() = error("unused")

    override suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) {
        refreshBalanceCalls++
    }
}
