package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.model.VoiceBusState
import com.example.ui.screens.MechanicScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.StageScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.WiringScreen
import com.example.ui.theme.CircuitAmber
import com.example.ui.theme.CircuitCyan
import com.example.ui.theme.CircuitEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.serialization.Serializable

@Serializable
object StageRoute

@Serializable
object VaultRoute

@Serializable
object WiringRoute

@Serializable
object MechanicRoute

@Serializable
object WizardRoute

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullstackAgentApp(
    state: AgentUiState,
    viewModel: AgentViewModel
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.lastStatusBanner) {
        state.lastStatusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val destinations = listOf(
        TopLevelDestination(
            route = StageRoute,
            label = stringResource(id = R.string.nav_stage),
            selectedIcon = Icons.Filled.GraphicEq,
            unselectedIcon = Icons.Outlined.GraphicEq,
            testTag = "nav_stage"
        ),
        TopLevelDestination(
            route = VaultRoute,
            label = stringResource(id = R.string.nav_vault),
            selectedIcon = Icons.Filled.Memory,
            unselectedIcon = Icons.Outlined.Memory,
            testTag = "nav_vault"
        ),
        TopLevelDestination(
            route = WiringRoute,
            label = stringResource(id = R.string.nav_wiring),
            selectedIcon = Icons.Filled.Cable,
            unselectedIcon = Icons.Outlined.Cable,
            testTag = "nav_wiring"
        ),
        TopLevelDestination(
            route = MechanicRoute,
            label = stringResource(id = R.string.nav_mechanic),
            selectedIcon = Icons.Filled.Build,
            unselectedIcon = Icons.Outlined.Build,
            testTag = "nav_mechanic"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val onWizardScreen = currentDestination?.hasRoute<WizardRoute>() == true || !state.config.setupCompleted

    val startDestination: Any = if (state.config.setupCompleted) StageRoute else WizardRoute

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !onWizardScreen

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!onWizardScreen) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotColor = when {
                                    !state.config.wiringHealthy -> MaterialTheme.colorScheme.error
                                    state.busState == VoiceBusState.SPEAKING -> CircuitEmerald
                                    state.busState == VoiceBusState.THINKING -> CircuitAmber
                                    else -> CircuitCyan
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = state.config.agentName,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${state.config.enabledPieces.size} modules · ${state.config.agentHomeDir}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (state.config.darkTheme) {
                                        Icons.Default.LightMode
                                    } else {
                                        Icons.Default.DarkMode
                                    },
                                    contentDescription = stringResource(id = R.string.cd_toggle_theme)
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail && !onWizardScreen) {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<WizardRoute> {
                        SetupWizardScreen(
                            initialConfig = state.config,
                            canCancel = state.config.setupCompleted,
                            onComplete = { newConfig ->
                                viewModel.completeWizard(newConfig)
                                navController.navigate(StageRoute) {
                                    popUpTo(WizardRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onCancel = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<StageRoute> {
                        StageScreen(
                            state = state,
                            onLaunchModeSelected = viewModel::setLaunchMode,
                            onFaceSelected = viewModel::selectFace,
                            onPlayGreeting = viewModel::playLaunchGreeting,
                            onStartPtt = viewModel::startPushToTalk,
                            onStopPtt = viewModel::stopPushToTalk,
                            onToggleMicMode = viewModel::toggleMicMode,
                            onSubmitPrompt = viewModel::submitUserPrompt,
                            onPresentCard = viewModel::presentCardToBoard,
                            onMoveCard = viewModel::moveBoardCard,
                            onPersistCards = viewModel::persistBoardCards,
                            onRemoveCard = viewModel::removeBoardCard,
                            onOpenWizard = { navController.navigate(WizardRoute) }
                        )
                    }

                    composable<VaultRoute> {
                        VaultScreen(
                            state = state,
                            claudeMdPreview = viewModel.getGeneratedClaudeMd(),
                            onUpsertNote = viewModel::upsertVaultNote,
                            onDeleteNote = viewModel::deleteVaultNote,
                            onPresentNote = viewModel::presentCardToBoard,
                            onUpdateIdentity = viewModel::updateIdentityAndMechanic
                        )
                    }

                    composable<WiringRoute> {
                        WiringScreen(
                            state = state,
                            backtalkJson = viewModel.getGeneratedBacktalkJson(),
                            visualizerJson = viewModel.getGeneratedVisualizerJson(),
                            barehandsJson = viewModel.getGeneratedBarehandsJson(),
                            onToggleMicMode = viewModel::toggleMicMode,
                            onTogglePermissionMode = viewModel::togglePermissionMode,
                            onToggleVoiceEngine = viewModel::toggleVoiceEngine,
                            onSimulateBreak = viewModel::simulateBrokenWiring,
                            onRepairWiring = viewModel::repairAllWiring
                        )
                    }

                    composable<MechanicRoute> {
                        MechanicScreen(
                            state = state,
                            onRunUpdates = viewModel::runStackUpdates,
                            onRepairAll = viewModel::repairAllWiring,
                            onOpenWizard = { navController.navigate(WizardRoute) },
                            onToggleMarketingSkills = viewModel::toggleMarketingSkills
                        )
                    }
                }
            }
        }
    }
}
