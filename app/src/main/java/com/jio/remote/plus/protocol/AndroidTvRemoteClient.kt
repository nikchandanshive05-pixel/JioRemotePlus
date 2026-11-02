package com.jio.remote.plus.protocol

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Handles Android TV Remote Protocol v2 pairing and secure commands.
 * Connects over TLS (port 6466 for pairing, 6467 for commands).
 */
class AndroidTvRemoteClient(private val context: Context) {
    private val tag = "TvRemoteClient"
    private var sslSocket: SSLSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var keyStore: KeyStore? = null

    suspend fun initializeKeyStore() = withContext(Dispatchers.IO) {
        val ks = KeyStore.getInstance("PKCS12")
        val prefs = context.getSharedPreferences("jio_remote_certs", Context.MODE_PRIVATE)

        // Generate self-signed client certificate if not already saved
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val keyPair: KeyPair = keyPairGen.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 1000L * 60 * 60 * 24)
        val notAfter = Date(now + 1000L * 60 * 60 * 24 * 365 * 10) // 10 years

        val certBuilder = JcaX509v3CertificateBuilder(
            X500Name("CN=JioRemotePlus"),
            BigInteger.valueOf(now),
            notBefore,
            notAfter,
            X500Name("CN=JioRemotePlus"),
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        ks.load(null, null)
        ks.setKeyEntry("client", keyPair.private, "jiorun".toCharArray(), arrayOf(cert))
        keyStore = ks
    }

    private fun getTrustAllSSLContext(): SSLContext {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, "jiorun".toCharArray())

        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, trustAll, SecureRandom())
        return sslContext
    }

    suspend fun connect(host: String, port: Int = 6467): Boolean = withContext(Dispatchers.IO) {
        try {
            if (keyStore == null) initializeKeyStore()
            val sslContext = getTrustAllSSLContext()
            val socketFactory = sslContext.socketFactory
            val sock = socketFactory.createSocket(host, port) as SSLSocket
            sock.startHandshake()

            sslSocket = sock
            outputStream = sock.outputStream
            inputStream = sock.inputStream
            Log.i(tag, "Successfully connected to $host:$port via TLS")
            true
        } catch (e: Exception) {
            Log.e(tag, "TLS connection failed to $host:$port", e)
            false
        }
    }

    suspend fun sendKeyCode(keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext false
        try {
            // Android TV Remote v2 proto frame: KeyDown followed by KeyUp
            // Packet format: [Payload Size Header] [Wire bytes]
            val packetDown = encodeKeyEvent(keyCode, isDown = true)
            out.write(packetDown)
            out.flush()

            Thread.sleep(50)

            val packetUp = encodeKeyEvent(keyCode, isDown = false)
            out.write(packetUp)
            out.flush()
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send keycode $keyCode", e)
            false
        }
    }

    private fun encodeKeyEvent(keyCode: Int, isDown: Boolean): ByteArray {
        // Tag 82: RemoteKeyCode / KeyEvent message structure in TV Remote v2 Proto
        val action = if (isDown) 1 else 2 // 1: START/DOWN, 2: END/UP
        val keyPayload = byteArrayOf(
            0x08.toByte(), action.toByte(),
            0x10.toByte(), (keyCode and 0x7F).toByte(),
            if (keyCode > 0x7F) ((keyCode shr 7) and 0x7F).toByte() else 0
        ).filterIndexed { index, b -> index < 4 || b != 0.toByte() }.toByteArray()

        val msg = byteArrayOf(
            0x52.toByte(), // field tag
            keyPayload.size.toByte()
        ) + keyPayload

        return byteArrayOf(msg.size.toByte()) + msg
    }

    fun disconnect() {
        try {
            outputStream?.close()
            inputStream?.close()
            sslSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing connection", e)
        } finally {
            sslSocket = null
            outputStream = null
            inputStream = null
        }
    }
}
