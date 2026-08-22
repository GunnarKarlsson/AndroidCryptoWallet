package network.bahn.androidcryptowallet.data.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class Bip39ExceptionMessageTest {
    @Test
    fun unknownWordIndexBecomesPosition() {
        assertEquals(
            "Unknown recovery word at position 1",
            formatBip39ExceptionMessage(RuntimeException("index=0")),
        )
        assertEquals(
            "Unknown recovery word at position 3",
            formatBip39ExceptionMessage(RuntimeException("unknown word at index 2")),
        )
    }

    @Test
    fun checksumMessageIsRewritten() {
        assertEquals(
            "Recovery phrase checksum is invalid",
            formatBip39ExceptionMessage(RuntimeException("checksum is invalid")),
        )
        assertEquals(
            "Recovery phrase checksum is invalid",
            formatBip39ExceptionMessage(InvalidChecksum()),
        )
    }

    @Test
    fun blankFallsBack() {
        assertEquals(
            "Invalid BIP-39 mnemonic",
            formatBip39ExceptionMessage(RuntimeException("")),
        )
    }

    private class InvalidChecksum : RuntimeException()
}
