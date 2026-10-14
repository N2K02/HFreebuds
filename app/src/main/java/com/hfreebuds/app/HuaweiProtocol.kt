package com.hfreebuds.app

/**
 * Huawei earbuds speak a small binary protocol over Bluetooth RFCOMM (serial port profile).
 *
 * Packet: `5A | length (2 bytes, big endian) | 00 | service | command | params… | CRC16`
 * - length counts the `00`, the two command bytes and the params.
 * - params are TLV: `type (1 byte) | length (1 byte) | value`.
 * - CRC16-XMODEM over everything before it.
 *
 * Example (FreeBuds 5, wear detection on): `5A 0006 00 2B10 01 01 01 A956`.
 * A read request lists the wanted param types with empty values (`01 00 02 00 …`);
 * the earbuds answer with the same command and filled-in values.
 */
class HuaweiPacket(val command: Int, val params: LinkedHashMap<Int, ByteArray> = LinkedHashMap()) {

    val service: Int get() = command shr 8
    val id: Int get() = command and 0xFF

    fun param(vararg types: Int): ByteArray? = types.firstNotNullOfOrNull { params[it] }

    /** Single byte param as a signed number (Huawei uses -1 for "off"). */
    fun byte(type: Int): Int? = params[type]?.takeIf { it.size == 1 }?.get(0)?.toInt()

    fun toBytes(): ByteArray {
        val body = ArrayList<Byte>()
        body += (command shr 8).toByte()
        body += command.toByte()
        params.forEach { (type, value) ->
            body += type.toByte()
            body += value.size.toByte()
            value.forEach { body += it }
        }
        val len = body.size + 1
        val head = byteArrayOf(0x5A, (len shr 8).toByte(), len.toByte(), 0x00)
        val noCrc = head + body.toByteArray()
        val crc = crc16(noCrc)
        return noCrc + byteArrayOf((crc shr 8).toByte(), crc.toByte())
    }

    override fun toString(): String =
        "%04X ".format(command) + params.entries.joinToString(" ") { (t, v) -> "$t=${v.hex()}" }

    companion object {
        /** A read request: the wanted param types with empty values. */
        fun read(command: Int, vararg types: Int) =
            HuaweiPacket(command, LinkedHashMap(types.associateWith { ByteArray(0) }))

        /** A write request; Int values become one signed byte. */
        fun write(command: Int, vararg params: Pair<Int, Any>) = HuaweiPacket(
            command,
            LinkedHashMap(params.associate { (t, v) ->
                t to when (v) {
                    is ByteArray -> v
                    is Int -> byteArrayOf(v.toByte())
                    is Boolean -> byteArrayOf(if (v) 1 else 0)
                    is String -> v.toByteArray(Charsets.UTF_8)
                    else -> error("Unsupported param $v")
                }
            }),
        )

        /** Parses one complete frame (checksum already checked). */
        fun parse(frame: ByteArray): HuaweiPacket? {
            if (frame.size < 8 || frame[0] != 0x5A.toByte()) return null
            val command = (frame[4].u8() shl 8) or frame[5].u8()
            val params = LinkedHashMap<Int, ByteArray>()
            var i = 6
            val end = frame.size - 2
            while (i + 2 <= end) {
                val type = frame[i].u8()
                val len = frame[i + 1].u8()
                if (i + 2 + len > end) break
                params[type] = frame.copyOfRange(i + 2, i + 2 + len)
                i += 2 + len
            }
            return HuaweiPacket(command, params)
        }

        fun crc16(data: ByteArray): Int {
            var crc = 0
            for (b in data) {
                crc = crc xor (b.u8() shl 8)
                repeat(8) {
                    crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                    crc = crc and 0xFFFF
                }
            }
            return crc
        }
    }
}

/** Collects bytes from the socket and cuts them into complete, checksum-valid frames. */
class FrameReader {
    private var buf = ByteArray(0)

    fun feed(bytes: ByteArray, n: Int): List<ByteArray> {
        buf += bytes.copyOf(n)
        val out = ArrayList<ByteArray>()
        while (true) {
            val start = buf.indexOfFirst { it == 0x5A.toByte() }
            if (start < 0) { buf = ByteArray(0); break }
            if (start > 0) buf = buf.copyOfRange(start, buf.size)
            if (buf.size < 4) break
            val len = (buf[1].u8() shl 8) or buf[2].u8()
            val total = len + 5
            if (len < 3 || len > 4096) { buf = buf.copyOfRange(1, buf.size); continue }
            if (buf.size < total) break
            val frame = buf.copyOfRange(0, total)
            val crc = (frame[total - 2].u8() shl 8) or frame[total - 1].u8()
            if (HuaweiPacket.crc16(frame.copyOfRange(0, total - 2)) == crc) {
                out += frame
                buf = buf.copyOfRange(total, buf.size)
            } else {
                buf = buf.copyOfRange(1, buf.size) // not a real frame start
            }
        }
        return out
    }
}

/** Command ids (service << 8 | command). Names follow what each one does. */
object Cmd {
    const val DEVICE_INFO = 0x0107
    const val BATTERY_READ = 0x0108
    const val BATTERY_NOTIFY = 0x0127
    const val DOUBLE_TAP_WRITE = 0x011F
    const val DOUBLE_TAP_READ = 0x0120
    const val TRIPLE_TAP_WRITE = 0x0125
    const val TRIPLE_TAP_READ = 0x0126
    const val LANGUAGE_WRITE = 0x0C01
    const val LANGUAGE_READ = 0x0C02
    const val STATE_EVENT = 0x2B03 // in-ear / button events
    const val ANC_WRITE = 0x2B04
    const val AUTO_PAUSE_WRITE = 0x2B10
    const val AUTO_PAUSE_READ = 0x2B11
    const val LONG_TAP_WRITE = 0x2B16
    const val LONG_TAP_READ = 0x2B17
    const val LONG_TAP_ANC_WRITE = 0x2B18
    const val LONG_TAP_ANC_READ = 0x2B19
    const val SWIPE_WRITE = 0x2B1E
    const val SWIPE_READ = 0x2B1F
    const val ANC_READ = 0x2B2A
    const val DUAL_WRITE = 0x2B2E
    const val DUAL_READ = 0x2B2F
    const val DUAL_LIST = 0x2B31
    const val DUAL_PREFERRED = 0x2B32
    const val DUAL_EXECUTE = 0x2B33
    const val DUAL_CHANGED = 0x2B36
    const val EQ_WRITE = 0x2B49
    const val EQ_READ = 0x2B4A
    const val LOW_LATENCY = 0x2B6C
    const val QUALITY_WRITE = 0x2BA2
    const val QUALITY_READ = 0x2BA3
}

internal fun Byte.u8() = toInt() and 0xFF
internal fun ByteArray.hex() = joinToString("") { "%02X".format(it) }
internal fun String.unhex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
