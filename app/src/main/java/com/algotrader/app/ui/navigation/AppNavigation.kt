package com.algotrader.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.algotrader.app.ui.screens.backtest.BacktestScreen
import com.algotrader.app.ui.screens.dashboard.DashboardScreen
import com.algotrader.app.ui.screens.market.MarketScreen
import com.algotrader.app.ui.screens.portfolio.PortfolioScreen
import com.algotrader.app.ui.screens.settings.SettingsScreen
import com.algotrader.app.ui.screens.strategy.StrategyEditorScreen
import com.algotrader.app.ui.screens.strategy.StrategyListScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Strategies : Screen("strategies", "Strategies", Icons.Filled.Code, Icons.Outlined.Code)
    data object Backtest : Screen("backtest", "Backtest", Icons.Filled.Science, Icons.Outlined.Science)
    data object Market : Screen("market", "Market", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp)
    data object Portfolio : Screen("portfolio", "Portfolio", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object StrategyEditor : Screen("strategy_editor/{strategyId}", "Edit Strategy", Icons.Filled.Code, Icons.Outlined.Code)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Strategies,
    Screen.Backtest,
    Screen.Market,
    Screen.Portfolio
)

@Composable
fun AlgoTraderNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    } || currentDestination?.route == Screen.Settings.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToStrategy = { id ->
                        navController.navigate("strategy_editor/$id")
                    },
                    onNavigateToPortfolio = {
                        navController.navigate(Screen.Portfolio.route)
                    }
                )
            }

            composable(Screen.Strategies.route) {
                StrategyListScreen(
                    onCreateNew = {
                        navController.navigate("strategy_editor/0")
                    },
                    onEditStrategy = { id ->
                        navController.navigate("strategy_editor/$id")
                    },
                    onBacktest = { id ->
                        navController.navigate(Screen.Backtest.route)
                    }
                )
            }

            composable(
                route = "strategy_editor/{strategyId}",
                arguments = listOf(navArgument("strategyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val strategyId = backStackEntry.arguments?.getLong("strategyId") ?: 0
                StrategyEditorScreen(
                    strategyId = strategyId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Backtest.route) {
                BacktestScreen()
            }

            composable(Screen.Market.route) {
                MarketScreen()
            }

            composable(Screen.Portfolio.route) {
                PortfolioScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
