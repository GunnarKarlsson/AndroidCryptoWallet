package network.bahn.androidcryptowallet.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores BIP-39 seed material only (mnemonic + optional passphrase), keyed by wallet id.
 * Does not store BIP-32 xprv or per-address private keys; those are derived in memory.
 */
@Singleton
class EncryptedBitcoinMnemonicStore @Inject constructor(
    @ApplicationContext context: Context,
) : BitcoinMnemonicStore {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun save(walletId: String, mnemonic: String, passphrase: String?) {
        prefs.edit()
            .putString(mnemonicKey(walletId), mnemonic)
            .putString(passphraseKey(walletId), passphrase.orEmpty())
            .apply()
    }

    private fun mnemonicKey(walletId: String) = "mnemonic_$walletId"

    private fun passphraseKey(walletId: String) = "passphrase_$walletId"

    private companion object {
        const val PREFS_FILE = "bitcoin_mnemonic"
    }
}
