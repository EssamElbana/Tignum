package com.example.tignum

import android.net.Uri
import com.example.tignum.view.FileItem

interface FileDownloaderContract {

    interface View {
       fun showErrorMessage(status: String)
        fun requestUserPermission()
        fun playMediaFile(fileUri: Uri)
        fun getInputUrl(): String
        fun showFileList(fileList: ArrayList<FileItem>)
        fun setCurrentProgress(value: Int, readSoFar:Long, totalLength:Long)
        fun setItemChanged(position: Int)
    }

    interface Presenter {
        fun onViewCreated()
        fun enqueueBtnClicked()
        fun start(position: Int)
        fun pause(position: Int)
        fun delete(position: Int)
        fun play(position: Int)
        fun onPermissionsReady()
    }
}