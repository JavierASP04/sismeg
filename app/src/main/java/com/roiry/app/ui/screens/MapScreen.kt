package com.roiry.app.ui.screens

import android.graphics.Point
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapAdapter
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.roundToInt

private enum class AlertFilter(
    val label: String,
    val icon: ImageVector,
    val accent: Color
) {
    Earthquakes("Sismos", Icons.Filled.Warning, Color(0xFFE11D48)),
    Floods("Inundaciones", Icons.Filled.Tsunami, Color(0xFF0EA5E9)),
    Fires("Incendios", Icons.Filled.LocalFireDepartment, Color(0xFFF97316))
}

private enum class ReportType {
    ActiveEmergency,
    Prevention
}

private data class MapReport(
    val id: String,
    val title: String,
    val description: String,
    val user: String,
    val photoUrl: String,
    val position: GeoPoint,
    val type: ReportType,
    val tags: Set<AlertFilter>
)

private data class ScreenMarker(
    val report: MapReport,
    val x: Int,
    val y: Int
)

private val trujilloCenter = GeoPoint(9.365, -70.435)

private val sampleReports = listOf(
    MapReport(
        "1", "Emergencia en Valera", "Riesgo estructural por sismo.",
        "Protección Civil", "https://images.unsplash.com/photo-1521295121783-8a321d551ad2?w=800",
        GeoPoint(9.3181, -70.6030), ReportType.ActiveEmergency, setOf(AlertFilter.Earthquakes)
    ),
    MapReport(
        "2", "Monitoreo Boconó", "Vigilancia de quebradas por lluvias.",
        "Brigada Verde", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800",
        GeoPoint(9.2539, -70.2511), ReportType.Prevention, setOf(AlertFilter.Floods)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val selectedFilters = remember { mutableStateListOf(*AlertFilter.entries.toTypedArray()) }
    var selectedReport by remember { mutableStateOf<MapReport?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var viewportTick by remember { mutableIntStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val filterSheetState = rememberModalBottomSheetState()

    val filteredReports = remember(selectedFilters.size) {
        sampleReports.filter { r -> r.tags.any { it in selectedFilters } }
    }

    val screenMarkers = remember(mapViewRef, viewportTick, filteredReports) {
        buildScreenMarkers(mapViewRef, filteredReports)
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(11.0)
                    controller.setCenter(trujilloCenter)
                    mapViewRef = this
                    addMapListener(object : MapAdapter() {
                        override fun onScroll(e: ScrollEvent?): Boolean { viewportTick++; return true }
                        override fun onZoom(e: ZoomEvent?): Boolean { viewportTick++; return true }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Markers
        Box(modifier = Modifier.fillMaxSize()) {
            screenMarkers.forEach { marker ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset(marker.x - 20.dp.roundToPx(), marker.y - 40.dp.roundToPx()) }
                        .clickable { selectedReport = marker.report }
                ) {
                    if (marker.report.type == ReportType.ActiveEmergency) {
                        PulsingEmergencyMarker()
                    } else {
                        PreventionMarker()
                    }
                }
            }
        }

        // Filter Button (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = { showFilters = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    Text("Filtros", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Report Details Sheet
        if (selectedReport != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedReport = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(40.dp, 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            ) {
                ReportDetailsSheet(report = selectedReport!!)
            }
        }

        // Filters Sheet
        if (showFilters) {
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState = filterSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                FilterMenuContent(
                    selectedFilters = selectedFilters,
                    onToggleFilter = { filter ->
                        if (filter in selectedFilters) selectedFilters.remove(filter)
                        else selectedFilters.add(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterMenuContent(
    selectedFilters: List<AlertFilter>,
    onToggleFilter: (AlertFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filtrar Mapa",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Selecciona las categorías que deseas visualizar en el mapa.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        AlertFilter.entries.forEach { filter ->
            val isSelected = filter in selectedFilters
            Surface(
                onClick = { onToggleFilter(filter) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) filter.accent.copy(alpha = 0.1f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) filter.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) filter.accent else filter.accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else filter.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) filter.accent else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleFilter(filter) },
                        colors = CheckboxDefaults.colors(checkedColor = filter.accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingEmergencyMarker() {
    val transition = rememberInfiniteTransition(label = "")
    val scale by transition.animateFloat(0.8f, 1.5f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "")
    val alpha by transition.animateFloat(0.4f, 0.1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "")

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp * scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(Color.Red)
        )
        Surface(
            shape = CircleShape,
            color = Color.Red,
            modifier = Modifier.size(24.dp).border(2.dp, Color.White, CircleShape),
            shadowElevation = 4.dp
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun PreventionMarker() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp).border(2.dp, Color.White, CircleShape),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun ReportDetailsSheet(report: MapReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = if (report.type == ReportType.ActiveEmergency) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (report.type == ReportType.ActiveEmergency) "EMERGENCIA" else "PREVENCIÓN",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (report.type == ReportType.ActiveEmergency) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(text = report.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(text = report.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        AsyncImage(
            model = report.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp))
            }
            Column {
                Text(text = "Reportado por", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = report.user, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildScreenMarkers(mapView: MapView?, reports: List<MapReport>): List<ScreenMarker> {
    if (mapView == null || mapView.width == 0) return emptyList()
    val projection = mapView.projection ?: return emptyList()
    return reports.mapNotNull { r ->
        val p = projection.toPixels(r.position, Point())
        ScreenMarker(r, p.x, p.y)
    }
}
