package com.tunnelcanvas

import android.content.ContentResolver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private val Ink = Color(0xFF09070F)
private val Panel = Color(0xFF171222)
private val Panel2 = Color(0xFF211832)
private val Purple = Color(0xFFB56CFF)
private val Lavender = Color(0xFFD9B5FF)
private val Muted = Color(0xFFB8ADC8)
private val Good = Color(0xFF78E6B1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TunnelCanvasApp(contentResolver) }
    }
}

private data class TunnelProfile(
    val id: Long,
    val name: String,
    val protocol: String,
    val server: String,
    val port: String,
    val note: String = ""
)

@Composable
private fun TunnelCanvasApp(contentResolver: ContentResolver) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var profileName by rememberSaveable { mutableStateOf("My Profile") }
    var protocol by rememberSaveable { mutableStateOf("trojan") }
    var server by rememberSaveable { mutableStateOf("example.com") }
    var port by rememberSaveable { mutableStateOf("443") }
    var note by rememberSaveable { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Ready to configure") }
    var importedConfig by remember { mutableStateOf("") }
    val profiles = remember {
        mutableStateListOf(
            TunnelProfile(1L, "Example profile", "trojan", "example.com", "443", "Demo only")
        )
    }

    val configPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    importedConfig = input.bufferedReader().readText()
                }
                val json = JSONObject(importedConfig)
                val metadata = json.optJSONObject("metadata")
                val connection = json.optJSONObject("connection")
                profileName = metadata?.optString("name", "Imported profile") ?: "Imported profile"
                protocol = connection?.optString("protocol", "trojan") ?: "trojan"
                server = connection?.optString("server", "") ?: ""
                port = connection?.optInt("port", 443)?.toString() ?: "443"
                statusMessage = "Configuration imported"
            } catch (e: Exception) {
                statusMessage = "Import failed: expected TunnelCanvas JSON profile"
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = buildConfigJson(profileName, protocol, server, port, note)
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                statusMessage = "Configuration exported"
            } catch (e: Exception) {
                statusMessage = "Export failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Purple,
        onPrimary = Ink,
        secondary = Lavender,
        background = Ink,
        surface = Panel,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text("Profiles") }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).background(Ink)
            ) {
                Header()
                when (selectedTab) {
                    0 -> Dashboard(
                        statusMessage = statusMessage,
                        profileName = profileName,
                        protocol = protocol,
                        server = server,
                        port = port,
                        onImport = { configPicker.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onExport = { exportLauncher.launch(safeFileName(profileName) + ".json") },
                        onOpenProfiles = { selectedTab = 1 }
                    )
                    1 -> ProfilesScreen(
                        profiles = profiles,
                        profileName = profileName,
                        onProfileNameChange = { profileName = it },
                        protocol = protocol,
                        onProtocolChange = { protocol = it },
                        server = server,
                        onServerChange = { server = it },
                        port = port,
                        onPortChange = { port = it },
                        note = note,
                        onNoteChange = { note = it },
                        onImport = { configPicker.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onSave = {
                            val numericId = System.currentTimeMillis()
                            profiles.add(0, TunnelProfile(numericId, profileName, protocol, server, port, note))
                            statusMessage = "Profile saved locally"
                        },
                        onSelect = {
                            profileName = it.name
                            protocol = it.protocol
                            server = it.server
                            port = it.port
                            note = it.note
                            statusMessage = "Selected ${it.name}"
                        },
                        onDelete = { profiles.remove(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Panel2),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Purple, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("TUNNELCANVAS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("CONFIGURATION STUDIO", style = MaterialTheme.typography.labelSmall, color = Lavender)
        }
        Box(Modifier.clip(CircleShape).background(Panel2).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("BETA", color = Purple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Dashboard(
    statusMessage: String,
    profileName: String,
    protocol: String,
    server: String,
    port: String,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onOpenProfiles: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Your workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Manage your connection profiles and configuration files.", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CONFIGURATION STATUS", color = Muted, style = MaterialTheme.typography.labelMedium)
                            Text("Local workspace", color = Good, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF193A31)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Good)
                        }
                    }
                    Divider(color = Panel2)
                    InfoRow("Profile", profileName)
                    InfoRow("Protocol", protocol.uppercase())
                    InfoRow("Endpoint", "$server:$port")
                    Text(statusMessage, color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("This build manages profiles only; it does not establish VPN connections yet.", color = Lavender, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export")
                }
            }
        }
        item {
            FeatureCard(
                title = "Configuration profiles",
                subtitle = "Edit server metadata and keep profiles in this session.",
                icon = { Icon(Icons.Default.FolderOpen, null, tint = Purple) },
                onClick = onOpenProfiles
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "—" }, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Panel2), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: List<TunnelProfile>,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    protocol: String,
    onProtocolChange: (String) -> Unit,
    server: String,
    onServerChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    onSelect: (TunnelProfile) -> Unit,
    onDelete: (TunnelProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Profile records are in memory in this starter build.", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = profileName, onValueChange = onProfileNameChange, label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = protocol, onValueChange = onProtocolChange, label = { Text("Protocol (e.g. trojan)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = server, onValueChange = onServerChange, label = { Text("Server hostname") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = port, onValueChange = { onPortChange(it.filter(Char::isDigit).take(5)) }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = note, onValueChange = onNoteChange, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Save profile")
                        }
                        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Import JSON")
                        }
                    }
                }
            }
        }
        item { Text("Saved in this session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(profiles, key = { it.id }) { profile ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onSelect(profile) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, fontWeight = FontWeight.Bold)
                        Text("${profile.protocol.uppercase()} • ${profile.server}:${profile.port}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        if (profile.note.isNotBlank()) Text(profile.note, color = Lavender, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete profile",
                        tint = Color(0xFFFF8C9A),
                        modifier = Modifier.clickable { onDelete(profile) }.padding(8.dp)
                    )
                }
            }
        }
    }
}

private fun buildConfigJson(
    name: String,
    protocol: String,
    server: String,
    port: String,
    note: String
): String {
    val metadata = JSONObject()
        .put("name", name.ifBlank { "Untitled profile" })
        .put("note", note)
    val connection = JSONObject()
        .put("protocol", protocol.trim().ifBlank { "trojan" })
        .put("server", server.trim())
        .put("port", port.toIntOrNull() ?: 443)
    return JSONObject()
        .put("format", "tunnelcanvas-profile")
        .put("version", 1)
        .put("metadata", metadata)
        .put("connection", connection)
        .toString(2)
}

private fun safeFileName(value: String): String {
    return value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "tunnelcanvas-profile" }
}
