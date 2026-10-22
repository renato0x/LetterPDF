package br.com.letterpdf.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.letterpdf.document.DocumentPage
import br.com.letterpdf.document.PageList
import br.com.letterpdf.pdf.DestinationWriteResult
import br.com.letterpdf.pdf.ExportScreen
import br.com.letterpdf.pdf.PdfExportResult
import br.com.letterpdf.pdf.PdfSavedScreen
import br.com.letterpdf.pdf.copyPdfToDestination
import br.com.letterpdf.pdf.createA4Pdf
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen { HOME, PAGES, EXPORT, SAVED }

@Composable
fun LetterPdfApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("letterpdf", 0) }
    var themeMode by rememberSaveable {
        mutableStateOf(runCatching { ThemeMode.valueOf(preferences.getString("theme_mode", ThemeMode.SYSTEM.name).orEmpty()) }.getOrDefault(ThemeMode.SYSTEM))
    }
    val scope = rememberCoroutineScope()
    val pages = remember(context) {
        (context as? ComponentActivity)?.let { activity ->
            ViewModelProvider(activity)[DocumentPagesViewModel::class.java].pages
        } ?: mutableStateListOf()
    }
    var removePageId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<List<DocumentPage>?>(null) }
    var savedPdf by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var discardDraft by remember { mutableStateOf(false) }

    BackHandler(enabled = screen != Screen.HOME) {
        when (screen) {
            Screen.EXPORT -> screen = Screen.PAGES
            Screen.PAGES -> if (pages.isEmpty()) screen = Screen.HOME else discardDraft = true
            Screen.SAVED -> {
                pages.clear()
                savedPdf = null
                screen = Screen.HOME
            }
            Screen.HOME -> Unit
        }
    }
    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val scanned = result?.pages.orEmpty().map { page -> DocumentPage(source = page.imageUri) }
            if (scanned.isEmpty()) {
                message = "Nenhuma página foi recebida do scanner."
            } else {
                pages.addAll(scanned.take(20 - pages.size))
                screen = Screen.PAGES
            }
        }
    }
    val startDocumentScan: () -> Unit = {
        val activity = context as? Activity
        val vacancies = 20 - pages.size
        when {
            vacancies == 0 -> message = "Este PDF já tem 20 páginas."
            activity == null -> message = "Não foi possível abrir o scanner neste aparelho."
            else -> {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(false)
                    .setPageLimit(vacancies)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()
                GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener {
                        message = "Scanner indisponível. Verifique o Google Play Services e a rede no primeiro uso, ou escolha imagens."
                    }
            }
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        if (selected.isNotEmpty()) {
            val vacancies = 20 - pages.size
            if (vacancies == 0) {
                message = "Este PDF já tem 20 páginas."
            } else {
                val accepted = selected.take(vacancies)
                pages.addAll(accepted.map { uri -> DocumentPage(source = uri) })
                screen = Screen.PAGES
                if (selected.size > accepted.size) {
                    message = "Foram adicionadas ${accepted.size} imagens. O limite é 20 páginas."
                }
            }
        }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        val snapshot = pendingExport
        pendingExport = null
        if (destination != null && snapshot != null) {
            saving = true
            scope.launch {
                val failure = withContext(Dispatchers.IO) {
                    when (val created = createA4Pdf(context, snapshot)) {
                        is PdfExportResult.Failure -> created.message
                        is PdfExportResult.Ready -> when (val copied = copyPdfToDestination(context, created.file, destination)) {
                            DestinationWriteResult.Saved -> null
                            is DestinationWriteResult.Failure -> copied.message
                        }
                    }
                }
                saving = false
                if (failure == null) {
                    savedPdf = destination
                    screen = Screen.SAVED
                } else {
                    message = failure
                }
            }
        }
    }
    LetterPdfTheme(themeMode) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onScan = startDocumentScan,
                    onGallery = { galleryPicker.launch(arrayOf("image/jpeg", "image/png", "image/heic", "image/heif")) },
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        preferences.edit().putString("theme_mode", mode.name).apply()
                    },
                )
                Screen.PAGES -> PageList(
                    pages = pages,
                    onMove = { pageId, steps ->
                        val from = pages.indexOfFirst { it.id == pageId }
                        val to = from + steps
                        if (from in pages.indices && to in pages.indices && from != to) {
                            pages.add(to, pages.removeAt(from))
                        }
                    },
                    onRemove = { pageId -> removePageId = pageId },
                    onAddCamera = {
                        startDocumentScan()
                    },
                    onAddGallery = { galleryPicker.launch(arrayOf("image/jpeg", "image/png", "image/heic", "image/heif")) },
                    onFinish = { screen = Screen.EXPORT },
                    onCancel = { if (pages.isEmpty()) screen = Screen.HOME else discardDraft = true },
                )
                Screen.EXPORT -> ExportScreen(
                    defaultName = defaultPdfName(),
                    saving = saving,
                    onSave = { writtenName ->
                        pendingExport = pages.toList()
                        savePdf.launch(pdfFileName(writtenName))
                    },
                    onBack = { screen = Screen.PAGES },
                )
                Screen.SAVED -> PdfSavedScreen(
                    fileName = savedPdf?.lastPathSegment.orEmpty(),
                    onOpen = {
                        savedPdf?.let { uri ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (_: ActivityNotFoundException) {
                                message = "Não há aplicativo para abrir PDF neste aparelho."
                            }
                        }
                    },
                    onShare = {
                        savedPdf?.let { uri ->
                            try {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Compartilhar PDF"))
                            } catch (_: ActivityNotFoundException) {
                                message = "Não há aplicativo para compartilhar PDF neste aparelho."
                            }
                        }
                    },
                    onNew = {
                        pages.clear()
                        savedPdf = null
                        screen = Screen.HOME
                    },
                )
            }
        }
    }
    removePageId?.let { pageId ->
        val index = pages.indexOfFirst { it.id == pageId }
        if (index == -1) {
            removePageId = null
            return@let
        }
        AlertDialog(
            onDismissRequest = { removePageId = null },
            title = { Text("Remover página?") },
            text = { Text("A página ${index + 1} será removida deste documento.") },
            confirmButton = {
                Button(onClick = {
                    val currentIndex = pages.indexOfFirst { it.id == pageId }
                    if (currentIndex != -1) pages.removeAt(currentIndex)
                    removePageId = null
                }) { Text("Remover") }
            },
            dismissButton = { OutlinedButton(onClick = { removePageId = null }) { Text("Cancelar") } },
        )
    }
    if (discardDraft) {
        AlertDialog(
            onDismissRequest = { discardDraft = false },
            title = { Text("Descartar documento?") },
            text = { Text("As ${pages.size} páginas não salvas serão descartadas.") },
            confirmButton = {
                Button(onClick = {
                    pages.clear()
                    discardDraft = false
                    screen = Screen.HOME
                }) { Text("Descartar") }
            },
            dismissButton = { OutlinedButton(onClick = { discardDraft = false }) { Text("Continuar editando") } },
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("LetterPDF") },
            text = { Text(text) },
            confirmButton = { Button(onClick = { message = null }) { Text("Entendi") } },
        )
    }
}

