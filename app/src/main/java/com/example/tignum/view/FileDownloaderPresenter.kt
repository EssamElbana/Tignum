package com.example.tignum.view

import android.util.Patterns
import com.example.tignum.RepoInterface
import com.example.tignum.model.FileItem
import com.example.tignum.model.FileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

private const val PAYLOAD_TAG_PROGRESS: Int = 50
private const val PAYLOAD_TAG_STATUS: Int = 100

class FileDownloaderPresenter(
    private var view: FileDownloaderContract.View?,
    private val repository: RepoInterface
) : FileDownloaderContract.Presenter {

    private var isPermissionsGranted = false
    private val fileItemList: ArrayList<FileItem> = ArrayList()

    override fun enqueueBtnClicked() {
        val url = view!!.getInputUrl()
        if (url.isEmpty())
            view!!.showErrorMessage("URL is Empty")
        else if (!Patterns.WEB_URL.matcher(url).matches())
            view!!.showErrorMessage("Invalid URL")
        else if (fileItemList.any { it.url == url }) {
            view!!.showErrorMessage("File is in list below already")
        } else {
            val fileName = url.split("/").last()
            CoroutineScope(Main).launch {
                val fileItem = repository.createFile(fileName, url)
                if (fileItem == null) {
                    view!!.showErrorMessage("Failed To Create File")
                } else {
                    fileItemList.add(fileItem)
                    view!!.showFileList(fileItemList)
                }
            }
        }
    }


    override fun startBtnClicked(position: Int) {
        if (isPermissionsGranted) {
            when (fileItemList[position].status) {
                FileStatus.IN_DOWNLOAD.name -> view!!.showErrorMessage("File is already being downloaded!")
                FileStatus.COMPLETE.name -> view!!.showErrorMessage("File is already Completed")
                FileStatus.PAUSED.name -> {
                    changeFileStatusTo(position, FileStatus.IN_DOWNLOAD)
                    startDownloadingFile(position)
                }
            }
        } else
            view!!.showErrorMessage("Application doesn't have necessary permissions")
    }

    private fun changeFileStatusTo(position: Int, status: FileStatus) {
        CoroutineScope(Main).launch {
            val fileItem = repository.findFileItemByName(fileItemList[position].fileName)
            if (fileItem != null) {
                fileItem.status = status.name
                repository.updateFileItem(fileItem)
                fileItemList[position].status = status.name
                view!!.setItemChanged(position, PAYLOAD_TAG_STATUS)
            } else {
                fileItemList.removeAt(position)
                view!!.showFileList(fileItemList)
            }
        }
    }

    private fun startDownloadingFile(position: Int) {
        CoroutineScope(Main).launch {
            val fileItem = fileItemList[position]
            val position = position
            val result = repository.download(fileItem, onProgress =
            { name, progress, downloadedSoFar, totalFileLength ->
                println("file: $name, progress: $progress, bytes:$downloadedSoFar, total:$totalFileLength ")
                fileItemList[position].progress = progress
                fileItem.progress = progress
                view!!.setItemChanged(position, PAYLOAD_TAG_PROGRESS)
            })
            when (result) {
                "completed" -> {
                    fileItemList[position].status = FileStatus.COMPLETE.name
                    view!!.setItemChanged(position, PAYLOAD_TAG_STATUS)
                    repository.updateFileItem(fileItem)
                }
                else -> {
                    val item = fileItemList[position]
                    item.status = FileStatus.PAUSED.name
                    repository.updateFileItem(item)
                    view!!.setItemChanged(position, PAYLOAD_TAG_STATUS)
                    if(!result!!.contains("stream closed"))
                        view!!.showErrorMessage("Failed reason: $result file name: ${item.fileName}")
                }
            }
        }
    }


    override fun onPermissionsReady() {
        isPermissionsGranted = true
    }

    override fun onDeletionConfirmed(position: Int, fileName: String) {
        CoroutineScope(Main).launch {
            val fileItem = repository.findFileItemByName(fileName)
            if (fileItem != null)
                repository.deleteFile(fileItem)
            fileItemList.removeAt(position)
            view!!.showFileList(fileItemList)
        }
    }

    override fun onViewDestroyed() {
        view = null
    }

    override fun pauseBtnClicked(position: Int) {
        if (fileItemList[position].status == FileStatus.IN_DOWNLOAD.name) {
            CoroutineScope(Main).launch {
                repository.stopDownload(fileItemList[position])
//                val fileItem = fileItemList[position]
//                fileItem.status = FileStatus.PAUSED.name
//                repository.updateFileItem(fileItem)
//                view!!.setItemChanged(position, PAYLOAD_TAG_STATUS)
            }
        } else if (fileItemList[position].status == FileStatus.COMPLETE.name)
            view!!.showErrorMessage("File downloaded")
        else
            view!!.showErrorMessage("File is already on pause")
    }

    override fun deleteBtnClicked(position: Int) {
        if (fileItemList[position].status == FileStatus.IN_DOWNLOAD.name)
            view!!.showErrorMessage("${fileItemList[position].fileName} is being downloaded. Kindly stop the download first.")
        else
            view!!.confirmUserDeletion(position, fileItemList[position].fileName)
    }

    override fun playBtnClicked(position: Int) {
        if (fileItemList[position].status == FileStatus.COMPLETE.name)
            view!!.playMediaFile(fileItemList[position].path)
        else
            view!!.showErrorMessage("File is not ready to be played!")
    }

    override fun onViewCreated() {
        CoroutineScope(Main).launch {
            val fileItems = repository.getAllFileItems()
            fileItemList.addAll(fileItems)
            fileItemList.removeAll(fileItems)
            fileItems.forEach {
                repository.deleteFile(it)
            }
            view!!.showFileList(fileItemList)
            view!!.requestUserPermission()
        }
    }
}