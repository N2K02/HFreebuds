package com.hfreebuds.app

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

    private val hasPermission = mutableStateOf(false)
    private val prefs by lazy { getSharedPreferences("hfreebuds", Context.MODE_PRIVATE) }

    private val handler = Handler(Looper.getMainLooper())
    private var watching = false

    /** Reconnects by itself when the saved earbuds come back (e.g. taken out of the case). */
    private val aclReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } ?: return
            if (device.address != prefs.getString("address", null)) return
            // The earbuds' serial service needs a moment after the Bluetooth link comes up.
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ connectSaved() }, 1500)
        }
    }

    private val askPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission.value = granted
        if (granted) {
            watchBluetooth()
            connectSaved()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        LiveUpdater.init(this)
        Earbuds.init(this)
        AppLog.init(applicationContext)
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

    override fun onStart() {
        super.onStart()
        watchBluetooth()
    }

    /** Listens for the earbuds' Bluetooth link while the app is open (needs Nearby devices). */
    private fun watchBluetooth() {
        if (watching || !hasBluetoothPermission()) return
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(aclReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(aclReceiver, filter)
        }
        watching = true
    }

    override fun onResume() {
        super.onResume()
        Earbuds.screenOpen = true
        LiveUpdater.onResume(this)
        hasPermission.value = hasBluetoothPermission()
        connectSaved()
    }

    private fun hasBluetoothPermission() =
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    override fun onStop() {
        if (watching) {
            unregisterReceiver(aclReceiver)
            watching = false
        }
        handler.removeCallbacksAndMessages(null)
        // The earbuds keep their settings; the link is only needed while the app is open.
        if (!isChangingConfigurations) {
            Earbuds.screenOpen = false
            FindTone.stop()
            Earbuds.disconnect()
        }
        super.onStop()
    }

    private fun connectSaved() {
        if (!hasPermission.value || !Earbuds.screenOpen) return
        val address = prefs.getString("address", null) ?: return
        if (!Earbuds.isConnected) Earbuds.connect(address)
    }
}
