package com.dldev.docscanlite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dldev.docscanlite.data.local.converter.BitmapConverter
import android.graphics.Bitmap

@Entity(tableName = "saved_documents")
@TypeConverters(BitmapConverter::class)
data class SavedDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    
    val createdAt: Long,
    
    val thumbnail: Bitmap,
    
    val pageCount: Int
)
