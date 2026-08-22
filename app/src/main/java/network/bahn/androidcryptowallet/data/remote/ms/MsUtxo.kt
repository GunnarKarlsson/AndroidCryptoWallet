package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.serialization.Serializable
import network.bahn.androidcryptowallet.domain.model.BitcoinUtxo

@Serializable
data class MsUtxoResponse(
    val txid: String,
    val vout: Int,
    val value: Long,
    val status: MsUtxoStatus? = null,
)

@Serializable
data class MsUtxoStatus(
    val confirmed: Boolean = false,
)

fun MsUtxoResponse.toDomain(): BitcoinUtxo = BitcoinUtxo(
    txid = txid,
    vout = vout,
    valueSatoshis = value,
    confirmed = status?.confirmed == true,
)
