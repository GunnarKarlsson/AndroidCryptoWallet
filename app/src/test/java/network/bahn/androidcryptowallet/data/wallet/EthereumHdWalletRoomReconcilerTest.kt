package network.bahn.androidcryptowallet.data.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumReceiveAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EthereumHdWalletRoomReconcilerTest {
    @Test
    fun emptyStoreLeavesDaoUnchanged() = runTest {
        val dao = FakeEthReconcileDao()
        val store = FakeEthReconcileMnemonicStore()
        val engine = FakeEthReconcileKeyEngine()
        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()
        assertTrue(dao.observeByNetwork(EthereumNetwork.SEPOLIA.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun emptyDaoInsertsHdRowByDerivingFromMnemonic() = runTest {
        val dao = FakeEthReconcileDao()
        val store = FakeEthReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = "secret-pass",
                network = EthereumNetwork.SEPOLIA,
            )
        }
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        val row = dao.observeByNetwork(EthereumNetwork.SEPOLIA.name).first().single()
        assertEquals(WALLET_ID, row.id)
        assertEquals(DERIVED_ADDRESS, row.address)
        assertEquals(0, row.derivationIndex)
        assertEquals(
            listOf(EthDeriveCall(MNEMONIC.split(" "), "secret-pass")),
            engine.deriveCalls,
        )
    }

    @Test
    fun skipsWhenMnemonicOrNetworkMissing() = runTest {
        val dao = FakeEthReconcileDao()
        val store = FakeEthReconcileMnemonicStore()
        store.put(id = "no-mnemonic", mnemonic = null, network = EthereumNetwork.SEPOLIA)
        store.put(id = "no-network", mnemonic = MNEMONIC, network = null)
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        assertTrue(dao.observeByNetwork(EthereumNetwork.SEPOLIA.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun existingRoomRowIsNotDuplicated() = runTest {
        val dao = FakeEthReconcileDao()
        dao.insertIgnore(
            EthereumWalletEntity(
                id = WALLET_ID,
                network = EthereumNetwork.SEPOLIA.name,
                address = "0xexisting",
                derivationIndex = 0,
            ),
        )
        val store = FakeEthReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = null,
                network = EthereumNetwork.SEPOLIA,
            )
        }
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        val rows = dao.observeByNetwork(EthereumNetwork.SEPOLIA.name).first()
        assertEquals(1, rows.size)
        assertEquals("0xexisting", rows.single().address)
    }

    private companion object {
        const val WALLET_ID = "eth-1"
        const val MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        const val DERIVED_ADDRESS = "0xderived"
    }
}

private data class EthDeriveCall(
    val mnemonicWords: List<String>,
    val passphrase: String?,
)

private class FakeEthReconcileKeyEngine : EthereumKeyEngine {
    val deriveCalls = mutableListOf<EthDeriveCall>()

    override fun generateMnemonic(): List<String> = error("unused")

    override fun validateMnemonic(words: List<String>) = error("unused")

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): EthereumReceiveAddress {
        deriveCalls += EthDeriveCall(mnemonicWords, passphrase)
        return EthereumReceiveAddress(address = "0xderived", index = 0)
    }
}

private class FakeEthReconcileMnemonicStore : EthereumMnemonicStore {
    private val mnemonics = mutableMapOf<String, String?>()
    private val passphrases = mutableMapOf<String, String?>()
    private val networks = mutableMapOf<String, EthereumNetwork?>()

    fun put(id: String, mnemonic: String?, network: EthereumNetwork?) {
        mnemonics[id] = mnemonic
        networks[id] = network
    }

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: EthereumNetwork,
    ) {
        mnemonics[walletId] = mnemonic
        passphrases[walletId] = passphrase
        networks[walletId] = network
    }

    override fun listHdWalletIds(): List<String> =
        (mnemonics.keys + networks.keys).distinct().sorted()

    override fun loadNetwork(walletId: String): EthereumNetwork? = networks[walletId]

    override fun loadMnemonic(walletId: String): String? = mnemonics[walletId]

    override fun loadPassphrase(walletId: String): String? = passphrases[walletId]
}

private class FakeEthReconcileDao : EthereumWalletDao {
    private val items = MutableStateFlow<List<EthereumWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<EthereumWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override suspend fun findByNetworkAndAddress(
        network: String,
        address: String,
    ): EthereumWalletEntity? = items.value.find {
        it.network == network && it.address == address
    }

    override suspend fun insert(entity: EthereumWalletEntity) {
        items.update { it + entity }
    }

    override suspend fun insertIgnore(entity: EthereumWalletEntity) {
        items.update { rows ->
            if (rows.any { it.id == entity.id }) rows else rows + entity
        }
    }
}
