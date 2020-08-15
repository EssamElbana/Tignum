package com.example.tignum

import com.example.tignum.model.FileItem

interface RepoInterface {

    suspend fun createFile(_fileName: String, url: String): FileItem?

    suspend fun findFileItemByName(fileName: String): FileItem?

    suspend fun updateFileItem(fileItem: FileItem): Int

    suspend fun deleteFile(fileItem: FileItem): Int

    suspend fun getAllFileItems(): List<FileItem>

    suspend fun download(
        fileItem: FileItem,
        onProgress: (suspend (name: String, percent: Int, downloaded: Long, total: Long) -> Unit)?
    ): String?

    suspend fun stopDownload(fileItem: FileItem)
}