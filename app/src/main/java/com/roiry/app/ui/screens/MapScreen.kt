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
import com.roiry.app.data.AlertFilter
import com.roiry.app.data.EmergencyRepository
import com.roiry.app.data.EmergencySeverity
import com.roiry.app.data.MapReport
import com.roiry.app.data.ReportType
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapAdapter
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.roundToInt

private data class ScreenMarker(
    val report: MapReport,
    val x: Int,
    val y: Int
)

private val trujilloCenter = GeoPoint(9.365, -70.435)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val selectedFilters = remember { mutableStateListOf(*AlertFilter.entries.toTypedArray()) }
    var showActiveEmergencies by remember { mutableStateOf(true) }
    var showPreventions by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<MapReport?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var viewportTick by remember { mutableIntStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val filterSheetState = rememberModalBottomSheetState()

    // Retrieve dynamically from shared repository with category and type filtering
    val filteredReports = remember(selectedFilters.size, showActiveEmergencies, showPreventions, EmergencyRepository.reports.size) {
        EmergencyRepository.reports.filter { r ->
            val matchesType = when (r.type) {
                ReportType.ActiveEmergency -> showActiveEmergencies
                ReportType.Prevention -> showPreventions
            }
            matchesType && r.tags.any { it in selectedFilters }
        }
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
                        PulsingEmergencyMarker(color = marker.report.severity.color)
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
                    showActiveEmergencies = showActiveEmergencies,
                    onToggleActiveEmergencies = { showActiveEmergencies = it },
                    showPreventions = showPreventions,
                    onTogglePreventions = { showPreventions = it },
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
    showActiveEmergencies: Boolean,
    onToggleActiveEmergencies: (Boolean) -> Unit,
    showPreventions: Boolean,
    onTogglePreventions: (Boolean) -> Unit,
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

        // 1. Report Type Filter Section
        Text(
            text = "Tipo de Reporte",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Option 1: Emergencias
            Surface(
                onClick = { onToggleActiveEmergencies(!showActiveEmergencies) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = if (showActiveEmergencies) Color.Red.copy(alpha = 0.15f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (showActiveEmergencies) Color.Red else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (showActiveEmergencies) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Emergencias",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showActiveEmergencies) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Option 2: Prevenciones
            Surface(
                onClick = { onTogglePreventions(!showPreventions) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = if (showPreventions) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (showPreventions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (showPreventions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Prevenciones",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showPreventions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.1f))

        // 2. Incident Categories Section
        Text(
            text = "Categoría de Incidente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

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
private fun PulsingEmergencyMarker(color: Color) {
    val transition = rememberInfiniteTransition(label = "")
    val scale by transition.animateFloat(0.8f, 1.5f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "")
    val alpha by transition.animateFloat(0.4f, 0.1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "")

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp * scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color)
        )
        Surface(
            shape = CircleShape,
            color = color,
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
                color = report.severity.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, report.severity.color)
            ) {
                Text(
                    text = report.severity.label.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = report.severity.color
                )
            }
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

        val imageModel = report.photoUri ?: report.photoUrl
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp))
            }
            Column {
                Text(text = "Reportado por", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = report.user, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        
        // Coordinates display
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Coordenadas: ${String.format("%.4f", report.position.latitude)}, ${String.format("%.4f", report.position.longitude)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
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
