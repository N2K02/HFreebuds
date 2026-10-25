package com.hfreebuds.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protect Quit access without changing this app's established navigation style. */
class QuitNavigationContractTest {
    @Test fun quitRemainsAvailableAndStopsTheApp() {
        val relative = "app/src/main/java/com/hfreebuds/app/HFreebudsScreen.kt"
        val source = listOf(File(relative), File("../$relative"))
            .firstOrNull { it.isFile }?.readText() ?: error("Missing $relative")
        assertTrue(source.contains("Text(\"Choose earbuds\")"))
        assertTrue(source.contains("Text(\"Quit\")"))
        assertTrue(source.contains("activity?.finishAndRemoveTask()"))
        assertTrue(source.indexOf("Text(\"Quit\")") > source.indexOf("Text(\"Choose earbuds\")"))
    }
}
