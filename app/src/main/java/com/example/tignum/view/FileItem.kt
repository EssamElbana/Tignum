package com.example.tignum.view

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "file_table")
class FileItem(
    @PrimaryKey @ColumnInfo(name = "file_name") @NotNull val fileName: String,
    @ColumnInfo(name = "progress") var progress: Int,
    @ColumnInfo(name = "status") var status: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "url") val url: String
)