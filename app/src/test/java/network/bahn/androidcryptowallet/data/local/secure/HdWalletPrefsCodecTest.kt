package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdWalletPrefsCodecTest {
    @Test
    fun walletIdsFromNetworkKeysIgnoreMnemonicOnlyOrphans() {
        val ids = HdWalletPrefsCodec.walletIdsFromKeys(
            setOf(
                "mnemonic_orphan",
                "passphrase_orphan",
                "network_hd-1",
                "address_hd-1",
                "network_hd-2",
            ),
        )
        assertEquals(listOf("hd-1", "hd-2"), ids)
    }

    @Test
    fun loadPublicReadsSnapshotFields() {
        val walletId = "hd-1"
        val public = HdWalletPrefsCodec.loadPublic(
            walletId = walletId,
            strings = mapOf(
                "network_$walletId" to BitcoinNetwork.TESTNET4.name,
                "address_$walletId" to "tb1qabc",
                "script_$walletId" to BitcoinScriptType.BIP84.name,
            ),
            ints = mapOf("index_$walletId" to 0),
        )
        assertEquals(walletId, public?.id)
        assertEquals(BitcoinNetwork.TESTNET4, public?.network)
        assertEquals("tb1qabc", public?.receiveAddress)
        assertEquals(0, public?.derivationIndex)
        assertEquals(BitcoinScriptType.BIP84, public?.scriptType)
    }

    @Test
    fun loadPublicReturnsNullWithoutNetworkOrAddress() {
        assertNull(
            HdWalletPrefsCodec.loadPublic(
                walletId = "hd-1",
                strings = mapOf("mnemonic_hd-1" to "abandon abandon"),
                ints = emptyMap(),
            ),
        )
    }

    @Test
    fun mnemonicPrefixIsNotUsedForListing() {
        assertTrue(
            HdWalletPrefsCodec.walletIdsFromKeys(setOf("mnemonic_only")).isEmpty(),
        )
    }
}
