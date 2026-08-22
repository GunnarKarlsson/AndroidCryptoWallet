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
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdWalletRoomReconcilerTest {
    @Test
    fun emptySnapshotLeavesDaoUnchanged() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore()
        HdWalletRoomReconciler(store, dao).reconcile()
        assertTrue(dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().isEmpty())
    }

    @Test
    fun emptyDaoInsertsHdRowFromSnapshotWithoutMnemonic() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore().apply {
            save(
                mnemonic = "secret words",
                passphrase = null,
                public = SNAPSHOT,
            )
        }

        HdWalletRoomReconciler(store, dao).reconcile()

        val row = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().single()
        assertEquals(SNAPSHOT.id, row.id)
        assertEquals(SNAPSHOT.receiveAddress, row.receiveAddress)
        assertEquals(BitcoinWalletKind.HD.name, row.kind)
        assertEquals(BitcoinScriptType.BIP84.name, row.scriptType)
        assertEquals(null, row.confirmedBalanceSatoshis)
        assertTrue(row.id.startsWith("mock:").not())
    }

    @Test
    fun existingRoomRowIsNotDuplicated() = runTest {
        val dao = FakeReconcileWalletDao()
        dao.insertIgnore(
            BitcoinWalletEntity(
                id = SNAPSHOT.id,
                network = SNAPSHOT.network.name,
                receiveAddress = SNAPSHOT.receiveAddress,
                derivationIndex = SNAPSHOT.derivationIndex,
                scriptType = SNAPSHOT.scriptType.name,
                kind = BitcoinWalletKind.HD.name,
                name = "Savings",
                confirmedBalanceSatoshis = 99L,
            ),
        )
        val store = FakeReconcileMnemonicStore().apply {
            save(mnemonic = "secret words", passphrase = null, public = SNAPSHOT)
        }

        HdWalletRoomReconciler(store, dao).reconcile()

        val rows = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first()
        assertEquals(1, rows.size)
        assertEquals(99L, rows.single().confirmedBalanceSatoshis)
        assertEquals("Savings", rows.single().name)
    }

    @Test
    fun doesNotInsertMockIdsFromStore() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore().apply {
            save(
                mnemonic = "unused",
                passphrase = null,
                public = SNAPSHOT.copy(id = "mock:TESTNET4:tb1qfake"),
            )
        }

        HdWalletRoomReconciler(store, dao).reconcile()

        assertTrue(dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().isEmpty())
    }
}

private val SNAPSHOT = BitcoinHdWalletPublic(
    id = "hd-restore-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1qrestore",
    derivationIndex = 0,
    scriptType = BitcoinScriptType.BIP84,
)

private class FakeReconcileMnemonicStore : BitcoinMnemonicStore {
    private val snapshots = mutableMapOf<String, BitcoinHdWalletPublic>()

    override fun save(
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    ) {
        snapshots[public.id] = public
    }

    override fun listHdWalletIds(): List<String> = snapshots.keys.sorted()

    override fun loadPublic(walletId: String): BitcoinHdWalletPublic? = snapshots[walletId]

    override fun loadMnemonic(walletId: String): String? = null

    override fun loadPassphrase(walletId: String): String? = null
}

private class FakeReconcileWalletDao : BitcoinWalletDao {
    private val items = MutableStateFlow<List<BitcoinWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override fun observeById(id: String): Flow<BitcoinWalletEntity?> =
        items.map { rows -> rows.find { it.id == id } }

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
