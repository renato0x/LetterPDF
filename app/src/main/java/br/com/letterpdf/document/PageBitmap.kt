package br.com.letterpdf.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.max

sealed interface PageBitmapResult {
    data class Ready(val bitmap: Bitmap) : PageBitmapResult
    data class Failure(val message: String) : PageBitmapResult
}

fun loadUprightBitmap(context: Context, uri: Uri, maxSide: Int): PageBitmapResult {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    } catch (_: FileNotFoundException) {
        return PageBitmapResult.Failure("A imagem não está mais disponível.")
    } catch (_: SecurityException) {
        return PageBitmapResult.Failure("Sem permissão para abrir esta imagem.")
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return PageBitmapResult.Failure("Não foi possível ler esta imagem.")

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
    val decoded = try {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: SecurityException) {
        null
    } ?: return PageBitmapResult.Failure("Não foi possível abrir esta imagem.")

    val orientation = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    } catch (_: SecurityException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    return PageBitmapResult.Ready(decoded.upright(orientation))
}

private fun Bitmap.upright(orientation: Int): Bitmap {
    val transform = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
    if (transform.isIdentity) return this
    return Bitmap.createBitmap(this, 0, 0, width, height, transform, true).also { if (it !== this) recycle() }
}
