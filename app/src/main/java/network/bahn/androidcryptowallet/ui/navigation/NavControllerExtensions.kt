package network.bahn.androidcryptowallet.ui.navigation

import androidx.navigation.NavHostController

/** Pops the back stack until [MainShellRoute] is the current destination. */
fun NavHostController.popBackToHome(): Boolean =
    popBackStack(MainShellRoute, inclusive = false)
