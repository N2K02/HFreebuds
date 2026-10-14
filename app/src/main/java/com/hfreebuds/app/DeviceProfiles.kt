package com.hfreebuds.app

/** One choice in a setting: the value the earbuds use and the label shown. */
data class Option(val value: Int, val label: String)

/**
 * What a model supports, and how its values are labelled. Features are also confirmed live:
 * a section only shows when the earbuds answer its read request, so an unlisted or unknown
 * model still gets whatever it actually supports.
 *
 * Sources: HuaweiPods (FreeBuds 5 packet captures) and OpenFreebuds (per-model feature lists).
 */
data class DeviceProfile(
    val name: String,
    /** Lowercase, letters/digits only, matched against the Bluetooth name. */
    val keys: List<String>,
    val tws: Boolean = true,
    val anc: Boolean = false,
    val awareness: Boolean = false,
    val cancelLevels: List<Option> = emptyList(),
    val awarenessLevels: List<Option> = emptyList(),
    val inEar: Boolean = false,
    val autoPause: Boolean = false,
    val quality: Boolean = false,
    val lowLatency: Boolean = false,
    val eqPresets: List<Option> = emptyList(),
    val doubleTap: Boolean = false,
    val doubleTapInCall: Boolean = false,
    val tripleTap: Boolean = false,
    val longTap: Boolean = false,
    val longTapRight: Boolean = false,
    val longTapAnc: Boolean = true,
    val longTapExtra: Boolean = false,
    val swipe: Boolean = false,
    val language: Boolean = false,
    val dualConnect: Boolean = false,
    val dualAutoConnect: Boolean = true,
    /** Features confirmed on this exact model by packet captures (others: best known). */
    val verified: Set<String> = emptySet(),
    /** Exact read requests captured from the official app, used instead of the generic ones. */
    val rawReads: Map<Int, String> = emptyMap(),
) {
    companion object {
        private val presets4 = listOf(Option(1, "Default"), Option(2, "Bass boost"), Option(3, "Treble boost"), Option(9, "Voices"))
        private val presetsPro = listOf(Option(5, "Default"), Option(1, "Bass boost"), Option(2, "Treble boost"), Option(9, "Voices"))
        private val levels4 = listOf(Option(3, "Smart"), Option(1, "Light"), Option(0, "Balanced"), Option(2, "Deep"))
        private val levels3 = listOf(Option(1, "Light"), Option(0, "Balanced"), Option(2, "Deep"))
        private val levelsPro3 = listOf(Option(1, "Smart"), Option(0, "Light"), Option(2, "Balanced"), Option(3, "Deep"))
        private val awarenessBoost = listOf(Option(2, "Normal"), Option(1, "Voice boost"))

        val FREEBUDS_5 = DeviceProfile(
            name = "HUAWEI FreeBuds 5",
            keys = listOf("freebuds5"),
            anc = true,
            // FreeBuds 5 (open-fit): Smart, Light and Balanced; no Deep, no awareness mode.
            cancelLevels = listOf(Option(3, "Smart"), Option(1, "Light"), Option(0, "Balanced")),
            inEar = true, autoPause = true, quality = true, lowLatency = true,
            eqPresets = presets4,
            doubleTap = true, doubleTapInCall = true, longTap = true, longTapRight = true, swipe = true,
            language = true, dualConnect = true,
            verified = setOf("battery", "anc", "autoPause", "quality", "lowLatency", "eq"),
            rawReads = mapOf(
                Cmd.AUTO_PAUSE_READ to "5A0005002B110100772A",
                Cmd.EQ_READ to "5A0005002B4A02008C46",
                Cmd.QUALITY_READ to "5A0005002BA30101F794",
            ),
        )

        val all: List<DeviceProfile> = listOf(
            FREEBUDS_5,
            DeviceProfile(
                "HUAWEI FreeBuds 5i", listOf("freebuds5i"), anc = true, awareness = true, cancelLevels = levels4,
                inEar = true, autoPause = true, quality = true, lowLatency = true, eqPresets = presets4,
                doubleTap = true, doubleTapInCall = true, tripleTap = true, longTap = true, longTapRight = true, swipe = true,
                language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds 6i", listOf("freebuds6i"), anc = true, awareness = true, cancelLevels = levels4,
                inEar = true, autoPause = true, quality = true, lowLatency = true, eqPresets = presets4,
                doubleTap = true, doubleTapInCall = true, tripleTap = true, longTap = true, longTapRight = true, swipe = true,
                language = true, dualConnect = true, dualAutoConnect = false,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds 4i", listOf("freebuds4i", "honorearbuds2", "honorearbuds2se", "honorearbuds2lite"), anc = true, awareness = true,
                inEar = true, autoPause = true, doubleTap = true, longTap = true, language = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds 3", listOf("freebuds3"), anc = true, inEar = true, doubleTap = true, language = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds Pro", listOf("freebudspro"), anc = true, awareness = true, cancelLevels = levels4,
                awarenessLevels = awarenessBoost, inEar = true, longTap = true, longTapRight = true, swipe = true,
                language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds Pro 2", listOf("freebudspro2"), anc = true, awareness = true, cancelLevels = levels3,
                inEar = true, autoPause = true, quality = true, eqPresets = presets4.take(3),
                longTap = true, longTapRight = true, swipe = true, language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds Pro 3", listOf("freebudspro3", "freebudspro4", "freeclip"), anc = true, awareness = true,
                cancelLevels = levelsPro3, awarenessLevels = awarenessBoost, inEar = true, autoPause = true, quality = true,
                lowLatency = true, eqPresets = presetsPro, doubleTap = true, longTap = true, longTapRight = true, swipe = true,
                language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds Pro 5", listOf("freebudspro5"), anc = true, awareness = true, cancelLevels = levels4,
                awarenessLevels = awarenessBoost, inEar = true, autoPause = true, quality = true, lowLatency = true,
                eqPresets = presetsPro, doubleTap = true, longTap = true, longTapRight = true, swipe = true,
                language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds SE", listOf("freebudsse"), doubleTap = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds SE 2", listOf("freebudsse2"), lowLatency = true, eqPresets = presets4,
                doubleTap = true, doubleTapInCall = true, tripleTap = true, longTap = true, longTapAnc = false,
            ),
            generic("HUAWEI FreeBuds 4").copy(keys = listOf("freebuds4")),
            // SE 3: no captures yet, so ask for everything and show only what it answers.
            generic("HUAWEI FreeBuds SE 3").copy(keys = listOf("freebudsse3")),
            DeviceProfile(
                "HUAWEI FreeBuds SE 4 ANC", listOf("freebudsse4"), anc = true, awareness = true, cancelLevels = levels4,
                lowLatency = true, eqPresets = presets4, doubleTap = true, doubleTapInCall = true, tripleTap = true,
                longTap = true, longTapRight = true, longTapExtra = true,
            ),
            DeviceProfile(
                "HUAWEI FreeClip 2", listOf("freeclip2"), autoPause = true, inEar = true, quality = true, lowLatency = true,
                eqPresets = presets4, doubleTap = true, tripleTap = true, longTap = true, longTapAnc = false,
                longTapExtra = true, swipe = true, language = true, dualConnect = true,
            ),
            DeviceProfile(
                "HUAWEI FreeBuds Studio", listOf("freebudsstudio"), tws = false, anc = true, awareness = true,
                cancelLevels = levels4, awarenessLevels = awarenessBoost, autoPause = true, eqPresets = presets4.take(3),
            ),
            DeviceProfile(
                "HUAWEI FreeLace Pro", listOf("freelacepro"), tws = false, anc = true, awareness = true,
                cancelLevels = levels3, language = true,
            ),
            DeviceProfile(
                "HUAWEI FreeLace Pro 2", listOf("freelacepro2"), tws = false, anc = true, awareness = true,
                cancelLevels = levels4, awarenessLevels = awarenessBoost, quality = true, lowLatency = true,
                longTap = true, language = true, dualConnect = true, dualAutoConnect = false,
            ),
        )

        /** Any other Huawei/Honor earbuds: try everything; only what answers is shown. */
        fun generic(name: String) = DeviceProfile(
            name = name, keys = emptyList(), anc = true, awareness = true, cancelLevels = levels4,
            awarenessLevels = awarenessBoost, inEar = true, autoPause = true, quality = true, lowLatency = true,
            eqPresets = presets4, doubleTap = true, doubleTapInCall = true, tripleTap = true, longTap = true,
            longTapRight = true, swipe = true, language = true, dualConnect = true,
        )

        /** Best match for a Bluetooth name: the longest matching key wins ("freebuds5i" over "freebuds5"). */
        fun forName(btName: String?): DeviceProfile {
            val n = btName.orEmpty().lowercase().filter { it.isLetterOrDigit() }
            return all.flatMap { p -> p.keys.map { k -> k to p } }
                .filter { (k, _) -> n.contains(k) }
                .maxByOrNull { it.first.length }?.second
                ?: generic(btName ?: "Huawei earbuds")
        }

        /** Names that look like Huawei / Honor audio devices (for the device list). */
        fun looksHuawei(btName: String?): Boolean {
            val n = btName.orEmpty().lowercase()
            return listOf("huawei", "freebuds", "freelace", "freeclip", "freearc", "honor earbuds", "honor").any { n.contains(it) }
        }
    }
}

// ---------------------------------------------------------------- option lists

object Options {
    val tap = listOf(
        Option(1, "Play/Pause"), Option(2, "Next song"),
        Option(7, "Previous song"), Option(0, "Wake voice assistant"), Option(-1, "None"),
    )
    val tapInCall = listOf(Option(0, "Answer/End call"), Option(-1, "None"))
    fun longTap(extra: Boolean) = buildList {
        add(Option(10, "Enable/Disable noise canceling"))
        if (extra) {
            add(Option(0, "Wake voice assistant"))
            add(Option(18, "Volume up"))
            add(Option(19, "Volume down"))
        }
        add(Option(-1, "None"))
    }
    val longTapAnc = listOf(
        Option(1, "Off ⇄ Noise cancelling"),
        Option(2, "Off → Cancelling → Awareness"),
        Option(3, "Cancelling ⇄ Awareness"),
        Option(4, "Off ⇄ Awareness"),
    )
    val swipe = listOf(Option(0, "Adjust volume"), Option(-1, "None"))
}
