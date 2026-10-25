package com.hfreebuds.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HFreebuds is tile-based: New goes on Updates, not unrelated Earbud settings. */
class UpdateBadgeContractTest {
    private fun source(path: String) = listOf(File(path), File("../$path"))
        .firstOrNull { it.isFile }?.readText() ?: error("Missing $path")

    @Test fun homeUpdateTileIsBadgedAndNoBottomMenuIsIntroduced() {
        val home = source("app/src/main/java/com/hfreebuds/app/HFreebudsScreen.kt")
        assertTrue(home.contains("target == Page.UPDATES"))
        assertFalse(home.contains("target == Page.UPDATES || target == Page.SETTINGS"))
        assertTrue(home.contains("Tile(\"About & updates\""))
        assertTrue(home.contains("updateStatus = updateStatus"))
        assertFalse(home.contains("NavigationBar {"))
        val tiles = source("app/src/main/java/com/hfreebuds/app/UiKit.kt")
        assertTrue(tiles.contains("N2K02UpdateBadge(it)"))
    }
}
