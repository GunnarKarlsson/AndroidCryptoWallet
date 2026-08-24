package network.bahn.androidcryptowallet.data.local.secure

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdWalletPrefsCodecTest {
    @Test
    fun walletIdsFromMnemonicKeysIgnoreNetworkOnlyOrphans() {
        val ids = HdWalletPrefsCodec.walletIdsFromKeys(
            setOf(
                "network_orphan",
                "address_orphan",
                "mnemonic_hd-1",
                "passphrase_hd-1",
                "network_hd-1",
                "mnemonic_hd-2",
            ),
        )
        assertEquals(listOf("hd-1", "hd-2"), ids)
    }

    @Test
    fun loadNetworkReadsStoredNetwork() {
        val walletId = "hd-1"
        val network = HdWalletPrefsCodec.loadNetwork(
            walletId = walletId,
            strings = mapOf("network_$walletId" to BitcoinNetwork.TESTNET4.name),
        )
        assertEquals(BitcoinNetwork.TESTNET4, network)
    }

    @Test
    fun loadNetworkReturnsNullWithoutNetwork() {
        assertNull(
            HdWalletPrefsCodec.loadNetwork(
                walletId = "hd-1",
                strings = mapOf("mnemonic_hd-1" to "abandon abandon"),
            ),
        )
    }

    @Test
    fun leftoverAddressKeysAreNotUsedForListing() {
        assertTrue(
            HdWalletPrefsCodec.walletIdsFromKeys(
                setOf("address_hd-1", "index_hd-1", "script_hd-1", "network_hd-1"),
            ).isEmpty(),
        )
    }
}
