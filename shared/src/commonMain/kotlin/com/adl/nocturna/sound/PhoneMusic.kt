package com.adl.nocturna.sound

import com.adl.nocturna.data.Station

/**
 * Whether the station plays music the user picked on the phone (an audio file, or a folder of them) rather
 * than a web stream or a built-in sleep sound. The phone's platform makes these stations and plays them.
 */
val Station.isOnPhone: Boolean
    get() = sleepSound == null && !url.startsWith("http://") && !url.startsWith("https://")

/** A file in a folder the user picked: its [path] inside the folder ("Disc 1/03 Rain.mp3"), and its [uri]. */
data class FolderFile(val path: String, val mimeType: String?, val uri: String)

/** How a folder of music plays: which of its files, and in what order. */
object PhoneMusic {
    /** A folder plays at most this many tracks, so a whole music library doesn't take minutes to open. */
    const val MAX_TRACKS = 1000

    /** File endings of audio, for file managers that don't say a file's type. */
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "ogg", "oga", "opus", "flac", "wav", "mka", "amr", "3gp")

    /** Types that are audio although they don't start with "audio/". */
    private val AUDIO_TYPES = setOf("application/ogg", "application/x-flac")

    fun isAudio(path: String, mimeType: String?): Boolean {
        val type = mimeType?.lowercase()
        if (type != null && (type.startsWith("audio/") || type in AUDIO_TYPES)) return true
        // Only a type that says nothing lets the file ending decide; a picture named .mp3 stays out.
        if (type != null && type != "application/octet-stream") return false
        return path.substringAfterLast('/').substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS
    }

    /**
     * The audio among [files], in the order a person would sort them by name: "2 Rain" before "10 Rain", each
     * subfolder's tracks together, upper and lower case alike. At most [MAX_TRACKS].
     */
    fun playlist(files: List<FolderFile>): List<FolderFile> =
        files.filter { isAudio(it.path, it.mimeType) }
            .sortedWith { a, b -> naturalCompare(a.path, b.path) }
            .take(MAX_TRACKS)

    /** Compares by name with runs of digits compared as numbers, ignoring case; ties fall back to the plain text. */
    fun naturalCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val ea = digitsEnd(a, i)
                val eb = digitsEnd(b, j)
                val na = a.substring(i, ea).trimStart('0')
                val nb = b.substring(j, eb).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = ea
                j = eb
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (c != 0) return c
                i++
                j++
            }
        }
        val rest = (a.length - i) - (b.length - j)
        return if (rest != 0) rest else a.compareTo(b)
    }

    private fun digitsEnd(s: String, from: Int): Int {
        var k = from
        while (k < s.length && s[k].isDigit()) k++
        return k
    }

    /** A station name from a file's name: "03 Night Rain.mp3" → "03 Night Rain". */
    fun stationName(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return (if (dot > 0) fileName.substring(0, dot) else fileName).trim().ifEmpty { fileName }
    }
}
