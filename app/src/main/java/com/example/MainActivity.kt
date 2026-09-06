package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.AppTopHeader
import com.example.ui.components.BottomNavControl
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CanvasBg
                ) {
                    FloodSafeApp()
                }
            }
        }
    }
}

@Composable
fun FloodSafeApp(
    viewModel: FloodSafeViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentAppMode by viewModel.currentAppMode.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val scenarioState by viewModel.scenarioState.collectAsState()
    val nowcastHorizons by viewModel.nowcastHorizons.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(fineGranted || coarseGranted)
    }

    LaunchedEffect(Unit) {
        if (!viewModel.locationTracker.checkPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.onLocationPermissionResult(true)
        }
    }

    val requestLocationPermissionAction: () -> Unit = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val headerHorizons = if (nowcastHorizons.isNotEmpty()) {
        nowcastHorizons.take(4).map { it.horizonLabel to it.riskPercentage }
    } else {
        listOf(
            "+15m" to (scenarioState.overallFloodRiskPercent * 0.55).toInt().coerceIn(5, 95),
            "+30m" to (scenarioState.overallFloodRiskPercent * 0.78).toInt().coerceIn(10, 95),
            "+60m" to scenarioState.overallFloodRiskPercent.coerceIn(15, 98),
            "+120m" to (scenarioState.overallFloodRiskPercent * 1.18).toInt().coerceIn(20, 99)
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = WhiteSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeaderDark)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandBlueLight),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "B",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text(
                                text = "FLOODSAFE BENGALURU",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "Urban Nowcasting & Navigation",
                                fontSize = 11.sp,
                                color = BrandBlueLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pilot: Bellandur–Agara Catchment",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                HorizontalDivider(color = BorderSlate)

                LazyColumn(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val screens = listOf(
                        Triple(AppScreen.HOME, Icons.Default.Home, "1. Home Overview"),
                        Triple(AppScreen.FLOOD_MAP, Icons.Default.Map, "2. Flood Map (Leaflet)"),
                        Triple(AppScreen.SAFE_NAV, Icons.Default.Navigation, "3. Safe Navigation"),
                        Triple(AppScreen.LIVE_NAV, Icons.Default.DirectionsCar, "4. Live Navigation"),
                        Triple(AppScreen.ALERTS, Icons.Default.Warning, "5. Public & Agency Alerts"),
                        Triple(AppScreen.NOWCAST, Icons.Default.Timer, "6. Nowcast (+15m to 6h)"),
                        Triple(AppScreen.DRAINAGE, Icons.Default.Waves, "7. Drainage + Rainfall Coupling"),
                        Triple(AppScreen.AUTHORITY, Icons.Default.AdminPanelSettings, "8. Authority Dashboard"),
                        Triple(AppScreen.HISTORY, Icons.Default.History, "9. Flood History & Archive"),
                        Triple(AppScreen.VALIDATION, Icons.Default.Analytics, "10. Model Validation"),
                        Triple(AppScreen.DATA_STATUS, Icons.Default.Storage, "11. Data Source Status"),
                        Triple(AppScreen.SETTINGS, Icons.Default.Settings, "12. Settings & Calibration")
                    )

                    items(screens.size) { i ->
                        val (screen, icon, title) = screens[i]
                        val isSelected = currentScreen == screen
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) BlueDark else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BlueDark else TextPrimary
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateTo(screen)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BlueContainer,
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopHeader(
                    appMode = currentAppMode,
                    onCycleMode = {
                        val nextMode = when (currentAppMode) {
                            "DEMO" -> "LIVE"
                            "LIVE" -> "HISTORICAL"
                            else -> "DEMO"
                        }
                        viewModel.setAppMode(nextMode)
                    },
                    onOpenDrawer = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onTriggerScenario = { viewModel.advanceScenario() },
                    onResetScenario = { viewModel.resetScenario() },
                    horizons = headerHorizons
                )
            },
            bottomBar = {
                BottomNavControl(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            },
            containerColor = CanvasBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onRequestLocationPermission = requestLocationPermissionAction
                    )
                    AppScreen.FLOOD_MAP -> FloodMapScreen(viewModel = viewModel)
                    AppScreen.SAFE_NAV -> SafeNavigationScreen(viewModel = viewModel)
                    AppScreen.LIVE_NAV -> LiveNavigationScreen(
                        viewModel = viewModel,
                        onRequestLocationPermission = requestLocationPermissionAction
                    )
                    AppScreen.ALERTS -> AlertsScreen(viewModel = viewModel)
                    AppScreen.NOWCAST -> NowcastScreen(viewModel = viewModel)
                    AppScreen.DRAINAGE -> DrainageCouplingScreen(viewModel = viewModel)
                    AppScreen.AUTHORITY -> AuthorityDashboardScreen(viewModel = viewModel)
                    AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppScreen.VALIDATION -> ValidationScreen(viewModel = viewModel)
                    AppScreen.DATA_STATUS -> DataStatusScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
