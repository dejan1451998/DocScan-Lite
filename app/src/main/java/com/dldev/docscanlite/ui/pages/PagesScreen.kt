package com.dldev.docscanlite.ui.pages

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dldev.docscanlite.DocumentViewModel
import com.dldev.docscanlite.core.storage.ExportManager

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
    viewModel: DocumentViewModel,
    onNavigateToCamera: () -> Unit,
    onRequestExportUri: () -> Unit,
    onNavigateToGallery: () -> Unit = {}
) {
    val pages by viewModel.pages.collectAsState()
    val exportUri by viewModel.exportUri.collectAsState()
    val context = LocalContext.current

    if (exportUri != null) {
        val shareIntent = ExportManager.createShareIntent(exportUri!!)
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        viewModel.setExportUri(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pages (${pages.size})") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCamera) {
                Icon(Icons.Filled.Add, contentDescription = "Add page")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pages yet. Tap + to scan a document.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pages, key = { it.id }) { page ->
                        val idx = pages.indexOf(page)
                        PageCard(
                            index = idx,
                            totalPages = pages.size,
                            page = page,
                            onDelete = { viewModel.deletePage(page.id) },
                            onMoveUp = {
                                if (idx > 0) viewModel.reorderPages(idx, idx - 1)
                            },
                            onMoveDown = {
                                if (idx < pages.size - 1) viewModel.reorderPages(idx, idx + 1)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveDocumentToGallery()
                            onNavigateToGallery()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", modifier = Modifier.padding(end = 8.dp))
                        Text("Save")
                    }
                    
                    Button(
                        onClick = onRequestExportUri,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Export", modifier = Modifier.padding(end = 8.dp))
                        Text("Export PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun PageCard(
    index: Int,
    totalPages: Int,
    page: com.dldev.docscanlite.domain.model.Page,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = page.bitmap.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Page ${index + 1}",
                modifier = Modifier.weight(1f)
            )

            Column {
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Text("\u25B2")
                }
                IconButton(onClick = onMoveDown, enabled = index < totalPages - 1) {
                    Text("\u25BC")
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete page")
            }
        }
    }
}
