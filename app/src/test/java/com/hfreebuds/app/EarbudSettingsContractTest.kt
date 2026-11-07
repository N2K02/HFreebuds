package com.hfreebuds.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarbudSettingsContractTest {
    private fun settings(): String {
        val path = "com/hfreebuds/app/Pages.kt"
        val source = listOf(File("src/main/java", path), File("app/src/main/java", path))
            .first { it.isFile }.readText()
        return source.substringAfter("internal fun EarbudSettingsPage(")
            .substringBefore("// ============================================================ Find device")
    }

    @Test fun settingsContainsOnlyOneConnectedDevicesEntry() {
        val page = settings()
        assertEquals(1, Regex("\"Connected devices\"").findAll(page).count())
        for (shortcut in listOf("Gesture control", "Sound effects", "Wear detection", "Find Device", "Phone audio", "About & updates")) {
            assertFalse("Duplicate Home shortcut: $shortcut", page.contains("\"$shortcut\""))
        }
        assertTrue(page.contains("PageScaffold(\"Earbud settings\""))
    }

    @Test fun supportedHdAudioChoicesRemainConnectedToDeviceCommands() {
        val page = settings()
        assertTrue(page.contains("if (s.hdAudio != null)"))
        assertTrue(page.contains("s.hdAudio == false"))
        assertTrue(page.contains("s.hdAudio == true"))
        assertTrue(page.contains("Earbuds.setHdAudio(false)"))
        assertTrue(page.contains("Earbuds.setHdAudio(true)"))
    }
    @Test fun hdCallsAndChannelTestsAreAvailableInSettings() {
        val page = settings()
        assertTrue(page.contains("\"HD calls\""))
        assertTrue(page.contains("Earbuds.setHdCalls(true)"))
        assertTrue(page.contains("Earbuds.setHdCalls(false)"))
        assertTrue(page.contains("FindTone.Side.LEFT"))
        assertTrue(page.contains("FindTone.Side.RIGHT"))
    }

}
