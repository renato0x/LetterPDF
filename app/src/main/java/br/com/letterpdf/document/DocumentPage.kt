package br.com.letterpdf.document

import android.net.Uri
import java.util.UUID

data class DocumentPage(
    val id: String = UUID.randomUUID().toString(),
    val source: Uri,
)
