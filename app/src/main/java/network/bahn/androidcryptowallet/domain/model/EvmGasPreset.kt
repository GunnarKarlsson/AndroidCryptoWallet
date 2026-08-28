package network.bahn.androidcryptowallet.domain.model

/**
 * Gas priority presets for native ETH sends.
 * Multiplier is applied to the node-suggested max priority fee.
 */
enum class EvmGasPreset(val priorityMultiplier: Double) {
    Slow(0.8),
    Normal(1.0),
    Fast(1.5),
}
