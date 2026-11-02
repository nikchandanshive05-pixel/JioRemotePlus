package com.jio.remote.plus.protocol

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

data class DiscoveredSTB(
    val name: String,
    val host: InetAddress?,
    val port: Int,
    val isJioDevice: Boolean = false
)

class JioDeviceDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val tag = "JioDeviceDiscovery"

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredSTB>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredSTB>> = _discoveredDevices.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    fun startDiscovery() {
        if (isDiscovering) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "Discovery start failed: $errorCode")
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(tag, "Discovery started for $serviceType")
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(tag, "Discovery stopped")
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { resolveService(it) }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { lost ->
                    _discoveredDevices.value = _discoveredDevices.value.filterNot { it.name == lost.serviceName }
                }
            }
        }

        try {
            // Android TV Remote v2 standard service type
            nsdManager.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start service discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "Resolve failed for ${info?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(info: NsdServiceInfo?) {
                if (info == null) return
                val isJio = info.serviceName.contains("Jio", ignoreCase = true) ||
                        info.serviceName.contains("AirFiber", ignoreCase = true) ||
                        info.serviceName.contains("STB", ignoreCase = true)
                
                val stb = DiscoveredSTB(
                    name = info.serviceName,
                    host = info.host,
                    port = info.port,
                    isJioDevice = isJio
                )

                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.name == stb.name }
                current.add(0, stb)
                _discoveredDevices.value = current
            }
        })
    }

    fun stopDiscovery() {
        if (!isDiscovering) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery", e)
        } finally {
            isDiscovering = false
        }
    }
}
