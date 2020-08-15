package com.example.tignum.caching

import androidx.room.*
import com.example.tignum.view.FileItem

@Dao
interface FileDao {
    @Query("SELECT * FROM file_table")
    fun getAll(): List<FileItem>

    @Query("SELECT * FROM file_table WHERE file_name = :fileName LIMIT 1")
    fun findByName(fileName: String): FileItem

    @Query("SELECT * FROM file_table WHERE url = :url LIMIT 1")
    fun findByURL(url: String): FileItem

    @Insert
    fun insertAll(vararg fileItem: FileItem)

    @Delete
    fun delete(fileItem: FileItem)

    @Update
    fun update(vararg fileItem: FileItem)
}