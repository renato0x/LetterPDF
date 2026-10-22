package br.com.letterpdf.document

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PageList(
    pages: List<DocumentPage>,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var previewPageId by remember { mutableStateOf<String?>(null) }
    val previewPage = pages.firstOrNull { it.id == previewPageId }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Revisar ordem", color = colors.onBackground, fontSize = 28.sp)
                Text("Confira antes de criar o PDF.", color = colors.onSurfaceVariant)
            }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
        if (pages.isEmpty()) {
            EmptyPages(onAddCamera = onAddCamera, onAddGallery = onAddGallery)
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.primaryContainer) {
                Text(
                    "${pages.size} de 20 páginas · toque em uma página para conferir.",
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                    PageRow(
                        page = page,
                        number = index + 1,
                        total = pages.size,
                        canMoveUp = index > 0,
                        canMoveDown = index < pages.lastIndex,
                        onPreview = { previewPageId = page.id },
                        onMoveUp = { onMove(page.id, -1) },
                        onMoveDown = { onMove(page.id, 1) },
                        onRemove = { onRemove(page.id) },
                    )
                }
            }
            OutlinedButton(
                onClick = onAddCamera,
                enabled = pages.size < 20,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Escanear outra página") }
            OutlinedButton(
                onClick = onAddGallery,
                enabled = pages.size < 20,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Adicionar imagens") }
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Criar PDF com ${pages.size} páginas" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) { Text("Finalizar PDF", fontSize = 18.sp) }
        }
    }
    previewPage?.let { page -> PagePreview(page = page, onDismiss = { previewPageId = null }) }
}

@Composable
private fun ColumnScope.EmptyPages(onAddCamera: () -> Unit, onAddGallery: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nenhuma página ainda", color = colors.onBackground, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text("Comece pelo scanner ou pelas imagens.", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddCamera,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
        ) { Text("Escanear documento") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAddGallery,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Escolher imagens") }
    }
}

@Composable
private fun PageRow(
    page: DocumentPage,
    number: Int,
    total: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPreview: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onPreview)
            .semantics { contentDescription = "Página $number de $total. Toque para conferir." },
        color = colors.surface,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = colors.primaryContainer, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(number.toString(), color = colors.onPrimaryContainer) }
            }
            Spacer(Modifier.width(10.dp))
            PageThumbnail(page)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Página $number", color = colors.onSurface, fontSize = 18.sp)
                Text("Toque para conferir", color = colors.onSurfaceVariant, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("Subir") }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("Descer") }
                TextButton(onClick = onRemove) { Text("Remover", color = colors.error) }
            }
        }
    }
}

@Composable
private fun PageThumbnail(page: DocumentPage) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var result by remember(page.source) { mutableStateOf<PageBitmapResult?>(null) }
    LaunchedEffect(page.source) { result = withContext(Dispatchers.IO) { loadUprightBitmap(context, page.source, maxSide = 256) } }
    Box(
        modifier = Modifier.size(width = 62.dp, height = 84.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        (result as? PageBitmapResult.Ready)?.bitmap?.let { bitmap ->
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun PagePreview(page: DocumentPage, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var result by remember(page.source) { mutableStateOf<PageBitmapResult?>(null) }
    LaunchedEffect(page.source) { result = withContext(Dispatchers.IO) { loadUprightBitmap(context, page.source, maxSide = 2048) } }
    val latestResult by rememberUpdatedState(result)
    DisposableEffect(page.source) {
        onDispose { (latestResult as? PageBitmapResult.Ready)?.bitmap?.recycle() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Página", color = colors.onSurface, fontSize = 22.sp)
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 520.dp), contentAlignment = Alignment.Center) {
                    when (val loaded = result) {
                        null -> Text("Abrindo página…", color = colors.onSurface)
                        is PageBitmapResult.Ready -> Image(
                            bitmap = loaded.bitmap.asImageBitmap(),
                            contentDescription = "Prévia da página",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        is PageBitmapResult.Failure -> Text(loaded.message, color = colors.onSurface)
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                ) { Text("Fechar") }
            }
        }
    }
}
