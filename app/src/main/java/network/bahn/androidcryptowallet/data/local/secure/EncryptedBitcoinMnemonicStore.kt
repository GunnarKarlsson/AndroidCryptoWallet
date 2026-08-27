package network.bahn.androidcryptowallet.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores BIP-39 seed material (mnemonic + optional passphrase) and the derivation
 * network, keyed by wallet id. Receive address, index, and script type live in Room.
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

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: BitcoinNetwork,
    ) {
        prefs.edit()
            .putString(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId, mnemonic)
            .putString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId, passphrase.orEmpty())
            .putString(HdWalletPrefsCodec.NETWORK_PREFIX + walletId, network.name)
            .remove(LEGACY_ADDRESS_PREFIX + walletId)
            .remove(LEGACY_INDEX_PREFIX + walletId)
            .remove(LEGACY_SCRIPT_PREFIX + walletId)
            .apply()
    }

    override fun listHdWalletIds(): List<String> =
        HdWalletPrefsCodec.walletIdsFromKeys(prefs.all.keys)

    override fun delete(walletId: String) {
        prefs.edit()
            .remove(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId)
            .remove(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId)
            .remove(HdWalletPrefsCodec.NETWORK_PREFIX + walletId)
            .apply()
    }

    override fun loadNetwork(walletId: String): BitcoinNetwork? =
        HdWalletPrefsCodec.loadNetwork(
            walletId,
            mapOf(
                HdWalletPrefsCodec.NETWORK_PREFIX + walletId to
                    prefs.getString(HdWalletPrefsCodec.NETWORK_PREFIX + walletId, null),
            ),
        )

    override fun loadMnemonic(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    override fun loadPassphrase(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    private companion object {
        const val PREFS_FILE = "bitcoin_mnemonic"
        const val LEGACY_ADDRESS_PREFIX = "address_"
        const val LEGACY_INDEX_PREFIX = "index_"
        const val LEGACY_SCRIPT_PREFIX = "script_"
    }
}
