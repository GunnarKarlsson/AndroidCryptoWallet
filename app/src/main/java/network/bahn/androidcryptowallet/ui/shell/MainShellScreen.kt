package network.bahn.androidcryptowallet.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.home.HomePlaceholderScreen
import network.bahn.androidcryptowallet.ui.navigation.HomeRoute
import network.bahn.androidcryptowallet.ui.navigation.SettingsRoute
import network.bahn.androidcryptowallet.ui.navigation.TransactionsRoute
import network.bahn.androidcryptowallet.ui.settings.SettingsPlaceholderScreen
import network.bahn.androidcryptowallet.ui.transactions.TransactionsPlaceholderScreen

@Composable
fun MainShellScreen(
    onAddWallet: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(navController = navController)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomePlaceholderScreen(onAddWallet = onAddWallet)
            }
            composable<TransactionsRoute> {
                TransactionsPlaceholderScreen()
            }
            composable<SettingsRoute> {
                SettingsPlaceholderScreen()
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            selected = currentDestination?.hasRoute(HomeRoute::class) == true,
            onClick = { navController.navigateToTopLevel(HomeRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = stringResource(R.string.tab_home),
                )
            },
            label = null,
            alwaysShowLabel = false,
        )
        NavigationBarItem(
            selected = currentDestination?.hasRoute(TransactionsRoute::class) == true,
            onClick = { navController.navigateToTopLevel(TransactionsRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.tab_transactions),
                )
            },
            label = null,
            alwaysShowLabel = false,
        )
        NavigationBarItem(
            selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
            onClick = { navController.navigateToTopLevel(SettingsRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.tab_settings),
                )
            },
            label = null,
            alwaysShowLabel = false,
        )
    }
}

private fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
