package com.hfreebuds.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevent replacing HFreebuds' tile-based Home with a bottom navigation menu. */
class TileNavigationContractTest {
    private fun source(file: String): String {
        val path = "com/hfreebuds/app/$file"
        return listOf(File("src/main/java", path), File("app/src/main/java", path))
            .firstOrNull { it.isFile }?.readText()
            ?: error("Missing app UI source: $path")
    }

    @Test fun homeUsesShortcutTilesInsteadOfBottomNavigation() {
        val screen = source("HFreebudsScreen.kt")
        val home = screen.substringAfter("private fun HomePage(")
            .substringBefore("// ============================================================ hero")
        assertFalse("HFreebuds must not have bottom navigation", screen.contains("NavigationBar {"))
        assertFalse("HFreebuds must not have bottom navigation items", screen.contains("NavigationBarItem("))
        assertTrue("Shortcut tiles must be shown in a two-column grid", home.contains("tiles.chunked(2)"))
        for (label in listOf("Find Device", "Phone audio", "Updates", "About", "Logs")) {
            assertTrue("Missing shortcut tile: $label", home.contains("\"$label\""))
        }
        assertTrue("Missing Earbud settings shortcut", home.contains("\"Earbud settings\""))
        assertTrue("Permission gate must retain direct update access", home.contains("Tile(\"About & updates\""))
    }

    @Test fun bluetoothPickerAndQuitHaveLabelsBelowTheTopBar() {
        val home = source("HFreebudsScreen.kt").substringAfter("private fun HomePage(")
            .substringBefore("// ============================================================ hero")
        val titleBar = home.substringAfter("TopAppBar(").substringBefore("scrollBehavior = scroll")
        val content = home.substringAfter("LazyColumn(")
        assertFalse("The Bluetooth picker should not be in the title bar", titleBar.contains("onClick = onChoose"))
        assertFalse("Quit should not be in the title bar", titleBar.contains("finishAndRemoveTask()"))
        assertTrue("The labeled Bluetooth picker belongs in Home content", content.contains("Text(\"Choose earbuds\")"))
        assertTrue("The labeled Quit button belongs in Home content", content.contains("Text(\"Quit\")"))
        assertTrue("Quit must retain its existing behavior", content.contains("activity?.finishAndRemoveTask()"))
        assertTrue("Buttons should be positioned left and right", content.contains("horizontalArrangement = Arrangement.SpaceBetween"))
    }

    @Test fun tileSubpagesHaveAnInAppBackAction() {
        val frame = source("UiKit.kt")
        assertTrue(frame.contains("IconButton(onClick = onBack)"))
        val screen = source("HFreebudsScreen.kt")
        assertTrue(screen.contains("BackHandler(enabled = page != Page.HOME)"))
        assertTrue(screen.contains("open = { subpageFromSettings = false; page = it }"))
    }
    @Test fun homeQuitCancelsBluetoothWorkBeforeActivityCloses() {
        val root = "app/src/main/java/com/hfreebuds/app/"
        fun source(name: String) = listOf(java.io.File(root + name),
            java.io.File("../" + root + name)).first { it.isFile }.readText()
        val home = source("HFreebudsScreen.kt")
        val earbuds = source("Earbuds.kt")
        assertTrue(home.contains("FindTone.stop(); Earbuds.quit(); activity?.finishAndRemoveTask()"))
        assertTrue(earbuds.contains("fun quit()"))
        assertTrue(earbuds.contains("scope.coroutineContext.cancelChildren()"))
        assertTrue(earbuds.contains("connectingSocket?.close()"))
        assertTrue(earbuds.contains("waiting.values.forEach { it.cancel() }"))
    }

}
