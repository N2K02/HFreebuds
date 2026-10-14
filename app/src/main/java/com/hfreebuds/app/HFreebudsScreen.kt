package com.hfreebuds.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HFreebudsScreen(
    hasPermission: Boolean,
    onAskPermission: () -> Unit,
    savedAddress: () -> String?,
    onPick: (String) -> Unit,
) {
    val s by Earbuds.state.collectAsState()
    var picking by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(FooterScroll.connection).nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        s.deviceName ?: "HFreebuds",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { picking = true }) { Icon(Icons.Rounded.SwapHoriz, "Choose earbuds") }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { UpdateFooterBar("HFreebuds") },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { UpdateCard() }
            if (!hasPermission) {
                item {
                    Section("Allow Bluetooth", Icons.Rounded.Bluetooth) {
                        Text("HFreebuds talks to your earbuds over Bluetooth. Android calls this permission \"Nearby devices\".")
                        Button(onClick = onAskPermission) { Text("Allow") }
                    }
                }
                return@LazyColumn
            }
            item { ConnectionCard(s, onPick = { picking = true }, onRetry = { savedAddress()?.let(onPick) }) }
            if (s.link == Link.CONNECTED) {
                s.battery?.let { b -> item { BatteryCard(b, s.inEar, s.profile?.tws != false) } }
                if (s.ancMode != null) item { NoiseCard(s) }
                if (s.eqPreset != null || s.hdAudio != null || s.lowLatency != null) item { SoundCard(s) }
                if (s.doubleTap != null || s.tripleTap != null || s.longTap != null || s.swipe != null) item { GesturesCard(s) }
                if (s.autoPause != null) item { WearCard(s) }
                if (s.dualEnabled != null) item { DualCard(s) }
                s.languages?.let { item { LanguageCard(it) } }
            }
            item { FindCard() }
            item { PhoneAudioCard(s.address) }
            if (s.info != null) item { InfoCard(s) }
            item { LogCard() }
        }
    }

    if (picking) DevicePicker(onDismiss = { picking = false }, onPick = { picking = false; onPick(it) })
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun Section(title: String, icon: ImageVector, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "Not tested on this model yet" for features the FreeBuds 5 captures don't cover. */
@Composable
private fun Untested(s: EarbudsState, feature: String) {
    val p = s.profile ?: return
    if (p.verified.isNotEmpty() && feature !in p.verified) {
        Note("Not tested on the ${p.name} yet: if a change doesn't stick, the earbuds don't support it.")
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Note(it) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A labelled dropdown: "Left earbud   [Play / pause ▾]". */
@Composable
private fun OptionRow(title: String, options: List<Option>, selected: Int?, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(options.firstOrNull { it.value == selected }?.label ?: selected?.let { "Option $it" } ?: "—", maxLines = 1)
                Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(o.label) }, onClick = { open = false; onSelect(o.value) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- cards

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectionCard(s: EarbudsState, onPick: () -> Unit, onRetry: () -> Unit) {
    val (title, icon) = when (s.link) {
        Link.CONNECTED -> "Connected" to Icons.Rounded.Bluetooth
        Link.CONNECTING -> "Connecting…" to Icons.Rounded.BluetoothSearching
        Link.FAILED -> "Not connected" to Icons.Rounded.Bluetooth
        Link.DISCONNECTED -> (if (s.address == null) "Choose your earbuds" else "Disconnected") to Icons.Rounded.Bluetooth
    }
    Section(title, icon, action = {
        if (s.link == Link.CONNECTING) LoadingIndicator(Modifier.size(32.dp))
        else if (s.link == Link.CONNECTED) IconButton(onClick = { Earbuds.refreshAll() }) { Icon(Icons.Rounded.Refresh, "Refresh") }
    }) {
        s.profile?.let { p ->
            Text(p.name, style = MaterialTheme.typography.bodyLarge)
            if (p.keys.isEmpty()) Note("Model not in the list: HFreebuds shows whatever these earbuds answer to.")
        }
        s.error?.takeIf { s.link != Link.CONNECTED }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (s.link != Link.CONNECTED && s.link != Link.CONNECTING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.address != null) Button(onClick = onRetry) { Text("Connect") }
                FilledTonalButton(onClick = onPick) { Text("Choose earbuds") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryCard(b: Battery, inEar: Boolean?, tws: Boolean) {
    Section("Battery", Icons.Rounded.Bolt, action = {
        IconButton(onClick = { Earbuds.refreshBattery() }) { Icon(Icons.Rounded.Refresh, "Refresh battery") }
    }) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            if (tws) {
                BatteryDial("Left", b.left, b.leftCharging)
                BatteryDial("Right", b.right, b.rightCharging)
                BatteryDial("Case", b.case, b.caseCharging)
            } else {
                BatteryDial("Battery", b.global, b.leftCharging || b.caseCharging)
            }
        }
        inEar?.let { Note(if (it) "In your ears" else "Not in your ears") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryDial(label: String, level: Int?, charging: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
            CircularWavyProgressIndicator(progress = { (level ?: 0) / 100f }, modifier = Modifier.size(76.dp))
            Text(
                level?.let { "$it%" } ?: "—",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            if (charging) Icon(Icons.Rounded.Bolt, "Charging", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoiseCard(s: EarbudsState) {
    val p = s.profile ?: return
    val modes = buildList {
        add(0 to "Off")
        add(1 to "Noise cancelling")
        if (p.awareness || s.ancMode == 2) add(2 to "Awareness")
    }
    Section("Noise control", Icons.Rounded.Hearing) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = s.ancMode == mode,
                    onClick = { Earbuds.setAnc(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        val levels = when (s.ancMode) {
            1 -> p.cancelLevels
            2 -> p.awarenessLevels
            else -> emptyList()
        }
        AnimatedVisibility(levels.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (s.ancMode == 1) "Strength" else "Awareness", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.forEach { o ->
                        FilterChip(
                            selected = s.ancLevel == o.value,
                            onClick = { Earbuds.setAnc(s.ancMode ?: 1, o.value) },
                            label = { Text(o.label) },
                        )
                    }
                }
                if (s.ancMode == 1 && levels.any { it.label == "Smart" }) {
                    Note("Smart adjusts the strength to the noise around you.")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundCard(s: EarbudsState) {
    val p = s.profile ?: return
    Section("Sound", Icons.Rounded.Equalizer) {
        if (s.eqPreset != null) {
            Text("Equalizer", style = MaterialTheme.typography.labelLarge)
            val presets = p.eqPresets.filter { s.eqAvailable == null || it.value in s.eqAvailable }.ifEmpty { p.eqPresets }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { o ->
                    FilterChip(selected = s.eqPreset == o.value, onClick = { Earbuds.setEq(o.value) }, label = { Text(o.label) })
                }
            }
            Untested(s, "eq")
        }
        s.hdAudio?.let {
            HorizontalDivider()
            SwitchRow(
                "HD audio",
                "On: best sound quality (uses LDAC when the phone allows it). Off: steadier connection.",
                it,
            ) { on -> Earbuds.setHdAudio(on) }
        }
        s.lowLatency?.let {
            HorizontalDivider()
            SwitchRow("Low latency", "Less audio delay for games and videos. Uses a bit more battery.", it) { on ->
                Earbuds.setLowLatency(on)
            }
        }
    }
}

@Composable
private fun GesturesCard(s: EarbudsState) {
    val p = s.profile ?: return
    Section("Gestures", Icons.Rounded.TouchApp) {
        fun opts(available: List<Int>?): List<Option> {
            val base = Options.tap
            if (available == null) return base
            return base.filter { it.value in available || it.value == -1 } +
                available.filter { v -> base.none { it.value == v } }.map { Option(it, "Option $it") }
        }
        s.doubleTap?.let { g ->
            Text("Double-tap", style = MaterialTheme.typography.labelLarge)
            val o = opts(s.doubleTapOptions)
            if (g.left != null) OptionRow("Left earbud", o, g.left) { Earbuds.setDoubleTap(1, it) }
            if (g.right != null) OptionRow("Right earbud", o, g.right) { Earbuds.setDoubleTap(2, it) }
            if (g.inCall != null && p.doubleTapInCall) OptionRow("During calls", Options.tapInCall, g.inCall) { Earbuds.setDoubleTap(4, it) }
        }
        s.tripleTap?.let { g ->
            HorizontalDivider()
            Text("Triple-tap", style = MaterialTheme.typography.labelLarge)
            val o = opts(s.tripleTapOptions)
            if (g.left != null) OptionRow("Left earbud", o, g.left) { Earbuds.setTripleTap(1, it) }
            if (g.right != null) OptionRow("Right earbud", o, g.right) { Earbuds.setTripleTap(2, it) }
        }
        s.longTap?.let { g ->
            HorizontalDivider()
            Text("Press and hold", style = MaterialTheme.typography.labelLarge)
            val o = Options.longTap(p.longTapExtra)
            if (g.left != null) OptionRow("Left earbud", o, g.left) { Earbuds.setLongTap(1, it) }
            if (g.right != null && p.longTapRight) OptionRow("Right earbud", o, g.right) { Earbuds.setLongTap(2, it) }
            s.longTapAnc?.let { a ->
                Note("When press and hold switches noise control, it cycles through:")
                if (a.left != null) OptionRow("Left", Options.longTapAnc, a.left) { Earbuds.setLongTapAnc(1, it) }
                if (a.right != null && p.longTapRight) OptionRow("Right", Options.longTapAnc, a.right) { Earbuds.setLongTapAnc(2, it) }
            }
        }
        s.swipe?.let { v ->
            HorizontalDivider()
            OptionRow("Swipe", Options.swipe, v) { Earbuds.setSwipe(it) }
        }
        Untested(s, "gestures")
    }
}

@Composable
private fun WearCard(s: EarbudsState) {
    Section("Wear detection", Icons.Rounded.Tune) {
        SwitchRow("Pause when taken out", "Pauses music when you take an earbud out, and plays again when you put it back.", s.autoPause == true) {
            Earbuds.setAutoPause(it)
        }
    }
}

@Composable
private fun DualCard(s: EarbudsState) {
    val autoSupported = s.profile?.dualAutoConnect == true
    Section("Dual-device connection", Icons.Rounded.Devices, action = {
        Switch(checked = s.dualEnabled == true, onCheckedChange = { Earbuds.setDualEnabled(it) })
    }) {
        Note("Stay connected to two phones or computers at once. ★ = preferred device.")
        if (s.dualEnabled == true) {
            s.dualDevices.orEmpty().forEach { d ->
                var menu by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(d.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            when {
                                d.playing -> "Connected · playing"
                                d.connected -> "Connected"
                                else -> "Not connected"
                            } + (if (d.autoConnect == true) " · auto-connect" else ""),
                        )
                    },
                    leadingContent = {
                        IconButton(onClick = { Earbuds.setDualPreferred(d.mac) }) {
                            Icon(if (d.preferred) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Preferred")
                        }
                    },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.ExpandMore, "More") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (d.connected) "Disconnect" else "Connect") },
                                    onClick = { menu = false; Earbuds.dualCommand(if (d.connected) 2 else 1, d.mac) },
                                )
                                if (autoSupported) DropdownMenuItem(
                                    text = { Text(if (d.autoConnect == true) "Turn off auto-connect" else "Turn on auto-connect") },
                                    onClick = { menu = false; Earbuds.dualCommand(if (d.autoConnect == true) 5 else 4, d.mac) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Forget this device") },
                                    onClick = { menu = false; Earbuds.dualCommand(3, d.mac) },
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
        Untested(s, "dual")
    }
}

@Composable
private fun LanguageCard(languages: List<String>) {
    Section("Voice prompts language", Icons.Rounded.Language) {
        val options = languages.mapIndexed { i, l -> Option(i, l) }
        var chosen by rememberSaveable { mutableIntStateOf(-1) }
        OptionRow("Language", options, chosen.takeIf { it >= 0 }) { i ->
            chosen = i
            Earbuds.setLanguage(languages[i])
        }
        Note("The earbuds don't report which language is active; choose one to change it.")
    }
}

@Composable
private fun FindCard() {
    val context = LocalContext.current
    val playing by FindTone.playing.collectAsState()
    Section("Find my earbuds", Icons.Rounded.Search) {
        Text(
            "Plays a loud chirp on the earbuds, getting louder. Take them off first. They need to be connected to this phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FindTone.Side.entries.forEach { side ->
                val label = when (side) { FindTone.Side.LEFT -> "Left"; FindTone.Side.RIGHT -> "Right"; FindTone.Side.BOTH -> "Both" }
                if (playing == side) {
                    Button(onClick = { FindTone.stop() }, modifier = Modifier.weight(1f)) { Text("Stop") }
                } else {
                    FilledTonalButton(onClick = { FindTone.play(context, side) }, modifier = Modifier.weight(1f)) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun PhoneAudioCard(address: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codec by remember { mutableStateOf<CodecInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    suspend fun load() { codec = PhoneCodec.read(context, address); loaded = true }
    LaunchedEffect(address) { load() }
    fun open(action: String) = runCatching { context.startActivity(Intent(action)) }

    Section("Phone audio", Icons.Rounded.VolumeUp, action = {
        IconButton(onClick = { scope.launch { load() } }) { Icon(Icons.Rounded.Refresh, "Refresh") }
    }) {
        val c = codec
        if (c != null) {
            val details = listOfNotNull(c.sampleRate, c.bits)
            Text(
                "Codec: ${c.codec}" + if (details.isEmpty()) "" else " · " + details.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
            )
            c.ldacMode?.let { Note("LDAC: $it") }
        } else if (loaded) {
            Note("Codec unknown: play something on the earbuds, then refresh.")
        }
        Note("Android lets only the system change the codec. Turn on HD audio above, then pick the codec and LDAC quality in Developer options → Bluetooth audio.")
        FlowRow2 {
            FilledTonalButton(onClick = { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }) { Text("Codec settings") }
            FilledTonalButton(onClick = { open(Settings.ACTION_BLUETOOTH_SETTINGS) }) { Text("Bluetooth") }
        }
        HorizontalDivider()
        Text("Left / right balance", style = MaterialTheme.typography.labelLarge)
        Note("Android sets the balance in Accessibility → Audio adjustment. Use the test tones to check it.")
        FlowRow2 {
            FilledTonalButton(onClick = { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text("Balance settings") }
            OutlinedButton(onClick = { FindTone.play(context, FindTone.Side.LEFT, seconds = 2, rising = false) }) { Text("Test left") }
            OutlinedButton(onClick = { FindTone.play(context, FindTone.Side.RIGHT, seconds = 2, rising = false) }) { Text("Test right") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow2(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
private fun InfoCard(s: EarbudsState) {
    val i = s.info ?: return
    Section("About these earbuds", Icons.Rounded.Info) {
        listOfNotNull(
            i.model?.let { "Model" to it },
            i.subModel?.let { "Variant" to it },
            i.firmware?.let { "Firmware" to it },
            i.hardware?.let { "Hardware" to it },
            i.serial?.let { "Serial number" to it },
            i.leftSerial?.let { "Left earbud" to it },
            i.rightSerial?.let { "Right earbud" to it },
            s.address?.let { "Bluetooth address" to it },
        ).forEach { (k, v) ->
            Row {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(140.dp))
                Text(v, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LogCard() {
    val context = LocalContext.current
    val lines by Earbuds.log.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    Section("Connection log", Icons.Rounded.Subject, action = {
        Row {
            if (open) IconButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("HFreebuds log", lines.joinToString("\n")))
            }) { Icon(Icons.Rounded.ContentCopy, "Copy log") }
            IconButton(onClick = { open = !open }) { Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "Show log") }
        }
    }) {
        if (!open) {
            Note("Packets sent to and received from the earbuds. Useful if a setting doesn't work.")
        } else {
            Column(Modifier.heightIn(max = 360.dp).horizontalScroll(rememberScrollState())) {
                lines.takeLast(60).forEach {
                    Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- device picker

@SuppressLint("MissingPermission")
@Composable
private fun DevicePicker(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val devices = remember {
        runCatching {
            context.getSystemService(BluetoothManager::class.java).adapter.bondedDevices
                .map { (it.alias ?: it.name ?: it.address) to it.address }
        }.getOrDefault(emptyList())
            .sortedWith(compareBy({ !DeviceProfile.looksHuawei(it.first) }, { it.first.lowercase() }))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your earbuds") },
        text = {
            if (devices.isEmpty()) {
                Text("No paired devices. Pair your earbuds in Android's Bluetooth settings first.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(devices) { (name, address) ->
                        val huawei = DeviceProfile.looksHuawei(name)
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(if (huawei) DeviceProfile.forName(name).name else address) },
                            leadingContent = { Icon(Icons.Rounded.Bluetooth, null, tint = if (huawei) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.clickable { onPick(address) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
