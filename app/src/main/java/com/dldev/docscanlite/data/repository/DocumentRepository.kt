package com.dldev.docscanlite.data.repository

import android.graphics.Bitmap
import com.dldev.docscanlite.data.local.dao.DocumentPageDao
import com.dldev.docscanlite.data.local.dao.SavedDocumentDao
import com.dldev.docscanlite.data.local.entity.DocumentPageEntity
import com.dldev.docscanlite.data.local.entity.SavedDocumentEntity
import com.dldev.docscanlite.domain.model.DocumentPage
import com.dldev.docscanlite.domain.model.SavedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepository(
    private val savedDocumentDao: SavedDocumentDao,
    private val documentPageDao: DocumentPageDao
) {
    
    fun getAllDocuments(): Flow<List<SavedDocument>> {
        return savedDocumentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getPagesByDocumentId(documentId: Long): Flow<List<DocumentPage>> {
        return documentPageDao.getPagesByDocumentId(documentId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun saveDocument(
        title: String,
        pages: List<Bitmap>
    ): Long {
        if (pages.isEmpty()) throw IllegalArgumentException("Document must have at least one page")
        
        val thumbnail = pages.first()
        val createdAt = System.currentTimeMillis()
        
        val documentEntity = SavedDocumentEntity(
            title = title,
            createdAt = createdAt,
            thumbnail = thumbnail,
            pageCount = pages.size
        )
        
        val documentId = savedDocumentDao.insertDocument(documentEntity)
        
        val pageEntities = pages.mapIndexed { index, bitmap ->
            DocumentPageEntity(
                documentId = documentId,
                pageNumber = index,
                originalImage = bitmap,
                annotatedImage = null
            )
        }
        
        documentPageDao.insertPages(pageEntities)
        
        return documentId
    }
    
    suspend fun updatePageAnnotation(pageId: Long, annotatedImage: Bitmap) {
        val page = documentPageDao.getPageById(pageId) ?: return
        val updated = page.copy(annotatedImage = annotatedImage)
        documentPageDao.updatePage(updated)
    }
    
    suspend fun deleteDocument(documentId: Long) {
        savedDocumentDao.deleteDocumentById(documentId)
    }
    
    suspend fun getDocumentById(documentId: Long): SavedDocument? {
        return savedDocumentDao.getDocumentById(documentId)?.toDomainModel()
    }
    
    private fun SavedDocumentEntity.toDomainModel() = SavedDocument(
        id = id,
        title = title,
        createdAt = createdAt,
        thumbnail = thumbnail,
        pageCount = pageCount
    )
    
    private fun DocumentPageEntity.toDomainModel() = DocumentPage(
        id = id,
        documentId = documentId,
        pageNumber = pageNumber,
        originalImage = originalImage,
        annotatedImage = annotatedImage
    )
}
