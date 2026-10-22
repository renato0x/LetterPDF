package br.com.letterpdf.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import br.com.letterpdf.document.DocumentPage
import br.com.letterpdf.document.RenderedPageResult
import br.com.letterpdf.document.renderPage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

sealed interface PdfExportResult {
    data class Ready(val file: File) : PdfExportResult
    data class Failure(val message: String) : PdfExportResult
}

sealed interface DestinationWriteResult {
    data object Saved : DestinationWriteResult
    data class Failure(val message: String) : DestinationWriteResult
}

fun createA4Pdf(context: Context, pages: List<DocumentPage>): PdfExportResult {
    if (pages.isEmpty()) return PdfExportResult.Failure("Adicione pelo menos uma página antes de salvar.")
    val temporary = File(context.cacheDir, "letterpdf-export-${UUID.randomUUID()}.pdf")
    val document = PdfDocument()
    try {
        pages.forEachIndexed { index, page ->
            when (val rendered = renderPage(context, page)) {
                is RenderedPageResult.Failure -> {
                    document.close()
                    temporary.delete()
                    return PdfExportResult.Failure("Página ${index + 1}: ${rendered.message}")
                }
                is RenderedPageResult.Ready -> {
                    val pageWidth = if (rendered.portrait) A4_WIDTH else A4_HEIGHT
                    val pageHeight = if (rendered.portrait) A4_HEIGHT else A4_WIDTH
                    val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                    pdfPage.canvas.drawColor(Color.WHITE)
                    pdfPage.canvas.drawBitmap(rendered.bitmap, null, Rect(0, 0, pageWidth, pageHeight), null)
                    document.finishPage(pdfPage)
                    rendered.bitmap.recycle()
                }
            }
        }
        FileOutputStream(temporary).use(document::writeTo)
        document.close()
        return PdfExportResult.Ready(temporary)
    } catch (_: IOException) {
        document.close()
        temporary.delete()
        return PdfExportResult.Failure("Não foi possível criar o PDF. Verifique o espaço disponível.")
    } catch (_: OutOfMemoryError) {
        document.close()
        temporary.delete()
        return PdfExportResult.Failure("As imagens exigem mais memória do que este aparelho possui. Tente menos páginas.")
    }
}

fun copyPdfToDestination(context: Context, temporary: File, destination: Uri): DestinationWriteResult {
    return try {
        context.contentResolver.openOutputStream(destination)?.use { output ->
            FileInputStream(temporary).use { input -> input.copyTo(output) }
        } ?: return DestinationWriteResult.Failure("Não foi possível abrir o local escolhido.")
        temporary.delete()
        DestinationWriteResult.Saved
    } catch (_: IOException) {
        temporary.delete()
        DestinationWriteResult.Failure("Não foi possível salvar no local escolhido.")
    } catch (_: SecurityException) {
        temporary.delete()
        DestinationWriteResult.Failure("Sem permissão para salvar neste local.")
    }
}

private const val A4_WIDTH = 595
private const val A4_HEIGHT = 842
