package network.bahn.androidcryptowallet.domain

fun interface TimeProvider {
    fun nowMillis(): Long
}
