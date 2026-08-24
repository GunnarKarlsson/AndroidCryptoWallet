package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BitcoinPaymentUriTest {
    @Test
    fun fromAddressUsesBip21Scheme() {
        assertEquals(
            "bitcoin:tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
            BitcoinPaymentUri.fromAddress("tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"),
        )
    }

    @Test
    fun fromAddressWorksForMainnetBech32() {
        assertEquals(
            "bitcoin:bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu",
            BitcoinPaymentUri.fromAddress("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"),
        )
    }
}
