package com.hfreebuds.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** A free-standing rounded panel for content that isn't a list row. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        
        modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } }
}

private class DialogSpec(val title: String, val options: List<Option>, val selected: Int?, val onSelect: (Int) -> Unit)

private fun labelOf(options: List<Option>, value: Int?): String? =
    options.firstOrNull { it.value == value }?.label ?: value?.let { "Option $it" }

// ============================================================ Gesture control

@Composable
fun GesturesPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val p = s.profile
    val tabs = buildList {
        if (s.doubleTap != null) add("Double-tap")
        if (s.tripleTap != null) add("Triple-tap")
        if (s.longTap != null) add("Press & hold")
        if (s.swipe != null) add("Swipe")
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<DialogSpec?>(null) }
    val current = tabs.getOrNull(tab.coerceAtMost((tabs.size - 1).coerceAtLeast(0)))

    fun tapOptions(available: List<Int>?): List<Option> {
        val base = Options.tap
        if (available == null) return base
        val known = base.filter { it.value in available || it.value == -1 }
        val extra = available.filter { v -> base.none { it.value == v } }.map { Option(it, "Option $it") }
        return known + extra
    }

    PageScaffold("Gesture control", onBack) {
        if (tabs.isEmpty() || p == null) {
            item { Panel { Text("Connect your earbuds to change their gestures.") } }
            return@PageScaffold
        }
        item {
            // Equal-ish tabs that never wrap: each gets space in proportion to its label.
            Column {
                Row(Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { i, name ->
                        val selected = tab == i
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(name.length.toFloat() + 2f)
                                .clickable(role = Role.Tab) { tab = i },
                        ) {
                            Text(
                                name, maxLines = 1, softWrap = false,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
                            )
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider()
            }
        }
        item {
            EarbudDiagram(
                spot = when (current) { "Swipe" -> TouchSpot.LOWER; else -> TouchSpot.UPPER },
                hold = current == "Press & hold",
            )
        }
        when (current) {
            "Double-tap" -> s.doubleTap?.let { g ->
                val o = tapOptions(s.doubleTapOptions)
                if (g.inCall != null && p.doubleTapInCall) {
                    item { GroupLabel("When there is a call") }
                    item {
                        NavRow(0, 1, "Either bud", labelOf(Options.tapInCall, g.inCall)) {
                            dialog = DialogSpec("Double-tap either bud", Options.tapInCall, g.inCall) { Earbuds.setDoubleTap(4, it) }
                        }
                    }
                    item { GroupLabel("When not on a call") }
                }
                sideRows(g, o, "Double-tap",
                    { side, v -> Earbuds.setDoubleTap(side, v) }) { dialog = it }
            }
            "Triple-tap" -> s.tripleTap?.let { g ->
                sideRows(g, s.tripleTapOptions?.let { tapOptions(it) } ?: Options.tap.filter { it.value in listOf(2, 7, -1) }, "Triple-tap",
                    { side, v -> Earbuds.setTripleTap(side, v) }) { dialog = it }
            }
            "Press & hold" -> s.longTap?.let { g ->
                val o = Options.longTap(p.longTapExtra)
                item { GroupLabel("Press & hold") }
                sideRows(g, o, "Press & hold", { side, v -> Earbuds.setLongTap(side, v) }, showHeader = false, rightEnabled = p.longTapRight) { dialog = it }
                s.longTapAnc?.let { a ->
                    item { GroupLabel("Noise control cycle") }
                    val rows = buildList {
                        if (a.left != null) add(1 to a.left)
                        if (a.right != null && p.longTapRight) add(2 to a.right)
                    }
                    rows.forEachIndexed { i, (side, value) ->
                        item {
                            val name = if (side == 1) "Left bud" else "Right bud"
                            NavRow(i, rows.size, name, labelOf(Options.longTapAnc, value)) {
                                dialog = DialogSpec("Noise control cycle, ${name.lowercase()}", Options.longTapAnc, value) {
                                    Earbuds.setLongTapAnc(side, it)
                                }
                            }
                        }
                    }
                    item { Note("Which noise control modes press and hold switches between.", Modifier.padding(horizontal = 12.dp)) }
                }
            }
            "Swipe" -> s.swipe?.let { v ->
                item {
                    NavRow(0, 1, "Swipe up/down", labelOf(Options.swipe, v)) {
                        dialog = DialogSpec("Swipe up/down", Options.swipe, v) { Earbuds.setSwipe(it) }
                    }
                }
            }
        }
        item { Untested(s, "gestures") }
    }
    dialog?.let { d -> ChoiceDialog(d.title, d.options, d.selected, d.onSelect) { dialog = null } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sideRows(
    g: Gesture, options: List<Option>, name: String,
    set: (side: Int, value: Int) -> Unit,
    showHeader: Boolean = true, rightEnabled: Boolean = true,
    open: (DialogSpec) -> Unit,
) {
    val rows = buildList {
        if (g.left != null) add(Triple(1, "Left bud", g.left))
        if (g.right != null && rightEnabled) add(Triple(2, "Right bud", g.right))
    }
    rows.forEachIndexed { i, (side, label, value) ->
        item {
            NavRow(i, rows.size, label, labelOf(options, value)) {
                open(DialogSpec("$name ${label.lowercase()}", options, value) { set(side, it) })
            }
        }
    }
    if (showHeader && rows.isEmpty()) item { Note("These earbuds don't report this gesture.") }
}

// ============================================================ Sound effects

private val presetBlurbs = mapOf(
    "Default" to "Natural and balanced sound.",
    "Bass boost" to "More powerful low end.",
    "Treble boost" to "Enjoy clear, singing high end.",
    "Voices" to "Bright and transparent, hear vocals in all their glory.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundEffectsPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val p = s.profile
    var editor by remember { mutableStateOf(false) }
    PageScaffold("Sound effects", onBack) {
        if (p == null || s.eqPreset == null) {
            item { Panel { Text("Connect your earbuds to change the sound effect.") } }
            return@PageScaffold
        }
        item { GroupLabel("Official preset EQ profiles") }
        val available = s.eqAvailable
        val presets = p.eqPresets.filter { available == null || it.value in available }.ifEmpty { p.eqPresets }
        presets.forEachIndexed { i, o ->
            item {
                RadioItem(i, presets.size, o.label, presetBlurbs[o.label], s.eqPreset == o.value) { Earbuds.setEq(o.value) }
            }
        }
        item { Untested(s, "eq") }
        item { GroupLabel("More EQ profiles") }
        item {
            Segment(0, 1, onClick = { editor = true }, role = Role.Button) {
                Text(
                    "Add custom EQ profiles", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
    if (editor) CustomEqSheet { editor = false }
}

private val eqBands = listOf("60", "125", "250", "500", "1K", "2K", "4K", "8K", "12K", "16K")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomEqSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hfreebuds", Context.MODE_PRIVATE) }
    val gains = remember {
        mutableStateListOf<Int>().apply {
            val saved = prefs.getString("customEq", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
            addAll(if (saved != null && saved.size == eqBands.size) saved else List(eqBands.size) { 0 })
        }
    }
    var saved by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Custom EQ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Effect name", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("My sound effect 1", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { gains.indices.forEach { gains[it] = 0 }; saved = false }) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                            Text("  Reset")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        eqBands.forEachIndexed { i, label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${gains[i]}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                VerticalGain(gains[i]) { gains[i] = it; saved = false }
                                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            Note("Adjust these while music is playing. Preview: custom curves are kept in the app for now; sending them to the earbuds isn't supported yet, because the official app's packets for it haven't been captured.")
            Button(
                onClick = { prefs.edit().putString("customEq", gains.joinToString(",")).apply(); saved = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(if (saved) "Saved ✓" else "Save") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** A vertical slider from -6 to +6 dB in 1 dB steps, drawn and dragged by hand. */
@Composable
private fun VerticalGain(value: Int, onChange: (Int) -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val thumb = MaterialTheme.colorScheme.onSurface
    Canvas(
        Modifier
            .width(26.dp)
            .height(190.dp)
            .pointerInput(Unit) {
                fun set(y: Float) {
                    val pad = 12.dp.toPx()
                    val f = ((y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
                    onChange((6 - f * 12).let { Math.round(it) }.coerceIn(-6, 6))
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    set(down.position.y)
                    while (true) {
                        val change = awaitPointerEvent().changes.first()
                        if (!change.pressed) break
                        set(change.position.y)
                        change.consume()
                    }
                }
            },
    ) {
        val pad = 12.dp.toPx()
        val usable = size.height - 2 * pad
        val x = size.width / 2
        val w = 8.dp.toPx()
        drawRoundRect(track, Offset(x - w / 2, pad), Size(w, usable), CornerRadius(w / 2))
        val zeroY = pad + usable / 2
        val valueY = pad + usable * (6 - value) / 12f
        drawRoundRect(
            fill, Offset(x - w / 2, minOf(zeroY, valueY)), Size(w, kotlin.math.abs(zeroY - valueY).coerceAtLeast(1f)),
            CornerRadius(w / 2),
        )
        drawCircle(thumb, 9.dp.toPx(), Offset(x, valueY))
    }
}

// ============================================================ Wear detection

@Composable
fun WearPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    PageScaffold("Wear detection", onBack) {
        item {
            SwitchItem(
                0, 1, "Smart wear detection", null, s.autoPause == true,
            ) { Earbuds.setAutoPause(it) }
        }
        item {
            Note(
                "Pause audio or switch the playback device when the earphones are removed. Auto-resume when they are put back in.",
                Modifier.padding(horizontal = 12.dp),
            )
        }
        s.inEar?.let { item { Note(if (it) "Right now: in your ears." else "Right now: not in your ears.", Modifier.padding(horizontal = 12.dp)) } }
    }
}

// ============================================================ Earbud settings

@Composable
fun EarbudSettingsPage(onBack: () -> Unit, openDual: () -> Unit, openUpdates: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val updaterState by LiveUpdater.controller.status.collectAsState()
    PageScaffold("Earbud settings", onBack) {
        item {
            Tile(
                "About & updates",
                Icons.Rounded.SystemUpdate,
                Modifier.fillMaxWidth(),
                updateStatus = updaterState,
            ) { openUpdates() }
        }
        if (s.dualEnabled != null) {
            item { GroupLabel("Connect preference") }
            item {
                SwitchItem(0, 1, "Multi connect", null, s.dualEnabled == true) { Earbuds.setDualEnabled(it) }
            }
            item {
                Note(
                    "Stay connected to multiple devices and switch between them seamlessly.",
                    Modifier.padding(horizontal = 12.dp),
                )
            }
            if (s.dualEnabled == true) item {
                NavRow(0, 1, "Connected devices", null, "Preferred device, connect, disconnect, forget") { openDual() }
            }
            item { Untested(s, "dual") }
        }
        if (s.hdAudio != null) {
            item { GroupLabel("Sound quality") }
            item {
                RadioItem(0, 2, "Prioritize connection quality", "Balance connection stability with sound quality.", s.hdAudio == false) {
                    Earbuds.setHdAudio(false)
                }
            }
            item {
                RadioItem(1, 2, "Prioritize sound quality", "High sound quality uses more power. Delay may occur in some scenarios.", s.hdAudio == true) {
                    Earbuds.setHdAudio(true)
                }
            }
        }
        s.lowLatency?.let { on ->
            item { Spacer(Modifier.height(6.dp)) }
            item { SwitchItem(0, 1, "Low audio latency", null, on) { Earbuds.setLowLatency(it) } }
            item {
                Note(
                    "To prevent choppy audio in environments with high network congestion, such as airports, this feature is disabled when your earphones are disconnected.",
                    Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        if (s.hdAudio == null && s.lowLatency == null && s.dualEnabled == null) {
            item { Panel { Text("These earbuds don't report any of these settings (or aren't connected).") } }
        }
    }
}

// ============================================================ Find device

@Composable
fun FindPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val context = LocalContext.current
    val playing by FindTone.playing.collectAsState()
    PageScaffold("Find Device", onBack) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EarbudDiagram(TouchSpot.NONE, modifier = Modifier.width(110.dp))
                    Text("L", style = MaterialTheme.typography.labelLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EarbudDiagram(TouchSpot.NONE, modifier = Modifier.width(110.dp))
                    Text("R", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item {
            Text(
                s.profile?.name ?: s.deviceName ?: "Your earbuds",
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item {
            Text(
                "If your earphones are nearby, you can play a sound to help you find them. Take them off first; the chirp gets louder.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FindButton("Play sound", "L", playing == FindTone.Side.LEFT, Modifier.weight(1f)) {
                    if (playing == FindTone.Side.LEFT) FindTone.stop() else FindTone.play(context, FindTone.Side.LEFT)
                }
                FindButton("Play sound", "R", playing == FindTone.Side.RIGHT, Modifier.weight(1f)) {
                    if (playing == FindTone.Side.RIGHT) FindTone.stop() else FindTone.play(context, FindTone.Side.RIGHT)
                }
            }
        }
        item {
            FindButton("Play sound", "L+R", playing == FindTone.Side.BOTH, Modifier.fillMaxWidth()) {
                if (playing == FindTone.Side.BOTH) FindTone.stop() else FindTone.play(context, FindTone.Side.BOTH)
            }
        }
        item {
            Note(
                "The earbuds have no known \"ring\" command, so the sound plays through this phone to the connected earbuds.",
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FindButton(label: String, badge: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (active) "Stop" else label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Text(
                    badge, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ============================================================ Connected devices (multi connect)

@Composable
fun DualPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val autoSupported = s.profile?.dualAutoConnect == true
    PageScaffold("Connected devices", onBack) {
        val devices = s.dualDevices.orEmpty()
        item { Note("★ marks the preferred device.", Modifier.padding(horizontal = 12.dp)) }
        if (devices.isEmpty()) item { Panel { Text("No devices reported yet. Turn on Multi connect and refresh.") } }
        devices.forEachIndexed { i, d ->
            item {
                var menu by remember { mutableStateOf(false) }
                Segment(i, devices.size) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { Earbuds.setDualPreferred(d.mac) }) {
                            Icon(if (d.preferred) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Preferred")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Note(
                                when {
                                    d.playing -> "Connected · playing"
                                    d.connected -> "Connected"
                                    else -> "Not connected"
                                } + (if (d.autoConnect == true) " · auto-connect" else ""),
                            )
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.ExpandMore, "More") }
                            DropdownMenu(
                                expanded = menu,
                                onDismissRequest = { menu = false },
                                modifier = Modifier.clip(RoundedCornerShape(22.dp)),
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (d.connected) "Disconnect" else "Connect") },
                                    leadingIcon = { Icon(if (d.connected) Icons.Rounded.BluetoothDisabled else Icons.Rounded.Bluetooth, null) },
                                    onClick = { menu = false; Earbuds.dualCommand(if (d.connected) 2 else 1, d.mac) },
                                )
                                if (autoSupported) DropdownMenuItem(
                                    text = { Text(if (d.autoConnect == true) "Turn off auto-connect" else "Turn on auto-connect") },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                                    onClick = { menu = false; Earbuds.dualCommand(if (d.autoConnect == true) 5 else 4, d.mac) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Forget this device") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = { menu = false; Earbuds.dualCommand(3, d.mac) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================ Voice prompts language

@Composable
fun LanguagePage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val languages = s.languages.orEmpty()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hfreebuds", Context.MODE_PRIVATE) }
    val key = "language_${s.address}"
    var saved by remember(s.address) { mutableStateOf(prefs.getString(key, null)) }
    // The earbuds don't report their language: show the one picked here, English until then.
    val english = languages.firstOrNull { it.equals("en-GB", true) || it.equals("en-US", true) }
        ?: languages.firstOrNull { it.startsWith("en", true) }
    val current = saved?.takeIf { it in languages } ?: english
    PageScaffold("Voice prompts language", onBack) {
        if (languages.isEmpty()) item { Panel { Text("These earbuds didn't report any languages.") } }
        languages.forEachIndexed { i, l ->
            item {
                RadioItem(i, languages.size, l, null, current == l) {
                    saved = l
                    prefs.edit().putString(key, l).apply()
                    Earbuds.setLanguage(l)
                }
            }
        }
        item { Note("English is shown until you choose another. The earbuds don't report which language is active.", Modifier.padding(horizontal = 12.dp)) }
    }
}

// ============================================================ Phone audio

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneAudioPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codec by remember { mutableStateOf<CodecInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    suspend fun load() { codec = PhoneCodec.read(context, s.address); loaded = true }
    LaunchedEffect(s.address) { load() }
    fun open(action: String) = runCatching { context.startActivity(Intent(action)) }

    PageScaffold("Phone audio", onBack, actions = {
        IconButton(onClick = { scope.launch { load() } }) { Icon(Icons.Rounded.Refresh, "Refresh") }
    }) {
        item {
            Panel {
                Text("Bluetooth codec", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val c = codec
                if (c != null) {
                    val details = listOfNotNull(c.sampleRate, c.bits)
                    Text("${c.codec}" + if (details.isEmpty()) "" else " · " + details.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                    c.ldacMode?.let { Note("LDAC: $it") }
                } else if (loaded) {
                    Note("Codec unknown (${PhoneCodec.lastError ?: "no reason"}). Play something on the earbuds, then refresh.")
                }
                Note("Android lets only the system change the codec. Turn on \"Prioritize sound quality\" in Earbud settings, then pick the codec and LDAC quality in Developer options → Bluetooth audio.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }, modifier = Modifier.weight(1f)) { Text("Codec settings", maxLines = 1) }
                    FilledTonalButton(onClick = { open(Settings.ACTION_BLUETOOTH_SETTINGS) }, modifier = Modifier.weight(1f)) { Text("Bluetooth", maxLines = 1) }
                }
            }
        }
        item {
            Panel {
                Text("Left / right balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Note("Android sets the balance in Accessibility → Audio adjustment. Use the test tones to check it.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pad = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    FilledTonalButton(onClick = { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        Text("Settings", maxLines = 1)
                    }
                    OutlinedButton(onClick = { FindTone.play(context, FindTone.Side.LEFT, seconds = 2, rising = false) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        Text("Test left", maxLines = 1)
                    }
                    OutlinedButton(onClick = { FindTone.play(context, FindTone.Side.RIGHT, seconds = 2, rising = false) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        Text("Test right", maxLines = 1)
                    }
                }
            }
        }
    }
}

// ============================================================ About / device information

@Composable
fun AboutPage(onBack: () -> Unit) {
    val s by Earbuds.state.collectAsState()
    val i = s.info
    val rows = listOfNotNull(
        s.profile?.name?.let { "Device name" to (s.deviceName ?: it) },
        i?.model?.let { "Model" to it },
        i?.subModel?.let { "Variant" to it },
        i?.firmware?.let { "Firmware" to it },
        i?.hardware?.let { "Hardware" to it },
        i?.serial?.let { "Serial number" to it },
        i?.leftSerial?.let { "Left earbud" to it },
        i?.rightSerial?.let { "Right earbud" to it },
        s.address?.let { "Bluetooth address" to it },
    )
    PageScaffold("About", onBack) {
        if (rows.isEmpty()) item { Panel { Text("Connect your earbuds to see their details.") } }
        rows.forEachIndexed { idx, (k, v) ->
            item {
                Segment(idx, rows.size) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(150.dp))
                        Text(v, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { GroupLabel("Shortcuts") }
        item {
            Panel {
                Text("Quick Settings tile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Note("Swipe down twice, tap the pencil and add \"Noise control\" to switch noise control without opening the app.")
            }
        }
    }
}

// ============================================================ Updates

@Composable
fun UpdatesPage(onBack: () -> Unit) {
    val experimental by LiveUpdater.experimentalEnabled.collectAsState()
    PageScaffold(
        "About & updates",
        onBack,
    ) {
        item {
            dev.n2k02.updater.N2K02UpdateScreen(
                controller = LiveUpdater.controller,
                installGuard = LiveUpdater.installGuard,
                experimentalEnabled = experimental,
                onExperimentalEnabledChange = LiveUpdater::setExperimentalEnabled,
            )
        }
        item {
            Note(
                "Earbud firmware updates aren't supported; use HUAWEI AI Life for those.",
                Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

// ============================================================ Connection log

@Composable
fun LogPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val lines by Earbuds.log.collectAsState()
    PageScaffold("Connection log", onBack, actions = {
        IconButton(onClick = {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("HFreebuds log", lines.joinToString("\n")))
        }) { Icon(Icons.Rounded.ContentCopy, "Copy log") }
    }) {
        item { Note("Packets sent to and received from the earbuds. Copy this if a setting doesn't work.", Modifier.padding(horizontal = 12.dp)) }
        item {
            Panel {
                Column(Modifier.heightIn(max = 560.dp).horizontalScroll(rememberScrollState())) {
                    if (lines.isEmpty()) Text("Nothing yet.")
                    lines.takeLast(80).forEach {
                        Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
