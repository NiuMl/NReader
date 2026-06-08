package com.niuml.nreader.data

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class DocumentLoader(private val file: File) {

    private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF_16LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val UTF_16BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    data class DocumentResult(
        val content: String,
        val encoding: String
    )

    suspend fun load(): DocumentResult {
        return loadSync()
    }
    
    fun loadSync(): DocumentResult {
        val encoding = detectEncodingFast()
        val content = readFileContent(encoding)
        
        Log.d("DocumentLoader", "Loaded file: ${file.name}, encoding: $encoding, length: ${content.length}")
        
        return DocumentResult(
            content = content,
            encoding = encoding
        )
    }

    private fun detectEncodingFast(): String {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            val bytesRead = input.read(buffer)
            
            if (bytesRead >= 3 && buffer[0] == UTF_8_BOM[0] && 
                buffer[1] == UTF_8_BOM[1] && buffer[2] == UTF_8_BOM[2]) {
                return "UTF-8"
            }

            if (bytesRead >= 2) {
                if (buffer[0] == UTF_16LE_BOM[0] && buffer[1] == UTF_16LE_BOM[1]) {
                    return "UTF-16LE"
                }
                if (buffer[0] == UTF_16BE_BOM[0] && buffer[1] == UTF_16BE_BOM[1]) {
                    return "UTF-16BE"
                }
            }

            if (isValidUtf8Sample(buffer, bytesRead)) {
                return "UTF-8"
            }

            val highByteRatio = calculateHighByteRatio(buffer, bytesRead)
            if (highByteRatio > 0.3) {
                return "GBK"
            }

            if (highByteRatio > 0.1) {
                if (detectShiftJisPattern(buffer, bytesRead)) {
                    return "Shift-JIS"
                }
                return "GB18030"
            }

            return "UTF-8"
        }
    }

    private fun isValidUtf8Sample(bytes: ByteArray, length: Int): Boolean {
        val sampleSize = minOf(length, 8192)
        var i = 0
        while (i < sampleSize) {
            val byte = bytes[i].toInt() and 0xFF
            
            when {
                byte < 0x80 -> i++
                byte < 0xE0 -> {
                    if (i + 1 >= sampleSize) return true
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    i += 2
                }
                byte < 0xF0 -> {
                    if (i + 2 >= sampleSize) return true
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    i += 3
                }
                byte < 0xF8 -> {
                    if (i + 3 >= sampleSize) return true
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 3].toInt() and 0xC0) != 0x80) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun calculateHighByteRatio(bytes: ByteArray, length: Int): Double {
        val sampleSize = minOf(length, 1024)
        var highByteCount = 0
        for (i in 0 until sampleSize) {
            if ((bytes[i].toInt() and 0xFF) >= 0x80) {
                highByteCount++
            }
        }
        return highByteCount.toDouble() / sampleSize
    }

    private fun detectShiftJisPattern(bytes: ByteArray, length: Int): Boolean {
        val sampleSize = minOf(length - 1, 1024)
        for (i in 0 until sampleSize) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            
            if (((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xFC)) &&
                ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFC))) {
                return true
            }
        }
        return false
    }

    private fun readFileContent(encoding: String): String {
        return try {
            val charset = when (encoding) {
                "UTF-8" -> StandardCharsets.UTF_8
                "UTF-16LE" -> Charset.forName("UTF-16LE")
                "UTF-16BE" -> Charset.forName("UTF-16BE")
                "GBK" -> Charset.forName("GBK")
                "GB18030" -> Charset.forName("GB18030")
                "Shift-JIS" -> Charset.forName("Shift-JIS")
                else -> StandardCharsets.UTF_8
            }
            
            FileInputStream(file).use { input ->
                val buffer = ByteArray(64 * 1024)
                val stringBuilder = StringBuilder()
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    stringBuilder.append(String(buffer, 0, bytesRead, charset))
                }
                
                stringBuilder.toString()
            }
        } catch (e: Exception) {
            Log.e("DocumentLoader", "Error reading file", e)
            ""
        }
    }

    companion object {
        fun isTxtFile(file: File): Boolean {
            return file.extension.equals("txt", ignoreCase = true)
        }

        fun isEpubFile(file: File): Boolean {
            return file.extension.equals("epub", ignoreCase = true)
        }
    }
}
