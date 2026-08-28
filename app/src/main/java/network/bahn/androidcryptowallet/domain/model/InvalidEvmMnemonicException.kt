package network.bahn.androidcryptowallet.domain.model

/** Thrown when a BIP-39 mnemonic fails wordlist or checksum checks. */
class InvalidEvmMnemonicException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
