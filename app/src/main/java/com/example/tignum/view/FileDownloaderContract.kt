package com.example.tignum.view

import com.example.tignum.model.FileItem

interface FileDownloaderContract {

    interface View {
        fun showErrorMessage(status: String)
        fun requestUserPermission()
        fun playMediaFile(filePath: String)
        fun getInputUrl(): String
        fun showFileList(fileList: ArrayList<FileItem>)
        fun setItemChanged(position: Int, payload: Int? = null)
        fun confirmUserDeletion(position: Int, fileName: String)
    }

    interface Presenter {
        fun onViewCreated()
        fun enqueueBtnClicked()
        fun startBtnClicked(position: Int)
        fun pauseBtnClicked(position: Int)
        fun deleteBtnClicked(position: Int)
        fun playBtnClicked(position: Int)
        fun onPermissionsReady()
        fun onDeletionConfirmed(position: Int, fileName: String)
        fun onDestroy()
    }
}