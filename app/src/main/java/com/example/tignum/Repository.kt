package com.example.tignum

interface Repository {

    fun downloadOrResume()
    fun cancelDownload()
    fun deleteFile()

}