package com.github.luckywheel.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import com.github.luckywheel.domain.scaledSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ImageKind(val maxLongSide: Int, val png: Boolean) {
    /** PNG keeps transparency (LG-01). */
    LOGO(1024, true),
    BACKGROUND(1600, false),
}

/**
 * Copies picked images into app-private storage, downscaled (LG-02, TH-05), so deleting the original
 * from the gallery does not affect the app.
 */
class ImageStore(private val context: Context) {
    private val dir get() = File(context.filesDir, "images")

    /** Returns the stored file's path, or null if the image could not be read. */
    suspend fun import(uri: Uri, kind: ImageKind): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder also applies EXIF orientation.
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = scaledSize(info.size.width, info.size.height, kind.maxLongSide)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            dir.mkdirs()
            val ext = if (kind.png) "png" else "jpg"
            val file = File(dir, "${kind.name.lowercase()}_${System.currentTimeMillis()}.$ext")
            file.outputStream().use { out ->
                val format = if (kind.png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, if (kind.png) 100 else 88, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** Deletes a file this store created; other paths are ignored. */
    fun delete(path: String?) {
        val file = path?.let(::File) ?: return
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }

    companion object {
        fun decode(path: String): Bitmap? = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
}
