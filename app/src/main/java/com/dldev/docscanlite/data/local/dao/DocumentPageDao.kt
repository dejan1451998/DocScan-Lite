package com.dldev.docscanlite.data.local.dao

import androidx.room.*
import com.dldev.docscanlite.data.local.entity.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentPageDao {
    
    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesByDocumentId(documentId: Long): Flow<List<DocumentPageEntity>>
    
    @Query("SELECT * FROM document_pages WHERE id = :pageId")
    suspend fun getPageById(pageId: Long): DocumentPageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>)
    
    @Update
    suspend fun updatePage(page: DocumentPageEntity)
    
    @Delete
    suspend fun deletePage(page: DocumentPageEntity)
    
    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: Long)
}
