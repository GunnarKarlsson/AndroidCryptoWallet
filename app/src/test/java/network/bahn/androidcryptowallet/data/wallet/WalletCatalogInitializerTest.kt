package network.bahn.androidcryptowallet.data.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletEntity
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.domain.model.BitcoinHdWalletPublic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCatalogInitializerTest {
    @Test
    fun initializeMarksReadyAfterReconcileAndSeed() = runTest {
        val dao = FakeCatalogWalletDao()
        val initializer = WalletCatalogInitializer(
            hdWalletRoomReconciler = HdWalletRoomReconciler(
                mnemonicStore = FakeCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("", ""),
                walletDao = dao,
            ),
        )

        assertFalse(initializer.observeReady().first())
        initializer.initialize()
        assertTrue(initializer.observeReady().first())
    }

    @Test
    fun initializeMarksReadyWhenReconcileFails() = runTest {
        val dao = FakeCatalogWalletDao()
        val initializer = WalletCatalogInitializer(
            hdWalletRoomReconciler = HdWalletRoomReconciler(
                mnemonicStore = ThrowingCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("", ""),
                walletDao = dao,
            ),
        )

        initializer.initialize()
        assertTrue(initializer.observeReady().first())
    }

    @Test
    fun initializeMarksReadyWhenSeedFails() = runTest {
        val dao = ThrowingMockIdsWalletDao()
        val initializer = WalletCatalogInitializer(
            hdWalletRoomReconciler = HdWalletRoomReconciler(
                mnemonicStore = FakeCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("tb1qmock", ""),
                walletDao = dao,
            ),
        )

        initializer.initialize()
        assertTrue(initializer.observeReady().first())
    }
}

private class FakeCatalogMnemonicStore : BitcoinMnemonicStore {
    override fun save(
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    ) = Unit

    override fun listHdWalletIds(): List<String> = emptyList()

    override fun loadPublic(walletId: String): BitcoinHdWalletPublic? = null

    override fun loadMnemonic(walletId: String): String? = null

    override fun loadPassphrase(walletId: String): String? = null
}

private class ThrowingCatalogMnemonicStore : BitcoinMnemonicStore {
    override fun save(
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    ) = Unit

    override fun listHdWalletIds(): List<String> = error("encrypted store unavailable")

    override fun loadPublic(walletId: String): BitcoinHdWalletPublic? = null

    override fun loadMnemonic(walletId: String): String? = null

    override fun loadPassphrase(walletId: String): String? = null
}

private open class FakeCatalogWalletDao : BitcoinWalletDao {
    private val items = MutableStateFlow<List<BitcoinWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override fun observeById(id: String): Flow<BitcoinWalletEntity?> =
        items.map { rows -> rows.find { it.id == id } }

    override suspend fun findByNetworkAndAddress(
        network: String,
        receiveAddress: String,
    ): BitcoinWalletEntity? = items.value.find {
        it.network == network && it.receiveAddress == receiveAddress
    }

    override suspend fun mockWalletIds(): List<String> =
        items.value.filter { it.id.startsWith("mock:") }.map { it.id }

    override suspend fun insert(entity: BitcoinWalletEntity) {
        items.update { it + entity }
    }

    override suspend fun insertIgnore(entity: BitcoinWalletEntity) {
        items.update { rows ->
            if (rows.any { it.id == entity.id }) rows else rows + entity
        }
    }

    override suspend fun deleteByIds(ids: List<String>) {
        val idSet = ids.toSet()
        items.update { rows -> rows.filter { it.id !in idSet } }
    }

    override suspend fun updateBalance(
        id: String,
        confirmedSatoshis: Long,
        unconfirmedSatoshis: Long,
        updatedAtMillis: Long,
    ) {
        error("unused")
    }

    override suspend fun updateName(id: String, name: String?) {
        error("unused")
    }
}

private class ThrowingMockIdsWalletDao : FakeCatalogWalletDao() {
    override suspend fun mockWalletIds(): List<String> = error("dao unavailable")
}
