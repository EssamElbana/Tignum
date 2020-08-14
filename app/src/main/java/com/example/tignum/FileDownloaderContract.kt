package com.example.tignum

import android.net.Uri

interface FileDownloaderContract {

    interface View {
        fun showCurrentStatus(status: String)
        fun showErrorMessage(status: String)
        fun requestUserPermission()
        fun playMediaFile(fileUri: Uri)
        fun enableButtons(map: HashMap<ButtonEnum,Boolean>)
        fun setProgressbarValue(value:Int)
    }

    interface Presenter {
        fun onViewCreated()
        fun startDownload()
        fun pauseDownload()
        fun deleteFile()
        fun onPermissionsReady()
        fun playFile()
    }
}