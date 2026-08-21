package network.bahn.androidcryptowallet.domain.usecase

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

class ObserveWalletCatalogReadyUseCase @Inject constructor(
    private val catalogReadiness: WalletCatalogReadiness,
) {
    operator fun invoke(): Flow<Boolean> = catalogReadiness.observeReady()
}
