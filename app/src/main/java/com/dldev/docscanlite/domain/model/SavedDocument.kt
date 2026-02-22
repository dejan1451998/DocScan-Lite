package com.dldev.docscanlite.domain.model

import android.graphics.Bitmap

data class SavedDocument(
    val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val thumbnail: Bitmap,
    val pageCount: Int
)
