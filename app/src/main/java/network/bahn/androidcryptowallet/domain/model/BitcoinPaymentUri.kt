package network.bahn.androidcryptowallet.domain.model

/**
 * BIP-21 payment URI. Amount and label are omitted; the address identifies the network.
 */
object BitcoinPaymentUri {
    fun fromAddress(address: String): String = "bitcoin:$address"
}
