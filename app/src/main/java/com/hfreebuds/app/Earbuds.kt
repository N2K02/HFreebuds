package com.hfreebuds.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Left / right / case battery. null = not reported (e.g. earbud in the case with the lid closed). */
data class Battery(
    val left: Int? = null, val right: Int? = null, val case: Int? = null, val global: Int? = null,
    val leftCharging: Boolean = false, val rightCharging: Boolean = false, val caseCharging: Boolean = false,
)

data class Gesture(val left: Int? = null, val right: Int? = null, val inCall: Int? = null)

data class DualDevice(
    val mac: String, val name: String, val connected: Boolean, val playing: Boolean,
    val preferred: Boolean, val autoConnect: Boolean?,
)

data class DeviceInfo(
    val model: String? = null, val subModel: String? = null, val hardware: String? = null,
    val firmware: String? = null, val serial: String? = null, val leftSerial: String? = null,
    val rightSerial: String? = null,
)

enum class Link { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

/** Everything the app knows about the connected earbuds. null = not supported / not answered. */
data class EarbudsState(
    val link: Link = Link.DISCONNECTED,
    val error: String? = null,
    val deviceName: String? = null,
    val address: String? = null,
    val profile: DeviceProfile? = null,
    val battery: Battery? = null,
    val inEar: Boolean? = null,
    /** 0 off, 1 noise cancelling, 2 awareness. */
    val ancMode: Int? = null,
    val ancLevel: Int? = null,
    val autoPause: Boolean? = null,
    /** true = sound quality first (HD / LDAC), false = connection first. */
    val hdAudio: Boolean? = null,
    val lowLatency: Boolean? = null,
    val eqPreset: Int? = null,
    val eqAvailable: List<Int>? = null,
    val doubleTap: Gesture? = null,
    val doubleTapOptions: List<Int>? = null,
    val tripleTap: Gesture? = null,
    val tripleTapOptions: List<Int>? = null,
    val longTap: Gesture? = null,
    val longTapAnc: Gesture? = null,
    val swipe: Int? = null,
    val languages: List<String>? = null,
    val dualEnabled: Boolean? = null,
    val dualDevices: List<DualDevice>? = null,
    val info: DeviceInfo? = null,
)

/**
 * The connection to one pair of earbuds: an RFCOMM socket, a reader thread that turns bytes
 * into packets, request/answer matching, and [state] for the UI. One instance for the app.
 */
object Earbuds {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(EarbudsState())
    val state: StateFlow<EarbudsState> = _state

    /** Last packets in and out, for the Log card (newest last). */
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    /** Clear only the session connection history, not the current Bluetooth state. */
    fun clearLog() { _log.value = emptyList() }

    private var socket: BluetoothSocket? = null
    private var readerJob: Job? = null
    private var connectJob: Job? = null
    private val writeLock = Mutex()
    private val waiting = ConcurrentHashMap<Int, CompletableDeferred<HuaweiPacket>>()
    private val dualRows = ConcurrentHashMap<Int, DualDevice>()
    private var dualCount = 0
    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    private fun log(line: String) {
        val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _log.update { (it + "$t  $line").takeLast(150) }
    }

    val isConnected get() = _state.value.link == Link.CONNECTED

    /** True while the app screen is open (the Quick Settings tile then leaves the link alone). */
    @Volatile
    var screenOpen = false

    // ---------------------------------------------------------------- connection

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (connectJob?.isActive == true) return
        val s = _state.value
        if (s.link == Link.CONNECTED && s.address == address) return
        disconnect()
        connectJob = scope.launch {
            val adapter = app.getSystemService(BluetoothManager::class.java).adapter
            val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            if (device == null) {
                _state.value = EarbudsState(link = Link.FAILED, error = "Unknown device")
                return@launch
            }
            val name = runCatching { device.alias ?: device.name }.getOrNull()
            val profile = DeviceProfile.forName(runCatching { device.name }.getOrNull() ?: name)
            _state.value = EarbudsState(link = Link.CONNECTING, deviceName = name, address = address, profile = profile)
            log("Connecting to $name ($address) as ${profile.name}")
            val sock = openSocket(device)
            if (sock == null) {
                _state.update {
                    it.copy(
                        link = Link.FAILED,
                        error = "Couldn't reach the earbuds. Make sure they're out of the case and connected to this phone.",
                    )
                }
                return@launch
            }
            socket = sock
            _state.update { it.copy(link = Link.CONNECTED, error = null) }
            log("Connected")
            readerJob = scope.launch { readLoop(sock) }
            refreshAll()
        }
    }

