package com.example.tignum.caching

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tignum.model.FileItem

@Database(entities = arrayOf(FileItem::class), version = 1, exportSchema = false)
abstract class FileRoomDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: FileRoomDatabase? = null

        fun getDatabase(context: Context): FileRoomDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FileRoomDatabase::class.java,
                    "file_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}