package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress

interface BitcoinWalletRepository {
    fun observeWalletExists(): Flow<Boolean>
    fun observeReceiveAddress(): Flow<BitcoinReceiveAddress?>

    /** BIP-39: generate a 12-word mnemonic. Does not persist. */
    fun generateMnemonic(): List<String>

    /**
     * Persist a confirmed BIP-39 mnemonic (optional passphrase) and cache BIP-84
     * receive addresses for both networks.
     */
    suspend fun createWallet(mnemonicWords: List<String>, passphrase: String?)

    /** BIP-39 import (12 or 24 words), then same persist path as [createWallet]. */
    suspend fun importWallet(mnemonic: String, passphrase: String?)
}
