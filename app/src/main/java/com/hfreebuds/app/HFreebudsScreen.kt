package com.hfreebuds.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import android.bluetooth.BluetoothManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal enum class Page {
    HOME, GESTURES, SOUND, WEAR, SETTINGS, FIND, DUAL, LANGUAGE, PHONE, ABOUT, UPDATES, LOG
}

@Composable
fun HFreebudsScreen(
    hasPermission: Boolean,
    onAskPermission: () -> Unit,
    savedAddress: () -> String?,
    onPick: (String) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var picking by remember { mutableStateOf(false) }
    var subpageFromSettings by rememberSaveable { mutableStateOf(false) }
    val back: () -> Unit = {
        val parent = if (page != Page.SETTINGS && subpageFromSettings) Page.SETTINGS else Page.HOME
        page = parent
        if (parent == Page.HOME) subpageFromSettings = false
    }
    BackHandler(enabled = page != Page.HOME) { back() }

    AnimatedContent(
        modifier = Modifier.fillMaxSize(),
        targetState = page,
        transitionSpec = {
            if (targetState == Page.HOME) {
                (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 5 } + fadeOut())
            }
        },
        label = "page",
    ) { p ->
        when (p) {
            Page.HOME -> HomePage(
                hasPermission, onAskPermission, savedAddress, onPick,
                onChoose = { picking = true }, open = { subpageFromSettings = false; page = it },
            )
            Page.GESTURES -> GesturesPage(back)
            Page.SOUND -> SoundEffectsPage(back)
            Page.WEAR -> WearPage(back)
            Page.SETTINGS -> EarbudSettingsPage(
                back,
                openDual = { subpageFromSettings = true; page = Page.DUAL },
                openUpdates = { subpageFromSettings = true; page = Page.UPDATES },
                openSubpage = { subpageFromSettings = true; page = it },
            )
            Page.FIND -> FindPage(back)
            Page.DUAL -> DualPage(back)
            Page.LANGUAGE -> LanguagePage(back)
            Page.PHONE -> PhoneAudioPage(back)
            Page.ABOUT -> AboutPage(back)
            Page.UPDATES -> UpdatesPage(back)
            Page.LOG -> LogPage(back)
        }
    }

    if (picking) DevicePicker(onDismiss = { picking = false }, onPick = { picking = false; onPick(it) })
}

