package com.jio.remote.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jio.remote.plus.protocol.KnownJioApps
import com.jio.remote.plus.protocol.RemoteAppShortcut
import com.jio.remote.plus.protocol.RemoteKey
import com.jio.remote.plus.ui.components.JioDpad
import com.jio.remote.plus.ui.components.performTickHaptic

@Composable
fun NormalQuickModeScreen(
    onSendKey: (RemoteKey) -> Unit,
    onLaunchApp: (RemoteAppShortcut) -> Unit,
    onSendText: (String) -> Unit
) {
    val context = LocalContext.current
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    if (showKeyboardDialog) {
        AlertDialog(
            onDismissRequest = { showKeyboardDialog = false },
            title = { Text("Direct STB Typing") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Search, movie, URL or password...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput)
                            textInput = ""
                        }
                        showKeyboardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078FF))
                ) {
                    Text("Send to TV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyboardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Quick Header (Power & Mute)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.POWER)
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = Color(0xFFFF4D4D))
            }

            Text(
                text = "QUICK CONTROL",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.MUTE)
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF2B313E), CircleShape)
            ) {
                Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = Color.White)
            }
        }

        // Quick Launch App Cards (YouTube, JioCinema, Hotstar, Netflix)
        Text(
            text = "Favorite Apps",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KnownJioApps.QUICK_APPS.forEach { app ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(app.primaryColorHex), Color(app.secondaryColorHex))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            performTickHaptic(context)
                            onLaunchApp(app)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Central D-Pad
        Spacer(modifier = Modifier.height(10.dp))
        JioDpad(
            size = 210.dp,
            onDirectionClick = { onSendKey(it) }
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Actions Row (Voice Search, Direct STB Keyboard typing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Voice Assistant Button
            Button(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.VOICE_SEARCH)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF00C6FF))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Voice Mic", color = Color.White, fontSize = 13.sp)
            }

            // Keyboard Button
            Button(
                onClick = {
                    performTickHaptic(context)
                    showKeyboardDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = "Keyboard", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Type Text", color = Color.White, fontSize = 13.sp)
            }
        }

        // Bottom Navigation Bar (Back, Home, Vol Down, Vol Up)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.BACK)
                },
                modifier = Modifier.size(52.dp).background(Color(0xFF262C38), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.HOME)
                },
                modifier = Modifier.size(52.dp).background(Color(0xFF0066FF), CircleShape)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
            }

            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.VOLUME_DOWN)
                },
                modifier = Modifier.size(52.dp).background(Color(0xFF262C38), CircleShape)
            ) {
                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = Color.White)
            }

            IconButton(
                onClick = {
                    performTickHaptic(context)
                    onSendKey(RemoteKey.VOLUME_UP)
                },
                modifier = Modifier.size(52.dp).background(Color(0xFF262C38), CircleShape)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = Color.White)
            }
        }
    }
}
