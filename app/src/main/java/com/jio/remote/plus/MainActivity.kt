package com.jio.remote.plus

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jio.remote.plus.protocol.*
import com.jio.remote.plus.ui.screens.JioReplicaModeScreen
import com.jio.remote.plus.ui.screens.NormalQuickModeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RemoteMode {
    QUICK,
    REPLICA
}

class MainActivity : ComponentActivity() {

    private lateinit var discovery: JioDeviceDiscovery
    private lateinit var tvClient: AndroidTvRemoteClient
    private val adbClient = AdbFallbackClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        discovery = JioDeviceDiscovery(applicationContext)
        tvClient = AndroidTvRemoteClient(applicationContext)

        val prefs = getSharedPreferences("jio_remote_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("last_stb_ip", "") ?: ""

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F1116),
                    surface = Color(0xFF181C24),
                    primary = Color(0xFF0078FF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JioRemoteMainScreen(
                        savedIp = savedIp,
                        discovery = discovery,
                        onSaveIp = { ip ->
                            prefs.edit().putString("last_stb_ip", ip).apply()
                        },
                        onSendKey = { host, key ->
                            dispatchKey(host, key)
                        },
                        onLaunchApp = { host, app ->
                            dispatchLaunchApp(host, app)
                        },
                        onSendText = { host, text ->
                            dispatchSendText(host, text)
                        }
                    )
                }
            }
        }

        lifecycleScope.launch {
            tvClient.initializeKeyStore()
            discovery.startDiscovery()
        }
    }

    private fun dispatchKey(host: String, key: RemoteKey) {
        lifecycleScope.launch {
            if (host.isNotBlank()) {
                val adbSuccess = adbClient.sendKeyEvent(host, key.keyCode)
                if (!adbSuccess) {
                    tvClient.sendKeyCode(key.keyCode)
                }
            }
        }
    }

    private fun dispatchLaunchApp(host: String, app: RemoteAppShortcut) {
        lifecycleScope.launch {
            if (host.isNotBlank()) {
                val ok = adbClient.launchApp(host, app.packageName)
                if (!ok) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Launching ${app.name} on $host", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun dispatchSendText(host: String, text: String) {
        lifecycleScope.launch {
            if (host.isNotBlank()) {
                adbClient.sendText(host, text)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery.stopDiscovery()
        tvClient.disconnect()
    }
}

@Composable
fun JioRemoteMainScreen(
    savedIp: String,
    discovery: JioDeviceDiscovery,
    onSaveIp: (String) -> Unit,
    onSendKey: (String, RemoteKey) -> Unit,
    onLaunchApp: (String, RemoteAppShortcut) -> Unit,
    onSendText: (String, String) -> Unit
) {
    var activeMode by remember { mutableStateOf(RemoteMode.QUICK) }
    var currentHost by remember { mutableStateOf(savedIp) }
    var showIpDialog by remember { mutableStateOf(false) }
    var ipInput by remember { mutableStateOf(savedIp) }

    val discoveredList by discovery.discoveredDevices.collectAsState()

    // Auto-select discovered Jio device if host is blank
    LaunchedEffect(discoveredList) {
        if (currentHost.isBlank() && discoveredList.isNotEmpty()) {
            discoveredList.firstOrNull { it.isJioDevice }?.host?.hostAddress?.let {
                currentHost = it
                onSaveIp(it)
            }
        }
    }

    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            title = { Text("JioAirFiber STB Connection") },
            text = {
                Column {
                    Text("Enter STB Wi-Fi IP Address (e.g. 192.168.29.X):", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        placeholder = { Text("192.168.29.100") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (discoveredList.isNotEmpty()) {
                        Text("Discovered Nearby Devices:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        discoveredList.forEach { dev ->
                            TextButton(onClick = {
                                dev.host?.hostAddress?.let { ipInput = it }
                            }) {
                                Text("${dev.name} (${dev.host?.hostAddress})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    currentHost = ipInput.trim()
                    onSaveIp(currentHost)
                    showIpDialog = false
                }) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar with Segmented Mode Switcher & Device IP Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Mode Control
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E222D))
                    .padding(3.dp)
            ) {
                FilterChip(
                    selected = activeMode == RemoteMode.QUICK,
                    onClick = { activeMode = RemoteMode.QUICK },
                    label = { Text("⚡ Quick Mode", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0078FF),
                        selectedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = activeMode == RemoteMode.REPLICA,
                    onClick = { activeMode = RemoteMode.REPLICA },
                    label = { Text("🎮 Jio Replica", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0078FF),
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Connection Badge / IP Selector
            SuggestionChip(
                onClick = {
                    ipInput = currentHost
                    showIpDialog = true
                },
                label = {
                    Text(
                        if (currentHost.isNotBlank()) currentHost else "Find STB",
                        fontSize = 11.sp,
                        color = if (currentHost.isNotBlank()) Color(0xFF00E676) else Color(0xFFFFB300)
                    )
                },
                icon = {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = "STB",
                        tint = if (currentHost.isNotBlank()) Color(0xFF00E676) else Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        // Active Remote Mode View
        Box(modifier = Modifier.weight(1f)) {
            when (activeMode) {
                RemoteMode.QUICK -> {
                    NormalQuickModeScreen(
                        onSendKey = { onSendKey(currentHost, it) },
                        onLaunchApp = { onLaunchApp(currentHost, it) },
                        onSendText = { onSendText(currentHost, it) }
                    )
                }
                RemoteMode.REPLICA -> {
                    JioReplicaModeScreen(
                        onSendKey = { onSendKey(currentHost, it) },
                        onLaunchApp = { onLaunchApp(currentHost, it) }
                    )
                }
            }
        }
    }
}
