package com.hfreebuds.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductIdentityTest {
    private fun device(model: String = "BTFT0013-000141", prefix: String = "BTFT0013",
                       code: ByteArray = byteArrayOf(0x01, 0x41),
                       colour: ByteArray = byteArrayOf(0x01)): HuaweiPacket =
        HuaweiPacket(Cmd.DEVICE_INFO, linkedMapOf(
            2 to code, 10 to model.toByteArray(Charsets.US_ASCII),
            15 to prefix.toByteArray(Charsets.US_ASCII), 25 to colour,
        ))

    @Test fun extractsModelAndColour() {
        assertEquals(ProductIdentity("000141", "01"), ProductIdentity.fromPacket(device()))
        assertEquals(ProductIdentity("000141", "FF"), ProductIdentity.fromPacket(device(colour = byteArrayOf(0xff.toByte()))))
    }

    @Test fun rejectsInconsistentOrIncompleteIdentity() {
        assertNull(ProductIdentity.fromPacket(device(prefix = "OTHER")))
        assertNull(ProductIdentity.fromPacket(device(code = byteArrayOf(0x01, 0x42))))
        assertNull(ProductIdentity.fromPacket(device(colour = byteArrayOf())))
        assertNull(ProductIdentity.fromPacket(device(model = "BTFT0013-00000A", code = byteArrayOf(0, 10))))
        assertNull(ProductIdentity.fromPacket(device(model = "../000141")))
        assertNull(ProductIdentity.fromPacket(HuaweiPacket(Cmd.BATTERY_READ, device().params)))
    }
}