@Composable
private fun HomeScreen(
    onScan: () -> Unit,
    onGallery: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                DocumentMark(colors.primary)
            Spacer(Modifier.height(28.dp))
                Text("LetterPDF", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
                Text("Escaneie ou junte imagens em um PDF. Sem conta.", fontSize = 18.sp, color = colors.onSurfaceVariant)
            }
            ThemeMenu(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Escanear documento" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) { Text("Escanear documento", fontSize = 18.sp) }
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Escolher imagens" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            ) { Text("Escolher imagens", fontSize = 18.sp) }
            Text("Suas páginas ficam neste aparelho. No primeiro uso, o scanner pode ser preparado.", color = colors.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ThemeMenu(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Tema: ${themeMode.label()}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    onClick = { onThemeModeChange(mode); expanded = false },
                )
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}

@Composable
private fun DocumentMark(color: Color) {
    Canvas(modifier = Modifier.size(52.dp)) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 16.dp.toPx()), end = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), 16.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height - 16.dp.toPx()), end = androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height - 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), size.height - 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height - 16.dp.toPx()), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

private fun defaultPdfName(): String = "Documento-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.pdf"

private fun pdfFileName(writtenName: String): String {
    val cleaned = writtenName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "-")
    val base = if (cleaned.isBlank()) defaultPdfName().removeSuffix(".pdf") else cleaned.removeSuffix(".pdf")
    return "$base.pdf"
}

class DocumentPagesViewModel : ViewModel() {
    val pages = mutableStateListOf<DocumentPage>()
}

