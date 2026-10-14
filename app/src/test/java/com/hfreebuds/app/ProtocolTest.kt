package com.hfreebuds.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Our packets must match, byte for byte, the ones captured from the official app. */
class ProtocolTest {
    private fun hex(p: HuaweiPacket) = p.toBytes().hex()

    @Test fun wearDetectionOn() = assertEquals("5A0006002B10010101A956", hex(HuaweiPacket.write(Cmd.AUTO_PAUSE_WRITE, 1 to true)))
    @Test fun wearDetectionOff() = assertEquals("5A0006002B10010100B977", hex(HuaweiPacket.write(Cmd.AUTO_PAUSE_WRITE, 1 to false)))
    @Test fun hdAudioOn() = assertEquals("5A0006002BA2010101B5EF", hex(HuaweiPacket.write(Cmd.QUALITY_WRITE, 1 to true)))
    @Test fun lowLatencyOn() = assertEquals("5A0006002B6C010101A411", hex(HuaweiPacket.write(Cmd.LOW_LATENCY, 1 to true)))
    @Test fun eqVoices() = assertEquals("5A0006002B49010109AE12", hex(HuaweiPacket.write(Cmd.EQ_WRITE, 1 to 9)))
    @Test fun eqBass() = assertEquals("5A0006002B490101021F79", hex(HuaweiPacket.write(Cmd.EQ_WRITE, 1 to 2)))
    @Test fun ancSmart() = assertEquals("5A0007002B0401020103D17F", hex(HuaweiPacket.write(Cmd.ANC_WRITE, 1 to byteArrayOf(1, 3))))
    @Test fun ancOff() = assertEquals("5A0007002B0401020000D22D", hex(HuaweiPacket.write(Cmd.ANC_WRITE, 1 to byteArrayOf(0, 0))))
    @Test fun ancDefault() = assertEquals("5A0007002B04010201FFFFEC", hex(HuaweiPacket.write(Cmd.ANC_WRITE, 1 to byteArrayOf(1, 0xFF.toByte()))))
    @Test fun batteryRead() = assertEquals("5A0009000108010002000300FBB9", hex(HuaweiPacket.read(Cmd.BATTERY_READ, 1, 2, 3)))
    @Test fun ancRead() = assertEquals("5A0005002B2A0100427E", hex(HuaweiPacket.read(Cmd.ANC_READ, 1)))

    @Test fun readerSplitsAndChecksFrames() {
        val a = "5A0006002B10010101A956".unhex()
        val b = "5A0007002B0401020103D17F".unhex()
        val junk = byteArrayOf(0x01, 0x5A, 0x00)
        val stream = junk + a + b
        val r = FrameReader()
        // Fed in two pieces, as a socket might deliver it.
        val first = r.feed(stream.copyOfRange(0, 9), 9)
        val rest = stream.copyOfRange(9, stream.size)
        val second = r.feed(rest, rest.size)
        val frames = first + second
        assertEquals(2, frames.size)
        val p = HuaweiPacket.parse(frames[1])
        assertNotNull(p)
        assertEquals(Cmd.ANC_WRITE, p!!.command)
        assertEquals("0103", p.params[1]!!.hex())
    }

    @Test fun modelMatching() {
        assertEquals("HUAWEI FreeBuds 5", DeviceProfile.forName("HUAWEI FreeBuds 5").name)
        assertEquals("HUAWEI FreeBuds 5i", DeviceProfile.forName("HUAWEI FreeBuds 5i").name)
        assertEquals("HUAWEI FreeBuds Pro 2", DeviceProfile.forName("HUAWEI FreeBuds Pro 2").name)
        assertEquals("HUAWEI FreeBuds Pro", DeviceProfile.forName("HUAWEI FreeBuds Pro").name)
        assertEquals("HUAWEI FreeClip 2", DeviceProfile.forName("HUAWEI FreeClip 2").name)
        assertEquals("HUAWEI FreeBuds SE 4 ANC", DeviceProfile.forName("HUAWEI FreeBuds SE 4 ANC").name)
    }
}