    /** Tries the standard serial-port service first, then the fixed channel some models use. */
    @SuppressLint("MissingPermission")
    private fun openSocket(device: BluetoothDevice): BluetoothSocket? {
        val attempts = listOf<Pair<String, () -> BluetoothSocket>>(
            "SPP secure" to { device.createRfcommSocketToServiceRecord(SPP) },
            "SPP insecure" to { device.createInsecureRfcommSocketToServiceRecord(SPP) },
            "channel 1" to {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as BluetoothSocket
            },
        )
        for ((label, make) in attempts) {
            val s = runCatching { make() }.getOrNull() ?: continue
            try {
                s.connect()
                log("Opened $label")
                return s
            } catch (e: IOException) {
                log("$label failed: ${e.message}")
                runCatching { s.close() }
            }
        }
        return null
    }

    /** Quit all app-owned asynchronous work before closing the activity. */
    fun quit() {
        screenOpen = false
        disconnect()
        scope.coroutineContext.cancelChildren()
    }

    fun disconnect() {
        connectJob?.cancel()
        readerJob?.cancel()
        runCatching { socket?.close() }
        socket = null
        waiting.values.forEach { it.cancel() }
        waiting.clear()
        if (_state.value.link != Link.DISCONNECTED) {
            _state.update { it.copy(link = Link.DISCONNECTED) }
            log("Disconnected")
        }
    }

    private fun readLoop(sock: BluetoothSocket) {
        val reader = FrameReader()
        val buf = ByteArray(1024)
        try {
            val input = sock.inputStream
            while (scope.isActive) {
                val n = input.read(buf)
                if (n < 0) break
                for (frame in reader.feed(buf, n)) {
                    val pkt = HuaweiPacket.parse(frame) ?: continue
                    log("← $pkt")
                    handle(pkt)
                    waiting.remove(pkt.command)?.complete(pkt)
                }
            }
        } catch (e: IOException) {
            log("Link closed: ${e.message}")
        }
        if (socket === sock) {
            socket = null
            _state.update { it.copy(link = Link.DISCONNECTED, error = "The earbuds disconnected.") }
        }
    }

    private suspend fun sendRaw(bytes: ByteArray, label: String): Boolean = writeLock.withLock {
        val out = socket?.outputStream ?: return false
        try {
            withContext(Dispatchers.IO) {
                out.write(bytes)
                out.flush()
            }
            log("→ $label")
            true
        } catch (e: IOException) {
            log("Send failed: ${e.message}")
            false
        }
    }

    suspend fun send(p: HuaweiPacket) = sendRaw(p.toBytes(), p.toString())

    /** Sends and waits for the answer with the same command (or [answer]). */
    private suspend fun request(p: HuaweiPacket, answer: Int = p.command, timeoutMs: Long = 2500): HuaweiPacket? {
        val d = CompletableDeferred<HuaweiPacket>()
        waiting[answer] = d
        if (!send(p)) { waiting.remove(answer); return null }
        return withTimeoutOrNull(timeoutMs) { d.await() }.also { waiting.remove(answer, d) }
    }

    /** A read: the model's captured request if there is one, else the generic one. */
    private suspend fun read(command: Int, vararg types: Int): HuaweiPacket? {
        val raw = _state.value.profile?.rawReads?.get(command)
        if (raw != null) {
            val d = CompletableDeferred<HuaweiPacket>()
            waiting[command] = d
            if (!sendRaw(raw.unhex(), "%04X (captured request)".format(command))) { waiting.remove(command); return null }
            return withTimeoutOrNull(2500) { d.await() }.also { waiting.remove(command, d) }
        }
        return request(HuaweiPacket.read(command, *types))
    }

