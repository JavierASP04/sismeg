package com.roiry.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roiry.app.data.AlertFilter
import com.roiry.app.data.EmergencyRepository
import com.roiry.app.data.EmergencySeverity
import com.roiry.app.data.MapReport
import com.roiry.app.data.ReportType
import com.roiry.app.ui.components.MeshBackground
import org.osmdroid.util.GeoPoint
import java.util.UUID

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    val providers = locationManager.getProviders(true)
    Toast.makeText(context, "Iniciando GPS... Proveedores: $providers", Toast.LENGTH_SHORT).show()
    
    var bestLocation: Location? = null
    
    // 1. GPS Last Known
    try {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) {
                bestLocation = loc
                Toast.makeText(context, "GPS guardado: Lat ${loc.latitude}", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error GPS: ${e.message}", Toast.LENGTH_SHORT).show()
    }
    
    // 2. Network Last Known
    if (bestLocation == null) {
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    bestLocation = loc
                    Toast.makeText(context, "Red/Wifi guardado: Lat ${loc.latitude}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error Red: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 3. Passive
    if (bestLocation == null) {
        try {
            val loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (loc != null) {
                bestLocation = loc
            }
        } catch (e: Exception) {}
    }
    
    // Trigger immediately with best available
    if (bestLocation != null) {
        onLocationReceived(bestLocation.latitude, bestLocation.longitude)
    }
    
    // 4. Request fresh update on Main Looper
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Toast.makeText(context, "Ubicación en vivo: Lat ${location.latitude}", Toast.LENGTH_LONG).show()
            onLocationReceived(location.latitude, location.longitude)
            locationManager.removeUpdates(this)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    
    try {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, context.mainLooper)
            Toast.makeText(context, "Buscando satélites GPS en vivo...", Toast.LENGTH_SHORT).show()
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, context.mainLooper)
            Toast.makeText(context, "Solicitando ubicación de red en vivo...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "GPS desactivado. Por favor, actívalo.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error al suscribir actualizaciones: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form States
    var selectedAlert by remember { mutableStateOf<AlertFilter?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(EmergencySeverity.URGENTE) }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Activity Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation(context) { lat, lng ->
                latitude = String.format("%.6f", lat)
                longitude = String.format("%.6f", lng)
            }
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    MeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            SosHeader()

            // 1. Selector de Tipo de Incidente (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "1. Selecciona el Tipo de Emergencia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                AlertFilter.entries.chunked(2).forEach { rowFilters ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowFilters.forEach { filter ->
                            val isSelected = selectedAlert == filter
                            Box(modifier = Modifier.weight(1f)) {
                                SosOptionCard(
                                    option = filter,
                                    selected = isSelected,
                                    onClick = { selectedAlert = filter }
                                )
                            }
                        }
                    }
                }
            }

            // Form Fields Section (Visible after selection)
            AnimatedVisibility(
                visible = selectedAlert != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // 2. Título y Descripción
                    Text(
                        text = "2. Detalles de la Emergencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la Emergencia") },
                        placeholder = { Text("Ej: Derrumbe en vía principal, Fuga de gas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedAlert?.accent ?: MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = selectedAlert?.accent ?: MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción del suceso") },
                        placeholder = { Text("Describe brevemente lo que ocurre para ayudar a los equipos de rescate.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedAlert?.accent ?: MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = selectedAlert?.accent ?: MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // 3. Nivel de Emergencia
                    Text(
                        text = "3. Nivel de Gravedad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmergencySeverity.entries.forEach { level ->
                            val isSelected = severity == level
                            val cardColor = if (isSelected) level.color else Color.White.copy(alpha = 0.05f)
                            val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                            val borderColor = if (isSelected) level.color else Color.White.copy(alpha = 0.1f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { severity = level }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level.label.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                        }
                    }

                    // 4. Ubicación Coordenadas
                    Text(
                        text = "4. Ubicación Geográfica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = latitude,
                                    onValueChange = { latitude = it },
                                    label = { Text("Latitud") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = longitude,
                                    onValueChange = { longitude = it },
                                    label = { Text("Longitud") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = selectedAlert?.accent ?: MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Obtener Ubicación Actual")
                            }
                        }
                    }

                    // 5. Adjuntar Imagen
                    Text(
                        text = "5. Evidencia Fotográfica (Opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Previsualización",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red)
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Adjuntar o capturar fotografía",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Haz clic para seleccionar de la galería",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Envío
            SendSosButton(
                enabled = selectedAlert != null && title.isNotBlank() && description.isNotBlank() && latitude.isNotBlank() && longitude.isNotBlank(),
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    if (lat == null || lng == null) {
                        Toast.makeText(context, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
                        return@SendSosButton
                    }
                    isSubmitting = true
                    
                    val newReport = MapReport(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        user = "Usuario Activo",
                        photoUri = imageUri?.toString(),
                        position = GeoPoint(lat, lng),
                        type = ReportType.ActiveEmergency,
                        severity = severity,
                        tags = setOf(selectedAlert!!)
                    )
                    
                    EmergencyRepository.addReport(newReport)
                    
                    // Clear fields
                    title = ""
                    description = ""
                    latitude = ""
                    longitude = ""
                    imageUri = null
                    selectedAlert = null
                    
                    Toast.makeText(context, "¡EMERGENCIA REPORTADA CORRECTAMENTE!", Toast.LENGTH_LONG).show()
                    isSubmitting = false
                }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SosHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Text(
                text = "SISTEMA DE EMERGENCIA SOS",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "¿Necesitas ayuda?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Text(
            text = "Completa los datos del incidente para notificar a las unidades de auxilio más cercanas en tiempo real.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SosOptionCard(
    option: AlertFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    // 0.dp elevation to remove boxy shadow overlays
    val elevation = 0.dp
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) option.accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = borderStroke(selected, option.accent),
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) option.accent else option.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else option.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = option.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) option.accent else Color.White
            )
        }
    }
}

@Composable
private fun borderStroke(selected: Boolean, color: Color) = if (selected) {
    androidx.compose.foundation.BorderStroke(2.dp, color)
} else {
    androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
}

@Composable
private fun SendSosButton(enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseSize"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (enabled) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = Color.Red,
                    radius = (size.minDimension / 2) * pulseSize,
                    alpha = pulseAlpha
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE11D48),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "ENVIAR ALERTA SOS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
