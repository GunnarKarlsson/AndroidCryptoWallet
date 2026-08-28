package network.bahn.androidcryptowallet.domain.model

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object EvmGasQuotes {
    /** Typical gas used by a simple native ETH transfer. */
    const val SIMPLE_TRANSFER_GAS_LIMIT = 21_000L

    fun quote(
        feeData: EvmFeeData,
        preset: EvmGasPreset,
        gasLimit: Long = SIMPLE_TRANSFER_GAS_LIMIT,
    ): EvmGasQuote {
        val suggested = BigInteger(feeData.suggestedPriorityFeePerGasWei)
        val priority = suggested.toBigDecimal()
            .multiply(BigDecimal.valueOf(preset.priorityMultiplier))
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger()
            .max(BigInteger.ONE)
        val base = BigInteger(feeData.baseFeePerGasWei)
        val maxFee = base.multiply(BigInteger.TWO).add(priority)
        val estimated = BigInteger.valueOf(gasLimit).multiply(maxFee)
        return EvmGasQuote(
            gasLimit = gasLimit,
            maxPriorityFeePerGasWei = priority.toString(),
            maxFeePerGasWei = maxFee.toString(),
            estimatedFeeWei = estimated.toString(),
        )
    }
}
