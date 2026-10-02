package com.example.casaesp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.example.casaesp.ui.theme.CasaESPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CasaESPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartHomeScreen()
                }
            }
        }
    }
}

@Composable
fun AlertBanner(active: Boolean, onSilence: () -> Unit) {
    if (active) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Alerta de Incendio",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                TextButton(
                    onClick = onSilence,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Silenciar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ControlCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onToggle: (Boolean) -> Unit
) {
    val cardBackground by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 250), label = "cardBackground"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 250), label = "iconContainer"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250), label = "iconTint"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggle(!isActive) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icono dentro de un contenedor circular minimalista
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = activeColor,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

// ============= PANTALLA PRINCIPAL =============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeScreen() {
    var luzSala by remember { mutableStateOf(false) }
    var luzRecamara by remember { mutableStateOf(false) }
    var puertaPrincipal by remember { mutableStateOf(false) }
    var alarmaIncendio by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf("Conectando...") }

    val database = remember { Firebase.database }
    val dbRef = remember { database.reference.child("casa_inteligente") }

    // AUTENTICACIÓN
    LaunchedEffect(Unit) {
        try {
            Firebase.auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        connectionStatus = "Conectado"
                    } else {
                        errorMessage = "Error de autenticación"
                        isLoading = false
                        connectionStatus = "Error"
                    }
                }
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    // LISTENER DE FIREBASE
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val luces = snapshot.child("luces")
                        luzSala = luces.child("sala").getValue(Boolean::class.java) ?: false
                        luzRecamara = luces.child("recamara").getValue(Boolean::class.java) ?: false
                        puertaPrincipal = snapshot.child("accesos")
                            .child("puerta_principal")
                            .getValue(Boolean::class.java) ?: false
                        alarmaIncendio = snapshot.child("seguridad")
                            .child("alarma_incendio")
                            .getValue(Boolean::class.java) ?: false

                        isLoading = false
                        errorMessage = null
                    }
                } catch (e: Exception) {
                    errorMessage = "Error procesando datos"
                    isLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Error de conexión: ${error.message}"
                connectionStatus = "Desconectado"
                isLoading = false
            }
        }

        val connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                connectionStatus = if (connected) "Conectado" else "Sin conexión"
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        dbRef.addValueEventListener(listener)
        database.reference.child(".info/connected").addValueEventListener(connectionListener)

        onDispose {
            dbRef.removeEventListener(listener)
            database.reference.child(".info/connected").removeEventListener(connectionListener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) {
                        Column {
                            Text(
                                "Mi Hogar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                "Panel de Control",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Estado de conexión discreto tipo chip
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (connectionStatus == "Conectado") Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (connectionStatus == "Conectado") Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    connectionStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
                return@Scaffold
            }

            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                Firebase.auth.signInAnonymously()
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            AlertBanner(
                active = alarmaIncendio,
                onSilence = {
                    dbRef.child("seguridad").child("alarma_incendio").setValue(false)
                }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ControlCard(
                        title = "Luz Sala",
                        subtitle = if (luzSala) "Encendida" else "Apagada",
                        icon = Icons.Default.Lightbulb,
                        isActive = luzSala,
                        activeColor = Color(0xFFFFB300), // Tono cálido para iluminación
                        onToggle = { newState ->
                            dbRef.child("luces").child("sala").setValue(newState)
                        }
                    )
                }

                item {
                    ControlCard(
                        title = "Luz Recámara",
                        subtitle = if (luzRecamara) "Encendida" else "Apagada",
                        icon = Icons.Default.Lightbulb,
                        isActive = luzRecamara,
                        activeColor = Color(0xFFFFB300),
                        onToggle = { newState ->
                            dbRef.child("luces").child("recamara").setValue(newState)
                        }
                    )
                }

                item {
                    ControlCard(
                        title = "Puerta Principal",
                        subtitle = if (puertaPrincipal) "Abierta (90°)" else "Cerrada (0°)",
                        icon = Icons.Default.MeetingRoom,
                        isActive = puertaPrincipal,
                        activeColor = Color(0xFF1E88E5), // Azul moderno para accesos
                        onToggle = { newState ->
                            dbRef.child("accesos").child("puerta_principal").setValue(newState)
                        }
                    )
                }

                item {
                    ControlCard(
                        title = "Alarma Incendio",
                        subtitle = if (alarmaIncendio) "Activada" else "En espera",
                        icon = if (alarmaIncendio) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        isActive = alarmaIncendio,
                        activeColor = Color(0xFFE53935), // Rojo para alertas
                        onToggle = { newState ->
                            dbRef.child("seguridad").child("alarma_incendio").setValue(newState)
                        }
                    )
                }
            }
        }
    }
}