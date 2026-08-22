package network.bahn.androidcryptowallet.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import network.bahn.androidcryptowallet.domain.model.BitcoinHdWalletPublic
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores BIP-39 seed material (mnemonic + optional passphrase) and a public snapshot
 * (network, receive address, derivation index, script type), keyed by wallet id.
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
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    ) {
        prefs.edit()
            .putString(HdWalletPrefsCodec.MNEMONIC_PREFIX + public.id, mnemonic)
            .putString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + public.id, passphrase.orEmpty())
            .putString(HdWalletPrefsCodec.NETWORK_PREFIX + public.id, public.network.name)
            .putString(HdWalletPrefsCodec.ADDRESS_PREFIX + public.id, public.receiveAddress)
            .putInt(HdWalletPrefsCodec.INDEX_PREFIX + public.id, public.derivationIndex)
            .putString(HdWalletPrefsCodec.SCRIPT_PREFIX + public.id, public.scriptType.name)
            .apply()
    }

    override fun listHdWalletIds(): List<String> =
        HdWalletPrefsCodec.walletIdsFromKeys(prefs.all.keys)

    override fun loadPublic(walletId: String): BitcoinHdWalletPublic? {
        val strings = mapOf(
            HdWalletPrefsCodec.NETWORK_PREFIX + walletId to
                prefs.getString(HdWalletPrefsCodec.NETWORK_PREFIX + walletId, null),
            HdWalletPrefsCodec.ADDRESS_PREFIX + walletId to
                prefs.getString(HdWalletPrefsCodec.ADDRESS_PREFIX + walletId, null),
            HdWalletPrefsCodec.SCRIPT_PREFIX + walletId to
                prefs.getString(HdWalletPrefsCodec.SCRIPT_PREFIX + walletId, null),
        )
        val ints = mapOf(
            HdWalletPrefsCodec.INDEX_PREFIX + walletId to
                prefs.getInt(HdWalletPrefsCodec.INDEX_PREFIX + walletId, 0),
        )
        return HdWalletPrefsCodec.loadPublic(walletId, strings, ints)
    }

    override fun loadMnemonic(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    override fun loadPassphrase(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    private companion object {
        const val PREFS_FILE = "bitcoin_mnemonic"
    }
}
