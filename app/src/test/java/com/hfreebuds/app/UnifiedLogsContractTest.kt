package com.hfreebuds.app
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class UnifiedLogsContractTest {
    private fun read(p: String) = listOf(File(p), File("../$p")).firstOrNull { it.isFile }
        ?.readText() ?: error(p)
    @Test fun tileOpensSharedThemedAndPagedLogsWithoutStandaloneActivity() {
        val pages = read("app/src/main/java/com/hfreebuds/app/Pages.kt")
        val ui = read("app/src/main/java/com/hfreebuds/app/HFreebudsScreen.kt")
        val logs = read("app/src/main/java/com/hfreebuds/app/UnifiedLogsPage.kt")
        val manifest = read("app/src/main/AndroidManifest.xml")
        assertTrue(ui.contains("Page.LOG -> LogPage(back)"))
        assertTrue(ui.contains("Triple(\"Logs\", Icons.Rounded.Article, Page.LOG)"))
        assertTrue(pages.contains("extraLines = connectionLines"))
        assertTrue(pages.contains("clearExtra = Earbuds::clearLog"))
        assertTrue(pages.contains("N2K02UnifiedLogsPage("))
        assertTrue(pages.contains("N2K02AppIcon"))
        listOf("followLatest", "Text(\"Prev\",", "Text(\"Next\",",
            "Text(\"Copy\",", "Text(\"Clear all\",", "Switch(checked = enabled").forEach {
            assertTrue(it, logs.contains(it))
        }
        assertFalse(manifest.contains("LogsActivity"))
        assertTrue(logs.contains("Icons.Filled.ContentCopy"))
        assertTrue(logs.contains("Icons.Filled.DeleteSweep"))
        assertTrue(logs.contains("Icons.AutoMirrored.Filled.KeyboardArrowLeft"))
        assertTrue(logs.contains("Icons.AutoMirrored.Filled.KeyboardArrowRight"))
        assertTrue(logs.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertFalse(logs.contains("Text(\"Back\")"))
        assertFalse(logs.contains("onBack: () -> Unit"))
        assertTrue(pages.contains("Triple(\"Logs\", Icons.Rounded.Article, Page.LOG)"))
        assertTrue(pages.contains("Icon(Icons.Rounded.Article, contentDescription = null,"))
    }
    @Test fun savedAndLiveEventsFollowTimeWithoutLosingLegacyDates() {
        val previous = "2026-10-28 23:59:59.000 [UI] old"
        val current = "2026-10-29 11:00:02.000 [LOG] new"
        val call = "[activity] 11:00:01 call"
        assertEquals(listOf(previous, call, current),
            LogEventOrdering.merge(listOf(previous, current), listOf(call), "2026-10-29"))
        assertEquals(listOf("untimed A", "untimed B"),
            LogEventOrdering.merge(listOf("untimed A", "untimed B"), emptyList(), "2026-10-29"))
    }

}
