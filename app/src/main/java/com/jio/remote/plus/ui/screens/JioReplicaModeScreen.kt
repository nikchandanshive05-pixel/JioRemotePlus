package com.jio.remote.plus.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jio.remote.plus.protocol.KnownJioApps
import com.jio.remote.plus.protocol.RemoteAppShortcut
import com.jio.remote.plus.protocol.RemoteKey
import com.jio.remote.plus.ui.components.JioDpad
import com.jio.remote.plus.ui.components.JioRockerSwitch
import com.jio.remote.plus.ui.components.performTickHaptic

@Composable
fun JioReplicaModeScreen(
    onSendKey: (RemoteKey) -> Unit,
    onLaunchApp: (RemoteAppShortcut) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Physical Remote Chassis Housing
        Box(
            modifier = Modifier
                .width(310.dp)
                .shadow(20.dp, RoundedCornerShape(42.dp), ambientColor = Color.Black, spotColor = Color(0xFF001122))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E222B),
                            Color(0xFF151820),
                            Color(0xFF0D0F14),
                            Color(0xFF151820)
                        )
                    ),
                    shape = RoundedCornerShape(42.dp)
                )
                .border(1.5.dp, Color(0xFF2E3544), RoundedCornerShape(42.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Indicator LED + Power + Mute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFFE50914), CircleShape)
                            .clickable {
                                performTickHaptic(context)
                                onSendKey(RemoteKey.POWER)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    // Jio Ambient Sensor / LED Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF0088FF), CircleShape)
                    )

                    // Mute Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFF282E3C), CircleShape)
                            .clickable {
                                performTickHaptic(context)
                                onSendKey(RemoteKey.MUTE)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Branded App Shortcut Pills Row 1: JioCinema & YouTube
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReplicaPillButton(
                        title = "JioCinema",
                        color = Color(0xFFD81B60),
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp(KnownJioApps.JIOCINEMA) }
                    )
                    ReplicaPillButton(
                        title = "YouTube",
                        color = Color(0xFFCC0000),
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp(KnownJioApps.YOUTUBE) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Branded App Shortcut Pills Row 2: Hotstar & JioTV+
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReplicaPillButton(
                        title = "Hotstar",
                        color = Color(0xFF0C2040),
                        textColor = Color(0xFF4DB5FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp(KnownJioApps.HOTSTAR) }
                    )
                    ReplicaPillButton(
                        title = "JioTV+",
                        color = Color(0xFF0055FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp(KnownJioApps.JIOTV_PLUS) }
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Iconic Jio D-Pad
                JioDpad(
                    size = 220.dp,
                    onDirectionClick = { onSendKey(it) }
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Navigation Row: Back, Home, Voice Search Mic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    ReplicaRoundButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { onSendKey(RemoteKey.BACK) }
                    )

                    // Assistant Mic Button (with Google Assistant colored ring)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(6.dp, CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853), Color(0xFF4285F4))
                                ),
                                shape = CircleShape
                            )
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141820))
                            .clickable {
                                performTickHaptic(context)
                                onSendKey(RemoteKey.VOICE_SEARCH)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    // Home
                    ReplicaRoundButton(
                        icon = Icons.Default.Home,
                        onClick = { onSendKey(RemoteKey.HOME) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Volume & Channel Rockers + Play/Pause & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Rocker
                    JioRockerSwitch(
                        title = "VOL",
                        onUp = { onSendKey(RemoteKey.VOLUME_UP) },
                        onDown = { onSendKey(RemoteKey.VOLUME_DOWN) }
                    )

                    // Center column: Settings & Play/Pause
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReplicaRoundButton(
                            icon = Icons.Default.PlayArrow,
                            onClick = { onSendKey(RemoteKey.PLAY_PAUSE) }
                        )
                        ReplicaRoundButton(
                            icon = Icons.Default.Settings,
                            onClick = { onSendKey(RemoteKey.SETTINGS) }
                        )
                    }

                    // Channel Rocker
                    JioRockerSwitch(
                        title = "CH",
                        onUp = { onSendKey(RemoteKey.CHANNEL_UP) },
                        onDown = { onSendKey(RemoteKey.CHANNEL_DOWN) }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Jio Brand Emblem at Bottom
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF0066FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Jio",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplicaPillButton(
    title: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(42.dp)
            .shadow(4.dp, RoundedCornerShape(21.dp))
            .background(color, RoundedCornerShape(21.dp))
            .clip(RoundedCornerShape(21.dp))
            .clickable {
                performTickHaptic(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ReplicaRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF282E3C), Color(0xFF181C26))
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable {
                performTickHaptic(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
