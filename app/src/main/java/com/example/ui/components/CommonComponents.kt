package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.FactorBreakdown
import com.example.data.model.RiskLevel
import com.example.ui.AppScreen
import com.example.ui.theme.*
import org.json.JSONObject

@Composable
fun RiskBadge(
    level: RiskLevel,
    riskPercentage: Int? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderCol) = when (level) {
        RiskLevel.SAFE -> Triple(SafeGreenBg, SafeGreenDark, SafeGreenBorder)
        RiskLevel.WATCH -> Triple(WatchAmberBg, WatchAmberDark, WatchAmberBorder)
        RiskLevel.HIGH -> Triple(HighOrangeBg, HighOrange, HighOrangeBorder)
        RiskLevel.SEVERE -> Triple(SevereRedBg, SevereRed, SevereRedBorder)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (riskPercentage != null) "${level.label} • $riskPercentage%" else level.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = BrandBlueLight,
    modifier: Modifier = Modifier,
    badgeLevel: RiskLevel? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                if (badgeLevel != null) {
                    RiskBadge(level = badgeLevel)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun FactorBarItem(
    factor: FactorBreakdown,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        factor.score <= 30 -> SafeGreen
        factor.score <= 60 -> WatchAmber
        factor.score <= 80 -> HighOrange
        else -> SevereRed
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = factor.factorName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "${factor.score}% (wt: ${(factor.weight * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { factor.score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = SurfaceVariant
        )
        Text(
            text = factor.contributingDesc,
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
fun AppTopHeader(
    appMode: String = "DEMO",
    onCycleMode: () -> Unit,
    onOpenDrawer: () -> Unit,
    onTriggerScenario: () -> Unit,
    onResetScenario: () -> Unit,
    horizons: List<Pair<String, Int>> = listOf("+15m" to 42, "+30m" to 58, "+60m" to 74, "+120m" to 88),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
        color = HeaderDark,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
        ) {
            // Top Bar: Navigation icon, Brand Title, Action controls & Mode Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Brand Icon "B"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "FLOODSAFE BENGALURU",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                }

                // Controls & Demo Mode Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Scenario step button
                    IconButton(
                        onClick = onTriggerScenario,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("step_scenario_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Step Scenario",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Reset button
                    IconButton(
                        onClick = onResetScenario,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("reset_scenario_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Scenario",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Mode Badge Pill (3 distinct modes)
                    val modeColor = when (appMode) {
                        "LIVE" -> Color(0xFF60A5FA)
                        "HISTORICAL" -> Color(0xFFFBBF24)
                        else -> Color(0xFF34D399) // DEMO
                    }
                    val modeBg = when (appMode) {
                        "LIVE" -> BrandBlue.copy(alpha = 0.25f)
                        "HISTORICAL" -> WatchAmberBg
                        else -> Color(0xFF10B981).copy(alpha = 0.18f)
                    }
                    val modeLabel = when (appMode) {
                        "LIVE" -> "● LIVE MODE"
                        "HISTORICAL" -> "● HISTORICAL"
                        else -> "● DEMO MODE"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(modeBg)
                            .border(
                                width = 1.dp,
                                color = modeColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onCycleMode() }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("mode_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = modeColor,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Urban Flood Nowcasting • Bellandur–Agara Pilot",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Column Balanced Geometric Horizon Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                horizons.forEach { (timeHorizon, riskPercent) ->
                    val horizonColor = when {
                        riskPercent <= 30 -> Color(0xFF34D399)
                        riskPercent <= 60 -> Color(0xFFFDE047)
                        riskPercent <= 80 -> Color(0xFFFB923C)
                        else -> Color(0xFFF87171)
                    }

                    val isHighlighted = timeHorizon == "+60m"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isHighlighted) Color.White.copy(alpha = 0.12f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isHighlighted) BrandBlueLight.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = timeHorizon,
                                fontSize = 10.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                color = if (isHighlighted) BrandBlueLight else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$riskPercent%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = horizonColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavControl(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(elevation = 16.dp)
            .testTag("bottom_navigation_bar"),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val items = listOf(
                Triple(AppScreen.HOME, Icons.Default.Home, "Home"),
                Triple(AppScreen.FLOOD_MAP, Icons.Default.Map, "Map"),
                Triple(AppScreen.SAFE_NAV, Icons.Default.Navigation, "Nav"),
                Triple(AppScreen.ALERTS, Icons.Default.Warning, "Alerts"),
                Triple(AppScreen.AUTHORITY, Icons.Default.AdminPanelSettings, "Admin")
            )

            items.forEach { (screen, icon, title) ->
                val isSelected = currentScreen == screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(screen) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_tab_${screen.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BlueContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) BlueDark else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) BlueDark else TextSecondary,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

class AndroidMapBridge(
    private val onLocationSelected: (String) -> Unit,
    private val onRoadSelected: (String) -> Unit
) {
    @JavascriptInterface
    fun onLocationSelected(jsonStr: String) {
        onLocationSelected.invoke(jsonStr)
    }

    @JavascriptInterface
    fun onRoadSelected(jsonStr: String) {
        onRoadSelected.invoke(jsonStr)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    onLocationClick: (String) -> Unit,
    onRoadClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    routesJson: String? = null,
    liveLat: Double? = null,
    liveLng: Double? = null,
    onRecenterCallback: ((() -> Unit) -> Unit)? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(webViewRef) {
        webViewRef?.let { wv ->
            onRecenterCallback?.invoke {
                wv.evaluateJavascript("if (window.recenterToUser) { window.recenterToUser(); }", null)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 FloodSafeBengaluru/1.0"
                }
                webChromeClient = android.webkit.WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                        if (liveLat != null && liveLng != null) {
                            view?.evaluateJavascript("if (window.updateGpsPosition) { window.updateGpsPosition($liveLat, $liveLng, 0); }", null)
                        }
                        if (routesJson != null) {
                            val escaped = JSONObject.quote(routesJson)
                            view?.evaluateJavascript("if (window.displayRoutes) { window.displayRoutes($escaped); }", null)
                        }
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        return true
                    }
                }
                addJavascriptInterface(
                    AndroidMapBridge(
                        onLocationSelected = { json -> onLocationClick(json) },
                        onRoadSelected = { json -> onRoadClick(json) }
                    ),
                    "Android"
                )
                loadUrl("file:///android_asset/leaflet_map.html")
                webViewRef = this
            }
        },
        update = { webView ->
            if (isPageLoaded) {
                if (routesJson != null) {
                    val escaped = JSONObject.quote(routesJson)
                    webView.evaluateJavascript("if (window.displayRoutes) { window.displayRoutes($escaped); }", null)
                }
                if (liveLat != null && liveLng != null) {
                    webView.evaluateJavascript("if (window.updateGpsPosition) { window.updateGpsPosition($liveLat, $liveLng, 0); }", null)
                }
            }
        },
        modifier = modifier.testTag("leaflet_webview_map")
    )
}
