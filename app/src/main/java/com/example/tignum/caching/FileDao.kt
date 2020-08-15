package com.example.tignum.caching

import androidx.room.*
import com.example.tignum.model.FileItem

@Dao
interface FileDao {
    @Query("SELECT * FROM file_table")
    fun getAll(): List<FileItem>

    @Query("SELECT * FROM file_table WHERE file_name = :fileName LIMIT 1")
    fun findByName(fileName: String): FileItem

    @Query("SELECT * FROM file_table WHERE url = :url LIMIT 1")
    fun findByURL(url: String): FileItem

    @Insert
    fun insertAll(vararg fileItem: FileItem) :List<Long>

    @Delete
    fun delete(fileItem: FileItem) :Int

    @Update
    fun update(vararg fileItem: FileItem) :Int
}