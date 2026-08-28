package network.bahn.androidcryptowallet.data.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletEntity
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinSignedTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCatalogInitializerTest {
    @Test
    fun initializeMarksReadyAfterReconcileAndSeed() = runTest {
        val dao = FakeCatalogWalletDao()
        val initializer = WalletCatalogInitializer(
            hdWalletRoomReconciler = HdWalletRoomReconciler(
                keyEngine = UnusedCatalogKeyEngine(),
                mnemonicStore = FakeCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("", ""),
                walletDao = dao,
            ),
            ethereumHdWalletRoomReconciler = unusedEthereumReconciler(),
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
                keyEngine = UnusedCatalogKeyEngine(),
                mnemonicStore = ThrowingCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("", ""),
                walletDao = dao,
            ),
            ethereumHdWalletRoomReconciler = unusedEthereumReconciler(),
        )

        initializer.initialize()
        assertTrue(initializer.observeReady().first())
    }

    @Test
    fun initializeMarksReadyWhenSeedFails() = runTest {
        val dao = ThrowingMockIdsWalletDao()
        val initializer = WalletCatalogInitializer(
            hdWalletRoomReconciler = HdWalletRoomReconciler(
                keyEngine = UnusedCatalogKeyEngine(),
                mnemonicStore = FakeCatalogMnemonicStore(),
                walletDao = dao,
            ),
            watchOnlyBitcoinWalletSeeder = WatchOnlyBitcoinWalletSeeder(
                config = MockBitcoinWalletConfig.fromRaw("tb1qmock", ""),
                walletDao = dao,
            ),
            ethereumHdWalletRoomReconciler = unusedEthereumReconciler(),
        )

        initializer.initialize()
        assertTrue(initializer.observeReady().first())
    }
}

private class UnusedCatalogKeyEngine : BitcoinKeyEngine {
    override fun generateMnemonic(): List<String> = error("unused")

    override fun validateMnemonic(words: List<String>) = error("unused")

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress = error("unused")

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

private class FakeCatalogMnemonicStore : BitcoinMnemonicStore {
    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: BitcoinNetwork,
    ) = Unit

    override fun listHdWalletIds(): List<String> = emptyList()

    override fun delete(walletId: String) = Unit

    override fun loadNetwork(walletId: String): BitcoinNetwork? = null

    override fun loadMnemonic(walletId: String): String? = null

    override fun loadPassphrase(walletId: String): String? = null
}

private class ThrowingCatalogMnemonicStore : BitcoinMnemonicStore {
    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: BitcoinNetwork,
    ) = Unit

    override fun listHdWalletIds(): List<String> = error("encrypted store unavailable")

    override fun delete(walletId: String) = Unit

    override fun loadNetwork(walletId: String): BitcoinNetwork? = null

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

private class ThrowingMockIdsWalletDao : FakeCatalogWalletDao() {
    override suspend fun mockWalletIds(): List<String> = error("dao unavailable")
}

private fun unusedEthereumReconciler() = EthereumHdWalletRoomReconciler(
    keyEngine = UnusedEthereumCatalogKeyEngine(),
    mnemonicStore = EmptyEthereumCatalogMnemonicStore(),
    walletDao = EmptyEthereumCatalogWalletDao(),
)

private class UnusedEthereumCatalogKeyEngine : EthereumKeyEngine {
    override fun generateMnemonic(): List<String> = error("unused")

    override fun validateMnemonic(words: List<String>) = error("unused")

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

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

private class EmptyEthereumCatalogMnemonicStore : EthereumMnemonicStore {
    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: network.bahn.androidcryptowallet.domain.model.EvmNetwork,
    ) = Unit

    override fun listHdWalletIds(): List<String> = emptyList()

    override fun delete(walletId: String) = Unit

    override fun loadNetwork(
        walletId: String,
    ): network.bahn.androidcryptowallet.domain.model.EvmNetwork? = null

    override fun loadMnemonic(walletId: String): String? = null

    override fun loadPassphrase(walletId: String): String? = null
}

private class EmptyEthereumCatalogWalletDao : EthereumWalletDao {
    override fun observeByNetwork(
        network: String,
    ): Flow<List<network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity>> =
        MutableStateFlow(emptyList())

    override fun observeById(
        id: String,
    ): Flow<network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity?> =
        MutableStateFlow(null)

    override suspend fun findByNetworkAndAddress(
        network: String,
        address: String,
    ) = null

    override suspend fun insert(
        entity: network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity,
    ) = Unit

    override suspend fun insertIgnore(
        entity: network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity,
    ) = Unit

    override suspend fun deleteById(id: String) = Unit

    override suspend fun updateBalance(
        id: String,
        balanceWei: String,
        updatedAtMillis: Long,
    ) = Unit

    override suspend fun updateName(id: String, name: String?) = Unit
}
