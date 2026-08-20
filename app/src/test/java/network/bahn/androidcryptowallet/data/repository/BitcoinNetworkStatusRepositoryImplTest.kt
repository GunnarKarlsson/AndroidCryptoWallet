package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinNetworkStatusDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinNetworkStatusEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitcoinNetworkStatusRepositoryImplTest {
    @Test
    fun refreshPersistsBlockHeightForSelectedNetwork() = runTest {
        val remote = FakeBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.refreshBlockHeight()

        val status = repo.observeStatus().first()
        assertEquals(BitcoinNetwork.TESTNET4, status?.network)
        assertEquals(100L, status?.blockHeight)
        assertEquals(1_700_000_000_000L, status?.updatedAtMillis)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), remote.calls)
    }

    @Test
    fun observeEmitsAfterRefresh() = runTest {
        val repo = createRepository()

        assertNull(repo.observeStatus().first())
        repo.refreshBlockHeight()
        assertEquals(100L, repo.observeStatus().first()?.blockHeight)
    }

    @Test
    fun switchingNetworkDoesNotCallRpc() = runTest {
        val remote = FakeBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.refreshBlockHeight()
        assertEquals(1, remote.calls.size)

        repo.setNetwork(BitcoinNetwork.MAINNET)

        val status = repo.observeStatus().first()
        assertNull(status)
        assertEquals(BitcoinNetwork.MAINNET, repo.selectedNetwork().first())
        assertEquals(listOf(BitcoinNetwork.TESTNET4), remote.calls)
    }

    @Test
    fun switchingBackShowsCachedHeightWithoutRpc() = runTest {
        val remote = FakeBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.refreshBlockHeight()
        repo.setNetwork(BitcoinNetwork.MAINNET)
        repo.setNetwork(BitcoinNetwork.TESTNET4)

        val status = repo.observeStatus().first()
        assertEquals(100L, status?.blockHeight)
        assertEquals(1, remote.calls.size)
    }

    @Test
    fun refreshUsesCurrentlySelectedNetwork() = runTest {
        val remote = FakeBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.setNetwork(BitcoinNetwork.MAINNET)
        repo.refreshBlockHeight()

        val status = repo.observeStatus().first()
        assertEquals(BitcoinNetwork.MAINNET, status?.network)
        assertEquals(200L, status?.blockHeight)
        assertEquals(listOf(BitcoinNetwork.MAINNET), remote.calls)
        assertTrue(remote.calls.none { it == BitcoinNetwork.TESTNET4 })
    }

    private fun createRepository(
        remote: FakeBitcoinRemoteDataSource = FakeBitcoinRemoteDataSource(),
    ): BitcoinNetworkStatusRepositoryImpl = BitcoinNetworkStatusRepositoryImpl(
        dao = FakeBitcoinNetworkStatusDao(),
        selectedBitcoinNetworkStore = FakeSelectedBitcoinNetworkStore(),
        remote = remote,
        timeProvider = TimeProvider { 1_700_000_000_000L },
    )
}

private class FakeBitcoinNetworkStatusDao : BitcoinNetworkStatusDao {
    private val items = MutableStateFlow<Map<String, BitcoinNetworkStatusEntity>>(emptyMap())

    override fun observe(network: String): Flow<BitcoinNetworkStatusEntity?> =
        items.map { it[network] }

    override suspend fun upsert(entity: BitcoinNetworkStatusEntity) {
        items.update { it + (entity.network to entity) }
    }
}

private class FakeSelectedBitcoinNetworkStore : SelectedBitcoinNetworkStore {
    override val selectedNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)

    override suspend fun setNetwork(network: BitcoinNetwork) {
        selectedNetwork.value = network
    }
}

private class FakeBitcoinRemoteDataSource : BitcoinRemoteDataSource {
    val calls = mutableListOf<BitcoinNetwork>()

    override suspend fun getBlockCount(network: BitcoinNetwork): Long {
        calls += network
        return when (network) {
            BitcoinNetwork.TESTNET4 -> 100L
            BitcoinNetwork.MAINNET -> 200L
        }
    }

    override suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ) = error("unused")
}
