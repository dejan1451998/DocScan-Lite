package com.dldev.docscanlite.domain.model

import android.graphics.Bitmap

data class Page(
    val id: Int,
    val bitmap: Bitmap,
    val extractedText: String? = null
)

enum class ImageFilter {
    ORIGINAL, GRAYSCALE, BW
}
