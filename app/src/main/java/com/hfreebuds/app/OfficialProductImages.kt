package com.hfreebuds.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** Identity is accepted only when all independently reported model fields agree. */
internal data class ProductIdentity(val model: String, val colour: String) {
    companion object {
        fun fromPacket(packet: HuaweiPacket): ProductIdentity? {
            if (packet.command != Cmd.DEVICE_INFO) return null
            val full = packet.params[10]?.toString(Charsets.US_ASCII) ?: return null
            val match = Regex("^([A-Z0-9]+)-([0-9A-F]{6})$").matchEntire(full) ?: return null
            val model = match.groupValues[2]
            if (model == "00000A") return null
            val prefix = packet.params[15]?.toString(Charsets.US_ASCII) ?: return null
            val code = packet.params[2] ?: return null
            val colour = packet.params[25] ?: return null
            if (prefix != match.groupValues[1] || code.size != 2 || colour.size != 1) return null
            val codeHex = code.joinToString("") { "%02X".format(it.toInt() and 255) }
            if (codeHex != model.takeLast(4)) return null
            return ProductIdentity(model, "%02X".format(colour[0].toInt() and 255))
        }
    }
}

/** Loads only the identified model and colour from Huawei's public resource catalog. */
internal object OfficialProductImages {
    private const val base = "https://smarthome-drcn.dbankcdn.cn/device/guide/AAM001/"
    private const val maxConfig = 512 * 1024
    private const val maxArchive = 64 * 1024 * 1024
    private const val maxImage = 8 * 1024 * 1024
    private const val maxExpanded = 128L * 1024 * 1024
    private val names = listOf("iv_device_logo.png", "device_icon.png", "mediacontroller_icon.png", "ota_device_icon.png", "ota_device.png")

    fun archiveName(identity: ProductIdentity, json: String): String? = runCatching {
        if (!identity.model.matches(Regex("[0-9A-F]{6}")) ||
            !identity.colour.matches(Regex("[0-9A-F]{2}")) || identity.model == "00000A") return null
        val root = JSONObject(json)
        if (root.optString("modelId") != identity.model) return null
        val entry = root.getJSONObject("resourceMap").getJSONObject(identity.colour)
        if (entry.has("resourceKey") && entry.optString("resourceKey") != identity.colour) return null
        val expected = "${identity.model}_${identity.colour}.zip"
        if (entry.optString("resourceRemark") != expected) return null
        if (entry.optInt("resourceFileSizeKb", 0) !in 1..(maxArchive / 1024)) return null
        expected
    }.getOrNull()

    fun load(cacheDir: File, identity: ProductIdentity): Bitmap? = runCatching {
        if (!identity.model.matches(Regex("[0-9A-F]{6}")) ||
            !identity.colour.matches(Regex("[0-9A-F]{2}")) || identity.model == "00000A") return null
        val directory = File(cacheDir, "huawei_product_images")
        if (!directory.isDirectory && !directory.mkdirs()) return null
        val cached = File(directory, "${identity.model}_${identity.colour}.png")
        if (cached.isFile && cached.length() in 1..maxImage.toLong()) {
            decode(cached.readBytes())?.let { return it }
            cached.delete()
        }
        val prefix = "$base${identity.model}/"
        val config = request(prefix + identity.model + ".json") { it.readBounded(maxConfig) } ?: return null
        val archive = archiveName(identity, config.toString(Charsets.UTF_8)) ?: return null
        val bytes = request(prefix + archive) { extractImage(it) } ?: return null
        val image = decode(bytes) ?: return null
        val tmp = File.createTempFile("image_", ".tmp", directory)
        try {
            tmp.writeBytes(bytes)
            tmp.renameTo(cached)
        } finally {
            tmp.delete()
        }
        image
    }.getOrNull()

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outMimeType != "image/png" || bounds.outWidth !in 1..4096 ||
            bounds.outHeight !in 1..4096 || bounds.outWidth.toLong() * bounds.outHeight > 16_777_216L) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = if (maxOf(bounds.outWidth, bounds.outHeight) > 2048) 2 else 1
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun <T> request(url: String, block: (InputStream) -> T): T? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            if (connection.responseCode != 200 || connection.contentLengthLong > maxArchive) return null
            connection.inputStream.use { block(it) }
        } finally {
            connection.disconnect()
        }
    }

    /** Scan entries without writing paths; cap compressed and decompressed data. */
    private fun extractImage(input: InputStream): ByteArray? {
        val bounded = object : java.io.FilterInputStream(input) {
            var count = 0L
            override fun read(): Int {
                val result = super.read()
                if (result >= 0 && ++count > maxArchive) error("archive too large")
                return result
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, len)
                if (n > 0) { count += n; if (count > maxArchive) error("archive too large") }
                return n
            }
        }
        var expanded = 0L
        var count = 0
        var best = names.size
        var result: ByteArray? = null
        ZipInputStream(bounded).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > 2048) error("too many entries")
                val name = entry.name.replace('\\', '/').substringAfterLast('/').lowercase()
                val priority = names.indexOf(name).let { if (it < 0) names.size else it }
                val capture = !entry.isDirectory && priority < best
                val out = if (capture) ByteArrayOutputStream() else null
                val buffer = ByteArray(8192)
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    expanded += n
                    if (expanded > maxExpanded) error("expanded archive too large")
                    if (out != null) {
                        if (out.size() + n > maxImage) error("image too large")
                        out.write(buffer, 0, n)
                    }
                }
                if (out != null) { result = out.toByteArray(); best = priority }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            if (out.size() + n > limit) error("response too large")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
