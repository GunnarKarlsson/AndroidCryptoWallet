package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.AssetPriceDao
import network.bahn.androidcryptowallet.data.local.db.AssetPriceEntity
import network.bahn.androidcryptowallet.data.remote.AssetPriceRemoteDataSource
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.NativeAssetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetPriceRepositoryImplTest {
    @Test
    fun refreshUpsertsFetchedPrices() = runTest {
        val remote = FakeAssetPriceRemoteDataSource(
            result = mapOf(NativeAssetId.BITCOIN to 65_000_000_000L),
        )
        val dao = FakeAssetPriceDao()
        val repo = createRepository(dao = dao, remote = remote)

        repo.refreshPrices()

        val prices = repo.observePrices().first()
        assertEquals(65_000_000_000L, prices.getValue(NativeAssetId.BITCOIN).priceUsdMicros)
        assertEquals(1_700_000_000_000L, prices.getValue(NativeAssetId.BITCOIN).updatedAtMillis)
        assertEquals(1, remote.calls)
    }

    @Test
    fun observeEmitsAfterRefresh() = runTest {
        val repo = createRepository(
            remote = FakeAssetPriceRemoteDataSource(
                result = mapOf(NativeAssetId.ETHEREUM to 3_000_000_000L),
            ),
        )

        assertTrue(repo.observePrices().first().isEmpty())
        repo.refreshPrices()
        assertEquals(
            3_000_000_000L,
            repo.observePrices().first().getValue(NativeAssetId.ETHEREUM).priceUsdMicros,
        )
    }

    @Test
    fun refreshFailureKeepsCachedPrices() = runTest {
        val dao = FakeAssetPriceDao()
        dao.upsertAll(
            listOf(
                AssetPriceEntity(
                    assetId = NativeAssetId.BITCOIN.name,
                    priceUsdMicros = 1_000_000L,
                    updatedAtMillis = 1L,
                ),
            ),
        )
        val remote = FakeAssetPriceRemoteDataSource(error = IllegalStateException("network down"))
        val repo = createRepository(dao = dao, remote = remote)

        repo.refreshPrices()

        val prices = repo.observePrices().first()
        assertEquals(1_000_000L, prices.getValue(NativeAssetId.BITCOIN).priceUsdMicros)
        assertEquals(1L, prices.getValue(NativeAssetId.BITCOIN).updatedAtMillis)
        assertEquals(1, remote.calls)
    }

    @Test
    fun emptyRemoteResponseDoesNotClearCache() = runTest {
        val dao = FakeAssetPriceDao()
        dao.upsertAll(
            listOf(
                AssetPriceEntity(
                    assetId = NativeAssetId.BITCOIN.name,
                    priceUsdMicros = 2_000_000L,
                    updatedAtMillis = 1L,
                ),
            ),
        )
        val repo = createRepository(dao = dao, remote = FakeAssetPriceRemoteDataSource())

        repo.refreshPrices()

        assertEquals(
            2_000_000L,
            repo.observePrices().first().getValue(NativeAssetId.BITCOIN).priceUsdMicros,
        )
    }

    private fun createRepository(
        dao: FakeAssetPriceDao = FakeAssetPriceDao(),
        remote: AssetPriceRemoteDataSource = FakeAssetPriceRemoteDataSource(),
    ) = AssetPriceRepositoryImpl(
        dao = dao,
        remote = remote,
        timeProvider = TimeProvider { 1_700_000_000_000L },
    )
}

private class FakeAssetPriceDao : AssetPriceDao {
    private val items = MutableStateFlow<Map<String, AssetPriceEntity>>(emptyMap())

    override fun observeAll(): Flow<List<AssetPriceEntity>> =
        items.map { it.values.toList() }

    override fun observeByIds(ids: List<String>): Flow<List<AssetPriceEntity>> =
        items.map { map -> ids.mapNotNull { id -> map[id] } }

    override suspend fun upsertAll(entities: List<AssetPriceEntity>) {
        items.update { current -> current + entities.associateBy { it.assetId } }
    }
}

private class FakeAssetPriceRemoteDataSource(
    var result: Map<NativeAssetId, Long> = emptyMap(),
    var error: Exception? = null,
) : AssetPriceRemoteDataSource {
    var calls = 0

    override suspend fun fetchUsdMicros(): Map<NativeAssetId, Long> {
        calls++
        error?.let { throw it }
        return result
    }
}
