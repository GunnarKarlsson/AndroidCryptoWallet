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
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinSignedTransaction
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdWalletRoomReconcilerTest {
    @Test
    fun emptyStoreLeavesDaoUnchanged() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore()
        val engine = FakeReconcileKeyEngine()
        HdWalletRoomReconciler(engine, store, dao).reconcile()
        assertTrue(dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun emptyDaoInsertsHdRowByDerivingFromMnemonic() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = "secret-pass",
                network = BitcoinNetwork.TESTNET4,
            )
        }
        val engine = FakeReconcileKeyEngine()

        HdWalletRoomReconciler(engine, store, dao).reconcile()

        val row = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().single()
        assertEquals(WALLET_ID, row.id)
        assertEquals(DERIVED_ADDRESS, row.receiveAddress)
        assertEquals(0, row.derivationIndex)
        assertEquals(BitcoinWalletKind.HD.name, row.kind)
        assertEquals(BitcoinScriptType.BIP84.name, row.scriptType)
        assertEquals(null, row.confirmedBalanceSatoshis)
        assertEquals(
            listOf(DeriveCall(MNEMONIC.split(" "), "secret-pass", BitcoinNetwork.TESTNET4)),
            engine.deriveCalls,
        )
    }

    @Test
    fun skipsWhenMnemonicOrNetworkMissing() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore()
        store.put(id = "no-mnemonic", mnemonic = null, network = BitcoinNetwork.TESTNET4)
        store.put(id = "no-network", mnemonic = MNEMONIC, network = null)
        val engine = FakeReconcileKeyEngine()

        HdWalletRoomReconciler(engine, store, dao).reconcile()

        assertTrue(dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun existingRoomRowIsNotDuplicated() = runTest {
        val dao = FakeReconcileWalletDao()
        dao.insertIgnore(
            BitcoinWalletEntity(
                id = WALLET_ID,
                network = BitcoinNetwork.TESTNET4.name,
                receiveAddress = "tb1qstale",
                derivationIndex = 0,
                scriptType = BitcoinScriptType.BIP84.name,
                kind = BitcoinWalletKind.HD.name,
                name = "Savings",
                confirmedBalanceSatoshis = 99L,
            ),
        )
        val store = FakeReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = null,
                network = BitcoinNetwork.TESTNET4,
            )
        }
        val engine = FakeReconcileKeyEngine()

        HdWalletRoomReconciler(engine, store, dao).reconcile()

        val rows = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first()
        assertEquals(1, rows.size)
        assertEquals(99L, rows.single().confirmedBalanceSatoshis)
        assertEquals("Savings", rows.single().name)
        assertEquals("tb1qstale", rows.single().receiveAddress)
        assertEquals(1, engine.deriveCalls.size)
    }

    @Test
    fun doesNotInsertMockIdsFromStore() = runTest {
        val dao = FakeReconcileWalletDao()
        val store = FakeReconcileMnemonicStore().apply {
            save(
                walletId = "mock:TESTNET4:tb1qfake",
                mnemonic = MNEMONIC,
                passphrase = null,
                network = BitcoinNetwork.TESTNET4,
            )
        }
        val engine = FakeReconcileKeyEngine()

        HdWalletRoomReconciler(engine, store, dao).reconcile()

        assertTrue(dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }
}

private const val WALLET_ID = "hd-restore-1"
private const val MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
private const val DERIVED_ADDRESS = "tb1qderived"

private data class DeriveCall(
    val mnemonicWords: List<String>,
    val passphrase: String?,
    val network: BitcoinNetwork,
)

private class FakeReconcileKeyEngine : BitcoinKeyEngine {
    val deriveCalls = mutableListOf<DeriveCall>()

    override fun generateMnemonic(): List<String> = error("unused")

    override fun validateMnemonic(words: List<String>) = error("unused")

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress {
        deriveCalls += DeriveCall(mnemonicWords, passphrase, network)
        return BitcoinReceiveAddress(network, DERIVED_ADDRESS, 0)
    }

    override fun isValidAddress(network: BitcoinNetwork, address: String): Boolean = error("unused")

    override fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
        fundingTxHexes: List<String>,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
        changeAddress: String,
    ): BitcoinSignedTransaction = error("unused")
}

private class FakeReconcileMnemonicStore : BitcoinMnemonicStore {
    private val mnemonics = mutableMapOf<String, String?>()
    private val passphrases = mutableMapOf<String, String?>()
    private val networks = mutableMapOf<String, BitcoinNetwork?>()

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: BitcoinNetwork,
    ) {
        put(walletId, mnemonic, network, passphrase)
    }

    fun put(
        id: String,
        mnemonic: String?,
        network: BitcoinNetwork?,
        passphrase: String? = null,
    ) {
        mnemonics[id] = mnemonic
        networks[id] = network
        passphrases[id] = passphrase
    }

    override fun listHdWalletIds(): List<String> = mnemonics.keys.sorted()

    override fun delete(walletId: String) {
        mnemonics.remove(walletId)
        passphrases.remove(walletId)
        networks.remove(walletId)
    }

    override fun loadNetwork(walletId: String): BitcoinNetwork? = networks[walletId]

    override fun loadMnemonic(walletId: String): String? =
        mnemonics[walletId]?.takeIf { it.isNotEmpty() }

    override fun loadPassphrase(walletId: String): String? =
        passphrases[walletId]?.takeIf { it.isNotEmpty() }
}

private class FakeReconcileWalletDao : BitcoinWalletDao {
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

    override suspend fun deleteById(id: String) {
        deleteByIds(listOf(id))
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
