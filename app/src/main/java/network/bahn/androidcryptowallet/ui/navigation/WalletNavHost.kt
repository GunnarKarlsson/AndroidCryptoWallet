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
import androidx.navigation.toRoute
import network.bahn.androidcryptowallet.ui.bitcoin.details.BitcoinWalletDetailsScreen
import network.bahn.androidcryptowallet.ui.bitcoin.details.BitcoinWalletDetailsViewModel
import network.bahn.androidcryptowallet.ui.bitcoin.edit.BitcoinEditWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.list.BitcoinWalletListScreen
import network.bahn.androidcryptowallet.ui.bitcoin.receive.BitcoinReceiveScreen
import network.bahn.androidcryptowallet.ui.bitcoin.send.BitcoinSendScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinConfirmMnemonicScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinCreateWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinImportWalletScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinPlaceholderMnemonic
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinRestoreEvent
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinRestoreViewModel
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSetupEvent
import network.bahn.androidcryptowallet.ui.bitcoin.setup.BitcoinSetupViewModel
import network.bahn.androidcryptowallet.ui.bitcoin.status.BitcoinNetworkStatusScreen
import network.bahn.androidcryptowallet.ui.chain.ChainSelectScreen
import network.bahn.androidcryptowallet.ui.chain.SupportedChain
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.ui.navigation.EvmCreateGraphRoute
import network.bahn.androidcryptowallet.ui.navigation.EvmRestoreGraphRoute
import network.bahn.androidcryptowallet.ui.navigation.EvmWalletListRoute
import network.bahn.androidcryptowallet.ui.evm.list.EvmWalletListScreen
import network.bahn.androidcryptowallet.ui.evm.details.EvmWalletDetailsScreen
import network.bahn.androidcryptowallet.ui.evm.details.EvmWalletDetailsViewModel
import network.bahn.androidcryptowallet.ui.evm.edit.EvmEditWalletScreen
import network.bahn.androidcryptowallet.ui.evm.receive.EvmReceiveScreen
import network.bahn.androidcryptowallet.ui.evm.send.EvmSendScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmConfirmMnemonicScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmCreateWalletScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmImportWalletScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmRestoreEvent
import network.bahn.androidcryptowallet.ui.evm.setup.EvmRestoreSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmRestoreViewModel
import network.bahn.androidcryptowallet.ui.evm.setup.EvmSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.evm.setup.EvmSetupEvent
import network.bahn.androidcryptowallet.ui.evm.setup.EvmSetupViewModel
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.ui.navigation.BitcoinWalletListRoute
import network.bahn.androidcryptowallet.ui.shell.MainShellScreen

