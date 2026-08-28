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
import network.bahn.androidcryptowallet.ui.ethereum.list.EthereumWalletListScreen
import network.bahn.androidcryptowallet.ui.ethereum.details.EthereumWalletDetailsScreen
import network.bahn.androidcryptowallet.ui.ethereum.details.EthereumWalletDetailsViewModel
import network.bahn.androidcryptowallet.ui.ethereum.edit.EthereumEditWalletScreen
import network.bahn.androidcryptowallet.ui.ethereum.receive.EthereumReceiveScreen
import network.bahn.androidcryptowallet.ui.ethereum.send.EthereumSendScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumConfirmMnemonicScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumCreateWalletScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumImportWalletScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumRestoreEvent
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumRestoreSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumRestoreViewModel
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumSelectNetworkScreen
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumSetupEvent
import network.bahn.androidcryptowallet.ui.ethereum.setup.EthereumSetupViewModel

@Composable
fun WalletNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ChainSelectRoute,
    ) {
        composable<ChainSelectRoute> {
            ChainSelectScreen(
                onChainSelected = { chain ->
                    when (chain) {
                        SupportedChain.BITCOIN -> navController.navigate(BitcoinWalletListRoute)
                        SupportedChain.ETHEREUM ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.ETHEREUM))
        SupportedChain.BSC ->
                            navController.navigate(EvmWalletListRoute(EvmFamily.BSC))
                    }
                },
            )
        }
        composable<BitcoinWalletListRoute> {
            BitcoinWalletListScreen(
                onBack = { navController.popBackStack() },
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
            EthereumWalletListScreen(
                onBack = { navController.popBackStack() },
                onCreateWallet = { navController.navigate(EvmCreateGraphRoute(family)) },
                onRestoreWallet = { navController.navigate(EvmRestoreGraphRoute(family)) },
                onWalletClick = { walletId ->
                    navController.navigate(EthereumWalletDetailsRoute(walletId))
                },
            )
        }
        composable<EthereumWalletDetailsRoute> { entry ->
            val walletId = entry.toRoute<EthereumWalletDetailsRoute>().walletId
            EthereumWalletDetailsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EthereumEditWalletRoute(walletId)) },
                onSend = { navController.navigate(EthereumSendRoute(walletId)) },
                onReceive = { navController.navigate(EthereumReceiveRoute(walletId)) },
            )
        }
        composable<EthereumEditWalletRoute> {
            EthereumEditWalletScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<EthereumSendRoute> {
            EthereumSendScreen(
                onBack = { navController.popBackStack() },
                onSent = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        EthereumWalletDetailsViewModel.RELOAD_WALLET_KEY,
                        true,
                    )
                    navController.popBackStack()
                },
            )
        }
        composable<EthereumReceiveRoute> {
            EthereumReceiveScreen(
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
                                navController.popBackStack(
                                    route = BitcoinRestoreGraphRoute,
                                    inclusive = true,
                                )
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
            startDestination = EthereumSelectNetworkRoute,
        ) {
            composable<EthereumSelectNetworkRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                EthereumSelectNetworkScreen(
                    networks = uiState.availableNetworks,
                    selectedNetwork = uiState.createNetwork,
                    onNetworkSelected = setupViewModel::onCreateNetworkSelected,
                    onContinue = {
                        setupViewModel.ensureMnemonicGenerated()
                        navController.navigate(EthereumCreateWalletRoute)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EthereumCreateWalletRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.ensureMnemonicGenerated()
                }
                EthereumCreateWalletScreen(
                    words = uiState.mnemonicWords,
                    passphrase = uiState.passphrase,
                    onPassphraseChange = setupViewModel::onPassphraseChange,
                    onContinue = { navController.navigate(EthereumConfirmMnemonicRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EthereumConfirmMnemonicRoute> { entry ->
                val setupViewModel = entry.evmCreateGraphViewModel(navController)
                val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(setupViewModel) {
                    setupViewModel.events.collect { event ->
                        when (event) {
                            EthereumSetupEvent.WalletCreated -> {
                                navController.popBackStack(
                                    route = EvmCreateGraphRoute(uiState.family),
                                    inclusive = true,
                                )
                            }
                        }
                    }
                }
                EthereumConfirmMnemonicScreen(
                    questions = BitcoinPlaceholderMnemonic.quizQuestions(uiState.mnemonicWords),
                    isSubmitting = uiState.isCreating,
                    errorMessage = uiState.errorMessage,
                    onConfirmed = setupViewModel::confirm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        navigation<EvmRestoreGraphRoute>(
            startDestination = EthereumRestoreSelectNetworkRoute,
        ) {
            composable<EthereumRestoreSelectNetworkRoute> { entry ->
                val restoreViewModel = entry.evmRestoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                EthereumRestoreSelectNetworkScreen(
                    networks = uiState.availableNetworks,
                    selectedNetwork = uiState.restoreNetwork,
                    onNetworkSelected = restoreViewModel::onRestoreNetworkSelected,
                    onContinue = { navController.navigate(EthereumRestoreWalletRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EthereumRestoreWalletRoute> { entry ->
                val restoreViewModel = entry.evmRestoreGraphViewModel(navController)
                val uiState by restoreViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(restoreViewModel) {
                    restoreViewModel.events.collect { event ->
                        when (event) {
                            EthereumRestoreEvent.WalletRestored -> {
                                navController.popBackStack(
                                    route = EvmRestoreGraphRoute(uiState.family),
                                    inclusive = true,
                                )
                            }
                        }
                    }
                }
                EthereumImportWalletScreen(
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
): EthereumSetupViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<EvmCreateGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}

@Composable
private fun NavBackStackEntry.evmRestoreGraphViewModel(
    navController: NavHostController,
): EthereumRestoreViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry<EvmRestoreGraphRoute>()
    }
    return hiltViewModel(parentEntry)
}
