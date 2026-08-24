package network.bahn.androidcryptowallet.ui.ethereum.list

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class EthereumWalletListViewModelTest {
    @Test
    fun defaultsToSepolia() {
        val viewModel = EthereumWalletListViewModel()
        assertEquals(EthereumNetwork.SEPOLIA, viewModel.uiState.value.selectedNetwork)
    }

    @Test
    fun selectingMainnetUpdatesState() {
        val viewModel = EthereumWalletListViewModel()
        viewModel.onNetworkSelected(EthereumNetwork.MAINNET)
        assertEquals(EthereumNetwork.MAINNET, viewModel.uiState.value.selectedNetwork)
    }
}
