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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    private lateinit var remoteManager: AndroidTvRemoteManager
    private val adbClient = AdbFallbackClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        discovery = JioDeviceDiscovery(applicationContext)
        remoteManager = AndroidTvRemoteManager(applicationContext)

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
                        remoteManager = remoteManager,
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
            remoteManager.getOrGenerateCertificates()
            discovery.startDiscovery()
        }
    }

    private fun dispatchKey(host: String, key: RemoteKey) {
        lifecycleScope.launch {
            if (host.isNotBlank()) {
                val ok = remoteManager.sendKey(host, key.keyCode)
                if (!ok) {
                    adbClient.sendKeyEvent(host, key.keyCode)
                }
            }
        }
    }

    private fun dispatchLaunchApp(host: String, app: RemoteAppShortcut) {
        lifecycleScope.launch {
            if (host.isNotBlank()) {
                val appLink = "https://www.youtube.com" // or market/custom intent
                val ok = remoteManager.launchAppUri(host, appLink)
                if (!ok) {
                    adbClient.launchApp(host, app.packageName)
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
        remoteManager.disconnectCommands()
    }
}

@Composable
fun JioRemoteMainScreen(
    savedIp: String,
    discovery: JioDeviceDiscovery,
    remoteManager: AndroidTvRemoteManager,
    onSaveIp: (String) -> Unit,
    onSendKey: (String, RemoteKey) -> Unit,
    onLaunchApp: (String, RemoteAppShortcut) -> Unit,
    onSendText: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var activeMode by remember { mutableStateOf(RemoteMode.QUICK) }
    var currentHost by remember { mutableStateOf(savedIp) }

    // Dialog States
    var showConnectDialog by remember { mutableStateOf(false) }
    var ipInput by remember { mutableStateOf(savedIp) }
    
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairCodeInput by remember { mutableStateOf("") }
    var pairingLoading by remember { mutableStateOf(false) }
    var pairingStatusMsg by remember { mutableStateOf("") }

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

    // 1. IP / Discovery Dialog
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("JioAirFiber Connection") },
            text = {
                Column {
                    Text("Enter STB Wi-Fi IP address:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        placeholder = { Text("192.168.29.X") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (discoveredList.isNotEmpty()) {
                        Text("Discovered STB Devices:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        discoveredList.forEach { dev ->
                            TextButton(
                                onClick = {
                                    dev.host?.hostAddress?.let { ipInput = it }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${dev.name} (${dev.host?.hostAddress})", textAlign = TextAlign.Start)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    currentHost = ipInput.trim()
                    onSaveIp(currentHost)
                    showConnectDialog = false
                    // Start pairing process automatically
                    if (currentHost.isNotBlank()) {
                        pairingStatusMsg = "Connecting to TV to show code..."
                        showPairingDialog = true
                        pairingLoading = true
                        coroutineScope.launch {
                            val res = remoteManager.startPairing(currentHost)
                            pairingLoading = false
                            if (res.isSuccess) {
                                pairingStatusMsg = "Enter the 6-character code showing on your TV screen:"
                            } else {
                                pairingStatusMsg = "Could not reach TV: ${res.exceptionOrNull()?.message ?: "Check IP & Wi-Fi"}"
                            }
                        }
                    }
                }) {
                    Text("Connect & Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. TV Code Pairing Dialog
    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!pairingLoading) showPairingDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pin, contentDescription = null, tint = Color(0xFF0078FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair with Jio STB")
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pairingStatusMsg,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (pairingLoading) {
                        CircularProgressIndicator(color = Color(0xFF0078FF))
                    } else {
                        OutlinedTextField(
                            value = pairCodeInput,
                            onValueChange = {
                                if (it.length <= 6) pairCodeInput = it.uppercase()
                            },
                            placeholder = { Text("e.g. A1B2C3") },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (!pairingLoading) {
                    Button(
                        onClick = {
                            if (pairCodeInput.length == 6) {
                                pairingLoading = true
                                pairingStatusMsg = "Pairing with TV..."
                                coroutineScope.launch {
                                    val res = remoteManager.finishPairing(pairCodeInput)
                                    pairingLoading = false
                                    if (res.isSuccess) {
                                        showPairingDialog = false
                                        pairCodeInput = ""
                                        // Connect commands
                                        remoteManager.connectCommands(currentHost)
                                    } else {
                                        pairingStatusMsg = "Incorrect code or pairing error. Please retry."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078FF)),
                        enabled = pairCodeInput.length == 6
                    ) {
                        Text("Pair Code")
                    }
                }
            },
            dismissButton = {
                if (!pairingLoading) {
                    TextButton(onClick = {
                        showPairingDialog = false
                        pairCodeInput = ""
                    }) {
                        Text("Cancel")
                    }
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
                    showConnectDialog = true
                },
                label = {
                    Text(
                        if (currentHost.isNotBlank()) currentHost else "Pair TV",
                        fontSize = 11.sp,
                        color = if (currentHost.isNotBlank()) Color(0xFF00E676) else Color(0xFFFFB300)
                    )
                },
                icon = {
                    Icon(
                        if (currentHost.isNotBlank()) Icons.Default.Tv else Icons.Default.Add,
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
