package network.bahn.androidcryptowallet.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores BIP-39 seed material only (mnemonic + optional passphrase).
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

    override fun hasWallet(): Boolean = prefs.contains(KEY_MNEMONIC)

    override fun save(mnemonic: String, passphrase: String?) {
        prefs.edit()
            .putString(KEY_MNEMONIC, mnemonic)
            .putString(KEY_PASSPHRASE, passphrase.orEmpty())
            .apply()
    }

    private companion object {
        const val PREFS_FILE = "bitcoin_mnemonic"
        const val KEY_MNEMONIC = "mnemonic"
        const val KEY_PASSPHRASE = "passphrase"
    }
}
