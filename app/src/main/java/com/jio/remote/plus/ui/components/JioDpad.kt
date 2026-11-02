package com.jio.remote.plus.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jio.remote.plus.protocol.RemoteKey

fun performTickHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    } catch (_: Exception) {}
}

@Composable
fun JioDpad(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    onDirectionClick: (RemoteKey) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, ambientColor = Color.Black, spotColor = Color(0xFF0078FF))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C3240), Color(0xFF161A22), Color(0xFF0D0F14))
                ),
                shape = CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Metallic Blue Accent Outer Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF0066FF),
                            Color(0xFF00C6FF),
                            Color(0xFF0044BB),
                            Color(0xFF0066FF)
                        )
                    )
                )
        )

        // Inner Matte Wheel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF262C38), Color(0xFF181C24))
                    )
                )
        )

        // Up Arrow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    performTickHaptic(context)
                    onDirectionClick(RemoteKey.UP)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp))
        }

        // Down Arrow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    performTickHaptic(context)
                    onDirectionClick(RemoteKey.DOWN)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp))
        }

        // Left Arrow
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    performTickHaptic(context)
                    onDirectionClick(RemoteKey.LEFT)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp))
        }

        // Right Arrow
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    performTickHaptic(context)
                    onDirectionClick(RemoteKey.RIGHT)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp))
        }

        // Center OK Button
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(6.dp, CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E3544), Color(0xFF151820))
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable {
                    performTickHaptic(context)
                    onDirectionClick(RemoteKey.CENTER)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
