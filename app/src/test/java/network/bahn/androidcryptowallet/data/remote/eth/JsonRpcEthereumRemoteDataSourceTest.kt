package network.bahn.androidcryptowallet.data.remote.eth

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonRpcEthereumRemoteDataSourceTest {
    @Test
    fun parseHexWeiConvertsOneEth() {
        assertEquals(
            "1000000000000000000",
            JsonRpcEthereumRemoteDataSource.parseHexWei("0xde0b6b3a7640000"),
        )
    }

    @Test
    fun parseHexWeiConvertsZero() {
        assertEquals("0", JsonRpcEthereumRemoteDataSource.parseHexWei("0x0"))
    }
}
