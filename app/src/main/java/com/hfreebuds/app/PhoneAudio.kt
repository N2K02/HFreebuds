package com.hfreebuds.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.sin

/**
 * Find my earbuds: plays a loud chirping tone on the left, right or both earbuds.
 * It goes out through the phone's media audio, so it works while they're connected.
 * The same tone checks left/right balance.
 */
object FindTone {
    enum class Side { LEFT, RIGHT, BOTH }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val _playing = MutableStateFlow<Side?>(null)
    val playing: StateFlow<Side?> = _playing

    /** Plays until [stop] or [seconds] pass. [rising]: starts quiet and gets louder (finding). */
    fun play(context: Context, side: Side, seconds: Int = 60, rising: Boolean = true) {
        stop()
        val am = context.getSystemService(AudioManager::class.java)
        _playing.value = side
        job = scope.launch {
            val rate = 44100
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(rate) // 0.25 s stereo 16-bit
                .build()
            // Media volume up for finding (put back afterwards).
            val oldVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (rising) am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3 / 4, 0)
            try {
                track.play()
                val chunk = ShortArray(rate / 5 * 2) // 200 ms stereo
                var t = 0L
                val end = System.currentTimeMillis() + seconds * 1000L
                while (isActive && System.currentTimeMillis() < end) {
                    val elapsed = 1f - (end - System.currentTimeMillis()) / (seconds * 1000f)
                    val gain = if (rising) (0.25f + elapsed * 3f).coerceAtMost(1f) else 0.6f
                    for (i in 0 until chunk.size / 2) {
                        // Chirp: 2.4 kHz → 3.4 kHz each 200 ms, then a short gap (easy to hear and locate).
                        val pos = (t % (rate / 5)).toDouble() / (rate / 5)
                        val on = pos < 0.7
                        val f = 2400 + 1000 * pos
                        val zero: Short = 0
                        val s: Short = if (on) (sin(2 * PI * f * t / rate) * 32767 * gain).toInt().toShort() else zero
                        chunk[i * 2] = if (side != Side.RIGHT) s else zero
                        chunk[i * 2 + 1] = if (side != Side.LEFT) s else zero
                        t++
                    }
                    track.write(chunk, 0, chunk.size)
                }
            } finally {
                runCatching { track.stop() }
                track.release()
                if (rising) am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol, 0)
                _playing.value = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _playing.value = null
    }
}

/** What the phone is sending to the earbuds: Bluetooth codec, sample rate, bit depth, LDAC mode. */
data class CodecInfo(val codec: String, val sampleRate: String?, val bits: String?, val ldacMode: String?)

object PhoneCodec {
    /**
     * Reads the active A2DP codec. Android lets apps read it but only system apps change it,
     * so changing goes through Developer options. Never throws; null if unknown.
     */
    @SuppressLint("MissingPermission")
    suspend fun read(context: Context, address: String?): CodecInfo? {
        val bm = context.getSystemService(BluetoothManager::class.java) ?: return null
        val a2dp = suspendCancellableCoroutine<BluetoothA2dp?> { cont ->
            val ok = bm.adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (cont.isActive) cont.resume(proxy as BluetoothA2dp)
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (cont.isActive) cont.resume(null)
                }
            }, BluetoothProfile.A2DP)
            if (!ok && cont.isActive) cont.resume(null)
        } ?: return null
        return try {
            val device: BluetoothDevice = a2dp.connectedDevices.firstOrNull { address == null || it.address == address }
                ?: return null
            val status = a2dp.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java).invoke(a2dp, device) ?: return null
            val config = status.javaClass.getMethod("getCodecConfig").invoke(status) ?: return null
            fun int(m: String) = runCatching { config.javaClass.getMethod(m).invoke(config) as Number }.getOrNull()
            val type = int("getCodecType")?.toInt()
            val codec = when (type) {
                0 -> "SBC"; 1 -> "AAC"; 2 -> "aptX"; 3 -> "aptX HD"; 4 -> "LDAC"; 5 -> "LC3"; 6 -> "Opus"
                else -> "Codec $type"
            }
            val rate = when (int("getSampleRate")?.toInt()) {
                0x1 -> "44.1 kHz"; 0x2 -> "48 kHz"; 0x4 -> "88.2 kHz"; 0x8 -> "96 kHz"; 0x10 -> "176.4 kHz"; 0x20 -> "192 kHz"
                else -> null
            }
            val bits = when (int("getBitsPerSample")?.toInt()) {
                0x1 -> "16-bit"; 0x2 -> "24-bit"; 0x4 -> "32-bit"; else -> null
            }
            val ldac = if (type == 4) when (int("getCodecSpecific1")?.toLong()) {
                1000L -> "Best audio quality (990 kbps)"
                1001L -> "Balanced (660 kbps)"
                1002L -> "Best connection (330 kbps)"
                1003L -> "Adaptive"
                else -> null
            } else null
            CodecInfo(codec, rate, bits, ldac)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { bm.adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp) }
        }
    }
}
