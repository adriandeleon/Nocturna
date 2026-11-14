// The framework ExifInterface reads streams reliably from API 25 on, and minSdk is 27: no extra library needed.
@file:SuppressLint("ExifInterface")

package com.adl.nocturna.day

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The photo the user picked as day mode's background. A downscaled copy lives in the app's own files,
 * so it keeps working after the original is moved or deleted, and no storage permission is needed.
 */
object DayPhoto {
    private const val FILE = "day_background.jpg"
    private const val MAX_SIDE = 2560

    private fun file(context: Context) = File(context.filesDir, FILE)

    /** Copies the photo at [uri] in; false if it can't be read as an image. */
    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE) }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching false
            val rotation = resolver.openInputStream(uri)?.use { exifRotation(exifOf(it)) } ?: 0
            val upright = if (rotation == 0) decoded else
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            tmp.renameTo(file(context))
        }.getOrDefault(false)
    }

    /** The saved photo, or null if there is none or it can't be read (e.g. before the first unlock). */
    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(file(context).path)?.asImageBitmap() }.getOrNull()
    }

    private fun exifOf(stream: java.io.InputStream) = ExifInterface(stream)

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun exifRotation(exif: ExifInterface): Int = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

/** The largest power-of-two step that still leaves the longer side at least [maxSide] pixels. */
fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}
