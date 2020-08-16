package com.example.tignum.repo

import android.content.Context
import com.example.tignum.caching.FileRoomDatabase
import com.example.tignum.model.FileItem
import com.example.tignum.model.FileStatus
import com.example.tignum.network_client.FileDownloaderAPI
import com.example.tignum.network_client.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSource
import okio.Okio
import retrofit2.awaitResponse
import java.io.File
import java.util.regex.Pattern
import kotlin.random.Random

class Repository(context: Context) : RepoInterface {

    private val fileItemsDao by lazy { FileRoomDatabase.getDatabase(context).fileDao() }
    private val downloadsDirectory: File by lazy { File(context.filesDir, "Tignum-Downloads") }
    private val api by lazy {
        RetrofitClient.buildRetrofitObject().create(
            FileDownloaderAPI::class.java
        )
    }
    private val requestsMap = HashMap<String, ResponseBody?>()

    init {
        downloadsDirectory.mkdir()
    }

    override suspend fun createFile(_fileName: String, url: String): FileItem? = withContext(IO) {
        // file name might match in different urls.
        val fileName =
            if (downloadsDirectory.list()!!.any { it == _fileName })
                "${Random.nextInt(500)}$_fileName"
            else
                _fileName
        val file = File(downloadsDirectory, fileName)

        val fileItem = FileItem(
            fileName,
            0,
            FileStatus.PAUSED.name,
            file.path,
            url
        )
        val result = fileItemsDao.insertAll(fileItem) // add to database
        if (result.isNotEmpty())
            return@withContext fileItem
        else
            return@withContext null
    }

    override suspend fun findFileItemByName(fileName: String): FileItem? = withContext(IO) {
        return@withContext fileItemsDao.findByName(fileName)
    }

    override suspend fun updateFileItem(fileItem: FileItem): Int = withContext(IO) {
        return@withContext fileItemsDao.update(fileItem)
    }

    override suspend fun deleteFile(fileItem: FileItem) = withContext(IO) {
        val file = File(fileItem.path)
        file.delete()
        fileItemsDao.delete(fileItem)
    }

    override suspend fun getAllFileItems(): List<FileItem> = withContext(IO) {
        val fileItems = fileItemsDao.getAll()
        val filesInDirectory = downloadsDirectory.list()
        if (downloadsDirectory.list() == null || downloadsDirectory.list()!!.isEmpty())
            fileItems.forEach { deleteFile(it) }
        else if (fileItems.size != filesInDirectory!!.size)
            fileItems.forEach {
                if (!filesInDirectory.contains(it.fileName))
                    deleteFile(it)
            }
        return@withContext fileItemsDao.getAll()
    }


    override suspend fun download(
        fileItem: FileItem,
        onProgress: (suspend (name: String, percent: Int, downloaded: Long, total: Long) -> Unit)?
    ): String? = withContext(IO) {
        var startingFrom = 0L
        val headers: HashMap<String, String> = HashMap()
        val file = File(fileItem.path)
        if (file.exists() && file.length() > 0L) {
            // send that header to the server.
            startingFrom = file.length()
            headers["Range"] = "bytes=${startingFrom}-"
            headers["Content-Encoding"] = "identity"
        }
        try {
            val response =
                api.downloadFile(fileItem.url, headers).awaitResponse() // fetch file from server.
            println("Download starting from $startingFrom - headers: $headers")
            println("-- downloadFileByUrl: parsing response! $response")
            var startingByte = 0L
            var endingByte = 0L
            var totalBytes = 0L
            if (!response.isSuccessful || response.body() == null)
                return@withContext "code ${response.code()} from server"
            else {
                val contentLength = response.body()!!.contentLength()
                val responseCode = response.code()
                if (responseCode == 206) {
                    // 206 means server allows for download continuation.
                    println("- http 206: Continue download")
                    val matcher = Pattern.compile("bytes ([0-9]*)-([0-9]*)/([0-9]*)")
                        .matcher(response.headers().get("Content-Range"))
                    if (matcher.find()) {
                        startingByte = matcher.group(1).toLong()
                        endingByte = matcher.group(2).toLong()
                        totalBytes = matcher.group(3).toLong()
                    }
                    println("Getting range from $startingByte to $endingByte of ${totalBytes} bytes")
                } else {
                    println("- new download")
                    if (contentLength <= 0) { // server didn't provide content-length
                        totalBytes = -1
                        endingByte = -1
                    } else {
                        endingByte = contentLength
                        totalBytes = contentLength
                    }
                    if (file.exists()) {
                        println("Delete previous download! it will be rewritten from the start")
                        file.delete()
                    }
                }

                println("Getting range from $startingByte to $endingByte of $totalBytes bytes")
                val sink: BufferedSink
                if (startingByte > 0) {
                    sink = Okio.buffer(Okio.appendingSink(file))
                } else {
                    sink = Okio.buffer(Okio.sink(file))
                }
                if (requestsMap.containsKey(file.name))
                    requestsMap.remove(file.name)
                requestsMap[file.name] = response.body()
                saveFile(
                    startingByte, file.name, totalBytes, sink, response.body(),
                    onProgress
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext ex.message.toString()
        }
    }

    private suspend fun saveFile(
        _totalRead: Long,
        fileName: String,
        totalBytes: Long,
        sink: BufferedSink,
        response: ResponseBody?,
        onProgress: (suspend (name: String, percent: Int, downloaded: Long, total: Long) -> Unit)? = null
    ): String = withContext(IO) {
        var totalRead = _totalRead
        var lastPercentage = -1
        try {
            sink.use {
                it.writeAll(object : ForwardingSource(response!!.source()) {
                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val bytesRead = super.read(sink, byteCount)
                        totalRead += bytesRead
                        val currentPercentage = (totalRead * 100 / totalBytes).toInt()
                        if (currentPercentage > lastPercentage) {
                            lastPercentage = currentPercentage
                            if (onProgress != null) {
                                CoroutineScope(Main).launch {
                                    onProgress(fileName, currentPercentage, totalRead, totalBytes)
                                }
                            }
                        }
                        return bytesRead
                    }
                })
            }
            return@withContext "completed"
        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext ex.message.toString()
        }

    }

    override suspend fun stopDownload(fileItem: FileItem) = withContext(IO) {
        val buff = requestsMap[fileItem.fileName]
        try {
            requestsMap.remove(fileItem.fileName)
            buff!!.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}