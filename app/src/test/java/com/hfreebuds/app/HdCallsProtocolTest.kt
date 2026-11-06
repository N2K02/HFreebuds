package com.hfreebuds.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HdCallsProtocolTest {
    @Test fun writesMatchCapturedFreeBuds5Commands() {
        assertEquals("5a0006002b450101007009", HdCallsProtocol.set(false).toBytes().hex())
        assertEquals("5a0006002b450101016028", HdCallsProtocol.set(true).toBytes().hex())
    }

    @Test fun parsesCapturedOnAndOffReports() {
        val off = FrameReader().feed("5a0006002b46010100ebd5".unhex(), 11).single()
        val on = FrameReader().feed("5a0006002b46010101fbf4".unhex(), 11).single()
        assertEquals(false, HdCallsProtocol.reported(HuaweiPacket.parse(off)!!))
        assertEquals(true, HdCallsProtocol.reported(HuaweiPacket.parse(on)!!))
    }

    @Test fun rejectsUnknownMalformedAndUnrelatedReports() {
        assertNull(HdCallsProtocol.reported(HuaweiPacket.write(Cmd.HD_CALLS_REPORT, 1 to 2)))
        assertNull(HdCallsProtocol.reported(HuaweiPacket(Cmd.HD_CALLS_REPORT)))
        assertNull(HdCallsProtocol.reported(HuaweiPacket.write(Cmd.HD_CALLS_WRITE, 1 to true)))
        assertNull(HdCallsProtocol.reported(HuaweiPacket.write(Cmd.HD_CALLS_REPORT, 1 to byteArrayOf(1, 0))))
    }
}
