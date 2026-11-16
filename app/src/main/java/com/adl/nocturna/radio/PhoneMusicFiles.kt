package com.adl.nocturna.radio

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.adl.nocturna.data.Station
import com.adl.nocturna.sound.FolderFile
import com.adl.nocturna.sound.PhoneMusic
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

/**
 * Music the user picked on the phone with the system's file picker. That needs no storage permission: the
 * picker grants access to what was picked, and [keep] makes the grant last. A folder station plays every audio
 * file in the folder and its subfolders, in the order [PhoneMusic.playlist] gives.
 */
object PhoneMusicFiles {
    private const val TAG = "PhoneMusicFiles"

    /** Subfolders deeper than this are left out. */
    private const val MAX_DEPTH = 4

    /** Stop looking once this many files are found, so a whole library doesn't take long to open. */
    private const val MAX_FILES = 4 * PhoneMusic.MAX_TRACKS

    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    fun isOnPhone(item: MediaItem): Boolean = item.requestMetadata.mediaUri?.scheme == ContentResolver.SCHEME_CONTENT

    /** Keeps access to [uri] after the app closes and the phone restarts; false when the system won't. */
    fun keep(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.onFailure { Log.w(TAG, "Can't keep access to $uri", it) }.isSuccess

    /** Gives up the access [keep] kept, once no station plays [url] any more. */
    fun forget(context: Context, url: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(url.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** A station for a picked file or folder, named after it. */
    fun station(context: Context, uri: Uri): Station {
        val folder = DocumentsContract.isTreeUri(uri)
        val named = if (folder) DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri)) else uri
        val name = runCatching { displayName(context, named) }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?: uri.toString()
        return Station(if (folder) name else PhoneMusic.stationName(name), uri.toString())
    }

    /** What [uri] plays: the file itself, or a folder's audio files in order (none if it has none). */
    fun tracks(context: Context, uri: Uri): List<Uri> {
        if (!DocumentsContract.isTreeUri(uri)) return listOf(uri)
        val files = mutableListOf<FolderFile>()
        collect(context, uri, DocumentsContract.getTreeDocumentId(uri), "", 0, files)
        return PhoneMusic.playlist(files).map { it.uri.toUri() }
    }

    /**
     * [items] made playable off the main thread: each one on the phone becomes its file, or a folder's files,
     * named like the station. A folder that can't be read, or holds no audio, keeps its URI, so the player
     * reports it as something that can't play.
     */
    fun playable(context: Context, items: List<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
        val app = context.applicationContext
        return executor.submit<MutableList<MediaItem>> {
            items.flatMap { item ->
                val uri = item.requestMetadata.mediaUri
                val tracks = if (uri != null && isOnPhone(item)) {
                    runCatching { tracks(app, uri) }.onFailure { Log.w(TAG, "Can't read $uri", it) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                if (tracks.isEmpty()) {
                    listOf(item.buildUpon().setUri(uri).build())
                } else {
                    tracks.map { item.buildUpon().setMediaId(it.toString()).setUri(it).build() }
                }
            }.toMutableList()
        }
    }

    private fun collect(context: Context, tree: Uri, parentId: String, prefix: String, depth: Int, into: MutableList<FolderFile>) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val folders = mutableListOf<Pair<String, String>>()
        context.contentResolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext() && into.size < MAX_FILES) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: id
                val type = c.getString(2)
                if (type == DocumentsContract.Document.MIME_TYPE_DIR) {
                    folders += id to "$prefix$name/"
                } else {
                    into += FolderFile(prefix + name, type, DocumentsContract.buildDocumentUriUsingTree(tree, id).toString())
                }
            }
        }
        if (depth < MAX_DEPTH) {
            for ((id, path) in folders) if (into.size < MAX_FILES) collect(context, tree, id, path, depth + 1, into)
        }
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
