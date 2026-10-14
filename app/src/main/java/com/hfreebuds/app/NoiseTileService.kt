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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Quick Settings tile: tap to cycle noise control (Off → Noise cancelling → Awareness when the
 * model has it). Connects for a moment if the app isn't open, then lets go again.
 */
class NoiseTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var releaseJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        render(Earbuds.state.value)
    }

    override fun onClick() {
        super.onClick()
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val address = getSharedPreferences("hfreebuds", Context.MODE_PRIVATE).getString("address", null) ?: return
        Earbuds.init(this)
        qsTile?.apply { subtitle = "Connecting…"; state = Tile.STATE_UNAVAILABLE; updateTile() }
        scope.launch {
            if (!Earbuds.isConnected) Earbuds.connect(address)
            val s = withTimeoutOrNull(10_000) { Earbuds.state.first { it.ancMode != null || it.link == Link.FAILED } }
            if (s == null || s.ancMode == null) {
                qsTile?.apply { subtitle = "Not connected"; state = Tile.STATE_INACTIVE; updateTile() }
                releaseLater()
                return@launch
            }
            val modes = if (s.profile?.awareness == true) listOf(0, 1, 2) else listOf(0, 1)
            val next = modes[(modes.indexOf(s.ancMode).coerceAtLeast(0) + 1) % modes.size]
            Earbuds.setAnc(next)
            delay(300)
            render(Earbuds.state.value)
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

    private fun render(s: EarbudsState) {
        val tile = qsTile ?: return
        tile.label = "Noise control"
        tile.subtitle = when (s.ancMode) {
            0 -> "Off"
            1 -> "Noise cancelling"
            2 -> "Awareness"
            else -> "Tap to switch"
        }
        tile.state = if (s.ancMode == 1 || s.ancMode == 2) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }
}