    // ---------------------------------------------------------------- reading everything

    /** Reads every setting the model may support. Unanswered ones stay null (hidden in the UI). */
    fun refreshAll() = scope.launch {
        val p = _state.value.profile ?: return@launch
        read(Cmd.DEVICE_INFO, *(0..31).toList().toIntArray())
        read(Cmd.BATTERY_READ, 1, 2, 3)
        if (p.anc) read(Cmd.ANC_READ, 1, 2)
        if (p.autoPause) read(Cmd.AUTO_PAUSE_READ, 1)
        if (p.quality) read(Cmd.QUALITY_READ, 1)
        if (p.lowLatency) read(Cmd.LOW_LATENCY, 2)
        if (p.eqPresets.isNotEmpty()) read(Cmd.EQ_READ, 1, 2, 3, 4, 5, 6, 7, 8)
        if (p.doubleTap) read(Cmd.DOUBLE_TAP_READ, 1, 2)
        if (p.tripleTap) read(Cmd.TRIPLE_TAP_READ, 1, 2)
        if (p.longTap) {
            read(Cmd.LONG_TAP_READ, 1, 2)
            if (p.longTapAnc) read(Cmd.LONG_TAP_ANC_READ, 1, 2)
        }
        if (p.swipe) read(Cmd.SWIPE_READ, 1, 2)
        if (p.language) read(Cmd.LANGUAGE_READ, 1, 2)
        if (p.dualConnect) refreshDual()
    }

    fun refreshBattery() = scope.launch { read(Cmd.BATTERY_READ, 1, 2, 3) }

    private suspend fun refreshDual() {
        read(Cmd.DUAL_READ, 1)
        dualRows.clear()
        dualCount = 0
        send(HuaweiPacket.read(Cmd.DUAL_LIST, 1))
        // Rows arrive one packet per device.
        delay(1500)
        val rows = (0 until maxOf(dualCount, dualRows.size)).mapNotNull { dualRows[it] }
        if (rows.isNotEmpty()) _state.update { it.copy(dualDevices = rows) }
    }

    // ---------------------------------------------------------------- incoming packets

