package com.hfreebuds.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ------------------------------------------------------------ page frame

/** A sub-screen: large collapsing title with a back arrow, content as a lazy list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(FooterScroll.connection).nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = { actions() },
                scrollBehavior = scroll,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

// ------------------------------------------------------------ grouped lists (Expressive style)

private val outer = 28.dp
private val inner = 6.dp

fun groupShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) outer else inner, topEnd = if (index == 0) outer else inner,
    bottomStart = if (index == count - 1) outer else inner, bottomEnd = if (index == count - 1) outer else inner,
)

/** Small coloured category label above a group, like "SOUND" or "EAR FIT". */
@Composable
fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Explains that a feature wasn't checked on this exact model. */
@Composable
fun Untested(s: EarbudsState, feature: String) {
    val p = s.profile ?: return
    if (p.verified.isNotEmpty() && feature !in p.verified) {
        Note(
            "Not tested on the ${p.name} yet: if a change doesn't stick, the earbuds don't support it.",
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** One segment of a grouped list: a rounded surface whose corners depend on its place in the group. */
@Composable
fun Segment(
    index: Int, count: Int, modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null, role: Role? = null,
    content: @Composable () -> Unit,
) {
    val base = Modifier.fillMaxWidth().clip(groupShape(index, count))
    val m = if (onClick != null) base.clickable(role = role, onClick = onClick) else base
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = groupShape(index, count),
        modifier = modifier.then(m),
    ) { content() }
}

/** Title on the left, current value and a chevron on the right. */
@Composable
fun NavRow(
    index: Int, count: Int, title: String, value: String? = null, subtitle: String? = null,
    icon: ImageVector? = null, onClick: () -> Unit,
) {
    Segment(index, count, onClick = onClick, role = Role.Button) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).heightIn(min = 32.dp),
        ) {
            if (icon != null) IconBadge(icon, Modifier.padding(end = 16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Note(it) }
            }
            if (value != null) {
                Text(
                    value, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp).widthIn(max = 190.dp),
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchItem(index: Int, count: Int, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Segment(index, count, onClick = { onChange(!checked) }, role = Role.Switch) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Note(it) }
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
fun RadioItem(index: Int, count: Int, title: String, subtitle: String?, selected: Boolean, onSelect: () -> Unit) {
    Segment(index, count, onClick = onSelect, role = Role.RadioButton) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Note(it) }
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Icon(icon, null, Modifier.size((size * 0.55f).dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** A square-ish tile for the "More" grid. */
@Composable
fun Tile(title: String, icon: ImageVector, modifier: Modifier = Modifier, subtitle: String? = null, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.clip(RoundedCornerShape(28.dp)).clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp).heightIn(min = 76.dp), verticalArrangement = Arrangement.SpaceBetween) {
            IconBadge(icon)
            Spacer(Modifier.height(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                subtitle?.let { Note(it) }
            }
        }
    }
}

// ------------------------------------------------------------ dialog with radio choices

/** "Double-tap left bud": a list of radio choices and Cancel, like the original app. */
@Composable
fun ChoiceDialog(
    title: String, options: List<Option>, selected: Int?,
    onSelect: (Int) -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { o ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.RadioButton) { onSelect(o.value); onDismiss() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(o.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        RadioButton(selected = o.value == selected, onClick = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ------------------------------------------------------------ earbud drawing

enum class TouchSpot { NONE, UPPER, LOWER }

/** A simple stylised earbud (drawn, no picture needed) with the touch area highlighted. */
@Composable
fun EarbudDiagram(spot: TouchSpot, hold: Boolean = false, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val metal = Brush.verticalGradient(listOf(scheme.surfaceContainerHighest, scheme.outlineVariant))
    val accent = scheme.primary
    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val cx = size.width / 2
        val h = size.height
        val headR = h * 0.20f
        val headC = Offset(cx - headR * 0.15f, h * 0.30f)
        // stem
        val stemW = h * 0.20f
        drawRoundRect(
            brush = metal,
            topLeft = Offset(cx - stemW / 2, h * 0.34f),
            size = Size(stemW, h * 0.60f),
            cornerRadius = CornerRadius(stemW / 2, stemW / 2),
        )
        // head
        drawCircle(brush = metal, radius = headR, center = headC)
        drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = headR * 0.38f, center = headC.copy(x = headC.x - headR * 0.1f))
        val dotY = when (spot) {
            TouchSpot.UPPER -> h * 0.40f
            TouchSpot.LOWER -> h * 0.70f
            TouchSpot.NONE -> return@Canvas
        }
        val dot = Offset(cx, dotY)
        if (hold) drawCircle(color = accent.copy(alpha = 0.25f), radius = stemW * 0.95f, center = dot)
        drawCircle(color = accent.copy(alpha = 0.40f), radius = stemW * 0.62f, center = dot)
        drawCircle(color = accent, radius = stemW * 0.36f, center = dot)
    }
}
