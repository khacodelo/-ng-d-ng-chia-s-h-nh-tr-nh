package com.example.apptheodihnhtrnh.data.api

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val callback: (progress: Int) -> Unit
) : RequestBody() {

    override fun contentType() = contentType

    override fun contentLength() = file.length()

    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(2048)
        val inputStream = FileInputStream(file)
        var uploaded: Long = 0

        try {
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                val progress = ((uploaded * 100) / fileLength).toInt()
                callback(progress)
            }
        } finally {
            inputStream.close()
        }
    }
}