    private fun handle(p: HuaweiPacket) {
        when (p.command) {
            Cmd.BATTERY_READ, Cmd.BATTERY_NOTIFY -> {
                val lvl = p.params[2]
                val chg = p.params[3]
                val tws = _state.value.profile?.tws != false
                _state.update { s ->
                    val old = s.battery ?: Battery()
                    s.copy(
                        battery = old.copy(
                            global = p.params[1]?.takeIf { it.size == 1 }?.get(0)?.u8() ?: old.global,
                            left = if (tws && lvl != null && lvl.size >= 3) lvl[0].u8().takeIf { it in 1..100 } else old.left,
                            right = if (tws && lvl != null && lvl.size >= 3) lvl[1].u8().takeIf { it in 1..100 } else old.right,
                            case = if (tws && lvl != null && lvl.size >= 3) lvl[2].u8().takeIf { it in 1..100 } else old.case,
                            leftCharging = chg?.getOrNull(0)?.toInt() == 1,
                            rightCharging = chg?.getOrNull(1)?.toInt() == 1,
                            caseCharging = chg?.getOrNull(2)?.toInt() == 1,
                        ),
                    )
                }
                // Kept for the Quick Settings battery tile.
                _state.value.battery?.let { BatteryCache.save(app, it) }
            }
            Cmd.STATE_EVENT -> {
                p.param(8, 9)?.takeIf { it.size == 1 }?.let { v -> _state.update { it.copy(inEar = v[0].toInt() == 1) } }
                // Noise control changed with a gesture: read the new mode.
                p.param(1)?.takeIf { it.size == 1 }?.let { scope.launch { read(Cmd.ANC_READ, 1, 2) } }
            }
            Cmd.ANC_READ -> p.params[1]?.takeIf { it.size == 2 }?.let { v ->
                _state.update { it.copy(ancMode = v[1].u8(), ancLevel = v[0].toInt()) }
            }
            Cmd.AUTO_PAUSE_READ -> p.byte(1)?.let { v -> _state.update { it.copy(autoPause = v == 1) } }
            Cmd.QUALITY_READ -> p.byte(2)?.let { v -> _state.update { it.copy(hdAudio = v == 1) } }
            Cmd.LOW_LATENCY -> p.byte(2)?.let { v -> _state.update { it.copy(lowLatency = v == 1) } }
            Cmd.EQ_READ -> _state.update { s ->
                s.copy(
                    eqPreset = p.byte(2) ?: s.eqPreset,
                    eqAvailable = p.params[3]?.takeIf { it.isNotEmpty() }?.map { it.toInt() } ?: s.eqAvailable,
                )
            }
            Cmd.DOUBLE_TAP_READ -> _state.update { s ->
                s.copy(
                    doubleTap = Gesture(p.byte(1), p.byte(2), p.byte(4)),
                    doubleTapOptions = p.params[3]?.takeIf { it.isNotEmpty() }?.map { it.toInt() } ?: s.doubleTapOptions,
                )
            }
            Cmd.TRIPLE_TAP_READ -> _state.update { s ->
                s.copy(
                    tripleTap = Gesture(p.byte(1), p.byte(2), p.byte(4)),
                    tripleTapOptions = p.params[3]?.takeIf { it.isNotEmpty() }?.map { it.toInt() } ?: s.tripleTapOptions,
                )
            }
            Cmd.LONG_TAP_READ -> if (p.byte(1) != null || p.byte(2) != null) {
                _state.update { it.copy(longTap = Gesture(p.byte(1), p.byte(2), p.byte(4))) }
            }
            Cmd.LONG_TAP_ANC_READ -> if (p.byte(1) != null || p.byte(2) != null) {
                _state.update { it.copy(longTapAnc = Gesture(p.byte(1), p.byte(2))) }
            }
            Cmd.SWIPE_READ -> p.byte(1)?.let { v -> _state.update { it.copy(swipe = v) } }
            Cmd.LANGUAGE_READ -> p.params[3]?.takeIf { it.size > 1 }?.let { v ->
                val list = String(v, Charsets.UTF_8).split(',').map { it.trim() }.filter { it.isNotEmpty() }
                _state.update { it.copy(languages = list) }
            }
            Cmd.DUAL_READ -> p.byte(1)?.let { v -> _state.update { it.copy(dualEnabled = v == 1) } }
            Cmd.DUAL_LIST -> {
                val mac = p.params[4]?.takeIf { it.size == 6 } ?: return
                val index = p.byte(3) ?: return
                dualCount = p.byte(2) ?: dualCount
                val state = p.params[5]?.getOrNull(0)?.u8() ?: 0
                dualRows[index] = DualDevice(
                    mac = mac.joinToString(":") { "%02X".format(it) },
                    name = p.params[9]?.let { String(it, Charsets.UTF_8) }.orEmpty().ifBlank { "Unknown device" },
                    connected = state > 0,
                    playing = state == 9,
                    preferred = p.params[7]?.getOrNull(0)?.toInt() == 1,
                    autoConnect = if (_state.value.profile?.dualAutoConnect == true) p.params[8]?.getOrNull(0)?.toInt() == 1 else null,
                )
            }
            Cmd.DUAL_CHANGED -> scope.launch { refreshDual() }
            Cmd.DEVICE_INFO -> {
                fun str(t: Int) = p.params[t]?.let { String(it, Charsets.UTF_8).trim { c -> c <= ' ' } }?.takeIf { it.isNotEmpty() }
                var ls: String? = null
                var rs: String? = null
                str(24)?.takeIf { it.startsWith("L-") }?.split(',')?.let { parts ->
                    ls = parts.getOrNull(0)?.removePrefix("L-")
                    rs = parts.getOrNull(1)?.removePrefix("R-")
                }
                _state.update {
                    it.copy(
                        info = DeviceInfo(
                            model = str(15), subModel = str(10), hardware = str(3), firmware = str(7),
                            serial = str(9), leftSerial = ls, rightSerial = rs,
                        ),
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- settings

    private fun write(p: HuaweiPacket, then: (suspend () -> Unit)? = null) = scope.launch {
        send(p)
        delay(400)
        then?.invoke()
    }

    /** [mode] 0 off, 1 noise cancelling, 2 awareness. [level] null = the earbuds' default. */
    fun setAnc(mode: Int, level: Int? = null) {
        val second = when {
            mode == 0 -> 0x00
            level != null -> level
            else -> 0xFF
        }
        _state.update { it.copy(ancMode = mode, ancLevel = if (mode == 0) it.ancLevel else level ?: it.ancLevel) }
        write(HuaweiPacket.write(Cmd.ANC_WRITE, 1 to byteArrayOf(mode.toByte(), second.toByte()))) { read(Cmd.ANC_READ, 1, 2) }
    }

    fun setAutoPause(on: Boolean) {
        _state.update { it.copy(autoPause = on) }
        write(HuaweiPacket.write(Cmd.AUTO_PAUSE_WRITE, 1 to on)) { read(Cmd.AUTO_PAUSE_READ, 1) }
    }

    fun setHdAudio(on: Boolean) {
        _state.update { it.copy(hdAudio = on) }
        write(HuaweiPacket.write(Cmd.QUALITY_WRITE, 1 to on)) { read(Cmd.QUALITY_READ, 1) }
    }

    fun setLowLatency(on: Boolean) {
        _state.update { it.copy(lowLatency = on) }
        write(HuaweiPacket.write(Cmd.LOW_LATENCY, 1 to on)) { delay(600); read(Cmd.LOW_LATENCY, 2) }
    }

    fun setEq(preset: Int) {
        _state.update { it.copy(eqPreset = preset) }
        write(HuaweiPacket.write(Cmd.EQ_WRITE, 1 to preset)) { read(Cmd.EQ_READ, 1, 2, 3, 4, 5, 6, 7, 8) }
    }

    /** [side] 1 left, 2 right, 4 in call. */
    fun setDoubleTap(side: Int, action: Int) =
        write(HuaweiPacket.write(Cmd.DOUBLE_TAP_WRITE, side to action)) { read(Cmd.DOUBLE_TAP_READ, 1, 2) }

    fun setTripleTap(side: Int, action: Int) =
        write(HuaweiPacket.write(Cmd.TRIPLE_TAP_WRITE, side to action)) { read(Cmd.TRIPLE_TAP_READ, 1, 2) }

    fun setLongTap(side: Int, action: Int) =
        write(HuaweiPacket.write(Cmd.LONG_TAP_WRITE, side to action)) { read(Cmd.LONG_TAP_READ, 1, 2) }

    fun setLongTapAnc(side: Int, cycle: Int) =
        write(HuaweiPacket.write(Cmd.LONG_TAP_ANC_WRITE, side to cycle)) { read(Cmd.LONG_TAP_ANC_READ, 1, 2) }

    fun setSwipe(action: Int) {
        _state.update { it.copy(swipe = action) }
        write(HuaweiPacket.write(Cmd.SWIPE_WRITE, 1 to action, 2 to action)) { read(Cmd.SWIPE_READ, 1, 2) }
    }

    fun setLanguage(locale: String) = write(HuaweiPacket.write(Cmd.LANGUAGE_WRITE, 1 to locale, 2 to 1))

    fun setDualEnabled(on: Boolean) {
        _state.update { it.copy(dualEnabled = on) }
        write(HuaweiPacket.write(Cmd.DUAL_WRITE, 1 to on)) { refreshDual() }
    }

    private fun macBytes(mac: String) = mac.replace(":", "").unhex()

    fun setDualPreferred(mac: String) = write(HuaweiPacket.write(Cmd.DUAL_PREFERRED, 1 to macBytes(mac))) { delay(800); refreshDual() }

    /** 1 connect, 2 disconnect, 3 forget, 4 auto-connect on, 5 auto-connect off. */
    fun dualCommand(action: Int, mac: String) =
        write(HuaweiPacket.write(Cmd.DUAL_EXECUTE, action to macBytes(mac))) { delay(1500); refreshDual() }
}
