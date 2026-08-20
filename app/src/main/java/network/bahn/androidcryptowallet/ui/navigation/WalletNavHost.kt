package network.bahn.androidcryptowallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import network.bahn.androidcryptowallet.ui.bitcoin.details.BitcoinWalletDetailsScreen
import network.bahn.androidcryptowallet.ui.bitcoin.list.BitcoinWalletListScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinConfirmMnemonicScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinCreateWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinPlaceholderMnemonic
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSetupEvent
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSetupViewModel
import network.bahn.androidcryptowallet.ui.bitcoin.status.BitcoinNetworkStatusScreen

@Composable
fun WalletNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = BitcoinWalletListRoute,
    ) {
        composable<BitcoinWalletListRoute> {
            BitcoinWalletListScreen(
                onCreateWallet = { navController.navigate(BitcoinCreateGraphRoute) },
                onNetworkStatus = { navController.navigate(BitcoinNetworkStatusRoute) },
                onWalletClick = { walletId ->
                    navController.navigate(BitcoinWalletDetailsRoute(walletId))
                },
            )
        }
        composable<BitcoinWalletDetailsRoute> {
            BitcoinWalletDetailsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinNetworkStatusRoute> {
            BitcoinNetworkStatusScreen(
                onBack = { navController.popBackStack() },
            )
        }
        navigation<BitcoinCreateGraphRoute>(
            startDestination = BitcoinSelectNetworkRoute,
        ) {
            composable<BitcoinSelectNetworkRoute> { entry ->
                val setupViewModel = entry.createGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                BitcoinSelectNetworkScreen(
                    selectedNetwork = uiState.createNetwork,
                    onNetworkSelected = setupViewModel::onCreateNetworkSelected,
                    onContinue = {
                        setupViewModel.ensureMnemonicGenerated()
                        navController.navigate(BitcoinCreateWalletRoute)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<BitcoinCreateWalletRoute> { entry ->
                val setupViewModel = entry.createGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.ensureMnemonicGenerated()
                }
                BitcoinCreateWalletScreen(
                    words = uiState.mnemonicWords,
                    passphrase = uiState.passphrase,
                    onPassphraseChange = setupViewModel::onPassphraseChange,
                    onContinue = { navController.navigate(BitcoinConfirmMnemonicRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<BitcoinConfirmMnemonicRoute> { entry ->
                val setupViewModel = entry.createGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.events.collect { event ->
                        when (event) {
                            BitcoinSetupEvent.WalletCreated -> {
                                navController.popBackStack(
                                    route = BitcoinCreateGraphRoute,
                                    inclusive = true,
                                )
                            }
                        }
                    }
                }
                BitcoinConfirmMnemonicScreen(
                    questions = BitcoinPlaceholderMnemonic.quizQuestions(uiState.mnemonicWords),
                    isSubmitting = uiState.isCreating,
                    errorMessage = uiState.errorMessage,
                    onConfirmed = setupViewModel::confirm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun NavBackStackEntry.createGraphViewModel(
    navController: NavHostController,
): BitcoinSetupViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<BitcoinCreateGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}
