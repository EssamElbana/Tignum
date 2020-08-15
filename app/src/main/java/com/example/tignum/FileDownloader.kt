package com.example.tignum

import com.example.tignum.network_client.FileDownloaderAPI
import com.example.tignum.network_client.RetrofitClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSource
import okio.Okio
import retrofit2.Response
import retrofit2.awaitResponse
import java.io.File
import java.util.regex.Pattern


object FileDownloader {

    private val Service by lazy {
        RetrofitClient.buildRetrofitObject().create(
            FileDownloaderAPI::class.java
        )
    }
    private lateinit var sink: BufferedSink
    private lateinit var response: Response<ResponseBody>
    private var currentPercentage = 1
    @Volatile
    private var isCancelled = false

    suspend fun downloadOrResume(
        url: String,
        destination: File,
        onProgress: ((percent: Int, downloaded: Long, total: Long) -> Unit)? = null,
        onCancel: ((percent: Int, fileName: String) -> Unit)? = null,
        onError: ((message: String) -> Unit)? = null,
        onComplete: ((fileName: String) -> Unit)? = null
    ) {
        isCancelled = false
        var startingFrom = 0L
        val headers: HashMap<String, String> = HashMap()
        if (destination.exists() && destination.length() > 0L) {
            startingFrom = destination.length()
            headers.put("Range", "bytes=${startingFrom}-")
        }
        println("---------- downloadFileByUrl: getting response -------------")
        try {
            response = Service.downloadFile(url, headers).awaitResponse() // fetch file from server.

            println("Download starting from $startingFrom - headers: $headers")
            println("-- downloadFileByUrl: parsing response! $response")
            var startingByte = 0L
            var endingByte = 0L
            var totalBytes = 0L

            if (!response.isSuccessful || response.body() == null)
                println(response.message())
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
                        onError!!("Failed to obtain content-length from server. however downloading will continue response code is $responseCode")
                    } else {
                        endingByte = contentLength
                        totalBytes = contentLength
                    }
                    if (destination.exists()) {
                        println("Delete previous download!")
                        destination.delete()
                    }
                }

                println("Getting range from $startingByte to $endingByte of $totalBytes bytes")

                if (startingByte > 0) {
                    sink = Okio.buffer(Okio.appendingSink(destination))
                } else {
                    sink = Okio.buffer(Okio.sink(destination))
                }
                var totalRead = startingByte
                sink.use {
                    it.writeAll(object : ForwardingSource(response.body()!!.source()) {
                        override fun read(sink: Buffer, byteCount: Long): Long {
                            val bytesRead = super.read(sink, byteCount)
                            totalRead += bytesRead

                            currentPercentage = (totalRead * 100 / totalBytes).toInt()
                            onProgress!!(currentPercentage, totalRead, totalBytes)

                            if (isCancelled) {
                                onCancel!!(currentPercentage, destination.name)
                                return -1
                            }
                            return bytesRead
                        }
                    })
                }
                sink.close()
                response.body()!!.source().close()
                if (!isCancelled) {
                    println("--- Download complete!")
                    println("destination exists: ${destination.exists()} , path: ${destination.name}")
                    if (onComplete != null)
                        onComplete(destination.name)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            println(ex.message)
            onCancel!!(currentPercentage,destination.name)
        }
    }

    fun cancelDownload() {
        isCancelled = true
    }
}