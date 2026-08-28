package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EthereumPaymentUriTest {
    @Test
    fun fromAddressUsesEip681WithSepoliaChainId() {
        assertEquals(
            "ethereum:0x9858EfFD232B4033E47d90003D41EC34EcaEda94@11155111",
            EthereumPaymentUri.fromAddress(
                "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                EvmNetwork.SEPOLIA,
            ),
        )
    }

    @Test
    fun fromAddressUsesEip681WithMainnetChainId() {
        assertEquals(
            "ethereum:0x9858EfFD232B4033E47d90003D41EC34EcaEda94@1",
            EthereumPaymentUri.fromAddress(
                "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
                EvmNetwork.MAINNET,
            ),
        )
    }
}
