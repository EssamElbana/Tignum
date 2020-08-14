package com.example.tignum

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.net.SocketException
import java.net.SocketTimeoutException

class FileDownloaderPresenter(
    private val context: Context,
    private val view: FileDownloaderContract.View
) : FileDownloaderContract.Presenter {

    private var isPermissionsGranted = false
    private var isFileDownloaded = false
    private var fileName = "videoFile"
    private lateinit var job: Job

    override fun startDownload() {
        if (isFileDownloaded) {
            view.showErrorMessage("File is already downloaded.")
        } else {
            if (isPermissionsGranted) {
                startDownloadingFile()
            } else
                view.requestUserPermission()
        }
    }

    // you have two directories
    // one for In Downloading progress
    // completed files
    // first check if the file exists in completed then take action based on that

    private fun startDownloadingFile() {

        val url = "https://d2gjspw5enfim.cloudfront.net/qot_web/tignum_x_video.mp4"
        fileName = url.split("/").last()

        context.fileList().forEach { println(it.toUri()) }

        if (context.fileList().contains(fileName)) {
            val file = context.fileList()[context.fileList().indexOf(fileName)]
            println("file already exists ${file.toUri()}")
        } else {
            val destination =
                File(context.filesDir, "InDownloading$fileName")
            println("destination is ${destination.isFile}")
            try {
                job = CoroutineScope(IO).launch {
                    FileDownloader.downloadOrResume(
                        url,
                        destination,
                        onProgress = { progress, read, total ->
                            println(">>> Download $progress% ($read/$total b)")
                            println(">>> File dir ${context.filesDir}")
//                            coroutineScope(Main).launch {
//                        view.showCurrentStatus(">>> Download $progress% ($read/$total b)")
//                        view.setProgressbarValue(read.toInt())
//                            }
                        })
                }
            } catch (e: SocketTimeoutException) {
                println("Download socket TIMEOUT exception: $e")
            } catch (e: SocketException) {
                println("Download socket exception: $e")
            } catch (e: HttpException) {
                println("Download HTTP exception: $e")
            }
        }
    }

    override fun onPermissionsReady() {
        isPermissionsGranted = true
        startDownloadingFile()
    }

    override fun pauseDownload() {
//        val map = hashMapOf<ButtonEnum, Boolean>()
//        map.put(ButtonEnum.START, true)
//        map.put(ButtonEnum.DELETE, false)
//        map.put(ButtonEnum.PLAY, false)
//        map.put(ButtonEnum.PAUSE, false)
//        view.enableButtons(map)
        FileDownloader.cancelDownload()
//        view.showCurrentStatus("Paused")
    }

    override fun deleteFile() {
        if (context.fileList().contains(fileName)) {
            val file = File(context.filesDir, fileName)

            println(">>> File Deleted ${file.delete()}")


        }
//        if (deleted) {
//            val map = hashMapOf<ButtonEnum, Boolean>()
//            map.put(ButtonEnum.START, true)
//            map.put(ButtonEnum.DELETE, false)
//            map.put(ButtonEnum.PLAY, false)
//            map.put(ButtonEnum.PAUSE, false)
//            view.enableButtons(map)
//        }
    }


    override fun playFile() {
//        val map = hashMapOf<ButtonEnum, Boolean>()
//        map.put(ButtonEnum.START, false)
//        map.put(ButtonEnum.DELETE, true)
//        map.put(ButtonEnum.PLAY, true)
//        map.put(ButtonEnum.PAUSE, false)
//        view.enableButtons(map)
//        val dir: File = context.filesDir.get
//        val file = File(dir, "tignum_x_video.mp4")
//        if (file.exists())
//            view.playMediaFile(file.toUri())
//        else
//            view.showErrorMessage("Failed doesn't exist. Can't play it")
    }

    override fun onViewCreated() {
        // create two directories
        // initial buttons state
        val map = hashMapOf<ButtonEnum, Boolean>()
        map.put(ButtonEnum.START, true)
        map.put(ButtonEnum.DELETE, true)
        map.put(ButtonEnum.PLAY, true)
        map.put(ButtonEnum.PAUSE, true)
        view.enableButtons(map)
    }

}