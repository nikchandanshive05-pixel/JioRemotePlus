package com.jio.remote.plus.protocol

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Implements Google TV / Android TV Remote Protocol v2:
 * 1. Port 6467: Polo Pairing Protocol (shows 6-digit code on TV, user enters in app)
 * 2. Port 6466: Remote Control Protocol (Protobuf commands: keys, apps, text)
 */
class AndroidTvRemoteManager(private val context: Context) {
    private val tag = "AndroidTvRemoteMgr"

    private var clientCert: X509Certificate? = null
    private var clientPrivateKey: PrivateKey? = null
    private var sslContext: SSLContext? = null

    // Active remote command socket (port 6466)
    private var commandSocket: SSLSocket? = null
    private var commandOut: OutputStream? = null
    private var commandIn: InputStream? = null
    private var isConfigured = false

    private val prefs = context.getSharedPreferences("atv_remote_v2_prefs", Context.MODE_PRIVATE)

    suspend fun getOrGenerateCertificates() = withContext(Dispatchers.IO) {
        val savedCertB64 = prefs.getString("client_cert_der", null)
        val savedKeyB64 = prefs.getString("client_key_der", null)

        if (savedCertB64 != null && savedKeyB64 != null) {
            try {
                val certBytes = Base64.decode(savedCertB64, Base64.DEFAULT)
                val keyBytes = Base64.decode(savedKeyB64, Base64.DEFAULT)

                val cf = CertificateFactory.getInstance("X.509")
                clientCert = cf.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

                val kf = KeyFactory.getInstance("RSA")
                clientPrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
                initSslContext()
                return@withContext
            } catch (e: Exception) {
                Log.e(tag, "Failed loading saved certs, generating new ones", e)
            }
        }

        // Generate RSA 2048 keypair
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp: KeyPair = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 1000L * 60 * 60 * 24)
        val notAfter = Date(now + 1000L * 60 * 60 * 24 * 365 * 10)

        val certBuilder = JcaX509v3CertificateBuilder(
            X500Name("CN=JioRemotePlus"),
            BigInteger.valueOf(now),
            notBefore,
            notAfter,
            X500Name("CN=JioRemotePlus"),
            kp.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(kp.private)
        val certHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        clientCert = cert
        clientPrivateKey = kp.private

        prefs.edit()
            .putString("client_cert_der", Base64.encodeToString(cert.encoded, Base64.NO_WRAP))
            .putString("client_key_der", Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
            .apply()

        initSslContext()
    }

    private fun initSslContext() {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        ks.setKeyEntry("client", clientPrivateKey, "jio123".toCharArray(), arrayOf(clientCert))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, "jio123".toCharArray())

        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val ctx = SSLContext.getInstance("TLS")
        ctx.init(kmf.keyManagers, trustAll, SecureRandom())
        sslContext = ctx
    }

    // ==========================================
    // PAIRING FLOW (Port 6467)
    // ==========================================

    private var pairingSocket: SSLSocket? = null
    private var pairingOut: OutputStream? = null
    private var pairingIn: InputStream? = null
    private var serverCert: X509Certificate? = null

