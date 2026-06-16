package com.roiry.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roiry.app.ui.components.MeshBackground

private data class ContactInstitution(
    val name: String,
    val type: String, // "Protección Civil", "Bomberos", "Policía", "Salud"
    val phone: String,
    val icon: ImageVector,
    val accentColor: Color,
    val municipality: String
)

// Contacts database containing various municipalities of Trujillo State
private val trujilloContactsData = listOf(
    ContactInstitution("Protección Civil Valera", "Protección Civil", "0271-2315555", Icons.Default.Warning, Color(0xFFF59E0B), "Valera"),
    ContactInstitution("Bomberos de Valera", "Bomberos", "0271-2253333", Icons.Default.LocalFireDepartment, Color(0xFFEF4444), "Valera"),
    ContactInstitution("Policía del Estado Trujillo - Valera", "Policía", "0271-2212222", Icons.Default.LocalPolice, Color(0xFF3B82F6), "Valera"),
    ContactInstitution("Hospital Central Dr. Pedro Emilio Carrillo", "Salud", "0271-2351111", Icons.Default.LocalHospital, Color(0xFF10B981), "Valera"),
    
    ContactInstitution("Protección Civil Estatal Trujillo", "Protección Civil", "0272-2364444", Icons.Default.Warning, Color(0xFFF59E0B), "Trujillo"),
    ContactInstitution("Bomberos Estación Principal Trujillo", "Bomberos", "0272-2361111", Icons.Default.LocalFireDepartment, Color(0xFFEF4444), "Trujillo"),
    ContactInstitution("Comandancia General de Policía", "Policía", "0272-2362222", Icons.Default.LocalPolice, Color(0xFF3B82F6), "Trujillo"),
    ContactInstitution("Hospital José Gregorio Hernández", "Salud", "0272-2012111", Icons.Default.LocalHospital, Color(0xFF10B981), "Trujillo"),
    
    ContactInstitution("Protección Civil Boconó", "Protección Civil", "0272-6523333", Icons.Default.Warning, Color(0xFFF59E0B), "Boconó"),
    ContactInstitution("Bomberos de Boconó", "Bomberos", "0272-6521111", Icons.Default.LocalFireDepartment, Color(0xFFEF4444), "Boconó"),
    ContactInstitution("Policía de Boconó", "Policía", "0272-6522222", Icons.Default.LocalPolice, Color(0xFF3B82F6), "Boconó"),
    ContactInstitution("Hospital Rafael Rangel", "Salud", "0272-6524444", Icons.Default.LocalHospital, Color(0xFF10B981), "Boconó"),
    
    ContactInstitution("Protección Civil Carache", "Protección Civil", "0272-9891111", Icons.Default.Warning, Color(0xFFF59E0B), "Carache"),
    ContactInstitution("Bomberos Rurales Carache", "Bomberos", "0272-9892222", Icons.Default.LocalFireDepartment, Color(0xFFEF4444), "Carache"),
    ContactInstitution("Ambulatorio de Carache", "Salud", "0272-9893333", Icons.Default.LocalHospital, Color(0xFF10B981), "Carache"),
    
    ContactInstitution("Protección Civil Escuque", "Protección Civil", "0271-8884444", Icons.Default.Warning, Color(0xFFF59E0B), "Escuque"),
    ContactInstitution("Policía Comunal Escuque", "Policía", "0271-8882222", Icons.Default.LocalPolice, Color(0xFF3B82F6), "Escuque"),
    ContactInstitution("Ambulatorio Tipo II Escuque", "Salud", "0271-8881111", Icons.Default.LocalHospital, Color(0xFF10B981), "Escuque"),
    
    ContactInstitution("Protección Civil Pampán", "Protección Civil", "0272-4141122", Icons.Default.Warning, Color(0xFFF59E0B), "Pampán"),
    ContactInstitution("Ambulatorio Pampán", "Salud", "0272-4141133", Icons.Default.LocalHospital, Color(0xFF10B981), "Pampán"),
    
    ContactInstitution("Protección Civil Rafael Rangel", "Protección Civil", "0271-2212345", Icons.Default.Warning, Color(0xFFF59E0B), "Rafael Rangel"),
    ContactInstitution("Ambulatorio Betijoque", "Salud", "0271-2212346", Icons.Default.LocalHospital, Color(0xFF10B981), "Rafael Rangel")
)

private val municipalitiesList = listOf("Todos", "Valera", "Trujillo", "Boconó", "Carache", "Escuque", "Pampán", "Rafael Rangel")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var selectedMunicipality by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(selectedMunicipality, searchQuery) {
        trujilloContactsData.filter { contact ->
            val matchesMunicipality = selectedMunicipality == "Todos" || contact.municipality == selectedMunicipality
            val matchesSearch = contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.type.contains(searchQuery, ignoreCase = true) ||
                    contact.municipality.contains(searchQuery, ignoreCase = true)
            matchesMunicipality && matchesSearch
        }
    }

    MeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            ContactHeader()

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, tipo o municipio...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                )
            )

            // Horizontal Scrollable Municipality Filter Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Filtrar por Municipio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    municipalitiesList.forEach { muni ->
                        val isSelected = selectedMunicipality == muni
                        Surface(
                            onClick = { selectedMunicipality = muni },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = muni,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Contacts List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Contactos Encontrados (${filteredContacts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron contactos para la búsqueda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    filteredContacts.forEach { contact ->
                        ContactRow(contact = contact) {
                            try {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${contact.phone}")
                                }
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el marcador de llamadas", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ContactHeader() {
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
                text = "DIRECTORIO DE SEGURIDAD",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        Text(
            text = "Números de Emergencia",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Encuentra y llama rápidamente a los bomberos, policía, protección civil y hospitales del Estado Trujillo.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ContactRow(
    contact: ContactInstitution,
    onCallClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(contact.accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = contact.icon,
                        contentDescription = null,
                        tint = contact.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Municipio ${contact.municipality} • ${contact.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669)) // Emerald Green dial icon
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Llamar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