// ============================================================ home

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomePage(
    hasPermission: Boolean,
    onAskPermission: () -> Unit,
    savedAddress: () -> String?,
    onPick: (String) -> Unit,
    onChoose: () -> Unit,
    open: (Page) -> Unit,
) {
    val s by Earbuds.state.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val updateStatus by LiveUpdater.controller.status.collectAsState()
    val connected = s.link == Link.CONNECTED
    val activity = LocalContext.current.activity()

    Scaffold(
        modifier = Modifier.nestedScroll(FooterScroll.connection).nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Home, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text("Home", fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    // The top title bar is reserved for page identity and HFreebuds branding.
                    dev.n2k02.updater.N2K02AppIcon(Modifier.size(28.dp))
                    Text("HFreebuds", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 6.dp, end = 12.dp))
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Home-only controls: one labeled Bluetooth picker on the left and Quit on the right.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(onClick = onChoose, enabled = hasPermission) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Choose earbuds")
                    }
                    FilledTonalButton(onClick = { activity?.finishAndRemoveTask() }) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Quit")
                    }
                }
            }
            if (!hasPermission) {
                item {
                    Panel {
                        Text("Allow Bluetooth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("HFreebuds talks to your earbuds over Bluetooth. Android calls this permission \"Nearby devices\".")
                        Button(onClick = onAskPermission) { Text("Allow") }
                    }
                }
                item { Tile("About & updates", Icons.Rounded.SystemUpdate, Modifier.fillMaxWidth(), updateStatus = updateStatus) { open(Page.UPDATES) } }
                return@LazyColumn
            }

            item { Hero(s, onChoose = onChoose, onRetry = { savedAddress()?.let(onPick) }) }
            if (connected) item { PhoneCard(s) }

            if (connected) {
                // ---- SOUND
                if (s.ancMode != null || s.eqPreset != null) item { GroupLabel("Sound") }
                if (s.ancMode != null) item { NoiseCard(s) }
                if (s.eqPreset != null) {
                    val label = s.profile?.eqPresets?.firstOrNull { it.value == s.eqPreset }?.label
                    item { NavRow(0, 1, "Sound effects", label, icon = Icons.Rounded.GraphicEq) { open(Page.SOUND) } }
                }
                // ---- INTERACTIONS
                if (s.doubleTap != null || s.tripleTap != null || s.longTap != null || s.swipe != null) {
                    item { GroupLabel("Interactions") }
                    item { NavRow(0, 1, "Gesture control", icon = Icons.Rounded.TouchApp) { open(Page.GESTURES) } }
                }
                // ---- EAR FIT
                if (s.autoPause != null) {
                    item { GroupLabel("Ear fit") }
                    item {
                        NavRow(0, 1, "Wear detection", if (s.autoPause == true) "On" else "Off", icon = Icons.Rounded.Hearing) { open(Page.WEAR) }
                    }
                }
            }

            // ---- MORE
            item { GroupLabel("More") }
            val tiles = buildList {
                add(Triple("Find Device", Icons.Rounded.LocationOn, Page.FIND))
                if (connected && (s.hdAudio != null || s.lowLatency != null || s.dualEnabled != null)) {
                    add(Triple("Earbud settings", Icons.Rounded.Tune, Page.SETTINGS))
                }
                if (connected && s.languages != null) add(Triple("Voice prompts", Icons.Rounded.Language, Page.LANGUAGE))
                add(Triple("Phone audio", Icons.Rounded.VolumeUp, Page.PHONE))
                add(Triple("Updates", Icons.Rounded.SystemUpdate, Page.UPDATES))
                add(Triple("About", Icons.Rounded.Info, Page.ABOUT))
                add(Triple("Logs", Icons.Rounded.Article, Page.LOG))
            }
            tiles.chunked(2).forEach { pair ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (title, icon, target) ->
                            Tile(
                                title, icon, Modifier.weight(1f),
                                updateStatus = updateStatus.takeIf { target == Page.UPDATES },
                            ) { open(target) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ============================================================ hero (connection + battery)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hero(s: EarbudsState, onChoose: () -> Unit, onRetry: () -> Unit) {
    val title = when (s.link) {
        Link.CONNECTED -> "Connected"
        Link.CONNECTING -> "Connecting…"
        Link.FAILED -> "Not connected"
        Link.DISCONNECTED -> if (s.address == null) "Choose your earbuds" else "Disconnected"
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        if (s.link == Link.CONNECTING) Icons.Rounded.BluetoothSearching else Icons.Rounded.Headphones, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    (s.profile?.name ?: s.deviceName)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                if (s.link == Link.CONNECTING) LoadingIndicator(Modifier.size(36.dp))
                else if (s.link == Link.CONNECTED) IconButton(onClick = { Earbuds.refreshAll() }) { Icon(Icons.Rounded.Refresh, "Refresh") }
            }
            if (s.link == Link.CONNECTED) {
                s.battery?.let { b ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (s.profile?.tws != false) {
                            BatteryDial("L", b.left, b.leftCharging)
                            BatteryDial("R", b.right, b.rightCharging)
                            BatteryDial("Case", b.case, b.caseCharging)
                        } else {
                            BatteryDial("Battery", b.global, b.leftCharging || b.caseCharging)
                        }
                    }
                }
                s.inEar?.let { Text(if (it) "In your ears" else "Not in your ears", style = MaterialTheme.typography.bodySmall) }
            }
            if (s.profile?.keys?.isEmpty() == true) {
                Text("Model not in the list: HFreebuds shows whatever these earbuds answer to.", style = MaterialTheme.typography.bodySmall)
            }
            s.error?.takeIf { s.link != Link.CONNECTED }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (s.link != Link.CONNECTED && s.link != Link.CONNECTING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.address != null) Button(onClick = onRetry) { Text("Connect") }
                    FilledTonalButton(onClick = onChoose) { Text("Choose earbuds") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryDial(label: String, level: Int?, charging: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
            CircularWavyProgressIndicator(progress = { (level ?: 0) / 100f }, modifier = Modifier.size(68.dp))
            Text(
                level?.let { "$it%" } ?: "—",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            if (charging) Icon(Icons.Rounded.Bolt, "Charging", Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ============================================================ noise control

private val modeBlurbs = mapOf(
    "Dynamic" to "Automatically adjust noise canceling based on your surroundings.",
    "Cozy" to "Ideal for places with a little noise.",
    "General" to "Ideal for noisy places.",
    "Smart" to "Automatically adjust noise canceling based on your surroundings.",
    "Light" to "Ideal for places with a little noise.",
    "Balanced" to "A balance of noise canceling and natural sound.",
    "Deep" to "Strongest noise canceling, for the noisiest places.",
    "Normal" to "Hear your surroundings as they are.",
    "Voice boost" to "Makes voices easier to hear.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoiseCard(s: EarbudsState) {
    val p = s.profile ?: return
    val modes = buildList {
        add(Triple(0, "Off", Icons.Rounded.NoiseControlOff))
        add(Triple(1, "Noise canceling", Icons.Rounded.VolumeOff))
        if (p.awareness || s.ancMode == 2) add(Triple(2, "Awareness", Icons.Rounded.NoiseAware))
    }
    val levels = when (s.ancMode) {
        1 -> p.cancelLevels
        2 -> p.awarenessLevels
        else -> emptyList()
    }
    val levelLabel = levels.firstOrNull { it.value == s.ancLevel }?.label
    var sheet by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .then(if (levels.isNotEmpty()) Modifier.clickable(role = Role.Button) { sheet = true } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(
                    "Noise control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (levels.isNotEmpty()) {
                    levelLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                modes.forEach { (mode, label, icon) ->
                    val selected = s.ancMode == mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.RadioButton) { Earbuds.setAnc(mode) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(if (selected) CircleShape else RoundedCornerShape(16.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                        ) {
                            Icon(
                                icon, label, Modifier.size(24.dp),
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
    if (sheet && levels.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)) {
                    Text(
                        "Select mode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { sheet = false }) { Icon(Icons.Rounded.Close, "Close") }
                }
                levels.forEachIndexed { i, o ->
                    RadioItem(i, levels.size, o.label, modeBlurbs[o.label], s.ancLevel == o.value) {
                        Earbuds.setAnc(s.ancMode ?: 1, o.value)
                        sheet = false
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================ this phone (tap to disconnect)

@Composable
private fun PhoneCard(s: EarbudsState) {
    var ask by remember { mutableStateOf(false) }
    val phone = Build.MODEL
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(phone, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bluetooth, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("  This phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(role = Role.Button) { ask = true },
            ) { Icon(Icons.Rounded.Smartphone, "Disconnect", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Disconnect?") },
            text = { Text("\u201C$phone\u201D will be disconnected.") },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    ask = false
                    // With Multi connect the earbuds list this phone: ask them to drop it. Otherwise close the app's link.
                    val me = s.dualDevices?.firstOrNull { it.connected && it.name.equals(phone, ignoreCase = true) }
                    if (me != null) Earbuds.dualCommand(2, me.mac) else Earbuds.disconnect()
                }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

// ============================================================ device picker

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


private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
