package com.hfreebuds.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

    private val hasPermission = mutableStateOf(false)
    private val prefs by lazy { getSharedPreferences("hfreebuds", Context.MODE_PRIVATE) }

    private val askPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission.value = granted
        if (granted) connectSaved()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppUpdater.init(this, "N2K02/HFreebuds", "HFreebuds")
        Earbuds.init(this)
        hasPermission.value = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        setContent {
            HFreebudsTheme {
                HFreebudsScreen(
                    hasPermission = hasPermission.value,
                    onAskPermission = { askPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                    savedAddress = { prefs.getString("address", null) },
                    onPick = { address ->
                        prefs.edit().putString("address", address).apply()
                        Earbuds.connect(address)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Earbuds.screenOpen = true
        AppUpdater.onResume(this)
        hasPermission.value = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        connectSaved()
    }

    override fun onStop() {
        // The earbuds keep their settings; the link is only needed while the app is open.
        if (!isChangingConfigurations) {
            Earbuds.screenOpen = false
            FindTone.stop()
            Earbuds.disconnect()
        }
        super.onStop()
    }

    private fun connectSaved() {
        if (!hasPermission.value) return
        val address = prefs.getString("address", null) ?: return
        if (!Earbuds.isConnected) Earbuds.connect(address)
    }
}
