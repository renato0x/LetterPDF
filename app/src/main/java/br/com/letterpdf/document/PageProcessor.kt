package br.com.letterpdf.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.min

sealed interface RenderedPageResult {
    data class Ready(val bitmap: Bitmap, val portrait: Boolean) : RenderedPageResult
    data class Failure(val message: String) : RenderedPageResult
}

fun renderPage(context: Context, page: DocumentPage): RenderedPageResult {
    val source = when (val loaded = loadUprightBitmap(context, page.source, maxSide = 4096)) {
        is PageBitmapResult.Ready -> loaded.bitmap
        is PageBitmapResult.Failure -> return RenderedPageResult.Failure(loaded.message)
    }
    val portrait = source.height >= source.width
    val outputWidth = if (portrait) 2480 else 3508
    val outputHeight = if (portrait) 3508 else 2480
    return try {
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val scale = min(outputWidth.toFloat() / source.width, outputHeight.toFloat() / source.height)
        val drawnWidth = (source.width * scale).toInt()
        val drawnHeight = (source.height * scale).toInt()
        val left = (outputWidth - drawnWidth) / 2
        val top = (outputHeight - drawnHeight) / 2
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, null, Rect(left, top, left + drawnWidth, top + drawnHeight), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        source.recycle()
        RenderedPageResult.Ready(output, portrait)
    } catch (_: OutOfMemoryError) {
        source.recycle()
        RenderedPageResult.Failure("A imagem é grande demais para este aparelho. Tente uma foto menor.")
    }
}
