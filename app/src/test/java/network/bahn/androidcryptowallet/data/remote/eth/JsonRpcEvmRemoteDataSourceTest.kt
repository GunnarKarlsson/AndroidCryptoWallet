package network.bahn.androidcryptowallet.data.remote.eth

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonRpcEvmRemoteDataSourceTest {
    @Test
    fun parseHexWeiConvertsOneEth() {
        assertEquals(
            "1000000000000000000",
            JsonRpcEvmRemoteDataSource.parseHexWei("0xde0b6b3a7640000"),
        )
    }

    @Test
    fun parseHexWeiConvertsZero() {
        assertEquals("0", JsonRpcEvmRemoteDataSource.parseHexWei("0x0"))
    }
}
