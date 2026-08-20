package network.bahn.androidcryptowallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import network.bahn.androidcryptowallet.ui.bitcoin.BitcoinHomeScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinConfirmMnemonicScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinCreateWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinImportWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinPlaceholderMnemonic
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinWelcomeScreen

@Composable
fun WalletNavHost(
    navController: NavHostController = rememberNavController(),
) {
    var createPassphrase by remember { mutableStateOf("") }
    var importMnemonic by remember { mutableStateOf("") }
    var importPassphrase by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = BitcoinWelcomeRoute,
    ) {
        composable<BitcoinWelcomeRoute> {
            BitcoinWelcomeScreen(
                onCreateWallet = { navController.navigate(BitcoinCreateWalletRoute) },
                onImportWallet = { navController.navigate(BitcoinImportWalletRoute) },
            )
        }
        composable<BitcoinCreateWalletRoute> {
            BitcoinCreateWalletScreen(
                words = BitcoinPlaceholderMnemonic.WORDS,
                passphrase = createPassphrase,
                onPassphraseChange = { createPassphrase = it },
                onContinue = { navController.navigate(BitcoinConfirmMnemonicRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinConfirmMnemonicRoute> {
            BitcoinConfirmMnemonicScreen(
                questions = BitcoinPlaceholderMnemonic.quizQuestions(),
                onConfirmed = { navController.navigateToBitcoinHome() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinImportWalletRoute> {
            BitcoinImportWalletScreen(
                mnemonic = importMnemonic,
                passphrase = importPassphrase,
                onMnemonicChange = { importMnemonic = it },
                onPassphraseChange = { importPassphrase = it },
                onImport = { navController.navigateToBitcoinHome() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinHomeRoute> {
            BitcoinHomeScreen()
        }
    }
}

private fun NavHostController.navigateToBitcoinHome() {
    navigate(BitcoinHomeRoute) {
        popUpTo(BitcoinWelcomeRoute) { inclusive = true }
        launchSingleTop = true
    }
}