    /**
     * Step 1: Connect to TV on port 6467 and initiate pairing handshake.
     * This will immediately cause the TV to display the 6-character code!
     */
    suspend fun startPairing(host: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (sslContext == null) getOrGenerateCertificates()
            disconnectPairing()

            val sock = sslContext!!.socketFactory.createSocket() as SSLSocket
            sock.connect(InetSocketAddress(host, 6467), 4000)
            sock.soTimeout = 10000
            sock.startHandshake()

            val peerCerts = sock.session.peerCertificates
            if (peerCerts.isNotEmpty() && peerCerts[0] is X509Certificate) {
                serverCert = peerCerts[0] as X509Certificate
            }

            pairingSocket = sock
            pairingOut = sock.outputStream
            pairingIn = sock.inputStream

            // Send 1: PairingRequest (service_name="atvremote", client_name="JioRemotePlus")
            val pReqBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldString(pReqBuf, 1, "atvremote")
            VarintUtil.writeFieldString(pReqBuf, 2, "JioRemotePlus")

            val outer1 = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(outer1, 1, 2) // protocol_version = 2
            VarintUtil.writeFieldVarint(outer1, 2, 200) // status = STATUS_OK (200)
            VarintUtil.writeFieldMessage(outer1, 10, pReqBuf.toByteArray()) // pairing_request = 10
            sendDelimitedMessage(pairingOut!!, outer1.toByteArray())

            // Read PairingRequestAck from TV
            val ackMsg = readDelimitedMessage(pairingIn!!)
            Log.d(tag, "Received PairingRequestAck (${ackMsg.size} bytes)")

            // Send 2: Options (role=INPUT, encoding=HEXADECIMAL, length=6)
            val encBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(encBuf, 1, 3) // ENCODING_TYPE_HEXADECIMAL = 3
            VarintUtil.writeFieldVarint(encBuf, 2, 6) // symbol_length = 6

            val optBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(optBuf, 1, encBuf.toByteArray()) // input_encodings
            VarintUtil.writeFieldVarint(optBuf, 3, 1) // preferred_role = ROLE_TYPE_INPUT (1)

            val outer2 = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(outer2, 1, 2)
            VarintUtil.writeFieldVarint(outer2, 2, 200)
            VarintUtil.writeFieldMessage(outer2, 20, optBuf.toByteArray()) // options = 20
            sendDelimitedMessage(pairingOut!!, outer2.toByteArray())

            // Read Options from TV
            readDelimitedMessage(pairingIn!!)

            // Send 3: Configuration (role=INPUT, encoding=HEXADECIMAL(3), length=6)
            val cfgBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(cfgBuf, 1, encBuf.toByteArray()) // encoding
            VarintUtil.writeFieldVarint(cfgBuf, 2, 1) // client_role = ROLE_TYPE_INPUT (1)

            val outer3 = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(outer3, 1, 2)
            VarintUtil.writeFieldVarint(outer3, 2, 200)
            VarintUtil.writeFieldMessage(outer3, 30, cfgBuf.toByteArray()) // configuration = 30
            sendDelimitedMessage(pairingOut!!, outer3.toByteArray())

            // Read ConfigurationAck from TV -> At this exact instant, the TV displays the code!
            readDelimitedMessage(pairingIn!!)
            Log.i(tag, "Pairing initiated successfully! Code is now visible on TV.")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start pairing", e)
            disconnectPairing()
            Result.failure(e)
        }
    }

    /**
     * Step 2: User enters the 6-character code shown on the TV screen.
     */
    suspend fun finishPairing(pairingCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val out = pairingOut ?: throw IllegalStateException("Pairing session not started")
            val input = pairingIn ?: throw IllegalStateException("Pairing session not started")
            val code = pairingCode.trim().uppercase()

            if (code.length != 6) {
                return@withContext Result.failure(IllegalArgumentException("Code must be 6 characters"))
            }

            val clientPub = clientCert?.publicKey as? RSAPublicKey ?: throw IllegalStateException("Client cert missing")
            val sCert = serverCert ?: throw IllegalStateException("Server cert missing")
            val serverPub = sCert.publicKey as? RSAPublicKey ?: throw IllegalStateException("Server cert not RSA")

            // Hash algorithm: SHA-256 over:
            // client_modulus + client_exponent + server_modulus + server_exponent + code[2:]
            val md = MessageDigest.getInstance("SHA-256")
            md.update(clientPub.modulus.toByteArray().stripLeadingZero())
            md.update(clientPub.publicExponent.toByteArray().stripLeadingZero())
            md.update(serverPub.modulus.toByteArray().stripLeadingZero())
            md.update(serverPub.publicExponent.toByteArray().stripLeadingZero())
            md.update(hexStringToByteArray(code.substring(2)))

            val hashResult = md.digest()

            // Verify first byte matches code[0:2]
            val expectedFirstByte = code.substring(0, 2).toInt(16).toByte()
            if (hashResult[0] != expectedFirstByte) {
                Log.w(tag, "Code verification hash mismatch: computed=${hashResult[0]}, expected=$expectedFirstByte")
            }

            // Send 4: Secret
            val secBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldBytes(secBuf, 1, hashResult)

            val outer4 = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(outer4, 1, 2)
            VarintUtil.writeFieldVarint(outer4, 2, 200)
            VarintUtil.writeFieldMessage(outer4, 40, secBuf.toByteArray()) // secret = 40
            sendDelimitedMessage(out, outer4.toByteArray())

            // Read SecretAck
            val secretAckMsg = readDelimitedMessage(input)
            Log.i(tag, "Pairing completed successfully! (${secretAckMsg.size} bytes response)")

            // Save paired state
            prefs.edit().putBoolean("is_paired", true).apply()
            disconnectPairing()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to finish pairing", e)
            disconnectPairing()
            Result.failure(e)
        }
    }

    private fun disconnectPairing() {
        try { pairingSocket?.close() } catch (_: Exception) {}
        pairingSocket = null
        pairingOut = null
        pairingIn = null
    }

    // ==========================================
    // COMMAND PROTOCOL (Port 6466)
    // ==========================================

    suspend fun connectCommands(host: String): Boolean = withContext(Dispatchers.IO) {
        if (commandSocket != null && commandSocket?.isConnected == true && isConfigured) {
            return@withContext true
        }
        try {
            if (sslContext == null) getOrGenerateCertificates()
            disconnectCommands()

            val sock = sslContext!!.socketFactory.createSocket() as SSLSocket
            sock.connect(InetSocketAddress(host, 6466), 4000)
            sock.soTimeout = 5000
            sock.startHandshake()

            commandSocket = sock
            commandOut = sock.outputStream
            commandIn = sock.inputStream

            // Handshake 1: RemoteConfigure (code1=622, model="JioRemotePlus", vendor="Android")
            val devInfoBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldString(devInfoBuf, 1, "JioRemotePlus")
            VarintUtil.writeFieldString(devInfoBuf, 2, "Android")
            VarintUtil.writeFieldVarint(devInfoBuf, 3, 1)
            VarintUtil.writeFieldString(devInfoBuf, 4, "1.0.0")
            VarintUtil.writeFieldString(devInfoBuf, 5, "com.jio.remote.plus")
            VarintUtil.writeFieldString(devInfoBuf, 6, "1.0.0")

            val cfgBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(cfgBuf, 1, 622)
            VarintUtil.writeFieldMessage(cfgBuf, 2, devInfoBuf.toByteArray())

            val msg1 = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(msg1, 1, cfgBuf.toByteArray()) // remote_configure = 1
            sendDelimitedMessage(commandOut!!, msg1.toByteArray())

            // Read TV configure response
            readDelimitedMessage(commandIn!!)

            // Handshake 2: RemoteSetActive (active=622)
            val activeBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(activeBuf, 1, 622)

            val msg2 = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(msg2, 2, activeBuf.toByteArray()) // remote_set_active = 2
            sendDelimitedMessage(commandOut!!, msg2.toByteArray())

            isConfigured = true
            Log.i(tag, "Command socket connected & configured on port 6466!")
            true
        } catch (e: Exception) {
            Log.e(tag, "Command connection failed to $host:6466", e)
            disconnectCommands()
            false
        }
    }

    suspend fun sendKey(host: String, keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connectCommands(host)) return@withContext false
            val out = commandOut ?: return@withContext false

            // RemoteKeyInject message: key_code (1), direction: SHORT = 3 (or START_LONG=1, END_LONG=2)
            val keyInjectBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldVarint(keyInjectBuf, 1, keyCode.toLong())
            VarintUtil.writeFieldVarint(keyInjectBuf, 2, 3) // SHORT = 3

            val outer = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(outer, 10, keyInjectBuf.toByteArray()) // remote_key_inject = 10
            sendDelimitedMessage(out, outer.toByteArray())
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed sending key $keyCode", e)
            disconnectCommands()
            false
        }
    }

    suspend fun launchAppUri(host: String, appUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connectCommands(host)) return@withContext false
            val out = commandOut ?: return@withContext false

            val launchBuf = ByteArrayOutputStream()
            VarintUtil.writeFieldString(launchBuf, 1, appUri)

            val outer = ByteArrayOutputStream()
            VarintUtil.writeFieldMessage(outer, 90, launchBuf.toByteArray()) // remote_app_link_launch_request = 90
            sendDelimitedMessage(out, outer.toByteArray())
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed launching app $appUri", e)
            disconnectCommands()
            false
        }
    }

    fun disconnectCommands() {
        try { commandSocket?.close() } catch (_: Exception) {}
        commandSocket = null
        commandOut = null
        commandIn = null
        isConfigured = false
    }

    // ==========================================
    // WIRE PROTOCOL HELPERS
    // ==========================================

    private fun sendDelimitedMessage(out: OutputStream, payload: ByteArray) {
        VarintUtil.writeVarint(out, payload.size)
        out.write(payload)
        out.flush()
    }

    private fun readDelimitedMessage(input: InputStream): ByteArray {
        val length = VarintUtil.readVarint(input)
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(buffer, totalRead, length - totalRead)
            if (read == -1) throw java.io.EOFException("Stream ended before full message read")
            totalRead += read
        }
        return buffer
    }

    private fun ByteArray.stripLeadingZero(): ByteArray {
        return if (this.isNotEmpty() && this[0] == 0.toByte()) {
            this.copyOfRange(1, this.size)
        } else {
            this
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
