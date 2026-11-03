package com.jio.remote.plus.protocol

import java.io.InputStream
import java.io.OutputStream

object VarintUtil {
    fun writeVarint(out: OutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    fun readVarint(input: InputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input.read()
            if (b == -1) throw java.io.EOFException("Unexpected EOF reading varint")
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Varint too long")
    }

    fun writeFieldVarint(out: OutputStream, fieldNumber: Int, value: Long) {
        val tag = (fieldNumber shl 3) or 0 // wire type 0 = varint
        writeVarint(out, tag)
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    fun writeFieldString(out: OutputStream, fieldNumber: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val tag = (fieldNumber shl 3) or 2 // wire type 2 = length-delimited
        writeVarint(out, tag)
        writeVarint(out, bytes.size)
        out.write(bytes)
    }

    fun writeFieldBytes(out: OutputStream, fieldNumber: Int, bytes: ByteArray) {
        val tag = (fieldNumber shl 3) or 2 // wire type 2 = length-delimited
        writeVarint(out, tag)
        writeVarint(out, bytes.size)
        out.write(bytes)
    }

    fun writeFieldMessage(out: OutputStream, fieldNumber: Int, messageBytes: ByteArray) {
        val tag = (fieldNumber shl 3) or 2 // wire type 2 = length-delimited
        writeVarint(out, tag)
        writeVarint(out, messageBytes.size)
        out.write(messageBytes)
    }
}
