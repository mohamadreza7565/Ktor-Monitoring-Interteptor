package app.mra.ktor.monitoring.interceptor.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class FileTypeInfo(
    val extension: String,
    val mimeType: String,
    val isAudio: Boolean
)

object AudioCacheManager {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun detectFileType(bytes: ByteArray, mimeTypeHeader: String? = null): FileTypeInfo {
        if (!mimeTypeHeader.isNullOrBlank()) {
            val lower = mimeTypeHeader.lowercase()
            if (lower.contains("audio/flac") || lower.contains("flac")) return FileTypeInfo("flac", "audio/flac", isAudio = true)
            if (lower.contains("audio/mpeg") || lower.contains("mp3")) return FileTypeInfo("mp3", "audio/mpeg", isAudio = true)
            if (lower.contains("audio/wav") || lower.contains("wave")) return FileTypeInfo("wav", "audio/wav", isAudio = true)
            if (lower.contains("audio/aac") || lower.contains("aac")) return FileTypeInfo("aac", "audio/aac", isAudio = true)
            if (lower.contains("audio/ogg") || lower.contains("ogg")) return FileTypeInfo("ogg", "audio/ogg", isAudio = true)
            if (lower.contains("audio/m4a") || lower.contains("m4a")) return FileTypeInfo("m4a", "audio/mp4", isAudio = true)
            if (lower.startsWith("image/")) return FileTypeInfo("img", lower, isAudio = false)
            if (lower.startsWith("application/pdf")) return FileTypeInfo("pdf", "application/pdf", isAudio = false)
        }

        if (bytes.size < 4) return FileTypeInfo("wav", "audio/wav", isAudio = true)

        if (bytes[0] == 'f'.code.toByte() && bytes[1] == 'L'.code.toByte() && bytes[2] == 'a'.code.toByte() && bytes[3] == 'C'.code.toByte()) {
            return FileTypeInfo("flac", "audio/flac", isAudio = true)
        }

        if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return FileTypeInfo("mp3", "audio/mpeg", isAudio = true)
        }

        if ((bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0) {
            return FileTypeInfo("mp3", "audio/mpeg", isAudio = true)
        }

        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return FileTypeInfo("wav", "audio/wav", isAudio = true)
        }

        if (bytes[0] == 'O'.code.toByte() && bytes[1] == 'g'.code.toByte() && bytes[2] == 'g'.code.toByte() && bytes[3] == 'S'.code.toByte()) {
            return FileTypeInfo("ogg", "audio/ogg", isAudio = true)
        }

        if (bytes.size >= 8 && bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() && bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()) {
            return FileTypeInfo("m4a", "audio/mp4", isAudio = true)
        }

        if ((bytes[0].toInt() and 0xFF) == 0x89 && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) {
            return FileTypeInfo("png", "image/png", isAudio = false)
        }

        if ((bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xFF) == 0xD8 && (bytes[2].toInt() and 0xFF) == 0xFF) {
            return FileTypeInfo("jpg", "image/jpeg", isAudio = false)
        }

        if (bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return FileTypeInfo("pdf", "application/pdf", isAudio = false)
        }

        return FileTypeInfo("pcm", "audio/wav", isAudio = true)
    }

    fun saveAudioBytes(context: Context? = null, bytes: ByteArray, fileInfo: FileTypeInfo = detectFileType(bytes)): String? {
        val targetContext = context?.applicationContext ?: appContext ?: return null
        return runCatching {
            val cacheDir = File(targetContext.cacheDir, "ktor_inspector_files").apply { if (!exists()) mkdirs() }
            val file = File(cacheDir, "file_${UUID.randomUUID()}.${fileInfo.extension}")

            val finalBytes = if (fileInfo.extension == "pcm") {
                addWavHeaderToPcm(bytes, 16000, 1, 16)
            } else {
                bytes
            }

            FileOutputStream(file).use { it.write(finalBytes) }
            file.absolutePath
        }.getOrNull()
    }

    private fun addWavHeaderToPcm(pcmBytes: ByteArray, sampleRate: Int = 16000, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val totalDataLen = pcmBytes.size.toLong()
        val totalAudioLen = totalDataLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = (totalAudioLen shr 8 and 0xff).toByte()
        header[6] = (totalAudioLen shr 16 and 0xff).toByte()
        header[7] = (totalAudioLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = (totalDataLen shr 8 and 0xff).toByte()
        header[42] = (totalDataLen shr 16 and 0xff).toByte()
        header[43] = (totalDataLen shr 24 and 0xff).toByte()

        return header + pcmBytes
    }

    fun isAudioContentType(contentType: String?): Boolean {
        if (contentType == null) return false
        val lower = contentType.lowercase()
        return lower.contains("audio/") || lower.contains("mpeg") || lower.contains("wav") || lower.contains("aac") || lower.contains("flac") || lower.contains("ogg")
    }
}
