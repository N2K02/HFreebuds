package com.hfreebuds.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceLanguagePolicyTest {
    @Test fun selectsAdvertisedEnglishLocale() {
        assertEquals("en-GB", chooseEnglishVoiceLanguage(listOf("zh-CN", "en-GB")))
        assertEquals("en-US", chooseEnglishVoiceLanguage(listOf("en-AU", "en-US")))
        assertEquals("en-AU", chooseEnglishVoiceLanguage(listOf("zh", " en-AU ")))
        assertEquals("EN", chooseEnglishVoiceLanguage(listOf("EN")))
    }

    @Test fun ignoresUnsupportedAndNonLanguagePrefixes() {
        assertNull(chooseEnglishVoiceLanguage(emptyList()))
        assertNull(chooseEnglishVoiceLanguage(listOf("zh-CN", "fr-FR")))
        assertNull(chooseEnglishVoiceLanguage(listOf("English", "enoch", "en-")))
    }

    @Test fun selectedLocaleIsEncodedInLanguageCommand() {
        val selected = chooseEnglishVoiceLanguage(listOf("zh-CN", "en-GB"))!!
        val command = HuaweiPacket.write(Cmd.LANGUAGE_WRITE, 1 to selected, 2 to 1)
        assertEquals("en-GB", command.params[1]!!.toString(Charsets.UTF_8))
        assertEquals(1, command.byte(2))
    }
}
