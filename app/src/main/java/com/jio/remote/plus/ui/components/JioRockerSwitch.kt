package com.jio.remote.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.jio.remote.plus.protocol.RemoteKey

@Composable
fun JioRockerSwitch(
    title: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(58.dp)
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(29.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2B313E), Color(0xFF181C24))
                ),
                shape = RoundedCornerShape(29.dp)
            )
            .clip(RoundedCornerShape(29.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Plus / Up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    performTickHaptic(context)
                    onUp()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Up", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        }

        // Center Title label (VOL or CH)
        Text(
            text = title,
            color = Color(0xFF0088FF),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        // Bottom Minus / Down
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    performTickHaptic(context)
                    onDown()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Down", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        }
    }
}
