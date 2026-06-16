package com.roiry.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tsunami
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.osmdroid.util.GeoPoint

enum class AlertFilter(
    val label: String,
    val icon: ImageVector,
    val accent: Color
) {
    Earthquakes("Sismos", Icons.Filled.Warning, Color(0xFFE11D48)),
    Floods("Inundaciones", Icons.Filled.Tsunami, Color(0xFF0EA5E9)),
    Fires("Incendios", Icons.Filled.LocalFireDepartment, Color(0xFFF97316)),
    Others("Otros", Icons.Filled.MoreHoriz, Color(0xFF94A3B8))
}

enum class ReportType {
    ActiveEmergency,
    Prevention
}

enum class EmergencySeverity(val label: String, val color: Color) {
    LEVE("Leve", Color(0xFF10B981)),      // Emerald Green
    URGENTE("Urgente", Color(0xFFF59E0B)),  // Amber
    GRAVE("Grave", Color(0xFFEF4444))      // Red
}

data class MapReport(
    val id: String,
    val title: String,
    val description: String,
    val user: String,
    val photoUrl: String? = null,
    val photoUri: String? = null, // Store local URI if attached
    val position: GeoPoint,
    val type: ReportType,
    val severity: EmergencySeverity,
    val tags: Set<AlertFilter>
)

object EmergencyRepository {
    // Shared list of alerts, initialized with mock data
    val reports = mutableStateListOf<MapReport>(
        MapReport(
            id = "1",
            title = "Emergencia en Valera",
            description = "Riesgo estructural por sismo detectado en la zona céntrica.",
            user = "Protección Civil",
            photoUrl = "https://images.unsplash.com/photo-1521295121783-8a321d551ad2?w=800",
            position = GeoPoint(9.3181, -70.6030),
            type = ReportType.ActiveEmergency,
            severity = EmergencySeverity.GRAVE,
            tags = setOf(AlertFilter.Earthquakes)
        ),
        MapReport(
            id = "2",
            title = "Monitoreo Boconó",
            description = "Vigilancia preventiva de quebradas por lluvias continuas.",
            user = "Brigada Verde",
            photoUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800",
            position = GeoPoint(9.2539, -70.2511),
            type = ReportType.Prevention,
            severity = EmergencySeverity.LEVE,
            tags = setOf(AlertFilter.Floods)
        ),
        MapReport(
            id = "3",
            title = "Alerta de Incendio Forestal",
            description = "Foco de incendio detectado cerca de áreas residenciales en Trujillo.",
            user = "Cuerpo de Bomberos",
            photoUrl = "https://images.unsplash.com/photo-1508873696983-2df519f0397e?w=800",
            position = GeoPoint(9.368, -70.432),
            type = ReportType.ActiveEmergency,
            severity = EmergencySeverity.URGENTE,
            tags = setOf(AlertFilter.Fires)
        )
    )

    fun addReport(report: MapReport) {
        reports.add(0, report) // Add at the beginning to show up first
    }
}
