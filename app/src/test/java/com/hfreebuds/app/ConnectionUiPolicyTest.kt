package com.hfreebuds.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionUiPolicyTest {
    @Test fun disconnectedTilesAllowFindAboutAndUpdates() {
        assertTrue(tileAvailable(Page.FIND, false))
        assertTrue(tileAvailable(Page.ABOUT, false))
        assertTrue(tileAvailable(Page.UPDATES, false))
        for (page in listOf(Page.SETTINGS, Page.LANGUAGE, Page.SOUND, Page.GESTURES, Page.WEAR)) {
            assertFalse("Unexpectedly enabled while disconnected: $page", tileAvailable(page, false))
        }
    }

    @Test fun reconnectEnablesAllTiles() {
        Page.entries.forEach { assertTrue("Still disabled after reconnect: $it", tileAvailable(it, true)) }
    }

    @Test fun earbudPagesRequireAConnection() {
        for (page in listOf(Page.SETTINGS, Page.FIND, Page.GESTURES, Page.SOUND, Page.WEAR, Page.DUAL, Page.LANGUAGE, Page.PHONE)) {
            assertTrue("Missing disconnected guard: $page", devicePageNeedsConnection(page))
        }
        assertFalse(devicePageNeedsConnection(Page.HOME))
        assertFalse(devicePageNeedsConnection(Page.ABOUT))
        assertFalse(devicePageNeedsConnection(Page.UPDATES))
    }
}
