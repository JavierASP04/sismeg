package com.roiry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.roiry.app.ui.components.MeshBackground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PreventionCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

private val categories = listOf(
    PreventionCategory("Alumbrado", Icons.Default.Lightbulb, Color(0xFFFACC15)),
    PreventionCategory("Infraestructura", Icons.Default.Construction, Color(0xFF94A3B8)),
    PreventionCategory("Seguridad", Icons.Default.Shield, Color(0xFF3B82F6)),
    PreventionCategory("Limpieza", Icons.Default.Delete, Color(0xFF10B981)),
    PreventionCategory("Otros", Icons.Default.MoreHoriz, Color(0xFF8B5CF6))
)

@Composable
fun PreventScreen() {
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var reportDescription by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    MeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Espacio para el botón de menú global
            Spacer(modifier = Modifier.height(64.dp))
            
            PreventHeader()

            // Categories Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Categoría del reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            // Form Section
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "Detalles del reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe la situación...", color = Color.White.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(24.dp),
                    minLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Location & Photo Actions
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionSurface(
                        icon = Icons.Default.LocationOn,
                        label = "Ubicación",
                        modifier = Modifier.weight(1f),
                        onClick = { /* TODO */ }
                    )
                    ActionSurface(
                        icon = Icons.Default.AddAPhoto,
                        label = "Evidencia",
                        modifier = Modifier.weight(1f),
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { /* TODO: Submit Report */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669), // Verde más oscuro para mejor contraste
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "PUBLICAR REPORTE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PreventHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "PREVENCIÓN COMUNITARIA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        Text(
            text = "Crea un reporte\nde seguridad",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp,
            color = Color.White
        )
    }
}

@Composable
private fun ActionSurface(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: PreventionCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) category.color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) category.color else Color.White.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) category.color else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) category.color else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
