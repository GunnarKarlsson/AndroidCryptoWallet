package network.bahn.androidcryptowallet.data.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletEntity
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchOnlyBitcoinWalletSeederTest {
    @Test
    fun emptyConfigLeavesListUnchanged() = runTest {
        val dao = FakeSeedBitcoinWalletDao()
        dao.insert(
            BitcoinWalletEntity(
                id = "hd-1",
                network = BitcoinNetwork.TESTNET4.name,
                receiveAddress = "tb1qhd",
                derivationIndex = 0,
                scriptType = BitcoinScriptType.BIP84.name,
                kind = BitcoinWalletKind.HD.name,
            ),
        )
        dao.insert(
            BitcoinWalletEntity(
                id = MockBitcoinWalletConfig.walletId(BitcoinNetwork.TESTNET4, "tb1qstale"),
                network = BitcoinNetwork.TESTNET4.name,
                receiveAddress = "tb1qstale",
                derivationIndex = 0,
                scriptType = BitcoinScriptType.EXTERNAL.name,
                kind = BitcoinWalletKind.WATCH_ONLY.name,
            ),
        )
        val seeder = WatchOnlyBitcoinWalletSeeder(
            config = MockBitcoinWalletConfig.fromRaw("", ""),
            walletDao = dao,
        )

        seeder.seed()

        val rows = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first()
        assertEquals(listOf("hd-1"), rows.map { it.id })
        assertTrue(dao.observeByNetwork(BitcoinNetwork.MAINNET.name).first().isEmpty())
    }

    @Test
    fun seedInsertsWatchOnlyRowsWithoutMnemonicOrKeys() = runTest {
        val dao = FakeSeedBitcoinWalletDao()
        val mnemonicStore = mutableMapOf<String, String>()
        val seeder = WatchOnlyBitcoinWalletSeeder(
            config = MockBitcoinWalletConfig.fromRaw(
                testnet4Raw = TESTNET_ADDRESS,
                mainnetRaw = MAINNET_ADDRESS,
            ),
            walletDao = dao,
        )

        seeder.seed()

        val testnet = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first().single().toDomain()
        assertEquals(MockBitcoinWalletConfig.walletId(BitcoinNetwork.TESTNET4, TESTNET_ADDRESS), testnet.id)
        assertEquals(TESTNET_ADDRESS, testnet.receiveAddress)
        assertEquals(BitcoinWalletKind.WATCH_ONLY, testnet.kind)
        assertEquals(BitcoinScriptType.EXTERNAL, testnet.scriptType)

        val mainnet = dao.observeByNetwork(BitcoinNetwork.MAINNET.name).first().single().toDomain()
        assertEquals(MAINNET_ADDRESS, mainnet.receiveAddress)
        assertEquals(BitcoinWalletKind.WATCH_ONLY, mainnet.kind)
        assertTrue(mnemonicStore.isEmpty())
    }

    @Test
    fun seedIsIdempotentAndPreservesCachedBalance() = runTest {
        val dao = FakeSeedBitcoinWalletDao()
        val seeder = WatchOnlyBitcoinWalletSeeder(
            config = MockBitcoinWalletConfig.fromRaw(TESTNET_ADDRESS, ""),
            walletDao = dao,
        )
        seeder.seed()
        val id = MockBitcoinWalletConfig.walletId(BitcoinNetwork.TESTNET4, TESTNET_ADDRESS)
        dao.updateBalance(id, 99L, 1L, 42L)

        seeder.seed()

        val rows = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first()
        assertEquals(1, rows.size)
        assertEquals(99L, rows.single().confirmedBalanceSatoshis)
        assertEquals(1L, rows.single().unconfirmedBalanceSatoshis)
        assertEquals(42L, rows.single().balanceUpdatedAtMillis)
    }

    @Test
    fun seedPrunesStaleMockRowsAndLeavesHdWallets() = runTest {
        val dao = FakeSeedBitcoinWalletDao()
        dao.insert(
            BitcoinWalletEntity(
                id = "hd-1",
                network = BitcoinNetwork.TESTNET4.name,
                receiveAddress = "tb1qhd",
                derivationIndex = 0,
                scriptType = BitcoinScriptType.BIP84.name,
                kind = BitcoinWalletKind.HD.name,
            ),
        )
        dao.insert(
            BitcoinWalletEntity(
                id = MockBitcoinWalletConfig.walletId(BitcoinNetwork.TESTNET4, "tb1qold"),
                network = BitcoinNetwork.TESTNET4.name,
                receiveAddress = "tb1qold",
                derivationIndex = 0,
                scriptType = BitcoinScriptType.EXTERNAL.name,
                kind = BitcoinWalletKind.WATCH_ONLY.name,
            ),
        )
        val seeder = WatchOnlyBitcoinWalletSeeder(
            config = MockBitcoinWalletConfig.fromRaw(TESTNET_ADDRESS, ""),
            walletDao = dao,
        )

        seeder.seed()

        val rows = dao.observeByNetwork(BitcoinNetwork.TESTNET4.name).first()
        assertEquals(2, rows.size)
        assertEquals(setOf("hd-1", MockBitcoinWalletConfig.walletId(BitcoinNetwork.TESTNET4, TESTNET_ADDRESS)), rows.map { it.id }.toSet())
    }

    @Test
    fun parseAddressesSplitsAndTrims() {
        assertEquals(
            listOf("tb1qa", "tb1qb"),
            MockBitcoinWalletConfig.parseAddresses(" tb1qa , tb1qb, tb1qa "),
        )
        assertEquals(emptyList<String>(), MockBitcoinWalletConfig.parseAddresses("  ,  "))
    }
}

private const val TESTNET_ADDRESS = "tb1qtestwatchonly"
private const val MAINNET_ADDRESS = "1MockMainnetAddress"

private class FakeSeedBitcoinWalletDao : BitcoinWalletDao {
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
        items.update { rows ->
            rows.map { row ->
                if (row.id != id) {
                    row
                } else {
                    row.copy(
                        confirmedBalanceSatoshis = confirmedSatoshis,
                        unconfirmedBalanceSatoshis = unconfirmedSatoshis,
                        balanceUpdatedAtMillis = updatedAtMillis,
                    )
                }
            }
        }
    }
}
