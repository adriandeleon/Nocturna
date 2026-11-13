package com.adl.nocturna.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CreditsTest {
    @Test fun releaseVersionLabel() = assertEquals("Version 0.1.0 (1)", versionLabel("0.1.0", 1, debug = false))

    @Test fun debugVersionLabel() = assertEquals("Version 0.2.0 (7) · debug", versionLabel("0.2.0", 7, debug = true))

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
