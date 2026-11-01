package com.hfreebuds.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BluetoothLogEfficiencyTest {
    @Test fun packetDumpIsOptInButErrorsStillLog() {
        val file = listOf(
            File("src/main/java/com/hfreebuds/app/Earbuds.kt"),
            File("app/src/main/java/com/hfreebuds/app/Earbuds.kt")
        ).first { it.exists() }
        val code = file.readText()
        assertTrue(code.contains("if (Log.isLoggable(\"HFreebuds-BT\", Log.VERBOSE)) log(\"← \$pkt\")"))
        assertTrue(code.contains("if (Log.isLoggable(\"HFreebuds-BT\", Log.VERBOSE)) log(\"→ \$label\")"))
        assertTrue(code.contains("log(\"Link closed:"))
        assertTrue(code.contains("log(\"Send failed:"))
    }
}
