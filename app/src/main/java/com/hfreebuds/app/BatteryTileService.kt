package com.hfreebuds.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DateFormat
import java.util.Date

/**
 * Quick Settings tile: shows the last left / right / case battery reading. Tap to read it again
 * (connects for a moment if the app isn't open, then lets go).
 */
class BatteryTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null
    private var releaseJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        render()
        // While the panel is open, follow live readings.
        watchJob?.cancel()
        watchJob = scope.launch { Earbuds.state.collect { if (it.battery != null) render() } }
    }

    override fun onStopListening() {
        watchJob?.cancel()
        watchJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val address = getSharedPreferences("hfreebuds", Context.MODE_PRIVATE).getString("address", null) ?: return
        Earbuds.init(this)
        qsTile?.apply { subtitle = "Checking…"; updateTile() }
        scope.launch {
            val before = BatteryCache.load(this@BatteryTileService)?.second ?: 0L
            if (Earbuds.isConnected) Earbuds.refreshBattery() else Earbuds.connect(address)
            // Wait for a reading newer than the one shown, or for the connection to fail.
            // (connect() resets an old failure straight away, so FAILED here is a new one.)
            withTimeoutOrNull(10_000) {
                while (Earbuds.state.value.link != Link.FAILED &&
                    (BatteryCache.load(this@BatteryTileService)?.second ?: 0L) <= before
                ) {
                    delay(200)
                }
            }
            val s = Earbuds.state.value
            if (s.link == Link.FAILED || s.battery == null) {
                render(note = "Not connected")
            } else {
                render()
            }
            releaseLater()
        }
    }

    /** Lets go of the earbuds after a while unless the app is open. */
    private fun releaseLater() {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(20_000)
            if (!Earbuds.screenOpen) Earbuds.disconnect()
        }
    }

    private fun render(note: String? = null) {
        val tile = qsTile ?: return
        val saved = BatteryCache.load(this)
        tile.label = "Earbuds battery"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_battery)
        tile.state = Tile.STATE_INACTIVE
        tile.subtitle = note ?: saved?.first?.let(BatteryCache::summary) ?: "Tap to check"
        tile.stateDescription = saved?.second?.let {
            "Checked " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
        }
        tile.updateTile()
    }
}

/** The last battery reading, kept so the tile can show it without connecting. */
object BatteryCache {
    private const val PREFS = "hfreebuds"

    fun save(context: Context, b: Battery) {
        if (b.left == null && b.right == null && b.case == null && b.global == null) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("battLeft", b.left ?: -1)
            .putInt("battRight", b.right ?: -1)
            .putInt("battCase", b.case ?: -1)
            .putInt("battGlobal", b.global ?: -1)
            .putLong("battAt", System.currentTimeMillis())
            .apply()
    }

    /** The reading and when it was taken, or null if there's none yet. */
    fun load(context: Context): Pair<Battery, Long>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = p.getLong("battAt", 0L).takeIf { it > 0 } ?: return null
        fun level(key: String) = p.getInt(key, -1).takeIf { it in 0..100 }
        return Battery(
            left = level("battLeft"),
            right = level("battRight"),
            case = level("battCase"),
            global = level("battGlobal"),
        ) to at
    }

    /** e.g. "L 80% · R 75% · Case 60%" (parts that weren't reported are left out). */
    fun summary(b: Battery): String? {
        val parts = listOfNotNull(
            b.left?.let { "L $it%" },
            b.right?.let { "R $it%" },
            b.case?.let { "Case $it%" },
        )
        return when {
            parts.isNotEmpty() -> parts.joinToString(" · ")
            b.global != null -> "${b.global}%"
            else -> null
        }
    }
}
