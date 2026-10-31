package com.hfreebuds.app
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class NoAppLogsContractTest {
    private fun src(p: String) = listOf(File(p), File("../$p")).first { it.isFile }.readText()
    @Test fun tileNavigationExcludesLogs() {
        assertFalse(src("app/src/main/java/com/hfreebuds/app/HFreebudsScreen.kt").contains("Page.LOG"))
        assertFalse(src("app/src/main/java/com/hfreebuds/app/Pages.kt").contains("fun LogPage("))
    }
    @Test fun noOnDiskLogs() {
        assertTrue(src("app/src/main/java/com/hfreebuds/app/AppLog.kt").contains("Log.d("))
        assertFalse(src("app/src/main/java/com/hfreebuds/app/AppLog.kt").contains("appendText("))
        assertFalse(src("app/src/main/AndroidManifest.xml").contains("CrashJournalProvider"))
    }
}
