package network.bahn.androidcryptowallet.data.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.EvmWalletDao
import network.bahn.androidcryptowallet.data.local.db.EvmWalletEntity
import network.bahn.androidcryptowallet.data.local.secure.EvmMnemonicStore
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmReceiveAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EthereumHdWalletRoomReconcilerTest {
    @Test
    fun emptyStoreLeavesDaoUnchanged() = runTest {
        val dao = FakeEvmReconcileDao()
        val store = FakeEthReconcileMnemonicStore()
        val engine = FakeEthReconcileKeyEngine()
        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()
        assertTrue(dao.observeByNetwork(EvmNetwork.SEPOLIA.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun emptyDaoInsertsHdRowByDerivingFromMnemonic() = runTest {
        val dao = FakeEvmReconcileDao()
        val store = FakeEthReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = "secret-pass",
                network = EvmNetwork.SEPOLIA,
            )
        }
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        val row = dao.observeByNetwork(EvmNetwork.SEPOLIA.name).first().single()
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
        val dao = FakeEvmReconcileDao()
        val store = FakeEthReconcileMnemonicStore()
        store.put(id = "no-mnemonic", mnemonic = null, network = EvmNetwork.SEPOLIA)
        store.put(id = "no-network", mnemonic = MNEMONIC, network = null)
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        assertTrue(dao.observeByNetwork(EvmNetwork.SEPOLIA.name).first().isEmpty())
        assertTrue(engine.deriveCalls.isEmpty())
    }

    @Test
    fun existingRoomRowIsNotDuplicated() = runTest {
        val dao = FakeEvmReconcileDao()
        dao.insertIgnore(
            EvmWalletEntity(
                id = WALLET_ID,
                network = EvmNetwork.SEPOLIA.name,
                address = "0xexisting",
                derivationIndex = 0,
            ),
        )
        val store = FakeEthReconcileMnemonicStore().apply {
            save(
                walletId = WALLET_ID,
                mnemonic = MNEMONIC,
                passphrase = null,
                network = EvmNetwork.SEPOLIA,
            )
        }
        val engine = FakeEthReconcileKeyEngine()

        EthereumHdWalletRoomReconciler(engine, store, dao).reconcile()

        val rows = dao.observeByNetwork(EvmNetwork.SEPOLIA.name).first()
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
    ): EvmReceiveAddress {
        deriveCalls += EthDeriveCall(mnemonicWords, passphrase)
        return EvmReceiveAddress(address = "0xderived", index = 0)
    }

    override fun isValidAddress(address: String): Boolean = error("unused")

    override fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        chainId: Long,
        to: String,
        valueWei: java.math.BigInteger,
        nonce: Long,
        gasLimit: Long,
        maxPriorityFeePerGasWei: java.math.BigInteger,
        maxFeePerGasWei: java.math.BigInteger,
    ): String = error("unused")
}

private class FakeEthReconcileMnemonicStore : EvmMnemonicStore {
    private val mnemonics = mutableMapOf<String, String?>()
    private val passphrases = mutableMapOf<String, String?>()
    private val networks = mutableMapOf<String, EvmNetwork?>()

    fun put(id: String, mnemonic: String?, network: EvmNetwork?) {
        mnemonics[id] = mnemonic
        networks[id] = network
    }

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: EvmNetwork,
    ) {
        mnemonics[walletId] = mnemonic
        passphrases[walletId] = passphrase
        networks[walletId] = network
    }

    override fun listHdWalletIds(): List<String> =
        (mnemonics.keys + networks.keys).distinct().sorted()

    override fun delete(walletId: String) {
        mnemonics.remove(walletId)
        passphrases.remove(walletId)
        networks.remove(walletId)
    }

    override fun loadNetwork(walletId: String): EvmNetwork? = networks[walletId]

    override fun loadMnemonic(walletId: String): String? = mnemonics[walletId]

    override fun loadPassphrase(walletId: String): String? = passphrases[walletId]
}

private class FakeEvmReconcileDao : EvmWalletDao {
    private val items = MutableStateFlow<List<EvmWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<EvmWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override fun observeById(id: String): Flow<EvmWalletEntity?> =
        items.map { rows -> rows.find { it.id == id } }

    override suspend fun findByNetworkAndAddress(
        network: String,
        address: String,
    ): EvmWalletEntity? = items.value.find {
        it.network == network && it.address == address
    }

    override suspend fun insert(entity: EvmWalletEntity) {
        items.update { it + entity }
    }

    override suspend fun insertIgnore(entity: EvmWalletEntity) {
        items.update { rows ->
            if (rows.any { it.id == entity.id }) rows else rows + entity
        }
    }

    override suspend fun deleteById(id: String) {
        items.update { rows -> rows.filter { it.id != id } }
    }

    override suspend fun updateBalance(
        id: String,
        balanceWei: String,
        updatedAtMillis: Long,
    ) = Unit

    override suspend fun updateName(id: String, name: String?) = Unit
}
