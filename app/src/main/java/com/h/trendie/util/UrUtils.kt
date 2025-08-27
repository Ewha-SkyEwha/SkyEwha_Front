package com.h.trendie.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.InputStream

/** SAF 쿼리로 파일명 가져오기 (fallback 포함) */
fun ContentResolver.getFileName(uri: Uri): String {
    var name: String? = null
    val cursor: Cursor? = query(uri, null, null, null, null)
    cursor?.use {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) name = it.getString(idx)
    }
    return (name ?: "upload.mp4").ifBlank { "upload.mp4" }
}

/** SAF 쿼리로 파일 크기 (모르면 -1) */
fun ContentResolver.getFileSize(uri: Uri): Long {
    var size = -1L
    val cursor: Cursor? = query(uri, null, null, null, null)
    cursor?.use {
        val idx = it.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0 && it.moveToFirst()) size = it.getLong(idx)
    }
    return size
}

/** content:// Uri 를 스트리밍 Multipart 로 */
class InputStreamRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val contentType: String
) : RequestBody() {
    override fun contentType() = contentType.toMediaTypeOrNull()
    override fun contentLength(): Long = contentResolver.getFileSize(uri)
    override fun writeTo(sink: BufferedSink) {
        contentResolver.openInputStream(uri).use { input: InputStream? ->
            requireNotNull(input) { "Cannot open input stream from URI: $uri" }
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                sink.write(buf, 0, read)
            }
        }
    }
}

fun buildVideoPart(cr: ContentResolver, videoUri: Uri): MultipartBody.Part {
    val body = InputStreamRequestBody(cr, videoUri, "video/mp4")
    val fileName = cr.getFileName(videoUri)
    return MultipartBody.Part.createFormData("file", fileName, body)
}

fun textPart(value: String): RequestBody =
    value.toRequestBody("text/plain".toMediaType())