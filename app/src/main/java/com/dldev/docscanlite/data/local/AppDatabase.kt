package com.dldev.docscanlite.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dldev.docscanlite.data.local.converter.BitmapConverter
import com.dldev.docscanlite.data.local.dao.DocumentPageDao
import com.dldev.docscanlite.data.local.dao.SavedDocumentDao
import com.dldev.docscanlite.data.local.entity.DocumentPageEntity
import com.dldev.docscanlite.data.local.entity.SavedDocumentEntity

@Database(
    entities = [
        SavedDocumentEntity::class,
        DocumentPageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BitmapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun savedDocumentDao(): SavedDocumentDao
    abstract fun documentPageDao(): DocumentPageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docscanlite_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
