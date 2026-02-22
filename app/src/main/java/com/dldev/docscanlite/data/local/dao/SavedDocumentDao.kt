package com.dldev.docscanlite.data.local.dao

import androidx.room.*
import com.dldev.docscanlite.data.local.entity.SavedDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDocumentDao {
    
    @Query("SELECT * FROM saved_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<SavedDocumentEntity>>
    
    @Query("SELECT * FROM saved_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: Long): SavedDocumentEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: SavedDocumentEntity): Long
    
    @Update
    suspend fun updateDocument(document: SavedDocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: SavedDocumentEntity)
    
    @Query("DELETE FROM saved_documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)
}