@Composable
fun WalletNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MainShellRoute,
    ) {
        composable<MainShellRoute> {
            MainShellScreen(
                onAddWallet = { navController.navigate(ChainSelectRoute) },
                onHoldingClick = { destination ->
                    when (destination) {
                        PortfolioHoldingDestination.Bitcoin ->
                            navController.navigate(BitcoinWalletListRoute)
                        is PortfolioHoldingDestination.Evm ->
                            navController.navigate(EvmWalletListRoute(destination.family))
                    }
                },
            )
        }
        composable<ChainSelectRoute> {
            ChainSelectScreen(
                onBack = { navController.popBackToHome() },
                onChainSelected = { chain ->
                    when (chain) {
                        SupportedChain.BITCOIN -> navController.navigate(BitcoinWalletListRoute)
                        SupportedChain.ETHEREUM ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.ETHEREUM))
        SupportedChain.BSC ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.BSC))
                        SupportedChain.POLYGON ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.POLYGON))
                        SupportedChain.ARBITRUM ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.ARBITRUM))
                        SupportedChain.BASE ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.BASE))
                        SupportedChain.OPTIMISM ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.OPTIMISM))
                        SupportedChain.AVALANCHE ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.AVALANCHE))
                    }
                },
            )
        }
        composable<BitcoinWalletListRoute> {
            BitcoinWalletListScreen(
                onBack = { navController.popBackToHome() },
                onCreateWallet = { navController.navigate(BitcoinCreateGraphRoute) },
                onRestoreWallet = { navController.navigate(BitcoinRestoreGraphRoute) },
                onNetworkStatus = { navController.navigate(BitcoinNetworkStatusRoute) },
                onWalletClick = { walletId ->
                    navController.navigate(BitcoinWalletDetailsRoute(walletId))
                },
            )
        }
        composable<EvmWalletListRoute> { entry ->
            val family = entry.toRoute<EvmWalletListRoute>().family
            EvmWalletListScreen(
                onBack = { navController.popBackToHome() },
                onCreateWallet = { navController.navigate(EvmCreateGraphRoute(family)) },
                onRestoreWallet = { navController.navigate(EvmRestoreGraphRoute(family)) },
                onWalletClick = { walletId ->
                    navController.navigate(EvmWalletDetailsRoute(walletId))
                },
            )
        }
        composable<EvmWalletDetailsRoute> { entry ->
            val walletId = entry.toRoute<EvmWalletDetailsRoute>().walletId
            EvmWalletDetailsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EvmEditWalletRoute(walletId)) },
                onSend = { navController.navigate(EvmSendRoute(walletId)) },
                onReceive = { navController.navigate(EvmReceiveRoute(walletId)) },
            )
        }
        composable<EvmEditWalletRoute> {
            EvmEditWalletScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<EvmSendRoute> {
            EvmSendScreen(
                onBack = { navController.popBackStack() },
                onSent = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        EvmWalletDetailsViewModel.RELOAD_WALLET_KEY,
                        true,
                    )
                    navController.popBackStack()
                },
            )
        }
        composable<EvmReceiveRoute> {
            EvmReceiveScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinWalletDetailsRoute> { entry ->
            val walletId = entry.toRoute<BitcoinWalletDetailsRoute>().walletId
            BitcoinWalletDetailsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(BitcoinEditWalletRoute(walletId)) },
                onSend = { navController.navigate(BitcoinSendRoute(walletId)) },
                onReceive = { navController.navigate(BitcoinReceiveRoute(walletId)) },
            )
        }
        composable<BitcoinSendRoute> {
            BitcoinSendScreen(
                onBack = { navController.popBackStack() },
                onSent = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        BitcoinWalletDetailsViewModel.RELOAD_WALLET_KEY,
                        true,
                    )
                    navController.popBackStack()
                },
            )
        }
        composable<BitcoinReceiveRoute> {
            BitcoinReceiveScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<BitcoinEditWalletRoute> {
            BitcoinEditWalletScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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
                                navController.popBackToHome()
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
        navigation<BitcoinRestoreGraphRoute>(
            startDestination = BitcoinRestoreSelectNetworkRoute,
        ) {
            composable<BitcoinRestoreSelectNetworkRoute> { entry ->
                val restoreViewModel = entry.restoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                BitcoinSelectNetworkScreen(
                    selectedNetwork = uiState.restoreNetwork,
                    onNetworkSelected = restoreViewModel::onRestoreNetworkSelected,
                    onContinue = { navController.navigate(BitcoinRestoreWalletRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<BitcoinRestoreWalletRoute> { entry ->
                val restoreViewModel = entry.restoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(restoreViewModel) {
                    restoreViewModel.events.collect { event ->
                        when (event) {
                            BitcoinRestoreEvent.WalletRestored -> {
                                navController.popBackToHome()
                            }
                        }
                    }
                }
                BitcoinImportWalletScreen(
                    mnemonicWords = uiState.mnemonicWords,
                    passphrase = uiState.passphrase,
                    isSubmitting = uiState.isRestoring,
                    canRestore = uiState.canRestore,
                    errorMessage = uiState.errorMessage,
                    onMnemonicWordChange = restoreViewModel::onMnemonicWordChange,
                    onPassphraseChange = restoreViewModel::onPassphraseChange,
                    onRestore = restoreViewModel::restore,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        navigation<EvmCreateGraphRoute>(
            startDestination = EvmSelectNetworkRoute,
        ) {
            composable<EvmSelectNetworkRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                EvmSelectNetworkScreen(
                    networks = uiState.availableNetworks,
                    selectedNetwork = uiState.createNetwork,
                    onNetworkSelected = setupViewModel::onCreateNetworkSelected,
                    onContinue = {
                        setupViewModel.ensureMnemonicGenerated()
                        navController.navigate(EvmCreateWalletRoute)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EvmCreateWalletRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.ensureMnemonicGenerated()
                }
                EvmCreateWalletScreen(
                    words = uiState.mnemonicWords,
                    passphrase = uiState.passphrase,
                    onPassphraseChange = setupViewModel::onPassphraseChange,
                    onContinue = { navController.navigate(EvmConfirmMnemonicRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EvmConfirmMnemonicRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.events.collect { event ->
                        when (event) {
                            EvmSetupEvent.WalletCreated -> {
                                navController.popBackToHome()
                            }
                        }
                    }
                }
                EvmConfirmMnemonicScreen(
                    questions = BitcoinPlaceholderMnemonic.quizQuestions(uiState.mnemonicWords),
                    isSubmitting = uiState.isCreating,
                    errorMessage = uiState.errorMessage,
                    onConfirmed = setupViewModel::confirm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        navigation<EvmRestoreGraphRoute>(
            startDestination = EvmRestoreSelectNetworkRoute,
        ) {
            composable<EvmRestoreSelectNetworkRoute> { entry ->
                val restoreViewModel = entry.evmRestoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                EvmRestoreSelectNetworkScreen(
                    networks = uiState.availableNetworks,
                    selectedNetwork = uiState.restoreNetwork,
                    onNetworkSelected = restoreViewModel::onRestoreNetworkSelected,
                    onContinue = { navController.navigate(EvmRestoreWalletRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EvmRestoreWalletRoute> { entry ->
                val restoreViewModel = entry.evmRestoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(restoreViewModel) {
                    restoreViewModel.events.collect { event ->
                        when (event) {
                            EvmRestoreEvent.WalletRestored -> {
                                navController.popBackToHome()
                            }
                        }
                    }
                }
                EvmImportWalletScreen(
                    mnemonicWords = uiState.mnemonicWords,
                    passphrase = uiState.passphrase,
                    isSubmitting = uiState.isRestoring,
                    canRestore = uiState.canRestore,
                    errorMessage = uiState.errorMessage,
                    onMnemonicWordChange = restoreViewModel::onMnemonicWordChange,
                    onPassphraseChange = restoreViewModel::onPassphraseChange,
                    onRestore = restoreViewModel::restore,
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

@Composable
private fun NavBackStackEntry.restoreGraphViewModel(
    navController: NavHostController,
): BitcoinRestoreViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<BitcoinRestoreGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}

@Composable
private fun NavBackStackEntry.evmCreateGraphViewModel(
    navController: NavHostController,
): EvmSetupViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<EvmCreateGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}

@Composable
private fun NavBackStackEntry.evmRestoreGraphViewModel(
    navController: NavHostController,
): EvmRestoreViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<EvmRestoreGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}
