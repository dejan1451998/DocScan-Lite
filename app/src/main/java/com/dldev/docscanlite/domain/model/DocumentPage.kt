package com.dldev.docscanlite.domain.model

import android.graphics.Bitmap

data class DocumentPage(
    val id: Long = 0,
    val documentId: Long,
    val pageNumber: Int,
    val originalImage: Bitmap,
    val annotatedImage: Bitmap? = null
)
