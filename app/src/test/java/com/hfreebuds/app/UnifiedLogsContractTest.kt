package com.hfreebuds.app
import java.io.File
import org.junit.Assert.assertFalse
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
        assertTrue(ui.contains("Triple(\"Logs\", Icons.Rounded.Subject, Page.LOG)"))
        assertTrue(pages.contains("extraLines = connectionLines"))
        assertTrue(pages.contains("N2K02UnifiedLogsPage("))
        assertTrue(pages.contains("N2K02AppIcon"))
        listOf("followLatest", "Text(\"Prev\")", "Text(\"Next\")",
            "Text(\"Copy\")", "Text(\"Clear all\")", "Switch(checked = enabled").forEach {
            assertTrue(it, logs.contains(it))
        }
        assertFalse(manifest.contains("LogsActivity"))
    }
}
