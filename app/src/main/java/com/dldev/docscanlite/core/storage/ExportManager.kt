package com.dldev.docscanlite.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream
import com.dldev.docscanlite.core.pdf.PdfExporter
import com.dldev.docscanlite.domain.model.Page

object ExportManager {

    fun createExportIntent(): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "DocScanLite_Export.pdf")
        }
    }

    fun writePdf(context: Context, uri: Uri, pages: List<Page>) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "w")
            ?: throw IllegalStateException("Could not open URI for writing")
        pfd.use { descriptor ->
            FileOutputStream(descriptor.fileDescriptor).use { stream ->
                PdfExporter.export(pages, stream)
            }
        }
    }

    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
