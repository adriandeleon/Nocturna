package com.adl.nocturna.settings

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class CreditsTest {
    private val built = LocalDateTime(2026, 11, 19, 9, 5)

    @Test fun releaseVersionLabel() =
        assertEquals("Version 0.1.0 (1) · 2026-11-19 09:05", versionLabel("0.1.0", 1, built, debug = false, Locale.UK))

    @Test fun debugVersionLabel() =
        assertEquals("Version 0.2.0 (7) · 2026-11-19 09:05 · debug", versionLabel("0.2.0", 7, built, debug = true, Locale.UK))

    /** Gradle runs unit tests from the module directory; the repository root is one up. */
    private val notice: String = listOf(File("../NOTICE"), File("NOTICE")).first { it.isFile }.readText()

    private val credits = LIBRARY_CREDITS + DATA_CREDITS

    @Test fun everyCreditIsInNotice() = credits.forEach { c ->
        assertTrue("${c.name}: ${c.url} is not in NOTICE", c.url in notice)
    }

    /** NOTICE opens each entry with "Name — https://…"; the About screen must list each of them. */
    @Test fun everyNoticeEntryIsCredited() {
        val entryUrls = Regex("""^.+ — (https://\S+)$""", RegexOption.MULTILINE).findAll(notice).map { it.groupValues[1] }.toList()
        assertTrue("found no entries in NOTICE", entryUrls.isNotEmpty())
        val credited = credits.map { it.url }.toSet()
        entryUrls.forEach { assertTrue("$it is in NOTICE but not on the About screen", it in credited) }
    }

    @Test fun linksAreHttps() = (credits.map { it.url } + REPOSITORY_URL).forEach { assertTrue(it, it.startsWith("https://")) }
}
