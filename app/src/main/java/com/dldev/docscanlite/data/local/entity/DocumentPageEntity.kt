package com.dldev.docscanlite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dldev.docscanlite.data.local.converter.BitmapConverter
import android.graphics.Bitmap

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = SavedDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
@TypeConverters(BitmapConverter::class)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val documentId: Long,
    
    val pageNumber: Int,
    
    val originalImage: Bitmap,
    
    val annotatedImage: Bitmap?
)
