package network.bahn.androidcryptowallet.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores BIP-39 seed material (mnemonic + optional passphrase) and the derivation
 * network, keyed by wallet id. Address and index live in Room.
 * Prefs file is [PREFS_FILE], separate from Bitcoin secrets.
 */
@Singleton
class EncryptedEthereumMnemonicStore @Inject constructor(
    @ApplicationContext context: Context,
) : EthereumMnemonicStore {
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
        network: EthereumNetwork,
    ) {
        prefs.edit()
            .putString(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId, mnemonic)
            .putString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId, passphrase.orEmpty())
            .putString(HdWalletPrefsCodec.NETWORK_PREFIX + walletId, network.name)
            .apply()
    }

    override fun listHdWalletIds(): List<String> =
        HdWalletPrefsCodec.walletIdsFromKeys(prefs.all.keys)

    override fun loadNetwork(walletId: String): EthereumNetwork? {
        val networkName = prefs.getString(HdWalletPrefsCodec.NETWORK_PREFIX + walletId, null)
            ?: return null
        return runCatching { EthereumNetwork.valueOf(networkName) }.getOrNull()
    }

    override fun loadMnemonic(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.MNEMONIC_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    override fun loadPassphrase(walletId: String): String? =
        prefs.getString(HdWalletPrefsCodec.PASSPHRASE_PREFIX + walletId, null)
            ?.takeIf { it.isNotEmpty() }

    private companion object {
        const val PREFS_FILE = "ethereum_mnemonic"
    }
}
