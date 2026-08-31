package network.bahn.androidcryptowallet.domain.model

import java.math.BigInteger

sealed class PortfolioHoldingDestination {
    data object Bitcoin : PortfolioHoldingDestination()

    data class Evm(val family: EvmFamily) : PortfolioHoldingDestination()
}

data class PortfolioHolding(
    val destination: PortfolioHoldingDestination,
    /** Display headline, e.g. "Bitcoin (BTC)". Used for sorting. */
    val headline: String,
    val nativeSymbol: String,
    val balanceSatoshis: Long? = null,
    val balanceWei: BigInteger? = null,
)
