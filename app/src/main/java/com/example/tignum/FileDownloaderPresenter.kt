package com.example.tignum

import android.content.Context
import android.util.Patterns
import com.example.tignum.caching.FileRoomDatabase
import com.example.tignum.view.FileItem
import com.example.tignum.view.FileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class FileDownloaderPresenter(
    private val context: Context,
    private val view: FileDownloaderContract.View
) : FileDownloaderContract.Presenter {

    private var isPermissionsGranted = false
    private var fileName = ""
    private lateinit var job: Job
    private val downloadsDirectory: File by lazy { File(context.filesDir, "Tignum-Downloads") }
    private val fileItemsDao by lazy { FileRoomDatabase.getDatabase(context).fileDao() }
    private val fileItemList: ArrayList<FileItem> = ArrayList()

    init {
        downloadsDirectory.mkdir()
    }

    override fun enqueueBtnClicked() {
        val url = view.getInputUrl()
        if (url.isEmpty())
            view.showErrorMessage("URL is Empty")
        else if (!Patterns.WEB_URL.matcher(url).matches())
            view.showErrorMessage("Invalid URL")
        else if (fileItemList.any { it.url == url }) {
            view.showErrorMessage("File is in list below already")
        } else {
            fileName = url.split("/").last()
            if (downloadsDirectory.list()!!.any {it == fileName}) // file name might match in different urls.
                fileName = "${Random.nextInt(500)}$fileName"
            val destination = File(downloadsDirectory, fileName)

            val fileItem = FileItem(
                fileName,
                0,
                FileStatus.PAUSED.name,
                destination.path,
                url
            )
            CoroutineScope(IO).launch {
                fileItemsDao.insertAll(fileItem) // add to database
            }
            fileItemList.add(fileItem) // add to list
            view.showFileList(fileItemList)
        }
    }

    private fun checkIfFileInList(url: String): Boolean {
        fileItemList.forEach {
            if (it.url == url) return true
        }
        return false
    }

    override fun start(position: Int) {
        if (fileItemList[position].status == FileStatus.IN_DOWNLOAD.name)
            view.showErrorMessage("File is already being downloaded!")
        else if (fileItemList.any { it.status == FileStatus.IN_DOWNLOAD.name }) {
            view.showErrorMessage("Sorry but only one download request can be handled at a time.")
        } else {
            if (isPermissionsGranted) {
                startDownloadingFile(position)
            } else
                view.showErrorMessage("Application doesn't have necessary permissions")
        }
    }

    private fun startDownloadingFile(position: Int) {
        if (fileItemList[position].status == FileStatus.PAUSED.name) {
            val url = fileItemList[position].url
            val destination = File(fileItemList[position].path)
            CoroutineScope(IO).launch {
                val fileItem = fileItemsDao.findByName(destination.name)
                fileItem.status = FileStatus.IN_DOWNLOAD.name
                fileItemsDao.update(fileItem)
            }
            fileItemList[position].status = FileStatus.IN_DOWNLOAD.name
            CoroutineScope(Main).launch {
                view.setItemChanged(position)
                view.setCurrentProgress(0,0,0)
            }
            println("before start downloading destination is file ? ${destination.isFile}  + name is ${destination.name}")
            try {

                job = CoroutineScope(IO).launch {
                    FileDownloader.downloadOrResume(
                        url,
                        destination,
                        onProgress = { progress, read, total ->
                            CoroutineScope(Main).launch {
                                if (progress >= 0) {
                                    val fileItem = fileItemList[position]
                                    fileItem.progress = progress
                                    view.setCurrentProgress(progress, read, total)
                                }
                            }


                        },
                        onCancel = { percent, fileName ->
                            val fileItem = fileItemsDao.findByName(fileName)
                            fileItem.status = FileStatus.PAUSED.name
                            fileItem.progress = percent
                            fileItemsDao.update(fileItem)
                            val index = fileItemList.indexOfFirst { it.fileName == fileName }
                            fileItemList[index].status = FileStatus.PAUSED.name
                            fileItemList[index].progress = percent
                            CoroutineScope(Main).launch {
                                view.showFileList(fileItemList)
                            }
                        },
                        onError = { message ->
                            CoroutineScope(Main).launch {
                                view.showErrorMessage(message)
                            }

                        }
                        ,
                        onComplete = { fileName ->
                            val fileItem = fileItemsDao.findByName(fileName)
                            fileItem.status = FileStatus.COMPLETE.name
                            fileItem.progress = 100
                            fileItemsDao.update(fileItem)
                            val index = fileItemList.indexOfFirst { it.fileName == fileName }
                            fileItemList[index].status = FileStatus.COMPLETE.name
                            fileItemList[index].progress = 100
                            CoroutineScope(Main).launch {
                                view.showFileList(fileItemList)
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                println("Download exception: $e")
                CoroutineScope(Main).launch {
                    view.showErrorMessage("$e")
                    fileItemList[position].status = FileStatus.PAUSED.name
                }
                CoroutineScope(IO).launch {
                    val fileItem = fileItemsDao.findByName(destination.name)
                    fileItem.status = FileStatus.PAUSED.name
                    fileItemsDao.update(fileItem)
                }
            }
        } else {
            CoroutineScope(Main).launch {
                view.showErrorMessage("operation can't be done. Status of file is ${fileItemList[position].status}")
            }
        }
    }

    override fun onPermissionsReady() {
        isPermissionsGranted = true
    }

    override fun pause(position: Int) {
        if (fileItemList[position].status == FileStatus.IN_DOWNLOAD.name) {
            CoroutineScope(IO).launch {
                FileDownloader.cancelDownload()
            }
        } else if (fileItemList[position].status == FileStatus.COMPLETE.name)
            view.showErrorMessage("File downloaded")
        else
            view.showErrorMessage("File is already on pause")
    }

    override fun delete(position: Int) {
        val fileItem = fileItemList[position]
        // todo ask the user with alert dialog to confirm deleting
        // todo in the first two cases
        if (fileItem.status == FileStatus.IN_DOWNLOAD.name)
            view.showErrorMessage("File is being downloaded")
        else if (fileItem.status == FileStatus.PAUSED.name)
            view.showErrorMessage("File is paused to be downloaded later.")
        else {
            val file = File(fileItem.path)
            file.delete()
            println(">>> File Exists ${file.exists()}")
            if (!file.exists()) {
                fileItemList.remove(fileItem)
                CoroutineScope(IO).launch {
                    fileItemsDao.delete(fileItem)
                }
                CoroutineScope(Main).launch {
                    view.showFileList(fileItemList)
                }
            }
        }
    }

    override fun play(position: Int) {}

    override fun onViewCreated() {
        CoroutineScope(IO).launch {
            fileItemList.addAll(fileItemsDao.getAll())
        }
        view.showFileList(fileItemList)
        view.requestUserPermission()
    }

}