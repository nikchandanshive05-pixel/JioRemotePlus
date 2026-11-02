package com.jio.remote.plus.protocol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Direct ADB Fallback Controller over Wi-Fi (Port 5555).
 * Allows sending instant keyevents, typing strings, and launching packages
 * directly without Google TV pairing dialogs if ADB Wi-Fi debugging is enabled.
 */
class AdbFallbackClient {
    private val tag = "AdbFallbackClient"

    suspend fun sendAdbCommand(host: String, command: String, port: Int = 5555): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2000)
            socket.soTimeout = 2000

            val out: OutputStream = socket.getOutputStream()
            // Send direct shell invocation or raw adb shell command format
            out.write("$command\n".toByteArray(Charsets.UTF_8))
            out.flush()
            true
        } catch (e: Exception) {
            Log.d(tag, "ADB connect to $host:$port ($command): ${e.message}")
            false
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun sendKeyEvent(host: String, keyCode: Int): Boolean {
        return sendAdbCommand(host, "input keyevent $keyCode")
    }

    suspend fun sendText(host: String, text: String): Boolean {
        val sanitized = text.replace(" ", "%s").replace("\"", "\\\"")
        return sendAdbCommand(host, "input text \"$sanitized\"")
    }

    suspend fun launchApp(host: String, packageName: String): Boolean {
        return sendAdbCommand(host, "monkey -p $packageName -c android.intent.category.LAUNCHER 1")
    }
}
