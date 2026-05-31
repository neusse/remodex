package com.remodex.mobile.ui.navigation

import com.remodex.mobile.AppContainer
import com.remodex.mobile.core.config.FeatureFlags
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.remodex.mobile.data.CodexRepository
import com.remodex.mobile.ui.about.AboutScreen
import com.remodex.mobile.ui.about.WhatsNewScreen
import com.remodex.mobile.ui.draft.NewChatDraftRoute
import com.remodex.mobile.ui.draft.NewChatDraftScreen
import com.remodex.mobile.ui.draft.NewChatDraftSource
import com.remodex.mobile.ui.home.RootReconnectUiState
import com.remodex.mobile.ui.home.HomeMainContent
import com.remodex.mobile.ui.archived.ArchivedChatsScreen
import com.remodex.mobile.ui.beta.TesterHqScreen
import com.remodex.mobile.ui.mydevices.MyDevicesScreen
import com.remodex.mobile.ui.settings.SettingsScreen
import com.remodex.mobile.terminal.TerminalRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: CodexRepository,
    reconnectUiState: RootReconnectUiState = RootReconnectUiState(),
    onReconnectSavedPairing: () -> Unit = {},
    onWakeSavedComputer: () -> Unit = {},
    onOpenPairingScanner: () -> Unit = {},
    onOpenPairingCode: () -> Unit = {},
    onGitContextChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home,
        modifier = modifier,
    ) {
        composable(AppRoutes.Home) {
            HomeMainContent(
                repository = repository,
                reconnectUiState = reconnectUiState,
                onReconnectSavedPairing = onReconnectSavedPairing,
                onWakeSavedComputer = onWakeSavedComputer,
                onOpenPairingScanner = onOpenPairingScanner,
                onGitContextChanged = onGitContextChanged,
            )
        }
        composable(
            route = AppRoutes.NewChatDraft,
            arguments =
                listOf(
                    navArgument(AppRoutes.NewChatDraftRouteIdArg) { type = NavType.StringType },
                    navArgument(AppRoutes.NewChatDraftSourceArg) { type = NavType.StringType },
                    navArgument(AppRoutes.NewChatDraftProjectPathArg) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { entry ->
            val routeId = entry.arguments?.getString(AppRoutes.NewChatDraftRouteIdArg).orEmpty()
            val sourceName = entry.arguments?.getString(AppRoutes.NewChatDraftSourceArg).orEmpty()
            val source =
                NewChatDraftSource.entries.firstOrNull { it.name == sourceName }
                    ?: NewChatDraftSource.generalChat
            val preferredProjectPath = entry.arguments?.getString(AppRoutes.NewChatDraftProjectPathArg)
            NewChatDraftScreen(
                route =
                    NewChatDraftRoute(
                        id = routeId,
                        source = source,
                        preferredProjectPath = preferredProjectPath,
                    ),
                repository = repository,
                subscriptionService = AppContainer.subscriptionService,
                onNavigateBack = { navController.popBackStack() },
                onThreadStarted = {
                    navController.popBackStack(AppRoutes.Home, inclusive = false)
                },
                onOpenTerminal = { cdPath ->
                    navController.navigate(AppRoutes.terminalRoute(cdPath))
                },
            )
        }
        composable(AppRoutes.Settings) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(AppRoutes.About) },
                onNavigateToWhatsNew = { navController.navigate(AppRoutes.WhatsNew) },
                onNavigateToTesterHq = {
                    if (FeatureFlags.betaEngagementEnabled) {
                        navController.navigate(AppRoutes.TesterHq)
                    }
                },
            )
        }
        composable(AppRoutes.MyDevices) {
            MyDevicesScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onScanQrCode = onOpenPairingScanner,
                onPairWithCode = onOpenPairingCode,
            )
        }
        composable(AppRoutes.Archived) {
            ArchivedChatsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.About) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(AppRoutes.WhatsNew) {
            WhatsNewScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(AppRoutes.TesterHq) {
            TesterHqScreen(
                repository = AppContainer.betaEngagementRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "${AppRoutes.Terminal}?${AppRoutes.TerminalCdHintQuery}={${AppRoutes.TerminalCdHintQuery}}",
            arguments =
                listOf(
                    navArgument(AppRoutes.TerminalCdHintQuery) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { entry ->
            TerminalRoute(
                cdHint = entry.arguments?.getString(AppRoutes.TerminalCdHintQuery),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
