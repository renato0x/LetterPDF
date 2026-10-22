package br.com.letterpdf.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun ExportScreen(defaultName: String, saving: Boolean, onSave: (String) -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    val save = { if (!saving) onSave(name) }
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Salvar PDF", color = colors.onBackground, fontSize = 29.sp)
        Spacer(Modifier.height(10.dp))
        Text("Dê um nome ao arquivo. Pode deixar em branco.", color = colors.onSurfaceVariant, fontSize = 17.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (!saving) name = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Nome do PDF" },
            singleLine = true,
            label = { Text("Nome do arquivo") },
            supportingText = { Text("Em branco: $defaultName") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            enabled = !saving,
        )
        Spacer(Modifier.height(22.dp))
        if (saving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(
                "Criando PDF. Aguarde até concluir.",
                color = colors.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = save,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Escolher local e salvar PDF" },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
        ) { Text(if (saving) "Criando PDF…" else "Escolher local para salvar", fontSize = 17.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Voltar às páginas") }
    }
}

@Composable
fun PdfSavedScreen(fileName: String, onOpen: () -> Unit, onShare: () -> Unit, onNew: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("PDF salvo", color = colors.onBackground, fontSize = 30.sp)
        Spacer(Modifier.height(10.dp))
        Text("Seu documento está pronto.", color = colors.onSurfaceVariant, fontSize = 17.sp)
        if (fileName.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(fileName, color = colors.onSurface, fontSize = 15.sp)
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) { Text("Abrir PDF", fontSize = 17.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Compartilhar") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Novo documento") }
    }
}
